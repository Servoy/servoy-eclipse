package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.cypress.Activator;
import com.servoy.eclipse.cypress.services.CypressToolchain;
import com.servoy.eclipse.cypress.services.CypressToolchain.NodeTools;

/**
 * SVY-21515: pure-logic tests for {@link CypressToolchain}. No process is
 * launched.
 */
class CypressToolchainTest {
	private static final String PLUGIN_DIR = "com.servoy.eclipse.cypress";

	@TempDir
	Path tmp;

	private File nodeFile;
	private File npmFile;
	private File pnpmFile;

	@BeforeEach
	void setUp() {
		nodeFile = tmp.resolve("bundled").resolve("node").resolve("node.exe").toFile();
		npmFile = tmp.resolve("custom-npm").resolve("lib").resolve("npm-cli.js").toFile();
		pnpmFile = tmp.resolve("pnpm-home").resolve("pnpm.exe").toFile();
	}

	private static String abs(File f) {
		return f.getAbsoluteFile().getPath();
	}

	@Nested
	class PluginDirectories {
		@Test
		@DisplayName("PLUGIN_DIR_NAME equals the cypress Activator.PLUGIN_ID")
		void pluginDirNameMatchesActivatorPluginId() {
			assertEquals(Activator.PLUGIN_ID, CypressToolchain.PLUGIN_DIR_NAME);
			assertEquals(PLUGIN_DIR, CypressToolchain.PLUGIN_DIR_NAME);
		}

		@ParameterizedTest
		@ValueSource(strings = { "/Users/dev/workspace", "/home/ci/build/servoy", "C:\\Users\\dev\\workspace" })
		@DisplayName("plugin metadata dir is <root>/.metadata/.plugins/com.servoy.eclipse.cypress")
		void pluginMetadataDir(String root) {
			Path workspaceRoot = Path.of(root);
			Path result = CypressToolchain.getPluginMetadataDir(workspaceRoot);

			assertEquals(workspaceRoot.resolve(".metadata").resolve(".plugins").resolve(PLUGIN_DIR), result);
			assertTrue(result.startsWith(workspaceRoot), "must be under the workspace root: " + result);
			assertFalse(result.toString().contains(".."), "must not contain parent traversal: " + result);
		}

		@ParameterizedTest
		@ValueSource(strings = { "/Users/dev/workspace", "/home/ci/build/servoy", "C:\\Users\\dev\\workspace" })
		@DisplayName("fallback cypress dir is <pluginDir>/cypress")
		void fallbackCypressDir(String root) {
			Path workspaceRoot = Path.of(root);
			Path result = CypressToolchain.getFallbackCypressDir(workspaceRoot);

			assertEquals(CypressToolchain.getPluginMetadataDir(workspaceRoot).resolve("cypress"), result);
			assertTrue(result.startsWith(CypressToolchain.getPluginMetadataDir(workspaceRoot)));
		}

		@Test
		@DisplayName("fallback dir does not use the old developer.mcp plugin folder")
		void fallbackDirIsNotUnderOldMcpFolder() {
			Path result = CypressToolchain.getFallbackCypressDir(Path.of("/Users/dev/workspace"));

			assertFalse(result.toString().contains("com.servoy.eclipse.developer.mcp"),
					"must not live under the old MCP plugin folder: " + result);
		}
	}

	@Nested
	class ResolveCacheFolder {
		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { " ", "   ", "\t" })
		@DisplayName("null or blank override falls back to <pluginDir>/cypress-cache under the workspace")
		void blankOverrideUsesMetadataCache(String override) {
			Path workspaceRoot = Path.of("/Users/dev/workspace");
			Path result = CypressToolchain.resolveCacheFolder(workspaceRoot, override);

			assertAll(
					() -> assertEquals(
							workspaceRoot.resolve(".metadata/.plugins/com.servoy.eclipse.cypress/cypress-cache"),
							result),
					() -> assertTrue(result.startsWith(workspaceRoot)),
					() -> assertFalse(result.toString().contains("..")));
		}

		@Test
		@DisplayName("non-blank override is returned exactly")
		void nonBlankOverrideWins() {
			Path override = tmp.resolve("my-own-cache");
			Path result = CypressToolchain.resolveCacheFolder(Path.of("/Users/dev/workspace"), override.toString());

			assertEquals(override, result);
		}

		@Test
		@DisplayName("override is trimmed")
		void overrideIsTrimmed() {
			Path override = tmp.resolve("my-own-cache");
			Path result = CypressToolchain.resolveCacheFolder(Path.of("/Users/dev/workspace"), "  " + override + "  ");

			assertEquals(override, result);
		}

