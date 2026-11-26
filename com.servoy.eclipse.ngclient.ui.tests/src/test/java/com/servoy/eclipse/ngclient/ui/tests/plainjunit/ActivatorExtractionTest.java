package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.ILog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives.TarItem;

class ActivatorExtractionTest {
	@TempDir
	Path root;

	private File baseDir;
	private ILog log;

	@BeforeEach
	void setUp() throws IOException {
		baseDir = Files.createDirectory(root.resolve("state")).toFile();
		log = mock(ILog.class);
	}

	private URL zipArchive(String name, String... nameAndContent) throws IOException {
		Path file = root.resolve(name);
		Files.write(file, TestArchives.zip(nameAndContent));
		return file.toUri().toURL();
	}

	private URL tarGzArchive(String name, TarItem... items) throws IOException {
		Path file = root.resolve(name);
		Files.write(file, TestArchives.gzip(TestArchives.tar(items)));
		return file.toUri().toURL();
	}

	@Nested
	class ExtractPath {
		@Test
		void firstExtractionCreatesTheFilesAndTheFullyGeneratedMarker() throws Exception {
			URL archive = zipArchive("node.zip", "node-v1/node.exe", "binary");
			File result = Activator.extractPath(baseDir, archive, "node-v1/node.exe", true, log);
			assertAll(() -> assertEquals(new File(baseDir, "node-v1/node.exe"), result),
					() -> assertEquals("binary", Files.readString(result.toPath())),
					() -> assertTrue(new File(baseDir, ".fullygenerated").exists()));
		}

		@Test
		void extractsTarGzArchives() throws Exception {
			URL archive = tarGzArchive("node.tar.gz", TarItem.file("node-v1/bin/node", "binary", 0755));
			File result = Activator.extractPath(baseDir, archive, "node-v1/bin/node", true, log);
			assertAll(() -> assertEquals("binary", Files.readString(result.toPath())),
					() -> assertTrue(new File(baseDir, ".fullygenerated").exists()));
		}

		@Test
		void withTheMarkerAndFilePresentNothingIsExtractedAgain() throws Exception {
			File existing = new File(baseDir, "node-v1/node.exe");
			Files.createDirectories(existing.getParentFile().toPath());
			Files.writeString(existing.toPath(), "old");
			Files.createFile(new File(baseDir, ".fullygenerated").toPath());

			URL archive = zipArchive("node.zip", "node-v1/node.exe", "new");
			File result = Activator.extractPath(baseDir, archive, "node-v1/node.exe", true, log);

			assertAll(() -> assertEquals(existing, result), () -> assertEquals("old",
					Files.readString(existing.toPath()), "archive must not be extracted again"));
		}

		@Test
		void withTheMarkerAndFilePresentTheArchiveIsNotNeeded() throws Exception {
			File existing = new File(baseDir, "node.exe");
			Files.writeString(existing.toPath(), "old");
			Files.createFile(new File(baseDir, ".fullygenerated").toPath());
			assertEquals(existing, Activator.extractPath(baseDir, null, "node.exe", true, log));
		}

		@Test
		void missingMarkerReExtractsAndDeletesOldDirectoriesExceptTheTargetFolder() throws Exception {
			File oldNode = new File(baseDir, "node-old");
			File ngTarget = new File(baseDir, "target");
			Files.createDirectories(oldNode.toPath());
			Files.createDirectories(ngTarget.toPath());
			Files.writeString(new File(oldNode, "x").toPath(), "x");
			Files.writeString(new File(ngTarget, "keep.txt").toPath(), "keep");

			URL archive = zipArchive("node.zip", "node-new/node.exe", "binary");
			File result = Activator.extractPath(baseDir, archive, "node-new/node.exe", true, log);

			assertAll(() -> assertFalse(oldNode.exists(), "old node install is removed"),
					() -> assertTrue(new File(ngTarget, "keep.txt").exists(), "the NG target folder is never removed"),
					() -> assertEquals("binary", Files.readString(result.toPath())),
					() -> assertTrue(new File(baseDir, ".fullygenerated").exists()));
		}

