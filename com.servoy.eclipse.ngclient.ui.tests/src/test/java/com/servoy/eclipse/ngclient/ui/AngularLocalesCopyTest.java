package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WebPackagesListener.copyAngularLocales")
class AngularLocalesCopyTest {

	private Path tempDir;
	private File parentDir;
	private File projectFolder;

	@BeforeEach
	void setUp() throws IOException {
		tempDir = Files.createTempDirectory("angular-locales-test");
		parentDir = tempDir.toFile();
		projectFolder = new File(parentDir, "solution");
		projectFolder.mkdirs();
	}

	@AfterEach
	void tearDown() throws IOException {
		try (Stream<Path> paths = Files.walk(tempDir)) {
			paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
	}

	@Nested
	@DisplayName("when locales are in parent node_modules (after dedup)")
	class ParentNodeModules {

		private File parentLocalesDir;

		@BeforeEach
		void setUp() throws IOException {
			parentLocalesDir = new File(parentDir, "node_modules/@angular/common/locales");
			parentLocalesDir.mkdirs();
			Files.writeString(parentLocalesDir.toPath().resolve("en.js"), "export default {};");
			Files.writeString(parentLocalesDir.toPath().resolve("nl.js"), "export default {};");
			Files.writeString(parentLocalesDir.toPath().resolve("fr.js"), "export default {};");
		}

		@Test
		@DisplayName("copies .js files to locales/angular in project folder")
		void copiesJsFiles() {
			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			File destDir = new File(projectFolder, "locales/angular");
			assertAll(() -> assertTrue(result, "should return true on success"),
					() -> assertTrue(destDir.isDirectory(), "destination directory should exist"),
					() -> assertTrue(new File(destDir, "en.js").exists(), "en.js should be copied"),
					() -> assertTrue(new File(destDir, "nl.js").exists(), "nl.js should be copied"),
					() -> assertTrue(new File(destDir, "fr.js").exists(), "fr.js should be copied"));
		}

		@Test
		@DisplayName("prefers parent node_modules over local node_modules")
		void prefersParentNodeModules() throws IOException {
			File localLocalesDir = new File(projectFolder, "node_modules/@angular/common/locales");
			localLocalesDir.mkdirs();
			Files.writeString(localLocalesDir.toPath().resolve("local-only.js"), "local");

			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			File destDir = new File(projectFolder, "locales/angular");
			assertAll(() -> assertTrue(result),
					() -> assertTrue(new File(destDir, "en.js").exists(), "should have files from parent"),
					() -> assertFalse(new File(destDir, "local-only.js").exists(), "should NOT have local-only file"));
		}

		@Test
		@DisplayName("filters out non-.js files")
		void filtersNonJsFiles() throws IOException {
			Files.writeString(parentLocalesDir.toPath().resolve("README.md"), "docs");
			Files.writeString(parentLocalesDir.toPath().resolve("package.json"), "{}");
			Files.writeString(parentLocalesDir.toPath().resolve("de.js"), "export default {};");

			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			File destDir = new File(projectFolder, "locales/angular");
			assertAll(() -> assertTrue(result),
					() -> assertTrue(new File(destDir, "de.js").exists(), "js file should be copied"),
					() -> assertFalse(new File(destDir, "README.md").exists(), "README should not be copied"),
					() -> assertFalse(new File(destDir, "package.json").exists(), "package.json should not be copied"));
		}

		@Test
		@DisplayName("copies locale files from subdirectories")
		void copiesFromSubdirectories() throws IOException {
			File globalDir = new File(parentLocalesDir, "global");
			globalDir.mkdirs();
			Files.writeString(globalDir.toPath().resolve("nl.js"), "global nl");

			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			File destDir = new File(projectFolder, "locales/angular/global");
			assertAll(() -> assertTrue(result),
					() -> assertTrue(destDir.isDirectory(), "subdirectory should be copied"),
					() -> assertTrue(new File(destDir, "nl.js").exists(), "nested js file should be copied"));
		}

		@Test
		@DisplayName("overwrites existing files in destination")
		void overwritesExistingFiles() throws IOException {
			File destDir = new File(projectFolder, "locales/angular");
			destDir.mkdirs();
			Files.writeString(destDir.toPath().resolve("en.js"), "old content");

			WebPackagesListener.copyAngularLocales(projectFolder);

			String content = Files.readString(destDir.toPath().resolve("en.js"));
			assertEquals("export default {};", content, "file should be overwritten with new content");
		}
	}

	@Nested
	@DisplayName("when locales are only in local node_modules (no dedup)")
	class LocalNodeModules {

		@BeforeEach
		void setUp() throws IOException {
			File localLocalesDir = new File(projectFolder, "node_modules/@angular/common/locales");
			localLocalesDir.mkdirs();
			Files.writeString(localLocalesDir.toPath().resolve("en.js"), "export default {};");
			Files.writeString(localLocalesDir.toPath().resolve("de.js"), "export default {};");
		}

		@Test
		@DisplayName("falls back to local node_modules when parent does not have locales")
		void fallsBackToLocalNodeModules() {
			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			File destDir = new File(projectFolder, "locales/angular");
			assertAll(() -> assertTrue(result, "should return true on success"),
					() -> assertTrue(new File(destDir, "en.js").exists(), "en.js should be copied from local"),
					() -> assertTrue(new File(destDir, "de.js").exists(), "de.js should be copied from local"));
		}
	}

	@Nested
	@DisplayName("when no locale source directory exists")
	class NoLocalesAvailable {

		@Test
		@DisplayName("returns false when neither parent nor local node_modules have locales")
		void returnsFalseWhenNoLocales() {
			boolean result = WebPackagesListener.copyAngularLocales(projectFolder);

			assertAll(() -> assertFalse(result, "should return false"),
					() -> assertFalse(new File(projectFolder, "locales/angular").exists(),
							"destination should not be created"));
		}
	}

	@Nested
	@DisplayName("destination directory creation")
	class DestinationDirectoryCreation {

		@Test
		@DisplayName("creates locales/angular directory structure when it does not exist")
		void createsDestinationDirectory() throws IOException {
			File parentLocalesDir = new File(parentDir, "node_modules/@angular/common/locales");
			parentLocalesDir.mkdirs();
			Files.writeString(parentLocalesDir.toPath().resolve("en.js"), "content");

			assertFalse(new File(projectFolder, "locales").exists(), "locales dir should not exist before");

			WebPackagesListener.copyAngularLocales(projectFolder);

			assertTrue(new File(projectFolder, "locales/angular").isDirectory(), "locales/angular should be created");
		}
	}
}
