package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ngclient.ui.PnpmBuildApprovals;
import com.servoy.eclipse.ngclient.ui.PnpmBuildScriptConfig;

/**
 * Tests for {@link PnpmBuildScriptConfig} (SVY-21502): the pnpm-12
 * version-scoped {@code allowBuilds} YAML
 * generation ({@code writeWorkspaceYaml}) and the two isolated versioned
 * parsers - the
 * {@code node_modules/.modules.yaml} {@code ignoredBuilds} reader
 * ({@code parseModulesYamlIgnoredBuilds(String)})
 * and the {@code ERR_PNPM_IGNORED_BUILDS} error-line fallback
 * ({@code parseIgnoredBuildsErrorLine(List)}).
 * <p>
 * {@code writeWorkspaceYaml} is public and SWT-free, so it is exercised
 * directly against a temp folder (manually
 * managed, matching {@code AngularLocalesCopyTest}, since the fragment does not
 * import
 * {@code org.junit.jupiter.api.io}). The two parsers are package-private
 * statics and are called directly - this
 * test lives in the same package. The discovery query
 * ({@code discoverPendingBuildScripts}) reads a real file but
 * delegates to the parser, the lockfile seed/commit-back and the ordered
 * developer/WAR flows need a live workspace
 * and are integration-level - out of scope for this unit test.
 * </p>
 */
