package com.servoy.eclipse.cypress.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.servoy.eclipse.cypress.services.CypressToolchain.NodeTools;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.startup.FormPreviewNGClient;
import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Runs Cypress test specs (.spec.cy.js) against Servoy forms using the Cypress CLI.
 * Node, npm and pnpm are resolved like the NG client build does (servoy.nodePath / servoy.npmPath / servoy.pnpmPath / servoy.jsRuntime).
 * The Cypress binary cache and the fallback Cypress install live in .metadata/.plugins/com.servoy.eclipse.cypress/.
 * Spec files live next to the .frm file in forms/ directory.
 */
@Creatable
@SuppressWarnings("restriction")
public class FormSpecRunner
{
	private static final int DEFAULT_TIMEOUT_SECONDS = 60;
	private static final String DEFAULT_CYPRESS_VERSION = "^15.13.1";
	private static final int PACKAGE_INSTALL_TIMEOUT_SECONDS = 300;
	private static final int BINARY_VERIFY_TIMEOUT_SECONDS = 120;
	private static final int BINARY_INSTALL_TIMEOUT_SECONDS = 600;
	private static final int NODE_EXTRACTION_TIMEOUT_SECONDS = 120;
	private static final Pattern VERSION_PATTERN = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");

	/** (projectDir, cacheFolder, cypressVersion) tuples whose Cypress binary was already verified in this Developer session. */
	private static final Set<String> VERIFIED_BINARIES = new HashSet<>();

	/**
	 * Serialises every step that installs packages or the Cypress binary, so that two parallel runs never install into the same folder at once.
	 * A second caller waits here and then finds the work already done.
	 */
	private static final Object INSTALL_LOCK = new Object();

	private record ProcessResult(int exitCode, boolean timedOut, String output)
	{
	}

	private record CypressInvocation(List<String> commandPrefix, Map<String, String> env, String error)
	{
		static CypressInvocation failed(String error)
		{
			return new CypressInvocation(null, null, error);
		}
	}

	private final FormSpecGenerator specGenerator = new FormSpecGenerator();
	private volatile Process activeProcess;

	/** Guards one-time cleanup of leftover preserved artifacts from previous Developer sessions. */
	private static volatile boolean preservedArtifactsCleaned = false;

	/**
	 * Cancels the currently running Cypress process, if any.
	 */
	public void cancel()
	{
		Process p = activeProcess;
		if (p != null && p.isAlive())
		{
			p.destroyForcibly();
		}
	}

	private void shutdownFormPreviewClients()
	{
		try
		{
			FormPreviewNGClient.shutdownExisting();
		}
		catch (Exception e)
		{
			ServoyLog.logWarning("shutdownFormPreviewClients: " + e.getMessage(), e);
		}
	}

	/**
	 * Copies Cypress-generated videos and screenshots referenced in the output to a stable
	 * per-run folder (cypress/preserved-artifacts/&lt;testName&gt;-&lt;timestamp&gt;/) so they
	 * survive the next run wiping the results/videos/screenshots folders. Rewrites the paths
	 * in the returned output to point at the preserved copies.
	 */
	/** Marker prefixes appended to the output so the parser can locate preserved media reliably. */
	public static final String PRESERVED_VIDEO_MARKER = "[Preserved Video] ";
	public static final String PRESERVED_SCREENSHOT_MARKER = "[Preserved Screenshot] ";