		@Test
		@DisplayName("system property wins over the CYPRESS_CACHE_FOLDER environment variable")
		void systemPropertyWinsOverEnvironment() {
			Path fromProperty = tmp.resolve("from-servoy-ini");
			Path fromEnv = tmp.resolve("from-env");
			Path result = CypressToolchain.resolveCacheFolder(Path.of("/Users/dev/workspace"), fromProperty.toString(),
					fromEnv.toString());

			assertEquals(fromProperty, result);
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { " ", "\t" })
		@DisplayName("blank system property falls back to the environment variable")
		void blankPropertyFallsBackToEnvironment(String property) {
			Path fromEnv = tmp.resolve("from-env");
			Path result = CypressToolchain.resolveCacheFolder(Path.of("/Users/dev/workspace"), property,
					fromEnv.toString());

			assertEquals(fromEnv, result);
		}

		@Test
		@DisplayName("system property is trimmed")
		void systemPropertyIsTrimmed() {
			Path fromProperty = tmp.resolve("from-servoy-ini");
			Path result = CypressToolchain.resolveCacheFolder(Path.of("/Users/dev/workspace"),
					"  " + fromProperty + "  ", null);

			assertEquals(fromProperty, result);
		}

		@Test
		@DisplayName("neither property nor environment set uses the .metadata cache")
		void neitherSetUsesMetadataCache() {
			Path workspaceRoot = Path.of("/Users/dev/workspace");
			Path result = CypressToolchain.resolveCacheFolder(workspaceRoot, null, null);

			assertEquals(workspaceRoot.resolve(".metadata/.plugins/com.servoy.eclipse.cypress/cypress-cache"), result);
		}