class PnpmBuildScriptConfigTest {
	private Path tempDir;
	private File projectFolder;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("pnpm-config-test");
		projectFolder = tempDir.toFile();
	}

	@AfterEach
	void tearDown() throws IOException {
		try (Stream<Path> paths = Files.walk(tempDir)) {
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	private String generatedYaml() throws IOException {
		File yaml = new File(projectFolder, PnpmBuildScriptConfig.PNPM_WORKSPACE_YAML);
		assertTrue(yaml.exists(), "writeWorkspaceYaml should have created pnpm-workspace.yaml");
		return Files.readString(yaml.toPath(), StandardCharsets.UTF_8);
	}

	@Nested
	class WriteWorkspaceYaml {
		@Test
		@DisplayName("keeps nodeLinker: hoisted and shamefullyHoist: true and sets strictDepBuilds: true")
		void keepsLinkerKeysAndStrictTrue() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("nodeLinker: hoisted"), "keeps nodeLinker: hoisted"),
					() -> assertTrue(yaml.contains("shamefullyHoist: true"), "keeps shamefullyHoist: true"),
					() -> assertTrue(yaml.contains("strictDepBuilds: true"), "sets strictDepBuilds: true"),
					() -> assertFalse(yaml.contains("strictDepBuilds: false"),
							"no leftover strictDepBuilds: false stopgap"));
		}

		@Test
		@DisplayName("allowed name@version becomes a single-quoted allowBuilds '<name@version>': true entry")
		void allowedBecomesTrue() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("allowBuilds:"), "has the allowBuilds map header"),
					() -> assertTrue(yaml.contains("'esbuild@0.28.1': true"),
							"allowed name@version maps to true, single-quoted"));
		}

		@Test
		@DisplayName("denied name@version becomes a single-quoted allowBuilds '<name@version>': false entry")
		void deniedBecomesFalse() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.deny("foo@1.2.3");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("allowBuilds:"), "has the allowBuilds map header"),
					() -> assertTrue(yaml.contains("'foo@1.2.3': false"),
							"denied name@version maps to false, single-quoted"));
		}

		@Test
		@DisplayName("two versions of the same package are both emitted as distinct allowBuilds entries")
		void twoVersionsBothEmitted() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.allow("esbuild@0.28.2");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("'esbuild@0.28.1': true"), "first version present"),
					() -> assertTrue(yaml.contains("'esbuild@0.28.2': true"), "second version present"));
		}

		@Test
		@DisplayName("both lists are rendered: allowed -> true and denied -> false in the same allowBuilds map")
		void bothListsRendered() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("core-js@3.38.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("'esbuild@0.28.1': true")),
					() -> assertTrue(yaml.contains("'core-js@3.38.1': false")));
		}

		@Test
		@DisplayName("a scoped name@version key is single-quoted as a YAML key")
		void scopedNameQuoted() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("@parcel/watcher@2.5.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertTrue(yaml.contains("'@parcel/watcher@2.5.1': true"), "scoped name@version is single-quoted");
		}

		@Test
		@DisplayName("empty both lists omits the allowBuilds block but still writes valid linker/strict keys")
		void emptyBothListsOmitsAllowBuilds() throws IOException {
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, new PnpmBuildApprovals(), null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertFalse(yaml.contains("allowBuilds"), "no allowBuilds block when both lists are empty"),
					() -> assertTrue(yaml.contains("nodeLinker: hoisted")),
					() -> assertTrue(yaml.contains("shamefullyHoist: true")),
					() -> assertTrue(yaml.contains("strictDepBuilds: true")));
		}

		@Test
		@DisplayName("never emits the removed pnpm-10 onlyBuiltDependencies/ignoredBuiltDependencies keys")
		void noPnpm10Keys() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("core-js@3.38.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, approvals, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertFalse(yaml.contains("onlyBuiltDependencies"), "no removed onlyBuiltDependencies key"),
					() -> assertFalse(yaml.contains("ignoredBuiltDependencies"),
							"no removed ignoredBuiltDependencies key"));
		}

		@Test
		@DisplayName("regenerating overwrites: the second call reflects the new lists, not the first")
		void regenerateIsIdempotentOverwrite() throws IOException {
			PnpmBuildApprovals first = new PnpmBuildApprovals();
			first.allow("esbuild@0.28.1");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, first, null);

			PnpmBuildApprovals second = new PnpmBuildApprovals();
			second.allow("lmdb@3.5.6");
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, second, null);
			String yaml = generatedYaml();
			assertAll(
					() -> assertTrue(yaml.contains("'lmdb@3.5.6': true"), "reflects the second generation"),
					() -> assertFalse(yaml.contains("esbuild"), "no stale entry from the first generation"));
		}

		@Test
		@DisplayName("a null project folder or null approvals is a safe no-op (no file written)")
		void nullArgsNoOp() {
			PnpmBuildScriptConfig.writeWorkspaceYaml(null, new PnpmBuildApprovals(), null);
			PnpmBuildScriptConfig.writeWorkspaceYaml(projectFolder, null, null);
			assertFalse(new File(projectFolder, PnpmBuildScriptConfig.PNPM_WORKSPACE_YAML).exists(),
					"no yaml should be written for null arguments");
		}
	}

	@Nested
	class ParseModulesYamlIgnoredBuilds {
		private static Method parseMethod;

		static {
			try {
				parseMethod = PnpmBuildScriptConfig.class.getDeclaredMethod("parseModulesYamlIgnoredBuilds",
						String.class);
				parseMethod.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		@SuppressWarnings("unchecked")
		private static Set<String> parse(String content) {
			try {
				return (Set<String>) parseMethod.invoke(null, content);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException re)
					throw re;
				if (cause instanceof Error err)
					throw err;
				throw new RuntimeException(cause);
			}
		}

		@Test
		@DisplayName("real pnpm 12 single-line flow-array form: collects the versioned entries, order preserved")
		void parsesFlowArrayForm() {
			String content = "hoistPattern:\n" +
					"  - '*'\n" +
					"ignoredBuilds: [\"esbuild@0.28.1\", \"esbuild@0.28.2\", \"lmdb@3.5.6\"]\n" +
					"pendingBuilds: []\n";
			assertIterableEquals(List.of("esbuild@0.28.1", "esbuild@0.28.2", "lmdb@3.5.6"), parse(content));
		}

		@Test
		@DisplayName("pendingBuilds entries are NOT collected (only ignoredBuilds)")
		void pendingBuildsNotCollected() {
			String content = "ignoredBuilds: [\"esbuild@0.28.1\"]\n" +
					"pendingBuilds: [\"lmdb@3.5.6\"]\n";
			assertIterableEquals(List.of("esbuild@0.28.1"), parse(content));
		}

		@Test
		@DisplayName("multi-line flow array that spills over lines is collected whole, stopping at the closing bracket")
		void parsesMultiLineFlowArray() {
			String content = "ignoredBuilds: [\n" +
					"  \"esbuild@0.28.1\",\n" +
					"  \"esbuild@0.28.2\",\n" +
					"  \"lmdb@3.5.6\"\n" +
					"]\n" +
					"pendingBuilds: []\n";
			assertIterableEquals(List.of("esbuild@0.28.1", "esbuild@0.28.2", "lmdb@3.5.6"), parse(content));
		}

		@Test
		@DisplayName("block-style YAML list form (- name@version) is collected")
		void parsesBlockListForm() {
			String content = "ignoredBuilds:\n" +
					"  - esbuild@0.28.1\n" +
					"  - lmdb@3.5.6\n" +
					"pendingBuilds:\n" +
					"  - should-not-be-collected@9.9.9\n";
			assertIterableEquals(List.of("esbuild@0.28.1", "lmdb@3.5.6"), parse(content));
		}

		@Test
		@DisplayName("a scoped name@version (@parcel/watcher@2.5.1) is collected")
		void parsesScopedNameAtVersion() {
			String content = "ignoredBuilds: [\"@parcel/watcher@2.5.1\", \"esbuild@0.28.1\"]\n";
			assertIterableEquals(List.of("@parcel/watcher@2.5.1", "esbuild@0.28.1"), parse(content));
		}

		@Test
		@DisplayName("collection stops at the next top-level key: a following publicHoistPattern array is not bled in")
		void stopsAtNextKey() {
			String content = "ignoredBuilds:\n" +
					"  - esbuild@0.28.1\n" +
					"publicHoistPattern:\n" +
					"  - '*eslint*@1.0.0'\n" +
					"skipped:\n" +
					"  - other@2.0.0\n";
			assertIterableEquals(List.of("esbuild@0.28.1"), parse(content));
		}

		@Test
		@DisplayName("real pnpm 12 JSON .modules.yaml with a QUOTED \"ignoredBuilds\" key is parsed")
		void parsesRealPnpm12JsonForm() {
			// the exact shape the bundled pnpm 12.7.0 writes: a JSON document with quoted
			// keys. The old parser only
			// matched a bare 'ignoredBuilds:' key, so it found nothing here (empty
			// discovery, no approve prompt) -
			// this is the regression guard for that bug.
			String content = "{\n" +
					"  \"hoistPattern\": [\n" +
					"    \"*\"\n" +
					"  ],\n" +
					"  \"included\": {},\n" +
					"  \"pendingBuilds\": [],\n" +
					"  \"ignoredBuilds\": [\n" +
					"    \"@parcel/watcher@2.6.0\",\n" +
					"    \"esbuild@0.28.1\",\n" +
					"    \"esbuild@0.28.2\",\n" +
					"    \"lmdb@3.5.6\",\n" +
					"    \"msgpackr-extract@3.0.4\"\n" +
					"  ],\n" +
					"  \"publicHoistPattern\": [\n" +
					"    \"*\"\n" +
					"  ]\n" +
					"}\n";
			assertIterableEquals(
					List.of("@parcel/watcher@2.6.0", "esbuild@0.28.1", "esbuild@0.28.2", "lmdb@3.5.6",
							"msgpackr-extract@3.0.4"),
					parse(content));
		}

		@Test
		@DisplayName("a quoted \"pendingBuilds\" key is NOT mistaken for ignoredBuilds (JSON form)")
		void quotedPendingBuildsNotCollected() {
			String content = "{\n" +
					"  \"pendingBuilds\": [\"should-not@1.0.0\"],\n" +
					"  \"ignoredBuilds\": [\"esbuild@0.28.1\"]\n" +
					"}\n";
			assertIterableEquals(List.of("esbuild@0.28.1"), parse(content));
		}

		@Test
		@DisplayName("an empty ignoredBuilds array yields an empty set")
		void emptyArrayYieldsEmpty() {
			assertTrue(parse("ignoredBuilds: []\npendingBuilds: []\n").isEmpty());
		}

		@Test
		@DisplayName("an absent ignoredBuilds key yields an empty set")
		void absentKeyYieldsEmpty() {
			String content = "hoistPattern:\n  - '*'\npendingBuilds: []\n";
			assertTrue(parse(content).isEmpty());
		}

		@Test
		@DisplayName("a bare name without @version is rejected (version-scoping guard)")
		void bareNameRejected() {
			String content = "ignoredBuilds: [\"esbuild\", \"lmdb@3.5.6\"]\n";
			assertIterableEquals(List.of("lmdb@3.5.6"), parse(content));
		}

		@Test
		@DisplayName("garbage, empty and null content each yield an empty (non-null) set")
		void garbageEmptyNullYieldEmpty() {
			assertAll(
					() -> assertTrue(parse(null).isEmpty(), "null -> empty"),
					() -> assertTrue(parse("").isEmpty(), "empty -> empty"),
					() -> assertTrue(parse("   \n\t\n").isEmpty(), "blank -> empty"),
					() -> assertTrue(parse("just some unrelated prose\nwith no ignoredBuilds key").isEmpty(),
							"garbage -> empty"));
		}
	}

	@Nested
	class ParseErrorLine {
		private static Method parseMethod;

		static {
			try {
				parseMethod = PnpmBuildScriptConfig.class.getDeclaredMethod("parseIgnoredBuildsErrorLine", List.class);
				parseMethod.setAccessible(true);
			} catch (NoSuchMethodException e) {
				throw new ExceptionInInitializerError(e);
			}
		}

		@SuppressWarnings("unchecked")
		private static Set<String> parse(List<String> lines) {
			try {
				return (Set<String>) parseMethod.invoke(null, lines);
			} catch (IllegalAccessException | IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				Throwable cause = e.getCause();
				if (cause instanceof RuntimeException re)
					throw re;
				if (cause instanceof Error err)
					throw err;
				throw new RuntimeException(cause);
			}
		}

		@Test
		@DisplayName("the versioned 'Ignored build scripts: a@1, b@2' line parses into the two name@version entries")
		void parsesIgnoredBuildScriptsLine() {
			List<String> output = List.of("Ignored build scripts: esbuild@0.28.1, esbuild@0.28.2, lmdb@3.5.6");
			assertIterableEquals(List.of("esbuild@0.28.1", "esbuild@0.28.2", "lmdb@3.5.6"), parse(output));
		}

		@Test
		@DisplayName("output with no such line yields an empty set")
		void noMarkerYieldsEmpty() {
			List<String> output = List.of("Lockfile is up to date\nAlready up to date\nDone in 300ms");
			assertTrue(parse(output).isEmpty());
		}

		@Test
		@DisplayName("null and empty input each yield an empty (non-null) set")
		void nullAndEmptyYieldEmpty() {
			assertAll(
					() -> assertTrue(parse(null).isEmpty(), "null -> empty"),
					() -> assertTrue(parse(List.of()).isEmpty(), "empty -> empty"));
		}
	}
}