	private String preserveArtifacts(String rawOutput, String testName)
	{
		try
		{
			Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
			Path cypressDir = workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress");
			Path preservedRoot = cypressDir.resolve("preserved-artifacts");
			// One-time cleanup of leftover artifacts from previous Developer sessions
			cleanPreservedArtifactsOnce(preservedRoot);
			String safeName = testName.replaceAll("[^a-zA-Z0-9._-]", "_");
			// Keep the folder name short; use a truncated safe name to avoid Windows MAX_PATH issues.
			String shortName = safeName.length() > 40 ? safeName.substring(0, 40) : safeName;
			Path destDir = preservedRoot.resolve(shortName + "-" + System.currentTimeMillis());

			StringBuilder markers = new StringBuilder();

			// Cypress stores media under cypress/videos and cypress/screenshots. Scan those
			// folders on disk (rather than parsing wrapped console text) for files whose spec
			// name matches this test, so we reliably pick up the current run's artifacts.
			String baseName = testName.replaceAll("\\.cy\\.(js|ts)$", "");

			// Video: cypress/videos/<spec>.mp4 (spec name usually "<base>.cy.ts" or "<base>.cy.js")
			Path videosDir = cypressDir.resolve("videos");
			Path video = findMatchingFile(videosDir, baseName, ".mp4");
			if (video != null)
			{
				Files.createDirectories(destDir);
				// Use a short, fixed filename so the full path stays under Windows MAX_PATH (260)
				// and can be opened by browsers via file:// URLs.
				Path dest = destDir.resolve("video.mp4");
				Files.copy(video, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				markers.append("\n").append(PRESERVED_VIDEO_MARKER).append(dest.toString());
			}

			// Screenshots: cypress/screenshots/<spec>/*.png (one subfolder per spec file)
			Path screenshotsDir = cypressDir.resolve("screenshots");
			Path shot = findScreenshot(screenshotsDir, baseName);
			if (shot != null)
			{
				Files.createDirectories(destDir);
				// Short, fixed filename (see video comment above).
				Path dest = destDir.resolve("screenshot.png");
				Files.copy(shot, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				markers.append("\n").append(PRESERVED_SCREENSHOT_MARKER).append(dest.toString());
			}

			return markers.length() > 0 ? rawOutput + markers : rawOutput;
		}
		catch (Exception e)
		{
			// Non-fatal - return the original output if preservation fails
			return rawOutput;
		}
	}

	/**
	 * Finds the media file Cypress wrote for the given spec. Cypress names the file after the
	 * full spec filename (e.g. an E2E "login" spec produces "login.cy.ts.mp4"; a form spec
	 * produces "mysolution.login.spec.cy.js.mp4" or the legacy "login.spec.cy.js.mp4"), and
	 * mirrors the spec's subfolder structure under {@code dir}. The {@code baseName} here has
	 * the .cy.js/.cy.ts suffix already stripped and may carry a subfolder prefix (e.g.
	 * "appB/login") from run-all discovery, so we match on the last path segment against both
	 * the E2E and form spec filename variants. Returns null if the directory is missing or
	 * nothing matches.
	 */
	private Path findMatchingFile(Path dir, String baseName, String extension)
	{
		if (!Files.isDirectory(dir))
		{
			return null;
		}
		String leaf = baseName.replace('\\', '/');
		int slash = leaf.lastIndexOf('/');
		if (slash >= 0)
		{
			leaf = leaf.substring(slash + 1);
		}
		String tsName = leaf + ".cy.ts" + extension;
		String jsName = leaf + ".cy.js" + extension;
		String formSpecSuffix = "." + leaf + ".spec.cy.js" + extension;
		String formSpecName = leaf + ".spec.cy.js" + extension;
		try (java.util.stream.Stream<Path> walk = Files.walk(dir))
		{
			return walk.filter(Files::isRegularFile).filter(p -> {
				String name = p.getFileName().toString();
				return name.equals(tsName) || name.equals(jsName) || name.equals(formSpecName) || name.endsWith(formSpecSuffix);
			}).findFirst().orElse(null);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Finds a screenshot .png for the given spec. Cypress nests screenshots under a per-spec
	 * folder named after the full spec filename (e.g. "login.cy.ts" for E2E, or
	 * "mysolution.login.spec.cy.js"/"login.spec.cy.js" for form specs), so we search recursively
	 * for the first .png under a matching folder. Prefers a "(failed)" screenshot.
	 */
	private Path findScreenshot(Path screenshotsDir, String baseName)
	{
		if (!Files.isDirectory(screenshotsDir))
		{
			return null;
		}
		String leaf = baseName.replace('\\', '/');
		int slash = leaf.lastIndexOf('/');
		if (slash >= 0)
		{
			leaf = leaf.substring(slash + 1);
		}
		String tsFolder = leaf + ".cy.ts";
		String jsFolder = leaf + ".cy.js";
		String formSpecSuffix = "." + leaf + ".spec.cy.js";
		String formSpecFolder = leaf + ".spec.cy.js";
		try (java.util.stream.Stream<Path> walk = Files.walk(screenshotsDir))
		{
			java.util.List<Path> pngs = walk.filter(Files::isRegularFile)
				.filter(p -> p.getFileName().toString().endsWith(".png"))
				.filter(p -> {
					for (Path segment : p)
					{
						String seg = segment.toString();
						if (seg.equals(tsFolder) || seg.equals(jsFolder) || seg.equals(formSpecFolder) || seg.endsWith(formSpecSuffix))
						{
							return true;
						}
					}
					return false;
				}).toList();
			// Prefer a screenshot marked "(failed)" if present
			return pngs.stream().filter(p -> p.getFileName().toString().contains("(failed)")).findFirst()
				.orElse(pngs.stream().findFirst().orElse(null));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Deletes preserved-artifacts left over from a previous Developer session. Runs once per
	 * JVM lifetime so artifacts created during the current session are kept (they should live
	 * until Developer is closed), while stale ones from prior sessions are cleaned up.
	 */
	private static synchronized void cleanPreservedArtifactsOnce(Path preservedRoot)
	{
		if (preservedArtifactsCleaned)
		{
			return;
		}
		preservedArtifactsCleaned = true;
		try
		{
			if (Files.exists(preservedRoot))
			{
				try (java.util.stream.Stream<Path> walk = Files.walk(preservedRoot))
				{
					walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
						try
						{
							if (!p.equals(preservedRoot))
							{
								Files.deleteIfExists(p);
							}
						}
						catch (Exception ignored)
						{
							// ignore individual failures
						}
					});
				}
			}
		}
		catch (Exception ignored)
		{
			// non-fatal
		}
	}


	/**
	 * Runs the Cypress spec for the given form using 'npx cypress run'.
	 *
	 * @param formName the form whose .spec.cy.js to run
	 * @param headless true for headless (default), false for headed (debugging)
	 * @return test results output
	 */
	public String runFormCypressTests(String formName, boolean headless)
	{
		return runFormCypressTests(formName, headless, DEFAULT_TIMEOUT_SECONDS, null);
	}

	/**
	 * Runs the Cypress spec for the given form using 'npx cypress run', with an explicit
	 * per-test timeout and optional extra Cypress arguments. Used by the headless CI runner.
	 *
	 * @param formName the form whose .spec.cy.js to run
	 * @param headless true for headless (default), false for headed (debugging)
	 * @param timeoutSeconds per-test timeout in seconds; values &lt;= 0 fall back to the default
	 * @param extraCypressArgs optional space-separated extra arguments appended to the Cypress command (may be null)
	 * @return test results output
	 */
	public String runFormCypressTests(String formName, boolean headless, int timeoutSeconds, String extraCypressArgs)
	{
		int effectiveTimeout = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
		try
		{
			if (formName == null || formName.isBlank())
			{
				return "Error: Form name must not be null or empty.";
			}

			ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeProject == null)
			{
				return "Error: No active Servoy project.";
			}

			String solutionName = activeProject.getSolution().getName();
			Path specFilePath = specGenerator.findExistingSpecFile(formName, solutionName);
			if (specFilePath == null || !Files.exists(specFilePath))
			{
				return "Error: Spec file not found: jenkins-custom/e2e-test-scripts/cypress/cy-form/" + formName +
					".spec.cy.js. Use showFormInBrowser first to auto-generate it.";
			}

			// The form spec lives under the e2e-test-scripts project tree, so Cypress MUST
			// be run with that folder as its project root (a spec outside the project root
			// is reported as "no spec files were found"). Reuse the same project-local
			// Cypress install + config that runE2ECypressTests uses, rather than the bundled
			// .metadata Cypress whose root is elsewhere.
			Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
			Path scriptsRoot = workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts");
			Path configFile = Files.exists(scriptsRoot.resolve("cypress.config.ts"))
				? scriptsRoot.resolve("cypress.config.ts")
				: scriptsRoot.resolve("cypress.config.js");
			if (!Files.exists(configFile))
			{
				int port = ApplicationServerRegistry.get().getWebServerPort();
				ensureCypressConfig(scriptsRoot, "http://localhost:" + port);
				configFile = scriptsRoot.resolve("cypress.config.js");
			}

			CypressInvocation invocation = prepareInvocation(scriptsRoot);
			if (invocation.error() != null)
			{
				return invocation.error();
			}
			List<String> command = new ArrayList<>(invocation.commandPrefix());
			command.add("--spec");
			command.add(specFilePath.toString());
			if (Files.exists(configFile))
			{
				command.add("--config-file");
				command.add(configFile.toString());
			}
			// Force video + screenshots on for this run only (does not modify the config file)
			command.add("--config");
			command.add("video=true,screenshotOnRunFailure=true");
			if (!headless)
			{
				command.add("--headed");
			}

			if (extraCypressArgs != null && !extraCypressArgs.isBlank())
			{
				for (String extra : extraCypressArgs.trim().split("\\s+"))
				{
					command.add(extra);
				}
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(scriptsRoot.toFile());
			pb.redirectErrorStream(true);
			pb.environment().clear();
			pb.environment().putAll(invocation.env());

			shutdownFormPreviewClients();
			try
			{
				Process process = pb.start();
				activeProcess = process;

				StringBuilder output = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						output.append(line).append("\n");
					}
				}

				boolean finished = process.waitFor(effectiveTimeout, TimeUnit.SECONDS);
				activeProcess = null;
				if (!finished)
				{
					process.destroyForcibly();
					return "Error: Cypress test timed out after " + effectiveTimeout + " seconds.";
				}

				String rawOutput = output.toString();

				if (process.exitValue() == 0)
				{
					return "**Form Spec Results: " + formName + "**\n\nAll tests passed!\n\n" + rawOutput;
				}
				else
				{
					rawOutput = preserveArtifacts(rawOutput, formName);
					return "**Form Spec Results: " + formName + "**\n\nSome tests failed:\n\n" + rawOutput;
				}
			}
			finally
			{
				shutdownFormPreviewClients();
			}
		}
		catch (Exception e)
		{
			return "Error running spec: " + e.getMessage();
		}
	}

	/**
	 * Writes cypress.config.js with baseUrl for the running Servoy solution.
	 */
	private void ensureCypressConfig(Path cypressDir, String baseUrl)
	{
		try
		{
			Files.createDirectories(cypressDir);
			Path configFile = cypressDir.resolve("cypress.config.js");
			String config = "// Auto-generated by Servoy MCP - do not edit manually\n" +
				"const { defineConfig } = require('cypress');\n\n" +
				"module.exports = defineConfig({\n" +
				"  e2e: {\n" +
				"    baseUrl: '" + baseUrl + "',\n" +
				"    supportFile: false,\n" +
				"    specPattern: '**/*.cy.{js,ts}',\n" +
				"    testIsolation: true,\n" +
				"    defaultCommandTimeout: 10000,\n" +
				"    pageLoadTimeout: 30000,\n" +
				"    video: true,\n" +
				"    screenshotOnRunFailure: true,\n" +
				"  },\n" +
				"});\n";
			Files.writeString(configFile, config, StandardCharsets.UTF_8);
		}
		catch (Exception e)
		{
			// Non-fatal
		}
	}

	public Path getCypressDir()
	{
		return getCypressDir(getWorkspaceRoot());
	}

	public Path getCypressDir(Path workspaceRoot)
	{
		return CypressToolchain.getFallbackCypressDir(workspaceRoot);
	}



	private static Path getWorkspaceRoot()
	{
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
	}

	private static Path resolveCacheFolder()
	{
		return CypressToolchain.resolveCacheFolder(getWorkspaceRoot(), System.getProperty(CypressToolchain.CACHE_FOLDER_PROPERTY),
			System.getenv(CypressToolchain.CACHE_FOLDER_ENV));
	}

	/**
	 * Resolves node/npm/pnpm exactly as the NG client build does (system properties / environment, otherwise the bundled ones).
	 * Returns empty tools if node is not available.
	 */
	private NodeTools resolveNodeTools()
	{
		Activator ngActivator = Activator.getInstance();
		if (ngActivator == null)
		{
			return NodeTools.empty();
		}
		if (ngActivator.getNodePath() == null)
		{
			ngActivator.extractNode();
		}
		if (!ngActivator.waitForNodeExtraction(NODE_EXTRACTION_TIMEOUT_SECONDS, TimeUnit.SECONDS))
		{
			return NodeTools.empty();
		}
		// pnpm mode (servoy.jsRuntime=pnpm) does not exist on lts_2026, so node + npm are always used
		NodeTools resolved = new NodeTools(ngActivator.getNodePath(), ngActivator.getNpmPath(), null, false);
		return resolved;
	}

	public File getNodePath()
	{
		File node = resolveNodeTools().node();
		return node;
	}

	private static String noToolsError(NodeTools tools)
	{
		if (tools.node() != null && tools.pnpmMode() && tools.pnpm() == null)
		{
			return "Error: servoy.jsRuntime=pnpm is set but no pnpm binary is available.";
		}
		return "Error: Node.js is not available (servoy.nodePath/servoy.npmPath not set and the bundled Node.js was not extracted). Check servoy.ini or the Developer log.";
	}

	/**
	 * Applies the PATH, NODE_PATH and CYPRESS_CACHE_FOLDER settings used by the runner to the given process builder,
	 * so that externally started Cypress processes use the same node and binary cache.
	 */
	public void configureCypressEnvironment(ProcessBuilder pb, Path projectDir)
	{
		NodeTools tools = resolveNodeTools();
		Path cacheFolder = resolveCacheFolder();
		try
		{
			Files.createDirectories(cacheFolder);
		}
		catch (IOException e)
		{
			ServoyLog.logWarning("Could not create Cypress cache folder " + cacheFolder + ": " + e.getMessage(), e);
		}
		Map<String, String> env = CypressToolchain.buildEnvironment(pb.environment(), tools, projectDir, projectDir.resolve("node_modules"), cacheFolder);
		pb.environment().clear();
		pb.environment().putAll(env);
	}

	/**
	 * Prepares everything needed to start 'cypress run': installs packages if needed, makes sure the Cypress binary is present
	 * and builds the command prefix and environment.
	 */
	private CypressInvocation prepareInvocation(Path scriptsRoot)
	{
		try
		{
			String localInstallError = ensureProjectCypressInstalled(scriptsRoot);
			if (localInstallError != null)
			{
				return CypressInvocation.failed(localInstallError);
			}
			NodeTools tools = resolveNodeTools();
			if (tools.node() == null)
			{
				return CypressInvocation.failed(noToolsError(tools));
			}

			Path projectDir;
			List<String> prefix = new ArrayList<>();
			String localCypressCmd = resolveLocalCypressCmd(scriptsRoot.resolve("node_modules").resolve(".bin"));
			if (localCypressCmd != null)
			{
				projectDir = scriptsRoot;
				prefix.add(localCypressCmd);
				prefix.add("run");
			}
			else
			{
				projectDir = getCypressDir();
				String setupError = ensureCypressInstalled(projectDir);
				if (setupError != null)
				{
					return CypressInvocation.failed(setupError);
				}
				prefix.addAll(CypressToolchain.buildCypressCliCommand(tools.node(), projectDir, List.of("run")));
			}

			String binaryError = ensureCypressBinary(projectDir);
			if (binaryError != null)
			{
				return CypressInvocation.failed(binaryError);
			}

			Path cacheFolder = resolveCacheFolder();
			Files.createDirectories(cacheFolder);
			Map<String, String> env = CypressToolchain.buildEnvironment(System.getenv(), tools, projectDir, projectDir.resolve("node_modules"), cacheFolder);
			return new CypressInvocation(prefix, env, null);
		}
		catch (Exception e)
		{
			return CypressInvocation.failed("Error preparing Cypress: " + e.getMessage());
		}
	}

	/**
	 * Runs a process, draining its merged output on a worker thread so that the timeout is really enforced.
	 * On timeout the process is killed and exitValue() is never accessed.
	 */
	private ProcessResult runProcess(List<String> command, Path workDir, Map<String, String> env, int timeoutSeconds) throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.directory(workDir.toFile());
		pb.redirectErrorStream(true);
		pb.environment().clear();
		pb.environment().putAll(env);
		Process process = pb.start();
		activeProcess = process;
		StringBuffer output = new StringBuffer();
		Thread drain = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					output.append(line).append('\n');
				}
			}
			catch (IOException e)
			{
				// process was killed or stream closed
			}
		}, "cypress-output-drain");
		drain.setDaemon(true);
		drain.start();
		try
		{
			boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
			if (!finished)
			{
				process.destroyForcibly();
				process.waitFor(5, TimeUnit.SECONDS);
				drain.join(2000);
				return new ProcessResult(-1, true, output.toString());
			}
			drain.join(5000);
			return new ProcessResult(process.exitValue(), false, output.toString());
		}
		finally
		{
			activeProcess = null;
		}
	}

	/**
	 * Runs the package manager install in the given directory. Returns an error string or null on success.
	 */
	private String runPackageInstall(String step, Path dir, NodeTools tools) throws IOException, InterruptedException
	{
		List<String> args = new ArrayList<>();
		args.add("install");
		if (!tools.pnpmMode())
		{
			args.add("--no-audit");
			args.add("--no-fund");
		}
		List<String> command = CypressToolchain.buildPackageManagerCommand(tools, args);
		Path cacheFolder = resolveCacheFolder();
		Files.createDirectories(cacheFolder);
		Map<String, String> env = CypressToolchain.buildEnvironment(System.getenv(), tools, dir, dir.resolve("node_modules"), cacheFolder);
		long start = System.currentTimeMillis();
		ServoyLog.logInfo("[Servoy Cypress] " + step + " in " + dir + " (this may take a few minutes): " + String.join(" ", command));
		ProcessResult result = runProcess(command, dir, env, PACKAGE_INSTALL_TIMEOUT_SECONDS);
		if (result.timedOut())
		{
			return "Error: " + step + " timed out after " + PACKAGE_INSTALL_TIMEOUT_SECONDS + " seconds.";
		}
		if (result.exitCode() != 0)
		{
			String error = CypressToolchain.formatFailure(step, command, result.exitCode(), cacheFolder, result.output());
			ServoyLog.logWarning("[Servoy Cypress] " + error, null);
			return error;
		}
		ServoyLog.logInfo("[Servoy Cypress] " + step + " finished in " + String.format("%.2f", (System.currentTimeMillis() - start) / 1000d) + " s");
		return null;
	}

	private static String readInstalledCypressVersion(Path projectDir)
	{
		try
		{
			Path packageJson = projectDir.resolve("node_modules").resolve("cypress").resolve("package.json");
			if (Files.exists(packageJson))
			{
				Matcher m = VERSION_PATTERN.matcher(Files.readString(packageJson, StandardCharsets.UTF_8));
				if (m.find()) return m.group(1);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logWarning("Could not read installed Cypress version: " + e.getMessage(), e);
		}
		return "unknown";
	}

	private static int majorOf(String version)
	{
		Matcher m = Pattern.compile("(\\d+)").matcher(version);
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}


	public String executeTestSetup(String serverName, String tableName, java.util.Map<String, Object> columnValues)
	{
		if (serverName == null || tableName == null || columnValues == null || columnValues.isEmpty())
			return "Error: serverName, tableName, and columnValues are required.";

		try
		{
			if (!com.servoy.j2db.server.shared.ApplicationServerRegistry.exists())
				return "Error: Servoy application server is not running.";

			IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, false, false);
			if (server == null) return "Error: Database server '" + serverName + "' not found.";

			StringBuilder cols = new StringBuilder();
			StringBuilder placeholders = new StringBuilder();
			List<Object> values = new ArrayList<>();

			for (java.util.Map.Entry<String, Object> entry : columnValues.entrySet())
			{
				if (cols.length() > 0)
				{
					cols.append(", ");
					placeholders.append(", ");
				}
				cols.append(entry.getKey());
				placeholders.append("?");
				values.add(entry.getValue());
			}

			String sql = "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + placeholders + ")";

			try (Connection conn = server.getRawConnection())
			{
				try (PreparedStatement ps = conn.prepareStatement(sql))
				{
					for (int i = 0; i < values.size(); i++)
					{
						ps.setObject(i + 1, values.get(i));
					}
					ps.executeUpdate();
				}
				if (!conn.getAutoCommit()) conn.commit();
			}

			return "Test setup: inserted 1 row into " + serverName + "." + tableName;
		}
		catch (Exception e)
		{
			return "Error in test setup: " + e.getMessage();
		}
	}

	public String executeTestTeardown(String serverName, String tableName, String whereColumn, Object whereValue)
	{
		if (serverName == null || tableName == null || whereColumn == null || whereValue == null)
			return "Error: serverName, tableName, whereColumn, and whereValue are required.";

		try
		{
			if (!com.servoy.j2db.server.shared.ApplicationServerRegistry.exists())
				return "Error: Servoy application server is not running.";

			IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, false, false);
			if (server == null) return "Error: Database server '" + serverName + "' not found.";

			String sql = "DELETE FROM " + tableName + " WHERE " + whereColumn + " = ?";
			int deleted = 0;

			try (Connection conn = server.getRawConnection())
			{
				try (PreparedStatement ps = conn.prepareStatement(sql))
				{
					ps.setObject(1, whereValue);
					deleted = ps.executeUpdate();
				}
				if (!conn.getAutoCommit()) conn.commit();
			}

			return "Test teardown: deleted " + deleted + " row(s) from " + serverName + "." + tableName + " where " + whereColumn + " = '" + whereValue + "'";
		}
		catch (Exception e)
		{
			return "Error in test teardown: " + e.getMessage();
		}
	}

	/**
	 * Runs the Cypress E2E spec for the given form from jenkins-custom/e2e-test-scripts/cypress/e2e/&lt;solutionName&gt;/.
	 * Falls back to a recursive search across all solution subdirectories.
	 *
	 * @param targetForm the form name whose .cy.js or .cy.ts to run (e.g. 'order_detail' → 'order_detail.cy.ts')
	 * @param headless true for headless (default), false for headed (debugging)
	 * @return test results output
	 */
	public String runE2ECypressTests(String targetForm, boolean headless)
	{
		try
		{
			Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
			Path e2eBaseDir = workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress").resolve("e2e");

			// resolve active solution name to look in solution-specific subdirectory first
			com.servoy.eclipse.model.nature.ServoyProject servoyProject = com.servoy.eclipse.model.ServoyModelFinder
				.getServoyModel().getActiveProject();
			String solutionName = (servoyProject != null && servoyProject.getProject() != null)
				? servoyProject.getProject().getName()
				: null;
			Path e2eDir = (solutionName != null) ? e2eBaseDir.resolve(solutionName) : e2eBaseDir;
			// prefer cypress.config.ts (TypeScript project), fall back to cypress.config.js
			Path scriptsRoot = workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts");
			Path configFile = Files.exists(scriptsRoot.resolve("cypress.config.ts"))
				? scriptsRoot.resolve("cypress.config.ts")
				: scriptsRoot.resolve("cypress.config.js");

			// find spec file:
			// 1. exact match in solution subdir (supports relative paths like "applications/environment/queryPerformance.cy.ts")
			// 2. <targetForm>.cy.js in solution subdir
			// 3. <targetForm>.cy.ts in solution subdir
			// 4. recursive search under e2eBaseDir (all solutions)
			Path specFilePath = e2eDir.resolve(targetForm);
			if (!Files.exists(specFilePath))
			{
				specFilePath = e2eDir.resolve(targetForm + ".cy.js");
			}
			if (!Files.exists(specFilePath))
			{
				specFilePath = e2eDir.resolve(targetForm + ".cy.ts");
			}
			if (!Files.exists(specFilePath) && Files.exists(e2eBaseDir))
			{
				// try the target as a path relative to the base e2e dir (handles names carrying a
				// subfolder such as "appB/login" produced by run-all discovery across solutions)
				Path baseRelative = e2eBaseDir.resolve(targetForm);
				if (Files.exists(baseRelative))
				{
					specFilePath = baseRelative;
				}
				else if (Files.exists(e2eBaseDir.resolve(targetForm + ".cy.js")))
				{
					specFilePath = e2eBaseDir.resolve(targetForm + ".cy.js");
				}
				else if (Files.exists(e2eBaseDir.resolve(targetForm + ".cy.ts")))
				{
					specFilePath = e2eBaseDir.resolve(targetForm + ".cy.ts");
				}
				else
				{
					// last resort: recursive walk matching the bare base name (may be ambiguous
					// when the same base name exists under multiple subfolders)
					String baseName = targetForm.replaceAll("\\.cy\\.(js|ts)$", "");
					try (java.util.stream.Stream<Path> walk = Files.walk(e2eBaseDir))
					{
						specFilePath = walk
							.filter(p -> {
								String name = p.getFileName().toString();
								return name.equals(baseName + ".cy.js") || name.equals(baseName + ".cy.ts");
							})
							.findFirst()
							.orElse(e2eDir.resolve(targetForm + ".cy.js")); // keep as missing path for error message
					}
				}
			}
			if (!Files.exists(specFilePath))
			{
				return "Error: E2E spec file not found for '" + targetForm + "'. " +
					"Searched recursively under " + e2eBaseDir + " for '" + targetForm + ".cy.js' or '" + targetForm + ".cy.ts'. " +
					"Use generateCypressE2ETest to create a new one, or pass the relative path (e.g. 'applications/environment/queryPerformance.cy.ts').";
			}
			if (!Files.exists(configFile))
			{
				int port = ApplicationServerRegistry.get().getWebServerPort();
				ensureCypressConfig(scriptsRoot, "http://localhost:" + port);
				configFile = scriptsRoot.resolve("cypress.config.js");
			}
			if (!Files.exists(configFile))
			{
				return "Error: cypress.config.ts/js not found at " + scriptsRoot + ". Use generateCypressE2ETest first to scaffold the E2E test structure.";
			}

			CypressInvocation invocation = prepareInvocation(scriptsRoot);
			if (invocation.error() != null)
			{
				return invocation.error();
			}
			List<String> command = new ArrayList<>(invocation.commandPrefix());
			command.add("--spec");
			command.add(specFilePath.toString());
			command.add("--config-file");
			command.add(configFile.toString());
			// Force video + screenshots on for this run only (does not modify the config file)
			command.add("--config");
			command.add("video=true,screenshotOnRunFailure=true");
			if (!headless)
			{
				command.add("--headed");
			}

			ProcessBuilder pb = new ProcessBuilder(command);
			pb.directory(scriptsRoot.toFile());
			pb.redirectErrorStream(true);
			pb.environment().clear();
			pb.environment().putAll(invocation.env());
			Process process = pb.start();
			activeProcess = process;

			StringBuilder output = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					output.append(line).append("\n");
				}
			}

			boolean finished = process.waitFor(DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			activeProcess = null;
			if (!finished)
			{
				process.destroyForcibly();
				return "Error: Cypress E2E test timed out after " + DEFAULT_TIMEOUT_SECONDS + " seconds.";
			}

			String rawOutput = output.toString();

			if (process.exitValue() == 0)
			{
				return "**E2E Spec Results: " + targetForm + "**\n\nAll tests passed!\n\n" + rawOutput;
			}
			else
			{
				// Preserve videos/screenshots before the next run wipes them
				rawOutput = preserveArtifacts(rawOutput, targetForm);
				return "**E2E Spec Results: " + targetForm + "**\n\nSome tests failed:\n\n" + rawOutput;
			}
		}
		catch (Exception e)
		{
			return "Error running E2E spec: " + e.getMessage();
		}
	}

	/**
	 * Returns the path to the project-local Cypress launcher (node_modules/.bin/cypress[.cmd]),
	 * or null if neither exists.
	 */
	public String resolveLocalCypressCmd(Path nodeModulesBin)
	{
		if (nodeModulesBin.resolve("cypress.cmd").toFile().exists())
		{
			return nodeModulesBin.resolve("cypress.cmd").toString();
		}
		if (nodeModulesBin.resolve("cypress").toFile().exists())
		{
			return nodeModulesBin.resolve("cypress").toString();
		}
		return null;
	}

	/**
	 * If the e2e-test-scripts project has a package.json that lists Cypress but the Cypress launcher is not present
	 * under node_modules/.bin, run the configured package manager's install in that directory.
	 * Returns an error string if the install fails, otherwise null. Does nothing (returns null) if there is no
	 * package.json, it does not list Cypress, or Cypress is already installed, letting the caller fall back to the
	 * internal Cypress install.
	 */
	private String ensureProjectCypressInstalled(Path scriptsDir)
	{
		synchronized (INSTALL_LOCK)
		{
			return doEnsureProjectCypressInstalled(scriptsDir);
		}
	}

	private String doEnsureProjectCypressInstalled(Path scriptsDir)
	{
		try
		{
			Path packageJson = scriptsDir.resolve("package.json");
			if (!Files.exists(packageJson))
			{
				return null;
			}
			if (resolveLocalCypressCmd(scriptsDir.resolve("node_modules").resolve(".bin")) != null)
			{
				return null;
			}
			if (!Files.readString(packageJson, StandardCharsets.UTF_8).contains("\"cypress\""))
			{
				return null;
			}
			NodeTools tools = resolveNodeTools();
			if (!tools.isUsable())
			{
				return noToolsError(tools);
			}
			return runPackageInstall("Package install for the e2e-test-scripts project", scriptsDir, tools);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return "Error: Interrupted while installing the e2e-test-scripts project dependencies.";
		}
		catch (Exception e)
		{
			return "Error ensuring project Cypress install: " + e.getMessage();
		}
	}

	/**
	 * Ensures Cypress (package and binary) is installed locally in the given directory (normally under the .metadata plugins directory).
	 * Returns null when Cypress is ready to run from cypressDir.
	 */
	public String ensureCypressInstalled(Path cypressDir)
	{
		synchronized (INSTALL_LOCK)
		{
			return doEnsureCypressInstalled(cypressDir);
		}
	}

	private String doEnsureCypressInstalled(Path cypressDir)
	{
		try
		{
			Path installedPackage = cypressDir.resolve("node_modules").resolve("cypress");
			boolean needsInstall = !Files.exists(installedPackage);
			if (!needsInstall && majorOf(readInstalledCypressVersion(cypressDir)) != majorOf(DEFAULT_CYPRESS_VERSION))
			{
				needsInstall = true;
			}

			if (needsInstall)
			{
				NodeTools tools = resolveNodeTools();
				if (!tools.isUsable())
				{
					return noToolsError(tools);
				}
				Files.createDirectories(cypressDir);
				String packageJson = "{\n" +
					"  \"name\": \"servoy-cypress\",\n" +
					"  \"version\": \"1.0.0\",\n" +
					"  \"private\": true,\n" +
					"  \"dependencies\": {\n" +
					"    \"cypress\": \"" + DEFAULT_CYPRESS_VERSION + "\"\n" +
					"  }\n" +
					"}\n";
				Files.writeString(cypressDir.resolve("package.json"), packageJson, StandardCharsets.UTF_8);

				String installError = runPackageInstall("Cypress package install", cypressDir, tools);
				if (installError != null)
				{
					return installError;
				}
			}
			return ensureCypressBinary(cypressDir);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return "Error: Interrupted while setting up Cypress.";
		}
		catch (Exception e)
		{
			return "Error setting up Cypress: " + e.getMessage();
		}
	}

	/**
	 * Makes sure the Cypress binary (not just the npm package) is present in the Cypress cache folder, installing it when missing.
	 * Returns null on success, or an error string.
	 */
	public String ensureCypressBinary(Path projectDir)
	{
		synchronized (INSTALL_LOCK)
		{
			return doEnsureCypressBinary(projectDir);
		}
	}

	private String doEnsureCypressBinary(Path projectDir)
	{
		try
		{
			Path cliEntry = projectDir.resolve("node_modules").resolve("cypress").resolve("bin").resolve("cypress");
			if (!Files.exists(cliEntry))
			{
				return "Error: Cypress package not installed in " + projectDir + " (missing " + cliEntry + ").";
			}
			NodeTools tools = resolveNodeTools();
			if (tools.node() == null)
			{
				return noToolsError(tools);
			}
			Path cacheFolder = resolveCacheFolder();
			Files.createDirectories(cacheFolder);
			String key = projectDir.toAbsolutePath() + "|" + cacheFolder.toAbsolutePath() + "|" + readInstalledCypressVersion(projectDir);
			synchronized (VERIFIED_BINARIES)
			{
				if (VERIFIED_BINARIES.contains(key))
				{
					return null;
				}
			}

			Map<String, String> env = CypressToolchain.buildEnvironment(System.getenv(), tools, projectDir, projectDir.resolve("node_modules"), cacheFolder);
			List<String> verify = CypressToolchain.buildCypressCliCommand(tools.node(), projectDir, List.of("verify"));
			ProcessResult result = runProcess(verify, projectDir, env, BINARY_VERIFY_TIMEOUT_SECONDS);
			if (!result.timedOut() && result.exitCode() == 0)
			{
			markVerified(key);
				return null;
			}

			List<String> install = CypressToolchain.buildCypressCliCommand(tools.node(), projectDir, List.of("install"));
			ServoyLog.logInfo("[Servoy Cypress] Cypress binary not verified, installing it into " + cacheFolder + " (about 250 MB, this may take a few minutes): " +
				String.join(" ", install));
			long start = System.currentTimeMillis();
			result = runProcess(install, projectDir, env, BINARY_INSTALL_TIMEOUT_SECONDS);
			if (result.timedOut())
			{
				return "Error: Cypress binary install timed out after " + BINARY_INSTALL_TIMEOUT_SECONDS + " seconds.";
			}
			if (result.exitCode() != 0)
			{
				String error = CypressToolchain.formatFailure("Cypress binary install", install, result.exitCode(), cacheFolder, result.output());
				ServoyLog.logWarning("[Servoy Cypress] " + error, null);
				return error;
			}
			ServoyLog.logInfo("[Servoy Cypress] Cypress binary installed in " + String.format("%.2f", (System.currentTimeMillis() - start) / 1000d) + " s");
			ProcessResult installResult = result;

			result = runProcess(verify, projectDir, env, BINARY_VERIFY_TIMEOUT_SECONDS);
			if (result.timedOut())
			{
				return "Error: Cypress binary verify timed out after " + BINARY_VERIFY_TIMEOUT_SECONDS + " seconds.";
			}
			if (result.exitCode() != 0)
			{
				String error = CypressToolchain.formatBinaryNotInstalled(install, installResult.output(), verify, result.exitCode(), cacheFolder, result.output(), env);
				ServoyLog.logWarning("[Servoy Cypress] " + error, null);
				return error;
			}
			markVerified(key);
			return null;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return "Error: Interrupted while ensuring the Cypress binary.";
		}
		catch (Exception e)
		{
			return "Error ensuring Cypress binary: " + e.getMessage();
		}
	}

	private static void markVerified(String key)
	{
		synchronized (VERIFIED_BINARIES)
		{
			VERIFIED_BINARIES.add(key);
		}
	}
}
