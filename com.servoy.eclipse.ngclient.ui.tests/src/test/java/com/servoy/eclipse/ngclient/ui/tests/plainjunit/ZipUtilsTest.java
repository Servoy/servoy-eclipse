package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestArchives.TarItem;
import com.servoy.eclipse.ngclient.ui.utils.ZipUtils;

class ZipUtilsTest {
	@TempDir
	Path root;

	private Path destination;

	@BeforeEach
	void createDestination() throws IOException {
		destination = Files.createDirectory(root.resolve("dest"));
	}

	// in-memory URL: ZipUtils does not close the opened stream when the archive is
	// invalid, which would
	// keep a real file locked on Windows and break the @TempDir cleanup
	private URL archive(String fileName, byte[] content) throws IOException {
		URLStreamHandler handler = new URLStreamHandler() {
			@Override
			protected URLConnection openConnection(URL u) {
				return new URLConnection(u) {
					@Override
					public void connect() {
					}

					@Override
					public InputStream getInputStream() {
						return new ByteArrayInputStream(content);
					}
				};
			}
		};
		return URL.of(URI.create("mem:/" + fileName), handler);
	}

	private String read(String relativePath) throws IOException {
		return Files.readString(destination.resolve(relativePath), StandardCharsets.UTF_8);
	}

	@Nested
	class Detection {
		@ParameterizedTest
		@CsvSource({ "file:/x/node.zip,true,false,false", "file:/x/node.tar.gz,false,true,false",
				"file:/x/node.tar.xz,false,false,true", "file:/x/NODE.TAR.GZ,false,true,false",
				"file:/x/NODE.TAR.XZ,false,false,true", "file:/x/node.tgz,false,false,false",
				"file:/x/node.ZIP,false,false,false" })
		void detectionIsBasedOnTheUrlSuffix(String url, boolean zip, boolean tarGz, boolean tarXz) throws Exception {
			URL u = java.net.URI.create(url).toURL();
			assertAll(() -> assertEquals(zip, ZipUtils.isZipFile(u), "zip"),
					() -> assertEquals(tarGz, ZipUtils.isTarGZFile(u), "tar.gz"),
					() -> assertEquals(tarXz, ZipUtils.isTarXZFile(u), "tar.xz"));
		}
	}

	@Nested
	class Zip {
		@Test
		void extractsFilesDirectoriesAndNestedPaths() throws Exception {
			URL url = archive("a.zip",
					TestArchives.zip("top.txt", "top", "emptydir/", "", "nested/deeper/file.txt", "deep"));
			ZipUtils.extractZip(url, destination.toFile());
			assertAll(() -> assertEquals("top", read("top.txt")),
					() -> assertEquals("deep", read("nested/deeper/file.txt")),
					() -> assertTrue(Files.isDirectory(destination.resolve("emptydir"))));
		}

		@Test
		void filesBelowABinFolderAreMadeExecutable() throws Exception {
			URL url = archive("a.zip", TestArchives.zip("node/bin/node", "binary"));
			ZipUtils.extractZip(url, destination.toFile());
			assertTrue(Files.isExecutable(destination.resolve("node/bin/node")));
		}

		@Test
		void modificationTimeOfTheEntryIsPreserved() throws Exception {
			URL url = archive("a.zip", TestArchives.zip("f.txt", "x"));
			ZipUtils.extractZip(url, destination.toFile());
			long expected = 1700000000000L;
			long actual = destination.resolve("f.txt").toFile().lastModified();
			// zip stores DOS time with a 2 second resolution
			assertTrue(Math.abs(actual - expected) <= 2000, "lastModified " + actual + " should be near " + expected);
		}

		@Test
		void missingArchiveFailsWithIOException() throws Exception {
			URL missing = root.resolve("missing.zip").toUri().toURL();
			assertThrows(IOException.class, () -> ZipUtils.extractZip(missing, destination.toFile()));
		}

		/**
		 * Pins the CURRENT behaviour: a "../" entry is normalized but not checked
		 * against the destination, so the file lands OUTSIDE of it (zip-slip). Reported
		 * as a separate case; if it is fixed this test must be updated.
		 */
		@Test
		void zipSlipEntryCurrentlyEscapesTheDestination() throws Exception {
			URL url = archive("evil.zip", TestArchives.zip("../escaped.txt", "evil"));
			ZipUtils.extractZip(url, destination.toFile());
			assertAll(
					() -> assertTrue(Files.exists(root.resolve("escaped.txt")),
							"entry was written outside of the destination"),
					() -> assertFalse(Files.exists(destination.resolve("escaped.txt"))));
		}
	}

