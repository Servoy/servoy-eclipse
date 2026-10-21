package com.servoy.eclipse.cypress.services;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-logic helpers used by {@link FormSpecRunner} to run node/npm/pnpm and
 * Cypress the same way the NG client build does. This class deliberately has no
 * dependency on OSGi, SWT or the workspace so that it can be unit tested
 * without launching any process.
 */
public final class CypressToolchain {
	/**
	 * The id of the cypress plugin; used as the folder name under
	 * {workspace}/.metadata/.plugins.
	 */
	public static final String PLUGIN_DIR_NAME = "com.servoy.eclipse.cypress";

	static final String FALLBACK_CYPRESS_DIR = "cypress";
	static final String CACHE_DIR = "cypress-cache";
	static final String CACHE_FOLDER_ENV = "CYPRESS_CACHE_FOLDER";

	/**
	 * System property (for example set with -D in servoy.ini after -vmargs) that
	 * overrides the Cypress binary cache folder. It wins over the
	 * CYPRESS_CACHE_FOLDER environment variable, the same order the servoy.nodePath
	 * / servoy.npmPath settings use.
	 */
	public static final String CACHE_FOLDER_PROPERTY = "servoy.cypress.cacheFolder";
	public static final String PNPM_UNBLOCK_FLAG = "--config.strictDepBuilds=false";
	private static final int MAX_OUTPUT_LINES = 60;

	private CypressToolchain() {
	}

	/**
	 * The node tooling resolved from the NG client {@code Activator}.
	 *
	 * @param node     the node executable
	 * @param npm      the npm script (run through node)
	 * @param pnpm     the pnpm executable (only used in pnpm mode)
	 * @param pnpmMode true if servoy.jsRuntime=pnpm
	 */
	public record NodeTools(File node, File npm, File pnpm, boolean pnpmMode) {
		public static NodeTools empty() {
			return new NodeTools(null, null, null, false);
		}

		public File packageManager() {
			return pnpmMode ? pnpm : npm;
		}

		public boolean isUsable() {
			return node != null && packageManager() != null;
		}
	}

	public static Path getPluginMetadataDir(Path workspaceRoot) {
		return workspaceRoot.resolve(".metadata").resolve(".plugins").resolve(PLUGIN_DIR_NAME);
	}

	public static Path getFallbackCypressDir(Path workspaceRoot) {
		return getPluginMetadataDir(workspaceRoot).resolve(FALLBACK_CYPRESS_DIR);
	}

	/**
	 * Resolves the Cypress binary cache folder. Order: the
	 * {@value #CACHE_FOLDER_PROPERTY} system property (for example from
	 * servoy.ini), then the CYPRESS_CACHE_FOLDER environment variable, then the
	 * cache folder under the plugin's .metadata directory. Blank values are
	 * ignored.
	 *
	 * @param systemPropertyValue   the value of the {@value #CACHE_FOLDER_PROPERTY}
	 *                              system property, may be null
	 * @param envCypressCacheFolder the value of the CYPRESS_CACHE_FOLDER
	 *                              environment variable, may be null
	 */
	public static Path resolveCacheFolder(Path workspaceRoot, String systemPropertyValue,
			String envCypressCacheFolder) {
		if (systemPropertyValue != null && !systemPropertyValue.isBlank()) {
			return Path.of(systemPropertyValue.trim());
		}
		return resolveCacheFolder(workspaceRoot, envCypressCacheFolder);
	}

	/**
	 * @return the user supplied CYPRESS_CACHE_FOLDER if not blank, otherwise the
	 *         cache folder under the plugin's .metadata directory
	 */
	public static Path resolveCacheFolder(Path workspaceRoot, String envCypressCacheFolder) {
		if (envCypressCacheFolder != null && !envCypressCacheFolder.isBlank()) {
			return Path.of(envCypressCacheFolder.trim());
		}
		Path cache = getPluginMetadataDir(workspaceRoot).resolve(CACHE_DIR);
		return cache;
	}

