package com.servoy.eclipse.cypress.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.di.annotations.Creatable;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.startup.FormPreviewNGClient;
import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Runs Cypress test specs (.spec.cy.js) against Servoy forms using npx cypress run.
 * Cypress is installed locally in .metadata/.plugins/com.servoy.eclipse.copilot/cypress/.
 * Spec files live next to the .frm file in forms/ directory.
 */
@Creatable
@SuppressWarnings("restriction")
public class FormSpecRunner
{
	private static final String MCP_PLUGIN_DIR = "com.servoy.eclipse.developer.mcp";
	private static final String CYPRESS_DIR = "cypress";
	private static final int DEFAULT_TIMEOUT_SECONDS = 60;

	private final FormSpecGenerator specGenerator = new FormSpecGenerator();
	private volatile Process activeProcess;

	/** Guards one-time cleanup of leftover preserved artifacts from previous Developer sessions. */
	private static volatile boolean preservedArtifactsCleaned = false;

	/**
	 * Cancels the currently running Cypress process, if any.
	 */
	public void cancel() {
		Process p = activeProcess;
		if (p != null && p.isAlive()) {
			p.destroyForcibly();
		}
	}

	private void shutdownFormPreviewClients() {
		try {
			FormPreviewNGClient.shutdownExisting();
		} catch (Exception e) {
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

	private String preserveArtifacts(String rawOutput, String testName) {
		try {
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
			if (video != null) {
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
			if (shot != null) {
				Files.createDirectories(destDir);
				// Short, fixed filename (see video comment above).
				Path dest = destDir.resolve("screenshot.png");
				Files.copy(shot, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				markers.append("\n").append(PRESERVED_SCREENSHOT_MARKER).append(dest.toString());
			}

			return markers.length() > 0 ? rawOutput + markers : rawOutput;
		} catch (Exception e) {
			// Non-fatal - return the original output if preservation fails
			return rawOutput;
		}
	}

	/**
	 * Finds a file directly under {@code dir} whose name starts with {@code baseName} and ends
	 * with {@code extension}, or null if the directory is missing / nothing matches.
	 */
	private Path findMatchingFile(Path dir, String baseName, String extension) {
		if (!Files.isDirectory(dir)) {
			return null;
		}
		try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
			return walk.filter(Files::isRegularFile).filter(p -> {
				String name = p.getFileName().toString();
				return name.startsWith(baseName) && name.endsWith(extension);
			}).findFirst().orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Finds a screenshot .png for the given spec. Cypress nests screenshots under a per-spec
	 * folder (cypress/screenshots/&lt;base&gt;.cy.ts/...), so we search recursively for the
	 * first .png whose path contains the spec base name. Prefers a "(failed)" screenshot.
	 */
	private Path findScreenshot(Path screenshotsDir, String baseName) {
		if (!Files.isDirectory(screenshotsDir)) {
			return null;
		}
		try (java.util.stream.Stream<Path> walk = Files.walk(screenshotsDir)) {
			java.util.List<Path> pngs = walk.filter(Files::isRegularFile)
					.filter(p -> p.getFileName().toString().endsWith(".png"))
					.filter(p -> p.toString().contains(baseName)).toList();
			// Prefer a screenshot marked "(failed)" if present
			return pngs.stream().filter(p -> p.getFileName().toString().contains("(failed)")).findFirst()
					.orElse(pngs.stream().findFirst().orElse(null));
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Deletes preserved-artifacts left over from a previous Developer session. Runs once per
	 * JVM lifetime so artifacts created during the current session are kept (they should live
	 * until Developer is closed), while stale ones from prior sessions are cleaned up.
	 */
	private static synchronized void cleanPreservedArtifactsOnce(Path preservedRoot) {
		if (preservedArtifactsCleaned) {
			return;
		}
		preservedArtifactsCleaned = true;
		try {
			if (Files.exists(preservedRoot)) {
				try (java.util.stream.Stream<Path> walk = Files.walk(preservedRoot)) {
					walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
						try {
							if (!p.equals(preservedRoot)) {
								Files.deleteIfExists(p);
							}
						} catch (Exception ignored) {
							// ignore individual failures
						}
					});
				}
			}
		} catch (Exception ignored) {
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

			// Install project-local Cypress if the repo manages its own but hasn't been bootstrapped
			String localInstallError = ensureProjectCypressInstalled(scriptsRoot);
			if (localInstallError != null)
			{
				return localInstallError;
			}

			List<String> command = new ArrayList<>();
			Path scriptsNodeModulesBin = scriptsRoot.resolve("node_modules").resolve(".bin");
			String localCypressCmd = resolveLocalCypressCmd(scriptsNodeModulesBin);

			File nodePath = getNodePath();

			if (localCypressCmd != null)
			{
				// use project-local cypress directly â no npm/npx lookup needed
				command.add(localCypressCmd);
				command.add("run");
			}
			else
			{
				// fall back to internal .metadata installation
				Path cypressDir = getCypressDir();
				String setupError = ensureCypressInstalled(cypressDir);
				if (setupError != null)
				{
					return setupError;
				}
				if (nodePath == null)
				{
					return "Error: Bundled Node.js not available and no local Cypress found in " + scriptsNodeModulesBin;
				}
				String npxPath = nodePath.getParent() + File.separator + "npx.cmd";
				if (!new File(npxPath).exists())
				{
					npxPath = nodePath.getParent() + File.separator + "npx";
				}
				command.add(npxPath);
				command.add("cypress");
				command.add("run");
			}
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
			// NODE_PATH: use the project-local node_modules if present, otherwise the internal cypress dir
			Path effectiveNodeModules = Files.exists(scriptsRoot.resolve("node_modules"))
				? scriptsRoot.resolve("node_modules")
				: getCypressDir().resolve("node_modules");
			pb.environment().put("NODE_PATH", effectiveNodeModules.toString());
			String existingPath = System.getenv("PATH");
			String prependPath = scriptsNodeModulesBin.toString();
			if (nodePath != null) prependPath = nodePath.getParent() + File.pathSeparator + prependPath;
			pb.environment().put("PATH", prependPath + File.pathSeparator + (existingPath != null ? existingPath : ""));

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
		Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
		Path metadataPlugins = workspaceRoot.getParent().resolve(".metadata").resolve(".plugins");
		return metadataPlugins.resolve(MCP_PLUGIN_DIR).resolve(CYPRESS_DIR);
	}

	public File getNodePath()
	{
		try
		{
			Activator ngActivator = Activator.getInstance();
			if (ngActivator == null) return null;
			ngActivator.extractNode();
			ngActivator.createNPMCommand(new File("."), java.util.List.of("--version"));
			var field = Activator.class.getDeclaredField("nodePath");
			field.setAccessible(true);
			return (File)field.get(ngActivator);
		}
		catch (Exception e)
		{
			return null;
		}
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
				// recursive walk: find first file whose base name (without .cy.js/.cy.ts) matches targetForm
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

			// scriptsRoot already resolved above (for configFile detection)
			Path scriptsDir = scriptsRoot;

				// Prefer the project-local Cypress binary (node_modules/.bin/cypress) if present â
				// this is the case for e2e-test-scripts repos that already have Cypress installed.
				// If the project has a package.json listing Cypress but node_modules is missing,
				// run npm install so the local binary becomes available before we try to use it.
				// Fall back to the internal .metadata-bundled installation only if not found.
				String localInstallError = ensureProjectCypressInstalled(scriptsDir);
				if (localInstallError != null)
				{
					return localInstallError;
				}
				Path scriptsNodeModulesBin = scriptsDir.resolve("node_modules").resolve(".bin");
				String localCypressCmd = resolveLocalCypressCmd(scriptsNodeModulesBin);

			List<String> command = new ArrayList<>();
			if (localCypressCmd != null)
			{
				// use project-local cypress directly â no npm/npx lookup needed
				command.add(localCypressCmd);
				command.add("run");
			}
			else
			{
				// fall back to internal .metadata installation
				Path cypressDir = getCypressDir();
				String setupError = ensureCypressInstalled(cypressDir);
				if (setupError != null)
				{
					return setupError;
				}

				File nodePath = getNodePath();
				if (nodePath == null)
				{
					return "Error: Bundled Node.js not available and no local Cypress found in " + scriptsNodeModulesBin;
				}

				String npxPath = nodePath.getParent() + File.separator + "npx.cmd";
				if (!new File(npxPath).exists())
				{
					npxPath = nodePath.getParent() + File.separator + "npx";
				}
				command.add(npxPath);
				command.add("cypress");
				command.add("run");
			}
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
			pb.directory(scriptsDir.toFile());
			pb.redirectErrorStream(true);
			// NODE_PATH: use the project-local node_modules if present, otherwise the internal cypress dir
			Path effectiveNodeModules = Files.exists(scriptsDir.resolve("node_modules"))
				? scriptsDir.resolve("node_modules")
				: getCypressDir().resolve("node_modules");
			pb.environment().put("NODE_PATH", effectiveNodeModules.toString());
			String existingPath = System.getenv("PATH");
			// Prepend the project-local .bin to PATH so cypress.cmd is found; also add system node
			String prependPath = scriptsDir.resolve("node_modules").resolve(".bin").toString();
			File sysNode = getNodePath();
			if (sysNode != null) prependPath = sysNode.getParent() + File.pathSeparator + prependPath;
			pb.environment().put("PATH", prependPath + File.pathSeparator + (existingPath != null ? existingPath : ""));
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
	 * If the e2e-test-scripts project has a package.json (i.e. it manages its own Cypress) but
	 * the Cypress binary is not present under node_modules/.bin, run "npm install" in that
	 * directory so the local Cypress becomes available. This mirrors how a developer would
	 * bootstrap the repo. Returns an error string if the install fails, otherwise null.
	 * Does nothing (returns null) if there is no package.json or Cypress is already installed,
	 * letting the caller fall back to the bundled Cypress.
	 */
	private String ensureProjectCypressInstalled(Path scriptsDir)
	{
		try
		{
			Path packageJson = scriptsDir.resolve("package.json");
			if (!Files.exists(packageJson))
			{
				// No project-managed Cypress - caller will use bundled install
				return null;
			}
			Path nodeModulesBin = scriptsDir.resolve("node_modules").resolve(".bin");
			if (resolveLocalCypressCmd(nodeModulesBin) != null)
			{
				// Already installed
				return null;
			}
			// package.json exists but Cypress binary missing - the repo needs its deps installed
			File nodePath = getNodePath();
			if (nodePath == null)
			{
				// Can't install; let caller fall back to bundled Cypress
				return null;
			}
			String npmPath = nodePath.getParent() + File.separator + "npm.cmd";
			if (!new File(npmPath).exists())
			{
				npmPath = nodePath.getParent() + File.separator + "npm";
			}
			System.out.println("[Servoy MCP] Cypress not installed in " + scriptsDir
				+ " - running npm install (this may take a few minutes)...");
			ProcessBuilder pb = new ProcessBuilder(npmPath, "install");
			pb.directory(scriptsDir.toFile());
			pb.redirectErrorStream(true);
			String existingPath = System.getenv("PATH");
			pb.environment().put("PATH",
				nodePath.getParent() + File.pathSeparator + (existingPath != null ? existingPath : ""));
			Process p = pb.start();
			StringBuilder npmOutput = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					npmOutput.append(line).append("\n");
				}
			}
			p.waitFor(300, TimeUnit.SECONDS);
			if (p.exitValue() != 0)
			{
				System.err.println("[Servoy MCP] npm install FAILED in " + scriptsDir + ". Output:\n" + npmOutput);
				return "Error: npm install failed in e2e-test-scripts directory.\n" + npmOutput;
			}
			System.out.println("[Servoy MCP] Project Cypress installed successfully.");
			return null;
		}
		catch (Exception e)
		{
			// Non-fatal - let caller fall back to bundled Cypress
			System.err.println("[Servoy MCP] Error ensuring project Cypress install: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Ensures Cypress is installed locally in the .metadata plugins directory.
	 */
	public String ensureCypressInstalled(Path cypressDir)
	{
		try
		{
			boolean needsInstall = !Files.exists(cypressDir.resolve("node_modules/cypress"));

			if (needsInstall)
			{
				System.out.println("[Servoy MCP] Cypress not found at " + cypressDir + " - installing via npm (this may take a few minutes)...");
				Files.createDirectories(cypressDir);
				String packageJson = "{\n" +
					"  \"name\": \"servoy-cypress\",\n" +
					"  \"version\": \"1.0.0\",\n" +
					"  \"private\": true,\n" +
					"  \"dependencies\": {\n" +
					"    \"cypress\": \"^13.0.0\"\n" +
					"  }\n" +
					"}\n";
				Files.writeString(cypressDir.resolve("package.json"), packageJson, StandardCharsets.UTF_8);

				File nodePath = getNodePath();
				if (nodePath == null) return "Error: Node.js not available.";

				String npmPath = nodePath.getParent() + File.separator + "npm.cmd";
				if (!new File(npmPath).exists())
				{
					npmPath = nodePath.getParent() + File.separator + "npm";
				}

				ProcessBuilder pb = new ProcessBuilder(npmPath, "install");
				pb.directory(cypressDir.toFile());
				pb.redirectErrorStream(true);
				// Ensure the bundled node is on PATH so npm (a node script) can find its runtime
				String existingPath = System.getenv("PATH");
				pb.environment().put("PATH",
					nodePath.getParent() + File.pathSeparator + (existingPath != null ? existingPath : ""));
				Process p = pb.start();
				StringBuilder npmOutput = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						npmOutput.append(line).append("\n");
					}
				}
				p.waitFor(180, TimeUnit.SECONDS);

				if (p.exitValue() != 0)
				{
					System.err.println("[Servoy MCP] npm install FAILED. Output:\n" + npmOutput);
					return "Error: npm install failed in cypress directory.\n" + npmOutput;
				}
				System.out.println("[Servoy MCP] Cypress installed successfully.");
			}
			return null;
		}
		catch (Exception e)
		{
			return "Error setting up Cypress: " + e.getMessage();
		}
	}
}

