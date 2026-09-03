package com.servoy.eclipse.cypress.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.resources.ResourcesPlugin;

public class CypressTestDiscoveryService {
	private static final String SPEC_CY_EXTENSION = ".spec.cy.js";
	private static final String E2E_CY_JS_EXTENSION = ".cy.js";
	private static final String E2E_CY_TS_EXTENSION = ".cy.ts";

	private final FormSpecGenerator specGenerator = new FormSpecGenerator();

	public TestType getTestType(String formName) {
		if (hasFormTest(formName)) {
			return TestType.FORM;
		}
		if (hasE2ETest(formName)) {
			return TestType.E2E;
		}
		return TestType.NONE;
	}

	public boolean hasTest(String formName) {
		return hasFormTest(formName) || hasE2ETest(formName);
	}

	public boolean hasFormTest(String formName) {
		Path testsDir = specGenerator.getFormSpecDir();
		if (testsDir == null || !Files.isDirectory(testsDir)) {
			return false;
		}
		if (Files.exists(testsDir.resolve(formName + SPEC_CY_EXTENSION))) {
			return true;
		}
		try (Stream<Path> files = Files.list(testsDir)) {
			return files.anyMatch(p -> p.getFileName().toString().endsWith("." + formName + SPEC_CY_EXTENSION));
		} catch (Exception e) {
			return false;
		}
	}

	public boolean hasE2ETest(String formName) {
		Path e2eDir = resolveE2EDir();
		if (e2eDir == null || !Files.isDirectory(e2eDir)) {
			return false;
		}
		if (Files.exists(e2eDir.resolve(formName + E2E_CY_JS_EXTENSION))
				|| Files.exists(e2eDir.resolve(formName + E2E_CY_TS_EXTENSION))) {
			return true;
		}
		try (Stream<Path> walk = Files.walk(e2eDir)) {
			return walk.anyMatch(p -> {
				String fileName = p.getFileName().toString();
				return fileName.equals(formName + E2E_CY_JS_EXTENSION)
						|| fileName.equals(formName + E2E_CY_TS_EXTENSION);
			});
		} catch (IOException e) {
			return false;
		}
	}

	public List<String> discoverAllTestForms() {
		Path testsDir = specGenerator.getFormSpecDir();
		if (testsDir == null || !Files.isDirectory(testsDir)) {
			return Collections.emptyList();
		}
		try (Stream<Path> files = Files.list(testsDir)) {
			return files.filter(p -> p.getFileName().toString().endsWith(SPEC_CY_EXTENSION)).map(p -> {
				String fileName = p.getFileName().toString();
				return fileName.substring(0, fileName.length() - SPEC_CY_EXTENSION.length());
			}).toList();
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public List<String> discoverAllE2ETests() {
		Path e2eDir = resolveE2EDir();
		if (e2eDir == null || !Files.isDirectory(e2eDir)) {
			return Collections.emptyList();
		}
		return collectE2ETestNames(e2eDir, e2eDir);
	}

	public List<String> discoverSolutionE2ETests() {
		Path e2eDir = resolveE2EDir();
		if (e2eDir == null || !Files.isDirectory(e2eDir)) {
			return Collections.emptyList();
		}
		com.servoy.eclipse.model.nature.ServoyProject servoyProject = com.servoy.eclipse.core.ServoyModelManager
				.getServoyModelManager().getServoyModel().getActiveProject();
		if (servoyProject == null || servoyProject.getProject() == null) {
			return Collections.emptyList();
		}
		Path solutionDir = e2eDir.resolve(servoyProject.getProject().getName());
		if (!Files.isDirectory(solutionDir)) {
			return Collections.emptyList();
		}
		return collectE2ETestNames(solutionDir, solutionDir);
	}

	/**
	 * Walks {@code scanRoot} recursively and returns a unique, run-ready name for
	 * each E2E spec. Names are relative to {@code baseDir} (with forward slashes
	 * and the .cy.js/.cy.ts suffix stripped) so that two specs sharing a bare base
	 * name in different subfolders (e.g. appA/login and appB/login) do not collapse
	 * to a single entry. The base is the top-level e2e directory so the returned
	 * names round-trip through
	 * {@link FormSpecRunner#runE2ECypressTests(String, boolean)}, which resolves
	 * them against it.
	 */
	private List<String> collectE2ETestNames(Path baseDir, Path scanRoot) {
		try (Stream<Path> walk = Files.walk(scanRoot)) {
			return walk.filter(p -> {
				String name = p.getFileName().toString();
				return name.endsWith(E2E_CY_JS_EXTENSION) || name.endsWith(E2E_CY_TS_EXTENSION);
			}).map(p -> {
				String relative = baseDir.relativize(p).toString().replace('\\', '/');
				if (relative.endsWith(E2E_CY_JS_EXTENSION)) {
					return relative.substring(0, relative.length() - E2E_CY_JS_EXTENSION.length());
				}
				return relative.substring(0, relative.length() - E2E_CY_TS_EXTENSION.length());
			}).toList();
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}

	public boolean hasAnyTest() {
		Path testsDir = specGenerator.getFormSpecDir();
		if (testsDir != null && Files.isDirectory(testsDir)) {
			try (Stream<Path> files = Files.list(testsDir)) {
				if (files.anyMatch(p -> p.getFileName().toString().endsWith(SPEC_CY_EXTENSION))) {
					return true;
				}
			} catch (IOException e) {
				// fall through to E2E check
			}
		}
		Path e2eDir = resolveE2EDir();
		if (e2eDir != null && Files.isDirectory(e2eDir)) {
			try (Stream<Path> files = Files.list(e2eDir)) {
				return files.anyMatch(p -> {
					String name = p.getFileName().toString();
					return name.endsWith(E2E_CY_JS_EXTENSION) || name.endsWith(E2E_CY_TS_EXTENSION);
				});
			} catch (IOException e) {
				// fall through
			}
		}
		return false;
	}

	private Path resolveE2EDir() {
		try {
			Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
			return workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress")
					.resolve("e2e");
		} catch (Exception e) {
			return null;
		}
	}

	public enum TestType {
		NONE, FORM, E2E
	}
}