		@Test
		@DisplayName("the system property name is servoy.cypress.cacheFolder")
		void propertyName() {
			assertEquals("servoy.cypress.cacheFolder", CypressToolchain.CACHE_FOLDER_PROPERTY);
		}
	}

	@Nested
	class NodeToolsTest {
		@Test
		@DisplayName("isUsable is false when node is null")
		void notUsableWithoutNode() {
			assertFalse(new NodeTools(null, npmFile, pnpmFile, false).isUsable());
			assertFalse(new NodeTools(null, npmFile, pnpmFile, true).isUsable());
		}

		@Test
		@DisplayName("isUsable is false when npm is null in npm mode")
		void notUsableWithoutNpmInNpmMode() {
			assertFalse(new NodeTools(nodeFile, null, pnpmFile, false).isUsable());
		}

		@Test
		@DisplayName("isUsable is false when pnpm is null in pnpm mode")
		void notUsableWithoutPnpmInPnpmMode() {
			assertFalse(new NodeTools(nodeFile, npmFile, null, true).isUsable());
		}

		@Test
		@DisplayName("isUsable is true when node and the active package manager are set")
		void usableCases() {
			assertAll(
					() -> assertTrue(new NodeTools(nodeFile, npmFile, null, false).isUsable(),
							"npm mode does not need pnpm"),
					() -> assertTrue(new NodeTools(nodeFile, null, pnpmFile, true).isUsable(),
							"pnpm mode does not need npm"));
		}

		@Test
		@DisplayName("packageManager follows the mode")
		void packageManagerFollowsMode() {
			assertEquals(npmFile, new NodeTools(nodeFile, npmFile, pnpmFile, false).packageManager());
			assertEquals(pnpmFile, new NodeTools(nodeFile, npmFile, pnpmFile, true).packageManager());
		}

		@Test
		@DisplayName("empty() is not usable")
		void emptyIsNotUsable() {
			NodeTools empty = NodeTools.empty();

			assertAll(() -> assertFalse(empty.isUsable()), () -> assertNull(empty.node()),
					() -> assertNull(empty.packageManager()));
		}
	}

	@Nested
	class BuildPackageManagerCommand {
		@Test
		@DisplayName("npm mode runs [node, npm, install, ...] with the configured npm file")
		void npmModeUsesConfiguredNpm() {
			NodeTools tools = new NodeTools(nodeFile, npmFile, null, false);
			assertFalse(npmFile.getParentFile().equals(nodeFile.getParentFile()),
					"precondition: npm lives in a different dir than node (servoy.npmPath)");

			List<String> cmd = CypressToolchain.buildPackageManagerCommand(tools,
					List.of("install", "--no-audit", "--no-fund"));

			assertEquals(List.of(abs(nodeFile), abs(npmFile), "install", "--no-audit", "--no-fund"), cmd);
		}

		@Test
		@DisplayName("pnpm mode runs [pnpm, install, --config.strictDepBuilds=false] without node")
		void pnpmModeInstall() {
			NodeTools tools = new NodeTools(nodeFile, npmFile, pnpmFile, true);

			List<String> cmd = CypressToolchain.buildPackageManagerCommand(tools, List.of("install"));

			assertAll(() -> assertEquals(List.of(abs(pnpmFile), "install", "--config.strictDepBuilds=false"), cmd),
					() -> assertFalse(cmd.contains(abs(nodeFile)), "node path must not be in the pnpm command"),
					() -> assertFalse(cmd.contains(abs(npmFile)), "npm path must not be in the pnpm command"));
		}

		@Test
		@DisplayName("pnpm mode does not add the unblock flag to non-install verbs")
		void pnpmModeOtherVerbHasNoUnblockFlag() {
			NodeTools tools = new NodeTools(nodeFile, npmFile, pnpmFile, true);

			List<String> cmd = CypressToolchain.buildPackageManagerCommand(tools, List.of("list"));

			assertEquals(List.of(abs(pnpmFile), "list"), cmd);
		}

		@Test
		@DisplayName("pnpm mode with no args runs just pnpm")
		void pnpmModeEmptyArgs() {
			NodeTools tools = new NodeTools(nodeFile, npmFile, pnpmFile, true);

			assertEquals(List.of(abs(pnpmFile)), CypressToolchain.buildPackageManagerCommand(tools, List.of()));
		}

		@Test
		@DisplayName("npm mode never adds the pnpm unblock flag")
		void npmModeHasNoUnblockFlag() {
			NodeTools tools = new NodeTools(nodeFile, npmFile, pnpmFile, false);

			List<String> cmd = CypressToolchain.buildPackageManagerCommand(tools, List.of("install"));

			assertFalse(cmd.contains(CypressToolchain.PNPM_UNBLOCK_FLAG));
		}

		@Test
		@DisplayName("input args are not mutated")
		void argsNotMutated() {
			List<String> args = new ArrayList<>(List.of("install"));

			CypressToolchain.buildPackageManagerCommand(new NodeTools(nodeFile, npmFile, pnpmFile, true), args);

			assertEquals(List.of("install"), args);
		}
	}

	@Nested
	class BuildCypressCliCommand {
		@Test
		@DisplayName("runs the cypress JS entry point through the configured node")
		void usesJsEntryPoint() {
			Path projectDir = tmp.resolve("e2e");

			List<String> cmd = CypressToolchain.buildCypressCliCommand(nodeFile, projectDir, List.of("run"));

			assertEquals(List.of(abs(nodeFile),
					projectDir.resolve("node_modules").resolve("cypress").resolve("bin").resolve("cypress").toString(),
					"run"), cmd);
		}

		@Test
		@DisplayName("extra cypress args are appended in order")
		void appendsArgs() {
			List<String> cmd = CypressToolchain.buildCypressCliCommand(nodeFile, tmp,
					List.of("run", "--spec", "a.cy.js"));

			assertEquals(List.of("run", "--spec", "a.cy.js"), cmd.subList(2, cmd.size()));
		}

		@Test
		@DisplayName("does not use npx or .bin shims")
		void noNpxOrShims() {
			List<String> cmd = CypressToolchain.buildCypressCliCommand(nodeFile, tmp, List.of("verify"));

			String joined = String.join(" ", cmd).toLowerCase();
			assertAll(() -> assertFalse(joined.contains("npx")), () -> assertFalse(joined.contains(".bin")));
		}
	}

	@Nested
	class BuildEnvironment {
		private Path projectDir;
		private Path nodeModules;
		private Path cache;

		@BeforeEach
		void init() {
			projectDir = tmp.resolve("e2e");
			nodeModules = projectDir.resolve("node_modules");
			cache = tmp.resolve("cache");
		}

		private Map<String, String> baseEnv(String pathKey, String pathValue) {
			Map<String, String> base = new LinkedHashMap<>();
			base.put(pathKey, pathValue);
			base.put("HTTPS_PROXY", "http://proxy.example:8080");
			base.put("CYPRESS_INSTALL_BINARY", "0");
			base.put("SOMETHING_ELSE", "keep-me");
			return base;
		}

		@Test
		@DisplayName("sets CYPRESS_CACHE_FOLDER and NODE_PATH")
		void setsCacheAndNodePath() {
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", "/usr/bin"),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			assertAll(() -> assertEquals(cache.toString(), env.get("CYPRESS_CACHE_FOLDER")),
					() -> assertEquals(nodeModules.toString(), env.get("NODE_PATH")));
		}

		@Test
		@DisplayName("PATH order: node dir, pnpm dir, node_modules/.bin, original PATH")
		void pathOrderingWithPnpm() {
			String original = "/usr/bin" + File.pathSeparator + "/bin";
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", original),
					new NodeTools(nodeFile, npmFile, pnpmFile, true), projectDir, nodeModules, cache);

			String expected = String.join(File.pathSeparator, nodeFile.getAbsoluteFile().getParentFile().getPath(),
					pnpmFile.getAbsoluteFile().getParentFile().getPath(),
					projectDir.resolve("node_modules").resolve(".bin").toString(), original);
			assertEquals(expected, env.get("PATH"));
		}

		@Test
		@DisplayName("pnpm dir is not duplicated when it equals the node dir")
		void pnpmDirSameAsNodeDirIsNotRepeated() {
			File pnpmNextToNode = new File(nodeFile.getParentFile(), "pnpm.exe");
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", "/usr/bin"),
					new NodeTools(nodeFile, npmFile, pnpmNextToNode, true), projectDir, nodeModules, cache);

			String nodeDir = nodeFile.getAbsoluteFile().getParentFile().getPath();
			String expected = String.join(File.pathSeparator, nodeDir,
					projectDir.resolve("node_modules").resolve(".bin").toString(), "/usr/bin");
			assertEquals(expected, env.get("PATH"));
		}

		@Test
		@DisplayName("PATH without a pnpm entry in npm mode: node dir, .bin, original")
		void pathOrderingNpmMode() {
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", "/usr/bin"),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			String expected = String.join(File.pathSeparator, nodeFile.getAbsoluteFile().getParentFile().getPath(),
					projectDir.resolve("node_modules").resolve(".bin").toString(), "/usr/bin");
			assertEquals(expected, env.get("PATH"));
		}

		@Test
		@DisplayName("keeps the existing 'Path' key spelling (Windows) and does not add a second PATH key")
		void keepsWindowsPathKeySpelling() {
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("Path", "C:\\Windows"),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			assertAll(() -> assertTrue(env.containsKey("Path")),
					() -> assertFalse(env.containsKey("PATH"), "must not add a duplicate PATH key"),
					() -> assertTrue(env.get("Path").startsWith(nodeFile.getAbsoluteFile().getParentFile().getPath())),
					() -> assertTrue(env.get("Path").endsWith("C:\\Windows")));
		}

		@Test
		@DisplayName("keeps an upper-case 'PATH' key")
		void keepsUpperCasePathKey() {
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", "/usr/bin"),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			assertAll(() -> assertTrue(env.containsKey("PATH")), () -> assertFalse(env.containsKey("Path")));
		}

		@Test
		@DisplayName("creates PATH from scratch when the base has none")
		void createsPathWhenMissing() {
			Map<String, String> env = CypressToolchain.buildEnvironment(new LinkedHashMap<>(),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			String expected = String.join(File.pathSeparator, nodeFile.getAbsoluteFile().getParentFile().getPath(),
					projectDir.resolve("node_modules").resolve(".bin").toString());
			assertEquals(expected, env.get("PATH"));
		}

		@Test
		@DisplayName("other variables (proxy, CYPRESS_INSTALL_BINARY) are inherited untouched")
		void otherVariablesUntouched() {
			Map<String, String> env = CypressToolchain.buildEnvironment(baseEnv("PATH", "/usr/bin"),
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			assertAll(() -> assertEquals("http://proxy.example:8080", env.get("HTTPS_PROXY")),
					() -> assertEquals("0", env.get("CYPRESS_INSTALL_BINARY")),
					() -> assertEquals("keep-me", env.get("SOMETHING_ELSE")));
		}

		@Test
		@DisplayName("a pre-existing NODE_PATH / CYPRESS_CACHE_FOLDER in base is overridden")
		void overridesExistingNodePathAndCache() {
			Map<String, String> base = baseEnv("PATH", "/usr/bin");
			base.put("NODE_PATH", "/old/node_modules");
			base.put("CYPRESS_CACHE_FOLDER", "/old/cache");

			Map<String, String> env = CypressToolchain.buildEnvironment(base,
					new NodeTools(nodeFile, npmFile, null, false), projectDir, nodeModules, cache);

			assertAll(() -> assertEquals(nodeModules.toString(), env.get("NODE_PATH")),
					() -> assertEquals(cache.toString(), env.get("CYPRESS_CACHE_FOLDER")));
		}

		@Test
		@DisplayName("returns a copy and does not mutate the input map")
		void doesNotMutateInput() {
			Map<String, String> base = baseEnv("PATH", "/usr/bin");
			Map<String, String> snapshot = new LinkedHashMap<>(base);

			Map<String, String> env = CypressToolchain.buildEnvironment(base,
					new NodeTools(nodeFile, npmFile, pnpmFile, true), projectDir, nodeModules, cache);

			assertAll(() -> assertNotSame(base, env), () -> assertEquals(snapshot, base, "input map must be unchanged"),
					() -> assertFalse(base.containsKey("NODE_PATH")),
					() -> assertFalse(base.containsKey("CYPRESS_CACHE_FOLDER")));
		}
	}

	@Nested
	class FormatFailure {
		private final List<String> command = List.of("node", "cypress", "install");

		@Test
		@DisplayName("includes step, exit code, command and cache folder")
		void includesBasics() {
			Path cache = tmp.resolve("cypress-cache");

			String msg = CypressToolchain.formatFailure("Cypress binary install", command, 1, cache, "boom", Map.of());

			assertAll(() -> assertTrue(msg.startsWith("Error: "), "must keep the Error: prefix contract"),
					() -> assertTrue(msg.contains("Cypress binary install")),
					() -> assertTrue(msg.contains("exit code 1")),
					() -> assertTrue(msg.contains("node cypress install")),
					() -> assertTrue(msg.contains(cache.toString())), () -> assertTrue(msg.contains("boom")));
		}

		@Test
		@DisplayName("keeps only the last 60 lines of output")
		void keepsOnlyTailOfOutput() {
			StringBuilder out = new StringBuilder();
			for (int i = 1; i <= 100; i++) {
				out.append("line-").append(i).append('\n');
			}

			String msg = CypressToolchain.formatFailure("step", command, 2, tmp, out.toString(), Map.of());

			assertAll(() -> assertTrue(msg.contains("line-100\n")),
					() -> assertTrue(msg.contains("line-41\n"), "line 41 is the first of the last 60"),
					() -> assertFalse(msg.contains("line-40\n"), "line 40 is outside the tail"),
					() -> assertFalse(msg.contains("line-1\n")));
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = { "   ", "\n\n" })
		@DisplayName("null or blank output yields no output section and no hint")
		void blankOutput(String output) {
			String msg = CypressToolchain.formatFailure("step", command, 3, tmp, output, Map.of());

			assertAll(() -> assertFalse(msg.contains("Output (last lines)")), () -> assertFalse(msg.contains("Hint:")));
		}

		@ParameterizedTest
		@ValueSource(strings = { "connect ECONNREFUSED 1.2.3.4:443", "request to download.cypress.io failed: ETIMEDOUT",
				"getaddrinfo ENOTFOUND download.cypress.io", "Error: socket hang up",
				"self signed certificate in certificate chain", "Proxy authentication required",
				"There was a network error" })
		@DisplayName("network or proxy error output adds the proxy hint")
		void networkOutputAddsProxyHint(String output) {
			String msg = CypressToolchain.formatFailure("step", command, 1, tmp, output, Map.of());

			assertTrue(msg.contains("proxy"), "expected a proxy hint for: " + output + "\n" + msg);
			assertTrue(msg.contains("Hint:"));
		}

		@Test
		@DisplayName("unrelated output does not add the proxy hint")
		void unrelatedOutputHasNoProxyHint() {
			String msg = CypressToolchain.formatFailure("step", command, 1, tmp, "SyntaxError: unexpected token",
					Map.of());

			assertFalse(msg.contains("Hint:"));
		}

		@Test
		@DisplayName("CYPRESS_INSTALL_BINARY=0 adds a hint that the download is skipped")
		void installBinaryZeroHint() {
			String msg = CypressToolchain.formatFailure("step", command, 1, tmp, "The cypress npm package is installed",
					Map.of("CYPRESS_INSTALL_BINARY", "0"));

			assertTrue(msg.contains("CYPRESS_INSTALL_BINARY=0"));
			assertTrue(msg.contains("skips the Cypress binary download"));
		}

		@Test
		@DisplayName("CYPRESS_INSTALL_BINARY other than 0 adds no hint")
		void installBinaryOtherValueNoHint() {
			String msg = CypressToolchain.formatFailure("step", command, 1, tmp, "oops",
					Map.of("CYPRESS_INSTALL_BINARY", "1"));

			assertFalse(msg.contains("CYPRESS_INSTALL_BINARY"));
		}
	}
}