		@Test
		void withoutDeletePreviousPathsOldDirectoriesAreKeptWhenTheMarkerExists() throws Exception {
			// npm is extracted next to node: it must not delete the node dir that was just
			// extracted
			File nodeDir = new File(baseDir, "node-v1");
			Files.createDirectories(nodeDir.toPath());
			Files.writeString(new File(nodeDir, "node.exe").toPath(), "node");
			Files.createFile(new File(baseDir, ".fullygenerated").toPath());

			URL archive = zipArchive("npm.zip", "npm/cli.js", "npm");
			File result = Activator.extractPath(baseDir, archive, "npm/cli.js", false, log);

			assertAll(() -> assertTrue(new File(nodeDir, "node.exe").exists(), "node dir is kept"),
					() -> assertEquals("npm", Files.readString(result.toPath())));
		}

		@Test
		void missingArchiveWhenExtractionIsNeededReturnsNullAndLogsInfo() {
			File result = Activator.extractPath(baseDir, null, "node.exe", true, log);
			assertNull(result);
			org.mockito.Mockito.verify(log).info(anyString());
		}

		@Test
		void unreadableArchiveIsLoggedAndTheMarkerIsNotCreated() throws Exception {
			Path file = root.resolve("broken.zip");
			Files.writeString(file, "not a zip");
			// a zip with no entries extracts nothing; use a missing file instead to force
			// an IOException
			URL missing = root.resolve("missing.zip").toUri().toURL();
			File result = Activator.extractPath(baseDir, missing, "node.exe", true, log);
			assertAll(() -> assertNotNull(result), () -> assertFalse(new File(baseDir, ".fullygenerated").exists()),
					() -> verify(log).error(org.mockito.ArgumentMatchers.contains("Error extracting path"),
							org.mockito.ArgumentMatchers.any(Throwable.class)));
		}
	}

	@Nested
	class ExtractPnpmPath {
		private static final String PNPM = "bin/pnpm.exe";

		@Test
		void firstExtractionGoesIntoAVersionDirectoryAndWritesTheTokenMarker() throws Exception {
			URL archive = zipArchive("pnpm.zip", PNPM, "pnpm");
			File result = Activator.extractPnpmPath(baseDir, archive, PNPM, "12.5.1", log);
			assertAll(() -> assertEquals(new File(new File(baseDir, "pnpm-12.5.1"), PNPM), result),
					() -> assertEquals("pnpm", Files.readString(result.toPath())),
					() -> assertEquals("12.5.1", Files.readString(new File(baseDir, ".pnpmgenerated").toPath())));
		}

		@Test
		void matchingMarkerSkipsExtraction() throws Exception {
			File existing = new File(new File(baseDir, "pnpm-12.5.1"), PNPM);
			Files.createDirectories(existing.getParentFile().toPath());
			Files.writeString(existing.toPath(), "old");
			Files.writeString(new File(baseDir, ".pnpmgenerated").toPath(), "12.5.1");

			File result = Activator.extractPnpmPath(baseDir, zipArchive("pnpm.zip", PNPM, "new"), PNPM, "12.5.1", log);

			assertAll(() -> assertEquals(existing, result),
					() -> assertEquals("old", Files.readString(existing.toPath())));
		}

		@Test
		void tokenChangeRemovesOnlyTheOldPnpmDirectoryAndLeavesNodeAndTargetAlone() throws Exception {
			File oldPnpm = new File(baseDir, "pnpm-12.0.0");
			File node = new File(baseDir, "node-v1");
			File ngTarget = new File(baseDir, "target");
			for (File dir : new File[] { oldPnpm, node, ngTarget }) {
				Files.createDirectories(dir.toPath());
				Files.writeString(new File(dir, "f").toPath(), "f");
			}
			Files.writeString(new File(baseDir, ".pnpmgenerated").toPath(), "12.0.0");

			File result = Activator.extractPnpmPath(baseDir, zipArchive("pnpm.zip", PNPM, "new"), PNPM, "12.5.1", log);

			assertAll(() -> assertFalse(oldPnpm.exists(), "old pnpm dir is removed"),
					() -> assertTrue(new File(node, "f").exists(), "node is untouched"),
					() -> assertTrue(new File(ngTarget, "f").exists(), "target is untouched"),
					() -> assertEquals("new", Files.readString(result.toPath())),
					() -> assertEquals("12.5.1", Files.readString(new File(baseDir, ".pnpmgenerated").toPath())));
		}

