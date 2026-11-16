package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.servoy.eclipse.cypress.services.FormSpecRunner;

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
	public void testFormSpecRunner_hasEnsureCypressBinaryMethod() throws NoSuchMethodException {
		Method m = FormSpecRunner.class.getMethod("ensureCypressBinary", Path.class);
		assertEquals(String.class, m.getReturnType(), "ensureCypressBinary must return String (null on success, error otherwise)");
	}

	@Test
	public void testFormSpecRunner_hasConfigureCypressEnvironmentMethod() throws NoSuchMethodException {
		Method m = FormSpecRunner.class.getMethod("configureCypressEnvironment", ProcessBuilder.class, Path.class);
		assertEquals(void.class, m.getReturnType(), "configureCypressEnvironment must return void");
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

@Test
public void testFindMatchingFile_matchesTypeScriptFormSpecVideo() throws Exception {
	Files.createFile(tempDir.resolve("mysolution.myform.spec.cy.ts.mp4"));

	Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

	assertNotNull(found, "video named after a solution-prefixed .spec.cy.ts form spec must be found");
	assertEquals("mysolution.myform.spec.cy.ts.mp4", found.getFileName().toString());
}

@Test
public void testFindMatchingFile_matchesBareTypeScriptFormSpecVideo() throws Exception {
	Files.createFile(tempDir.resolve("myform.spec.cy.ts.mp4"));

	assertNotNull(invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4"));
}

@Test
public void testFindMatchingFile_typeScriptFormSpecDoesNotFalsePositiveOnSubstringFormName() throws Exception {
	Files.createFile(tempDir.resolve("adminlogin.spec.cy.ts.mp4"));

	assertNull(invokeFindMatchingFile(new FormSpecRunner(), tempDir, "login", ".mp4"));
}

@Test
public void testFindScreenshot_matchesTypeScriptFormSpecScreenshotFolder() throws Exception {
	Path specFolder = Files.createDirectories(tempDir.resolve("mysolution.myform.spec.cy.ts"));
	Files.createFile(specFolder.resolve("test -- should work.png"));

	assertNotNull(invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform"),
		"screenshot under a .spec.cy.ts form spec folder must be found");
}

@Test
public void testFindScreenshot_ignoresOtherFormsTypeScriptSpecFolder() throws Exception {
	Path specFolder = Files.createDirectories(tempDir.resolve("otherform.spec.cy.ts"));
	Files.createFile(specFolder.resolve("test.png"));

	assertNull(invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform"));
}

	// --- hoistMeasurementMarkers (SVY-21619) -----------------------------------
	// Private helper that both runners call on the captured Cypress output (pass AND fail)
	// to surface the SVY_MEASURE lines into a readable "Measured layout" summary. Exercised
	// via reflection since it is private, the same technique as findMatchingFile above.

	private static String invokeHoistMeasurementMarkers(FormSpecRunner runner, String rawOutput) throws Exception {
		Method m = FormSpecRunner.class.getDeclaredMethod("hoistMeasurementMarkers", String.class);
		m.setAccessible(true);
		return (String) m.invoke(runner, rawOutput);
	}

	@Test
	public void testSvyMeasureMarker_isTheSharedLiteral() {
		assertEquals("SVY_MEASURE ", FormSpecRunner.SVY_MEASURE_MARKER,
				"the runner marker must be the same literal the generated specs log");
	}

	@Test
	public void testHoistMeasurementMarkers_extractsSummaryWithNumbers() throws Exception {
		String rawOutput = "some cypress noise\n"
				+ "SVY_MEASURE innerWidth=375 container=.svy-form containerWidth=373 requested=375\n"
				+ "more noise\n"
				+ "SVY_MEASURE innerWidth=1024 container=.svy-form containerWidth=1022 requested=1024\n"
				+ "trailing noise\n";

		String summary = invokeHoistMeasurementMarkers(new FormSpecRunner(), rawOutput);

		assertNotNull(summary);
		assertFalse(summary.isBlank(), "a summary must be produced when markers are present");
		assertTrue(summary.contains("Measured layout"), "summary must carry the 'Measured layout' heading: " + summary);
		assertTrue(summary.contains("innerWidth=375") && summary.contains("containerWidth=373"),
				"summary must surface the first measurement's numbers: " + summary);
		assertTrue(summary.contains("innerWidth=1024") && summary.contains("containerWidth=1022"),
				"summary must surface the second measurement's numbers: " + summary);
	}

	@Test
	public void testHoistMeasurementMarkers_matchesCypressPrefixedMarkerLine() throws Exception {
		// cy.log output appears with a Cypress log prefix; the helper must match on contains, not startsWith.
		String rawOutput = "  cy:log  ✓  SVY_MEASURE innerWidth=480 container=.svy-form containerWidth=478\n";

		String summary = invokeHoistMeasurementMarkers(new FormSpecRunner(), rawOutput);

		assertFalse(summary.isBlank(), "a Cypress-prefixed marker line must still be matched (contains, not startsWith)");
		assertTrue(summary.contains("innerWidth=480") && summary.contains("containerWidth=478"),
				"the prefixed line's numbers must be surfaced: " + summary);
		assertFalse(summary.contains("cy:log"), "the Cypress log decoration before the marker must be dropped: " + summary);
	}

	@Test
	public void testHoistMeasurementMarkers_noMarker_returnsEmpty() throws Exception {
		String rawOutput = "Running form spec\n2 passing\nAll good\n";

		String summary = invokeHoistMeasurementMarkers(new FormSpecRunner(), rawOutput);

		assertNotNull(summary);
		assertTrue(summary.isEmpty(), "with no SVY_MEASURE line the helper must return an empty string so the caller output is unchanged");
	}

	@Test
	public void testHoistMeasurementMarkers_nullOrEmpty_returnsEmpty() throws Exception {
		assertEquals("", invokeHoistMeasurementMarkers(new FormSpecRunner(), null));
		assertEquals("", invokeHoistMeasurementMarkers(new FormSpecRunner(), ""));
	}

	@Test
	public void testHoistMeasurementMarkers_isDeclaredPrivate() throws Exception {
		// The hoisting helper that both runFormCypressTests and runE2ECypressTests prepend on
		// pass and fail must exist as a private helper (the marker-append precedent, SVY-21619).
		Method m = FormSpecRunner.class.getDeclaredMethod("hoistMeasurementMarkers", String.class);
		assertEquals(String.class, m.getReturnType(), "hoistMeasurementMarkers must return the summary String");
		assertTrue(java.lang.reflect.Modifier.isPrivate(m.getModifiers()), "hoistMeasurementMarkers must be a private helper");
	}

	@Test
	public void testHoistMeasurementMarkers_markerWithNoPayload_returnsEmpty() throws Exception {
		// A bare marker line carries no measurement: it must not produce a "Measured layout" heading with no rows.
		assertEquals("", invokeHoistMeasurementMarkers(new FormSpecRunner(), "SVY_MEASURE \nSVY_MEASURE    \n"));
	}

	@Test
	public void testHoistMeasurementMarkers_dropsTrailingCarriageReturn() throws Exception {
		String summary = invokeHoistMeasurementMarkers(new FormSpecRunner(),
				"SVY_MEASURE innerWidth=375 container=.svy-form containerWidth=375 requested=375\r\n");

		assertTrue(summary.contains("requested=375\n"), "the CR from a Windows line ending must be trimmed: " + summary.replace("\r", "\\r"));
		assertFalse(summary.contains("\r"));
	}

	// --- ensureCypressConfig / isConfigStale (SVY-21619) -----------------------
	// Private helpers that keep the generated cypress.config.js able to carry the svyMeasure task to stdout.

	private static void invokeEnsureCypressConfig(FormSpecRunner runner, Path cypressDir, String baseUrl) throws Exception {
		Method m = FormSpecRunner.class.getDeclaredMethod("ensureCypressConfig", Path.class, String.class);
		m.setAccessible(true);
		m.invoke(runner, cypressDir, baseUrl);
	}

	private static boolean invokeIsConfigStale(FormSpecRunner runner, Path configFile) throws Exception {
		Method m = FormSpecRunner.class.getDeclaredMethod("isConfigStale", Path.class);
		m.setAccessible(true);
		return (Boolean) m.invoke(runner, configFile);
	}

	@Test
	public void testEnsureCypressConfig_writesConfigRegisteringTheMeasureTask() throws Exception {
		Path cypressDir = tempDir.resolve("cypress");

		invokeEnsureCypressConfig(new FormSpecRunner(), cypressDir, "http://localhost:8183");

		String config = Files.readString(cypressDir.resolve("cypress.config.js"));
		assertTrue(config.contains("baseUrl: 'http://localhost:8183'"), config);
		assertTrue(config.contains("supportFile: 'cypress/support/e2e.js'"), "the support file must be enabled so the commands load: " + config);
		assertTrue(config.contains("setupNodeEvents"), config);
		assertTrue(config.contains("svyMeasure"), "the svyMeasure task must be registered so the marker reaches stdout: " + config);
		assertFalse(invokeIsConfigStale(new FormSpecRunner(), cypressDir.resolve("cypress.config.js")),
				"a freshly written config is never stale");
	}

	@Test
	public void testEnsureCypressConfig_overwritesAStaleConfig() throws Exception {
		Path cypressDir = Files.createDirectories(tempDir.resolve("cypress"));
		Path config = cypressDir.resolve("cypress.config.js");
		Files.writeString(config, "module.exports = { e2e: { supportFile: false } };\n");

		invokeEnsureCypressConfig(new FormSpecRunner(), cypressDir, "http://localhost:8183");

		assertTrue(Files.readString(config).contains("svyMeasure"), "the stale config must be refreshed to the canonical content");
	}

	@Test
	public void testEnsureCypressConfig_isNonFatalWhenTheDirectoryCannotBeCreated() throws Exception {
		Path blocker = Files.createFile(tempDir.resolve("not-a-directory"));

		invokeEnsureCypressConfig(new FormSpecRunner(), blocker.resolve("cypress"), "http://localhost:8183");

		assertFalse(Files.exists(blocker.resolve("cypress")), "nothing can be created below a regular file, and no exception may escape");
	}

	@Test
	public void testIsConfigStale_missingFile_isNotStale() throws Exception {
		assertFalse(invokeIsConfigStale(new FormSpecRunner(), tempDir.resolve("absent.config.js")),
				"a missing config is created by the caller's own branch, it is not 'stale'");
	}

	@Test
	public void testIsConfigStale_configWithoutTheMeasureTask_isStale() throws Exception {
		Path config = Files.writeString(tempDir.resolve("cypress.config.js"), "module.exports = { e2e: { supportFile: 'cypress/support/e2e.js' } };\n");

		assertTrue(invokeIsConfigStale(new FormSpecRunner(), config));
	}

	@Test
	public void testIsConfigStale_configThatDisablesTheSupportFile_isStale() throws Exception {
		Path config = Files.writeString(tempDir.resolve("cypress.config.js"),
				"// svyMeasure\nmodule.exports = { e2e: { supportFile: false } };\n");

		assertTrue(invokeIsConfigStale(new FormSpecRunner(), config), "supportFile: false disables the measurement commands entirely");
	}

	@Test
	public void testIsConfigStale_currentConfig_isNotStale() throws Exception {
		Path config = Files.writeString(tempDir.resolve("cypress.config.js"),
				"// svyMeasure\nmodule.exports = { e2e: { supportFile: 'cypress/support/e2e.js' } };\n");

		assertFalse(invokeIsConfigStale(new FormSpecRunner(), config));
	}

	@Test
	public void testIsConfigStale_unreadableConfig_isTreatedAsStale() throws Exception {
		Path directoryNamedLikeAConfig = Files.createDirectory(tempDir.resolve("cypress.config.js"));

		assertTrue(invokeIsConfigStale(new FormSpecRunner(), directoryNamedLikeAConfig),
				"a config that cannot be read must be regenerated, never trusted");
	}
}