	@Nested
	class TarGz {
		@Test
		void extractsFilesAndNestedPathsAndCreatesParents() throws Exception {
			byte[] tar = TestArchives.tar(TarItem.directory("pkg/"), TarItem.file("pkg/a.txt", "A"),
					TarItem.file("pkg/sub/b.txt", "B"));
			ZipUtils.extractTarGZ(archive("a.tar.gz", TestArchives.gzip(tar)), destination.toFile());
			assertAll(() -> assertEquals("A", read("pkg/a.txt")), () -> assertEquals("B", read("pkg/sub/b.txt")));
		}

		@Test
		void fileWithOwnerExecuteBitIsExecutableAndPlainFileKeepsItsContent() throws Exception {
			byte[] tar = TestArchives.tar(TarItem.file("run.sh", "#!/bin/sh", 0755),
					TarItem.file("data.txt", "d", 0644));
			ZipUtils.extractTarGZ(archive("a.tar.gz", TestArchives.gzip(tar)), destination.toFile());
			assertAll(() -> assertTrue(Files.isExecutable(destination.resolve("run.sh"))),
					() -> assertEquals("d", read("data.txt")));
		}

		@Test
		void emptyDirectoryEntriesAreCurrentlyNotCreated() throws Exception {
			// KNOWN DEFECT (reported, not fixed here): the DIRECTORY branch of
			// ZipUtils#extractTar only calls mkdirs() when the directory already exists,
			// so empty directories are never created. Flip this test when that is fixed.
			byte[] tar = TestArchives.tar(TarItem.directory("emptydir/"), TarItem.file("f.txt", "x"));
			ZipUtils.extractTarGZ(archive("a.tar.gz", TestArchives.gzip(tar)), destination.toFile());
			assertAll(() -> assertFalse(Files.exists(destination.resolve("emptydir"))),
					() -> assertEquals("x", read("f.txt")));
		}

		@Test
		void tarEntryTimeInSecondsIsAppliedAsMilliseconds() throws Exception {
			// KNOWN DEFECT (reported, not fixed here): TarEntry#getTime is in seconds but
			// File#setLastModified takes milliseconds, so the extracted mtime is wrong.
			// Flip this test when that is fixed.
			byte[] tar = TestArchives.tar(TarItem.file("f.txt", "x"));
			ZipUtils.extractTarGZ(archive("a.tar.gz", TestArchives.gzip(tar)), destination.toFile());
			long actual = destination.resolve("f.txt").toFile().lastModified();
			long nowSeconds = System.currentTimeMillis() / 1000;
			assertTrue(Math.abs(actual - nowSeconds) <= 60,
					"lastModified " + actual + " should be the entry time in seconds (" + nowSeconds + ")");
		}

		@Test
		void invalidTarContentIsReportedAsIOException() throws Exception {
			URL url = archive("bad.tar.gz", TestArchives.gzip(new byte[2048]));
			IOException e = assertThrows(IOException.class, () -> ZipUtils.extractTarGZ(url, destination.toFile()));
			assertEquals("not in tar format", e.getCause().getMessage());
		}

		@Test
		void notGzipContentFailsWithIOException() throws Exception {
			URL url = archive("bad.tar.gz", "plain text".getBytes(StandardCharsets.UTF_8));
			assertThrows(IOException.class, () -> ZipUtils.extractTarGZ(url, destination.toFile()));
		}

		/**
		 * Pins the CURRENT behaviour: tar entry names are not checked against the
		 * destination, so a "../" entry is written OUTSIDE of it (zip-slip). Reported
		 * as a separate case.
		 */
		@Test
		void zipSlipEntryCurrentlyEscapesTheDestination() throws Exception {
			byte[] tar = TestArchives.tar(TarItem.file("../escaped-tar.txt", "evil"));
			ZipUtils.extractTarGZ(archive("evil.tar.gz", TestArchives.gzip(tar)), destination.toFile());
			assertAll(
					() -> assertTrue(Files.exists(root.resolve("escaped-tar.txt")),
							"entry was written outside of the destination"),
					() -> assertFalse(Files.exists(destination.resolve("escaped-tar.txt"))));
		}
	}

	@Nested
	class TarXz {
		@Test
		void extractsFilesFromAnXzCompressedTar() throws Exception {
			byte[] tar = TestArchives.tar(TarItem.file("node/bin/node", "binary", 0755),
					TarItem.file("node/README", "readme"));
			ZipUtils.extractTarXZ(archive("a.tar.xz", TestArchives.xz(tar)), destination.toFile());
			assertAll(() -> assertEquals("binary", read("node/bin/node")),
					() -> assertEquals("readme", read("node/README")),
					() -> assertTrue(Files.isExecutable(destination.resolve("node/bin/node"))));
		}

		@Test
		void gzipContentIsNotAcceptedAsXz() throws Exception {
			URL url = archive("a.tar.xz", TestArchives.gzip(TestArchives.tar(TarItem.file("f", "x"))));
			assertThrows(IOException.class, () -> ZipUtils.extractTarXZ(url, destination.toFile()));
		}
	}
}