	/**
	 * Builds the command to run the configured package manager. In npm mode npm is
	 * a node script, so it is run through the configured node; in pnpm mode the
	 * pnpm executable is run directly. Never relies on PATH lookup for the launcher
	 * itself.
	 */
	public static List<String> buildPackageManagerCommand(NodeTools tools, List<String> args) {
		List<String> command = new ArrayList<>();
		if (tools.pnpmMode()) {
			command.add(absolute(tools.pnpm()));
			command.addAll(args);
			if (!args.isEmpty() && "install".equals(args.get(0))) {
				command.add(PNPM_UNBLOCK_FLAG);
			}
		} else {
			command.add(absolute(tools.node()));
			command.add(absolute(tools.npm()));
			command.addAll(args);
		}
		return command;
	}

	/**
	 * Builds the command to run the cypress CLI through its JS entry point with the
	 * configured node (no .bin shims or npx needed).
	 */
	public static List<String> buildCypressCliCommand(File node, Path projectDir, List<String> cypressArgs) {
		List<String> command = new ArrayList<>();
		command.add(absolute(node));
		command.add(projectDir.resolve("node_modules").resolve("cypress").resolve("bin").resolve("cypress").toString());
		command.addAll(cypressArgs);
		return command;
	}

	/**
	 * Returns a copy of base with PATH, NODE_PATH and CYPRESS_CACHE_FOLDER set.
	 * Everything else is inherited unchanged.
	 */
	public static Map<String, String> buildEnvironment(Map<String, String> base, NodeTools tools, Path projectDir,
			Path nodeModulesDir, Path cacheFolder) {
		Map<String, String> env = new LinkedHashMap<>(base);
		String pathKey = "PATH";
		for (String key : env.keySet()) {
			if ("PATH".equalsIgnoreCase(key)) {
				pathKey = key;
				break;
			}
		}
		String originalPath = env.get(pathKey);

		List<String> entries = new ArrayList<>();
		File nodeDir = tools.node() != null ? tools.node().getAbsoluteFile().getParentFile() : null;
		if (nodeDir != null) {
			entries.add(nodeDir.getPath());
		}
		File pnpm = tools.pnpm();
		File pnpmDir = pnpm != null ? pnpm.getAbsoluteFile().getParentFile() : null;
		if (pnpmDir != null && !pnpmDir.equals(nodeDir)) {
			entries.add(pnpmDir.getPath());
		}
		entries.add(projectDir.resolve("node_modules").resolve(".bin").toString());
		if (originalPath != null && !originalPath.isEmpty()) {
			entries.add(originalPath);
		}
		env.put(pathKey, String.join(File.pathSeparator, entries));
		env.put("NODE_PATH", nodeModulesDir.toString());
		env.put(CACHE_FOLDER_ENV, cacheFolder.toString());
		return env;
	}

	public static String formatFailure(String step, List<String> command, int exitCode, Path cacheFolder,
			String output) {
		return formatFailure(step, command, exitCode, cacheFolder, output, System.getenv());
	}

	public static String formatFailure(String step, List<String> command, int exitCode, Path cacheFolder, String output,
			Map<String, String> env) {
		StringBuilder sb = new StringBuilder();
		sb.append("Error: ").append(step).append(" failed (exit code ").append(exitCode).append(").\n");
		sb.append("Command: ").append(String.join(" ", command)).append('\n');
		sb.append("Cypress cache folder: ").append(cacheFolder).append('\n');
		String tail = tail(output, MAX_OUTPUT_LINES);
		if (!tail.isEmpty()) {
			sb.append("Output (last lines):\n").append(tail).append('\n');
		}
		String lower = output == null ? "" : output.toLowerCase(Locale.ROOT);
		if ("0".equals(env.get("CYPRESS_INSTALL_BINARY"))) {
			sb.append(
					"Hint: CYPRESS_INSTALL_BINARY=0 is set, which skips the Cypress binary download. Unset it to let the binary be installed.\n");
		}
		if (looksLikeNetworkProblem(lower)) {
			sb.append(
					"Hint: this looks like a network or proxy problem. Check your proxy settings (HTTP_PROXY/HTTPS_PROXY/NO_PROXY) and that the Cypress download host is reachable.\n");
		}
		return sb.toString();
	}

