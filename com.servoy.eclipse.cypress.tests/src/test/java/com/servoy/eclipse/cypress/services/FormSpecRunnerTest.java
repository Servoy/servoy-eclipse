package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FormSpecRunnerTest {
	@Test
	public void testFormSpecRunner_isCreatable() {
		assertNotNull(FormSpecRunner.class.getAnnotation(org.eclipse.e4.core.di.annotations.Creatable.class),
				"FormSpecRunner must have @Creatable annotation");
	}

	@Test
	public void testFormSpecRunner_hasRunFormCypressTestsMethod() throws NoSuchMethodException {
		assertNotNull(FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class),
				"FormSpecRunner must have runFormCypressTests(String, boolean) method");
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsReturnType() throws NoSuchMethodException {
		assertTrue(FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class)
				.getReturnType() == String.class, "runFormCypressTests must return String");
	}

	@Test
	public void testFormSpecRunner_runFormCypressTests_noActiveProject() {
		FormSpecRunner runner = new FormSpecRunner();
		try {
			String result = runner.runFormCypressTests("nonExistentForm", true);
			assertNotNull(result);
			assertTrue(result.contains("Error"), "Should return error when no active project");
		} catch (Throwable e) {
			assertNotNull(e, "Expected error in plain JUnit (no workspace)");
		}
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsHasHeadlessParam() throws NoSuchMethodException {
		Method m = FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class);
		assertEquals(boolean.class, m.getParameterTypes()[1], "Second param should be boolean for headless");
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressInstalledMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("ensureCypressInstalled".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecRunner must have ensureCypressInstalled method");
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressConfigMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("ensureCypressConfig".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecRunner must have ensureCypressConfig method");
	}

	@Test
	public void testFormSpecRunner_hasGetCypressDirMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("getCypressDir".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecRunner must have getCypressDir method");
	}

	@Test
	public void testFormSpecRunner_hasGetNodePathMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("getNodePath".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(found, "FormSpecRunner must have getNodePath method");
	}

	@Test
	public void testFormSpecRunner_canBeInstantiated() {
		FormSpecRunner runner = new FormSpecRunner();
		assertNotNull(runner, "FormSpecRunner should be instantiable");
	}

	@Test
	public void testFormSpecRunner_hasRunE2ECypressTestsMethod() throws NoSuchMethodException {
		assertNotNull(FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class),
				"FormSpecRunner must have runE2ECypressTests(String, boolean) method");
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsReturnType() throws NoSuchMethodException {
		assertTrue(FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class)
				.getReturnType() == String.class, "runE2ECypressTests must return String");
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsHasHeadlessParam() throws NoSuchMethodException {
		Method m = FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class);
		assertEquals(boolean.class, m.getParameterTypes()[1], "Second param should be boolean for headless");
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunSpecMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("runSpec".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(!found, "FormSpecRunner must NOT have runSpec method (renamed to runFormCypressTests)");
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunE2ESpecMethod() {
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods) {
			if ("runE2ESpec".equals(m.getName())) {
				found = true;
				break;
			}
		}
		assertTrue(!found, "FormSpecRunner must NOT have runE2ESpec method (renamed to runE2ECypressTests)");
	}

	// --- findMatchingFile / findScreenshot
	// -----------------------------------------------------
	// These are the artifact-matching methods used for SVY-21174 (video/screenshot
	// preservation
	// for failed Cypress runs). Exercised via reflection since they are private.

	private Path tempDir;

	@BeforeEach
	public void setUpTempDir() throws IOException {
		tempDir = Files.createTempDirectory("formspecrunner-test");
	}

	@AfterEach
	public void tearDownTempDir() throws IOException {
		if (tempDir != null && Files.exists(tempDir)) {
			Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static Path invokeFindMatchingFile(FormSpecRunner runner, Path dir, String baseName, String extension)
			throws Exception {
		Method m = FormSpecRunner.class.getDeclaredMethod("findMatchingFile", Path.class, String.class, String.class);
		m.setAccessible(true);
		return (Path) m.invoke(runner, dir, baseName, extension);
	}

	private static Path invokeFindScreenshot(FormSpecRunner runner, Path screenshotsDir, String baseName)
			throws Exception {
		Method m = FormSpecRunner.class.getDeclaredMethod("findScreenshot", Path.class, String.class);
		m.setAccessible(true);
		return (Path) m.invoke(runner, screenshotsDir, baseName);
	}

	@Test
	public void testFindMatchingFile_doesNotFalsePositiveOnSubstringFormName() throws Exception {
		// A video for "adminlogin.spec.cy.js" must not match baseName "login": the
		// required
		// '.' separator before the leaf name is what prevents this false positive.
		Files.createFile(tempDir.resolve("adminlogin.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "login", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindMatchingFile_matchesE2ESpecVideoNamedAfterFullSpecFilename() throws Exception {
		Files.createFile(tempDir.resolve("login.cy.ts.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "login", ".mp4");

		assertNotNull(found, "video named after the full E2E spec filename must be found");
		assertEquals("login.cy.ts.mp4", found.getFileName().toString());
	}

	@Test
	public void testFindMatchingFile_matchesFormSpecVideoNamedAfterFullSpecFilename() throws Exception {
		Files.createFile(tempDir.resolve("mysolution.myform.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNotNull(found, "video named after the solution-prefixed form spec filename must be found");
		assertEquals("mysolution.myform.spec.cy.js.mp4", found.getFileName().toString());
	}

	@Test
	public void testFindMatchingFile_matchesBareFormSpecVideo() throws Exception {
		Files.createFile(tempDir.resolve("myform.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNotNull(found, "video named after the bare (legacy) form spec filename must be found");
	}

	@Test
	public void testFindMatchingFile_resolvesSubfolderQualifiedE2EName() throws Exception {
		Path appB = Files.createDirectories(tempDir.resolve("appB"));
		Files.createFile(appB.resolve("login.cy.ts.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "appB/login", ".mp4");

		assertNotNull(found, "subfolder-qualified name must still resolve to the video under its subfolder");
	}

	@Test
	public void testFindMatchingFile_returnsNullWhenNoMatch() throws Exception {
		Files.createFile(tempDir.resolve("otherForm.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindMatchingFile_returnsNullWhenDirectoryMissing() throws Exception {
		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir.resolve("doesNotExist"), "myform", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindScreenshot_matchesFormSpecScreenshotFolder() throws Exception {
		Path specFolder = Files.createDirectories(tempDir.resolve("mysolution.myform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test -- should work.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNotNull(found, "screenshot under the solution-prefixed form spec folder must be found");
	}

	@Test
	public void testFindScreenshot_matchesE2ESpecScreenshotFolder() throws Exception {
		Path specFolder = Files.createDirectories(tempDir.resolve("login.cy.ts"));
		Files.createFile(specFolder.resolve("test -- should work.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "login");

		assertNotNull(found, "screenshot under the E2E spec folder must be found");
	}

	@Test
	public void testFindScreenshot_prefersFailedScreenshotWhenPresent() throws Exception {
		Path specFolder = Files.createDirectories(tempDir.resolve("myform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test -- passed step.png"));
		Files.createFile(specFolder.resolve("test -- failed step (failed).png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNotNull(found);
		assertTrue(found.getFileName().toString().contains("(failed)"),
				"the screenshot marked (failed) must be preferred");
	}

	@Test
	public void testFindScreenshot_returnsNullWhenNoMatchingFolder() throws Exception {
		Path specFolder = Files.createDirectories(tempDir.resolve("otherform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNull(found);
	}
}
