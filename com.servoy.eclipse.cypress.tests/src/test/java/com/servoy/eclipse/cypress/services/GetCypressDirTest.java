package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GetCypressDirTest {
	private FormSpecRunner runner;

	@BeforeEach
	void setUp() {
		runner = new FormSpecRunner();
	}

	@Nested
	class PathStructure {
		@Test
		@DisplayName("returns path under workspace-root/.metadata/.plugins/")
		void returnPathUnderMetadataPlugins() {
			Path workspaceRoot = Path.of("/Users/dev/servoy-workspace");
			Path result = runner.getCypressDir(workspaceRoot);

			assertTrue(result.startsWith(workspaceRoot),
					"Cypress dir must be under the workspace root, got: " + result);
			assertTrue(result.startsWith(workspaceRoot.resolve(".metadata").resolve(".plugins")),
					"Cypress dir must be under .metadata/.plugins/, got: " + result);
		}

		@Test
		@DisplayName("does not traverse above workspace root")
		void doesNotTraverseAboveWorkspaceRoot() {
			Path workspaceRoot = Path.of("/Users/dev/servoy-workspace");
			Path result = runner.getCypressDir(workspaceRoot);

			assertFalse(result.toString().contains(".."),
					"Path must not contain parent traversal (..), got: " + result);
			assertTrue(result.startsWith(workspaceRoot), "Path must not escape workspace root, got: " + result);
		}

		@Test
		@DisplayName("ends with com.servoy.eclipse.developer.mcp/cypress")
		void endsWithExpectedSuffix() {
			Path workspaceRoot = Path.of("/Users/dev/servoy-workspace");
			Path result = runner.getCypressDir(workspaceRoot);

			Path expected = workspaceRoot.resolve(".metadata").resolve(".plugins")
					.resolve("com.servoy.eclipse.developer.mcp").resolve("cypress");
			assertEquals(expected, result);
		}

		@ParameterizedTest
		@ValueSource(strings = { "/Users/dev/workspace", "/home/ci/build/servoy", "C:\\Users\\dev\\workspace" })
		@DisplayName("produces correct structure for various workspace roots")
		void correctStructureForVariousRoots(String root) {
			Path workspaceRoot = Path.of(root);
			Path result = runner.getCypressDir(workspaceRoot);

			assertTrue(result.startsWith(workspaceRoot), "Must start with workspace root for: " + root);
			assertTrue(result.startsWith(workspaceRoot.resolve(".metadata").resolve(".plugins")),
					"Must contain .metadata/.plugins/ for: " + root);
			assertEquals(workspaceRoot.resolve(".metadata/.plugins/com.servoy.eclipse.developer.mcp/cypress"), result);
		}
	}

	@Nested
	class RegressionSVY21369 {
		@Test
		@DisplayName("workspace parent directory is NOT part of the resolved path")
		void parentDirectoryNotInPath() {
			Path workspaceRoot = Path.of("/Users/dev/servoy-workspace");
			Path parentDir = workspaceRoot.getParent();
			Path result = runner.getCypressDir(workspaceRoot);

			assertFalse(result.startsWith(parentDir.resolve(".metadata")),
					"Must NOT resolve .metadata under parent dir (" + parentDir + "), got: " + result);
		}
	}
}
