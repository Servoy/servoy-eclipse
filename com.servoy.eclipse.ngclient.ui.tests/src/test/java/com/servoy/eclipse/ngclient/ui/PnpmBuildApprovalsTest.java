package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PnpmBuildApprovals} (SVY-21502): the SWT-free model and
 * committed-file IO for
 * {@code ng_web_packages/pnpm-build-approvals.json}.
 * <p>
 * The allowed/denied lists hold <b>exact {@code name@version} entries</b> (the
 * JSON shape is unchanged:
 * {@code {version, allowed[], denied[]}}). These exercise the public API
 * directly: parse/serialize round-trip,
 * allowed/denied/pending sets, the allow/deny/remove mutations, the
 * missing/empty/old-version file handling and -
 * the headline version-scoping behavior - that a NEW version of an
 * already-decided package is pending again (and
 * that multiple versions of one package coexist in a list). No OSGi runtime is
 * needed; the file IO uses a
 * manually-managed temp directory (matching {@code AngularLocalesCopyTest})
 * because the test fragment does not
 * import {@code org.junit.jupiter.api.io}. All happy-path IO uses valid,
 * writable files.
 * </p>
 */
class PnpmBuildApprovalsTest {
	private Path tempDir;
	private File folder;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("pnpm-approvals-test");
		folder = tempDir.toFile();
	}

	@AfterEach
	void tearDown() throws IOException {
		try (Stream<Path> paths = Files.walk(tempDir)) {
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	private File approvalsFile() {
		return new File(folder, PnpmBuildApprovals.APPROVALS_FILE_NAME);
	}

	@Nested
	class EmptyAndDefaults {
		@Test
		@DisplayName("a fresh PnpmBuildApprovals has no allowed, no denied and the current version")
		void freshApprovalsAreEmpty() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty(), "allowed should start empty"),
					() -> assertTrue(approvals.getDenied().isEmpty(), "denied should start empty"),
					() -> assertEquals(PnpmBuildApprovals.CURRENT_VERSION, approvals.getVersion()));
		}

		@Test
		@DisplayName("fromFile(null) yields empty (but valid) approvals")
		void fromNullFileYieldsEmpty() {
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(null);
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty()),
					() -> assertTrue(approvals.getDenied().isEmpty()),
					() -> assertEquals(PnpmBuildApprovals.CURRENT_VERSION, approvals.getVersion()));
		}

		@Test
		@DisplayName("a missing file yields empty approvals (a build with no committed decisions is well-defined)")
		void missingFileYieldsEmpty() {
			File missing = new File(folder, "does-not-exist.json");
			assertFalse(missing.exists());
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(missing);
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty()),
					() -> assertTrue(approvals.getDenied().isEmpty()));
		}

		@Test
		@DisplayName("an empty file yields empty approvals")
		void emptyFileYieldsEmpty() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty()),
					() -> assertTrue(approvals.getDenied().isEmpty()));
		}

		@Test
		@DisplayName("a blank (whitespace-only) file yields empty approvals")
		void blankFileYieldsEmpty() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "   \n\t  \n");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty()),
					() -> assertTrue(approvals.getDenied().isEmpty()));
		}
	}

	@Nested
	class Parse {
		@Test
		@DisplayName("parses both allowed and denied lists of name@version and the version from a committed file")
		void parsesBothLists() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(),
					"{\n  \"version\": 1,\n  \"allowed\": [\"esbuild@0.28.1\", \"@parcel/watcher@2.5.1\"],\n" +
							"  \"denied\": [\"core-js@3.38.1\"]\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertEquals(1, approvals.getVersion()),
					() -> assertIterableEquals(List.of("esbuild@0.28.1", "@parcel/watcher@2.5.1"),
							approvals.getAllowed()),
					() -> assertIterableEquals(List.of("core-js@3.38.1"), approvals.getDenied()));
		}

		@Test
		@DisplayName("a missing version key defaults to the current version (forward/backward compatible)")
		void missingVersionDefaultsToCurrent() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "{\n  \"allowed\": [\"esbuild@0.28.1\"],\n  \"denied\": []\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertEquals(PnpmBuildApprovals.CURRENT_VERSION, approvals.getVersion()),
					() -> assertIterableEquals(List.of("esbuild@0.28.1"), approvals.getAllowed()));
		}

		@Test
		@DisplayName("an explicit older version is preserved as read (not silently upgraded)")
		void olderVersionPreserved() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "{\n  \"version\": 0,\n  \"allowed\": [\"esbuild@0.28.1\"]\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertEquals(0, approvals.getVersion());
		}

		@Test
		@DisplayName("missing allowed/denied arrays are treated as empty lists")
		void missingArraysAreEmpty() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "{\n  \"version\": 1\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertTrue(approvals.getAllowed().isEmpty()),
					() -> assertTrue(approvals.getDenied().isEmpty()));
		}

		@Test
		@DisplayName("blank/empty entries in the arrays are skipped and names are trimmed")
		void blankEntriesSkippedAndTrimmed() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(),
					"{\n  \"allowed\": [\"  esbuild@0.28.1  \", \"\", \"   \"],\n  \"denied\": [\"core-js@3.38.1\"]\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertIterableEquals(List.of("esbuild@0.28.1"), approvals.getAllowed()),
					() -> assertIterableEquals(List.of("core-js@3.38.1"), approvals.getDenied()));
		}

		@Test
		@DisplayName("scoped name@version entries survive the round-trip intact")
		void scopedNamesParsed() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(), "{\n  \"allowed\": [\"@parcel/watcher@2.5.1\", \"@servoy/foo@1.0.0\"]\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertIterableEquals(List.of("@parcel/watcher@2.5.1", "@servoy/foo@1.0.0"), approvals.getAllowed());
		}

		@Test
		@DisplayName("multiple versions of the same package coexist in a list")
		void multipleVersionsOfSamePackageCoexist() throws IOException {
			File file = approvalsFile();
			Files.writeString(file.toPath(),
					"{\n  \"allowed\": [\"esbuild@0.28.1\", \"esbuild@0.28.2\"]\n}");
			PnpmBuildApprovals approvals = PnpmBuildApprovals.fromFile(file);
			assertIterableEquals(List.of("esbuild@0.28.1", "esbuild@0.28.2"), approvals.getAllowed());
		}
	}

	@Nested
	class RoundTrip {
		@Test
		@DisplayName("save then fromFile preserves version, allowed and denied name@version entries in order")
		void saveThenLoadRoundTrips() throws IOException {
			PnpmBuildApprovals original = new PnpmBuildApprovals();
			original.allow("esbuild@0.28.1");
			original.allow("@parcel/watcher@2.5.1");
			original.deny("core-js@3.38.1");
			File file = approvalsFile();
			original.save(file);
			assertTrue(file.exists(), "save should create the file");

			PnpmBuildApprovals reloaded = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertEquals(original.getVersion(), reloaded.getVersion()),
					() -> assertIterableEquals(List.of("esbuild@0.28.1", "@parcel/watcher@2.5.1"),
							reloaded.getAllowed()),
					() -> assertIterableEquals(List.of("core-js@3.38.1"), reloaded.getDenied()));
		}

		@Test
		@DisplayName("multiple versions of one package round-trip as distinct entries, order preserved")
		void multipleVersionsRoundTrip() throws IOException {
			PnpmBuildApprovals original = new PnpmBuildApprovals();
			original.allow("esbuild@0.28.1");
			original.allow("esbuild@0.28.2");
			File file = approvalsFile();
			original.save(file);
			PnpmBuildApprovals reloaded = PnpmBuildApprovals.fromFile(file);
			assertIterableEquals(List.of("esbuild@0.28.1", "esbuild@0.28.2"), reloaded.getAllowed());
		}

		@Test
		@DisplayName("save writes a hand-reviewable JSON document with the version and both list keys")
		void savedContentIsReadableJson() throws IOException {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			File file = approvalsFile();
			approvals.save(file);
			String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
			assertAll(
					() -> assertTrue(content.contains("\"version\""), "should contain version key"),
					() -> assertTrue(content.contains("\"allowed\""), "should contain allowed key"),
					() -> assertTrue(content.contains("\"denied\""), "should contain denied key"),
					() -> assertTrue(content.contains("esbuild@0.28.1"), "should contain the allowed name@version"));
		}

		@Test
		@DisplayName("save creates the parent directory when it does not exist")
		void saveCreatesParentDir() throws IOException {
			File nested = new File(folder, "ng_web_packages/" + PnpmBuildApprovals.APPROVALS_FILE_NAME);
			assertFalse(nested.getParentFile().exists());
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.save(nested);
			assertAll(
					() -> assertTrue(nested.exists()),
					() -> assertIterableEquals(List.of("esbuild@0.28.1"),
							PnpmBuildApprovals.fromFile(nested).getAllowed()));
		}

		@Test
		@DisplayName("an empty approvals round-trips to empty allowed/denied lists")
		void emptyRoundTrips() throws IOException {
			File file = approvalsFile();
			new PnpmBuildApprovals().save(file);
			PnpmBuildApprovals reloaded = PnpmBuildApprovals.fromFile(file);
			assertAll(
					() -> assertTrue(reloaded.getAllowed().isEmpty()),
					() -> assertTrue(reloaded.getDenied().isEmpty()));
		}
	}

	@Nested
	class Mutations {
		@Test
		@DisplayName("allow adds a name@version to the allowed list")
		void allowAdds() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			assertAll(
					() -> assertTrue(approvals.getAllowed().contains("esbuild@0.28.1")),
					() -> assertFalse(approvals.getDenied().contains("esbuild@0.28.1")));
		}

		@Test
		@DisplayName("deny adds a name@version to the denied list")
		void denyAdds() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.deny("core-js@3.38.1");
			assertAll(
					() -> assertTrue(approvals.getDenied().contains("core-js@3.38.1")),
					() -> assertFalse(approvals.getAllowed().contains("core-js@3.38.1")));
		}

		@Test
		@DisplayName("allow and deny are mutually exclusive for the same name@version: deny of an allowed one moves it")
		void denyMovesFromAllowed() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("esbuild@0.28.1");
			assertAll(
					() -> assertFalse(approvals.getAllowed().contains("esbuild@0.28.1"), "no longer allowed"),
					() -> assertTrue(approvals.getDenied().contains("esbuild@0.28.1"), "now denied"));
		}

		@Test
		@DisplayName("allow and deny are mutually exclusive for the same name@version: allow of a denied one moves it")
		void allowMovesFromDenied() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.deny("esbuild@0.28.1");
			approvals.allow("esbuild@0.28.1");
			assertAll(
					() -> assertTrue(approvals.getAllowed().contains("esbuild@0.28.1"), "now allowed"),
					() -> assertFalse(approvals.getDenied().contains("esbuild@0.28.1"), "no longer denied"));
		}

		@Test
		@DisplayName("mutual exclusivity is per exact version: denying one version leaves another allowed")
		void mutualExclusivityIsPerVersion() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.allow("esbuild@0.28.2");
			approvals.deny("esbuild@0.28.2");
			assertAll(
					() -> assertIterableEquals(List.of("esbuild@0.28.1"), approvals.getAllowed()),
					() -> assertIterableEquals(List.of("esbuild@0.28.2"), approvals.getDenied()));
		}

		@Test
		@DisplayName("remove takes a name@version out of both lists (making it pending again)")
		void removeClearsBothLists() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("core-js@3.38.1");
			approvals.remove("esbuild@0.28.1");
			approvals.remove("core-js@3.38.1");
			assertAll(
					() -> assertFalse(approvals.getAllowed().contains("esbuild@0.28.1")),
					() -> assertFalse(approvals.getDenied().contains("core-js@3.38.1")));
		}

		@Test
		@DisplayName("allow/deny trim the name@version and ignore null/blank input")
		void allowDenyTrimAndIgnoreBlank() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("  esbuild@0.28.1  ");
			approvals.allow(null);
			approvals.allow("   ");
			approvals.deny(null);
			assertAll(
					() -> assertIterableEquals(List.of("esbuild@0.28.1"), approvals.getAllowed()),
					() -> assertTrue(approvals.getDenied().isEmpty()));
		}

		@Test
		@DisplayName("getAllowed/getDenied return defensive copies that do not mutate the model")
		void gettersReturnDefensiveCopies() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			Set<String> allowed = approvals.getAllowed();
			allowed.add("injected@9.9.9");
			assertFalse(approvals.getAllowed().contains("injected@9.9.9"),
					"mutating the returned set must not affect the model");
		}
	}

	@Nested
	class Pending {
		@Test
		@DisplayName("pending = discovered minus allowed minus denied, by exact name@version")
		void pendingSubtractsBothLists() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("core-js@3.38.1");
			Set<String> pending = approvals.pending(new LinkedHashSet<>(
					List.of("esbuild@0.28.1", "core-js@3.38.1", "@parcel/watcher@2.5.1", "lmdb@3.5.6")));
			assertIterableEquals(List.of("@parcel/watcher@2.5.1", "lmdb@3.5.6"), pending);
		}

		@Test
		@DisplayName("a NEW version of an already-approved package is pending / re-asked (headline version-scoping)")
		void newVersionOfApprovedPackageIsPending() {
			// esbuild@0.28.1 and @0.28.2 are already allowed; the install now surfaces a
			// third version @0.28.3 that
			// the developer never reviewed. It must be pending (re-asked) - a
			// published-but-unreviewed version cannot
			// inherit a prior version's trust.
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.allow("esbuild@0.28.2");
			Set<String> pending = approvals.pending(new LinkedHashSet<>(
					List.of("esbuild@0.28.1", "esbuild@0.28.2", "esbuild@0.28.3")));
			assertIterableEquals(List.of("esbuild@0.28.3"), pending);
		}

		@Test
		@DisplayName("pending preserves the discovered order and trims names")
		void pendingPreservesDiscoveredOrder() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			Set<String> pending = approvals
					.pending(new LinkedHashSet<>(List.of("  zeta@2.0.0  ", "alpha@1.0.0", "mu@3.0.0")));
			assertIterableEquals(List.of("zeta@2.0.0", "alpha@1.0.0", "mu@3.0.0"), pending);
		}

		@Test
		@DisplayName("a copy/clean build does not re-ask: everything discovered is already decided -> empty pending")
		void nothingPendingWhenAllDecided() {
			PnpmBuildApprovals approvals = new PnpmBuildApprovals();
			approvals.allow("esbuild@0.28.1");
			approvals.deny("core-js@3.38.1");
			Set<String> pending = approvals.pending(new LinkedHashSet<>(List.of("esbuild@0.28.1", "core-js@3.38.1")));
			assertTrue(pending.isEmpty());
		}

		@Test
		@DisplayName("pending(null) is an empty (never null) set")
		void pendingNullYieldsEmpty() {
			Set<String> pending = new PnpmBuildApprovals().pending(null);
			assertAll(
					() -> assertTrue(pending != null, "must not be null"),
					() -> assertTrue(pending.isEmpty()));
		}

		@Test
		@DisplayName("pending skips null/blank discovered entries")
		void pendingSkipsBlankDiscovered() {
			Set<String> discovered = new LinkedHashSet<>();
			discovered.add("esbuild@0.28.1");
			discovered.add("");
			discovered.add("   ");
			Set<String> pending = new PnpmBuildApprovals().pending(discovered);
			assertIterableEquals(List.of("esbuild@0.28.1"), pending);
		}
	}
}