		@Test
		void missingMarkerReExtractsEvenWhenTheBinaryExists() throws Exception {
			File existing = new File(new File(baseDir, "pnpm-12.5.1"), PNPM);
			Files.createDirectories(existing.getParentFile().toPath());
			Files.writeString(existing.toPath(), "old");

			File result = Activator.extractPnpmPath(baseDir, zipArchive("pnpm.zip", PNPM, "new"), PNPM, "12.5.1", log);

			assertEquals("new", Files.readString(result.toPath()));
		}

		@Test
		void missingArchiveWhenExtractionIsNeededReturnsNull() {
			assertNull(Activator.extractPnpmPath(baseDir, null, PNPM, "12.5.1", log));
			verify(log).info(anyString());
		}

		@Test
		void extractionFailureIsLoggedAndNoMarkerIsWritten() throws Exception {
			URL missing = root.resolve("missing.zip").toUri().toURL();
			Activator.extractPnpmPath(baseDir, missing, PNPM, "12.5.1", log);
			assertFalse(new File(baseDir, ".pnpmgenerated").exists());
			verify(log).error(org.mockito.ArgumentMatchers.contains("Error extracting pnpm"),
					org.mockito.ArgumentMatchers.any(Throwable.class));
		}

		@Test
		void extractsTarGz() throws Exception {
			URL archive = tarGzArchive("pnpm.tar.gz", TarItem.file("pnpm", "bin", 0755));
			File result = Activator.extractPnpmPath(baseDir, archive, "pnpm", "1.0.0", log);
			assertEquals("bin", Files.readString(result.toPath()));
			verify(log, never()).error(anyString(), org.mockito.ArgumentMatchers.any(Throwable.class));
		}
	}

	@Nested
	class ReadPnpmMarkerToken {
		@Test
		void returnsTheTrimmedToken() throws Exception {
			File marker = new File(baseDir, ".pnpmgenerated");
			Files.writeString(marker.toPath(), "  12.5.1\n", StandardCharsets.UTF_8);
			assertEquals("12.5.1", Activator.readPnpmMarkerToken(marker));
		}

		@Test
		void missingFileYieldsNull() {
			assertNull(Activator.readPnpmMarkerToken(new File(baseDir, "nope")));
		}

		@Test
		void blankFileYieldsNull() throws Exception {
			File marker = new File(baseDir, ".pnpmgenerated");
			Files.writeString(marker.toPath(), " \n ", StandardCharsets.UTF_8);
			assertNull(Activator.readPnpmMarkerToken(marker));
		}
	}

	@Nested
	class SystemOrEnvironmentProperty {
		@Test
		void systemPropertyIsReturnedNormalized() throws Exception {
			String key = "servoy.test.ngclient.ui.prop";
			System.setProperty(key, "a/./b/../c");
			try {
				assertEquals(Path.of("a/c").toString(), Activator.getSystemOrEvironmentProperty(key));
			} finally {
				System.clearProperty(key);
			}
		}

		@Test
		void unknownPropertyYieldsNull() {
			assertNull(Activator.getSystemOrEvironmentProperty("servoy.test.ngclient.ui.does.not.exist"));
		}
	}

	@Nested
	class SelectAndExtractRuntime {
		@Test
		void withoutNodeExtensionNothingIsBundledAndTheConfiguredPnpmWins() {
			File[] selected = Activator.selectAndExtractRuntime(new IConfigurationElement[0], true,
					"C:/tools/pnpm.exe");
			assertAll(() -> assertNull(selected[0], "node"), () -> assertNull(selected[1], "npm"),
					() -> assertEquals(new File("C:/tools/pnpm.exe"), selected[2], "configured pnpm"));
		}

		@Test
		void withoutNodeExtensionAndNoConfiguredPnpmAllAreNull() {
			File[] selected = Activator.selectAndExtractRuntime(new IConfigurationElement[0], false, null);
			assertAll(() -> assertNull(selected[0]), () -> assertNull(selected[1]), () -> assertNull(selected[2]));
		}
	}
}
