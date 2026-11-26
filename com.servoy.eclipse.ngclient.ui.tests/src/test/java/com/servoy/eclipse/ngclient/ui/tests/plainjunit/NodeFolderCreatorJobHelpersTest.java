package com.servoy.eclipse.ngclient.ui.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.ngclient.ui.NodeFolderCreatorJob;
import com.servoy.eclipse.ngclient.ui.tests.plainjunit.utils.TestConsole;

class NodeFolderCreatorJobHelpersTest {
	@TempDir
	Path root;

	@Nested
	class IgnoredResource {
		@ParameterizedTest
		@ValueSource(strings = { "/scripts/build.js", "/.vscode/settings.json", "/e2e/app.e2e.ts",
				"/projects/x/node_modules/y/index.js", "/node/readme", "/dist/app/main.js", "/dist-public/x.js",
				"/src/app/app.component.spec.ts", "/.gitignore", "/.angular/cache/x", "/tarballs/a.tgz" })
		void buildOutputAndToolingResourcesAreIgnored(String name) {
			assertTrue(NodeFolderCreatorJob.ignoredResource(name), name);
		}

		@ParameterizedTest
		@ValueSource(strings = { "/package.json", "/src/app/app.component.ts", "/angular.json",
				"/projects/servoy_public/src/index.ts", "/tsconfig.json", "/src/distribution/x.ts" })
		void sourceResourcesAreKept(String name) {
			assertFalse(NodeFolderCreatorJob.ignoredResource(name), name);
		}
	}

	@Nested
	class Files_ {
		@Test
		void createFolderCreatesNestedFolders() {
			java.io.File folder = root.resolve("a/b").toFile();
			NodeFolderCreatorJob.createFolder(folder);
			assertTrue(folder.isDirectory());
		}

		@Test
		void copyOrCreateFileWritesAndOverwrites() throws Exception {
			NodeFolderCreatorJob.copyOrCreateFile("x/y.txt", root.toFile(),
					new ByteArrayInputStream("one-long-content".getBytes(StandardCharsets.UTF_8)));
			NodeFolderCreatorJob.copyOrCreateFile("x/y.txt", root.toFile(),
					new ByteArrayInputStream("two".getBytes(StandardCharsets.UTF_8)));
			assertEquals("two", Files.readString(root.resolve("x/y.txt")));
		}
	}

	@Nested
	class RemovePublicFileDependency {
		@Test
		void removesOnlyTheServoyPublicDependencyAndReportsIt() throws Exception {
			Path packageJson = root.resolve("package.json");
			Files.writeString(packageJson,
					"{\"name\":\"app\",\"dependencies\":{\"@servoy/public\":\"file:dist-public\",\"rxjs\":\"^7.0.0\"}}");
			TestConsole console = new TestConsole();

			NodeFolderCreatorJob.removePublicFileDependency(packageJson.toFile(), console);

			JSONObject json = new JSONObject(Files.readString(packageJson));
			JSONObject deps = json.getJSONObject("dependencies");
			assertAll(() -> assertFalse(deps.has("@servoy/public")),
					() -> assertEquals("^7.0.0", deps.getString("rxjs")),
					() -> assertEquals("app", json.getString("name")),
					() -> assertTrue(console.text().contains("removed '@servoy/public: file:dist-public'"),
							console.text()));
		}

		@Test
		void packageJsonWithoutTheDependencyIsLeftByteIdentical() throws Exception {
			Path packageJson = root.resolve("package.json");
			String original = "{ \"dependencies\": { \"rxjs\": \"^7.0.0\" } }";
			Files.writeString(packageJson, original);
			TestConsole console = new TestConsole();

			NodeFolderCreatorJob.removePublicFileDependency(packageJson.toFile(), console);

			assertAll(() -> assertEquals(original, Files.readString(packageJson)),
					() -> assertEquals("", console.text()));
		}

		@Test
		void packageJsonWithoutDependenciesSectionIsLeftAlone() throws Exception {
			Path packageJson = root.resolve("package.json");
			String original = "{\"name\":\"app\"}";
			Files.writeString(packageJson, original);
			NodeFolderCreatorJob.removePublicFileDependency(packageJson.toFile(), new TestConsole());
			assertEquals(original, Files.readString(packageJson));
		}

		@Test
		void missingPackageJsonIsIgnored() {
			TestConsole console = new TestConsole();
			NodeFolderCreatorJob.removePublicFileDependency(root.resolve("missing.json").toFile(), console);
			assertEquals("", console.text());
		}
	}
}