	/**
	 * Builds the error shown when "cypress install" exited with code 0 but "cypress
	 * verify" still cannot find a usable binary. This typically happens when the
	 * download is silently blocked (proxy, firewall), so the output of both
	 * commands is included and a proxy hint is added whenever a proxy variable is
	 * set in the environment of the process.
	 */
	public static String formatBinaryNotInstalled(List<String> installCommand, String installOutput,
			List<String> verifyCommand, int verifyExitCode, Path cacheFolder, String verifyOutput,
			Map<String, String> env) {
		StringBuilder sb = new StringBuilder();
		sb.append(
				"Error: Cypress binary install did not produce a usable binary. 'cypress install' exited with code 0, but 'cypress verify' still failed (exit code ")
				.append(verifyExitCode).append(").\n");
		sb.append("Install command: ").append(String.join(" ", installCommand)).append('\n');
		sb.append("Verify command: ").append(String.join(" ", verifyCommand)).append('\n');
		sb.append("Cypress cache folder: ").append(cacheFolder).append('\n');
		appendOutput(sb, "Install output (last lines)", installOutput);
		appendOutput(sb, "Verify output (last lines)", verifyOutput);
		if ("0".equals(env.get("CYPRESS_INSTALL_BINARY"))) {
			sb.append(
					"Hint: CYPRESS_INSTALL_BINARY=0 is set, which skips the Cypress binary download. Unset it to let the binary be installed.\n");
		}
		List<String> proxyVariables = configuredProxyVariables(env);
		String combined = ((installOutput == null ? "" : installOutput) + "\n"
				+ (verifyOutput == null ? "" : verifyOutput)).toLowerCase(Locale.ROOT);
		if (!proxyVariables.isEmpty() || looksLikeNetworkProblem(combined)) {
			sb.append("Hint: the Cypress binary could not be downloaded. ");
			if (!proxyVariables.isEmpty()) {
				sb.append("The proxy variable(s) ").append(String.join(", ", proxyVariables)).append(
						" are set for this run; make sure that proxy is reachable and allows the Cypress download host. ");
			}
			sb.append(
					"Check your proxy settings (HTTP_PROXY/HTTPS_PROXY/NO_PROXY) and that the Cypress download host is reachable.\n");
		}
		return sb.toString();
	}

	/**
	 * @return the names (never the values, which may contain credentials) of the
	 *         proxy environment variables that are set and not blank
	 */
	static List<String> configuredProxyVariables(Map<String, String> env) {
		List<String> names = new ArrayList<>();
		for (Map.Entry<String, String> entry : env.entrySet()) {
			String key = entry.getKey();
			if (key != null && ("HTTP_PROXY".equalsIgnoreCase(key) || "HTTPS_PROXY".equalsIgnoreCase(key))
					&& entry.getValue() != null && !entry.getValue().isBlank()) {
				names.add(key);
			}
		}
		return names;
	}

	private static void appendOutput(StringBuilder sb, String title, String output) {
		String tail = tail(output, MAX_OUTPUT_LINES);
		if (!tail.isEmpty()) {
			sb.append(title).append(":\n").append(tail).append('\n');
		}
	}

	private static boolean looksLikeNetworkProblem(String lowerOutput) {
		return lowerOutput.contains("proxy") || lowerOutput.contains("econnrefused")
				|| lowerOutput.contains("etimedout") || lowerOutput.contains("enotfound")
				|| lowerOutput.contains("econnreset") || lowerOutput.contains("eai_again")
				|| lowerOutput.contains("getaddrinfo") || lowerOutput.contains("socket hang up")
				|| lowerOutput.contains("self signed certificate")
				|| lowerOutput.contains("unable to get local issuer certificate") || lowerOutput.contains("network");
	}

	private static String tail(String output, int maxLines) {
		if (output == null || output.isBlank())
			return "";
		String[] lines = output.stripTrailing().split("\\R");
		int start = Math.max(0, lines.length - maxLines);
		return String.join("\n", java.util.Arrays.copyOfRange(lines, start, lines.length));
	}

	private static String absolute(File f) {
		return f.getAbsoluteFile().getPath();
	}
}
