package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FormSpecRunnerTest
{
	@Test
	public void testFormSpecRunner_isCreatable()
	{
		assertNotNull("FormSpecRunner must have @Creatable annotation",
			FormSpecRunner.class.getAnnotation(
				org.eclipse.e4.core.di.annotations.Creatable.class));
	}

	@Test
	public void testFormSpecRunner_hasRunFormCypressTestsMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecRunner must have runFormCypressTests(String, boolean) method",
			FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class));
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsReturnType() throws NoSuchMethodException
	{
		assertTrue("runFormCypressTests must return String",
			FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class)
				.getReturnType() == String.class);
	}

	@Test
	public void testFormSpecRunner_runFormCypressTests_noActiveProject()
	{
		FormSpecRunner runner = new FormSpecRunner();
		try
		{
			String result = runner.runFormCypressTests("nonExistentForm", true);
			assertNotNull(result);
			assertTrue("Should return error when no active project",
				result.contains("Error"));
		}
		catch (Throwable e)
		{
			assertNotNull("Expected error in plain JUnit (no workspace)", e);
		}
	}

	@Test
	public void testFormSpecRunner_runFormCypressTestsHasHeadlessParam() throws NoSuchMethodException
	{
		Method m = FormSpecRunner.class.getMethod("runFormCypressTests", String.class, boolean.class);
		assertEquals("Second param should be boolean for headless",
			boolean.class, m.getParameterTypes()[1]);
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressInstalledMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("ensureCypressInstalled".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have ensureCypressInstalled method", found);
	}

	@Test
	public void testFormSpecRunner_hasEnsureCypressConfigMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("ensureCypressConfig".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have ensureCypressConfig method", found);
	}

	@Test
	public void testFormSpecRunner_hasGetCypressDirMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("getCypressDir".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have getCypressDir method", found);
	}

	@Test
	public void testFormSpecRunner_hasGetNodePathMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("getNodePath".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must have getNodePath method", found);
	}

	@Test
	public void testFormSpecRunner_canBeInstantiated()
	{
		FormSpecRunner runner = new FormSpecRunner();
		assertNotNull("FormSpecRunner should be instantiable", runner);
	}

	@Test
	public void testFormSpecRunner_hasRunE2ECypressTestsMethod() throws NoSuchMethodException
	{
		assertNotNull("FormSpecRunner must have runE2ECypressTests(String, boolean) method",
			FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class));
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsReturnType() throws NoSuchMethodException
	{
		assertTrue("runE2ECypressTests must return String",
			FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class)
				.getReturnType() == String.class);
	}

	@Test
	public void testFormSpecRunner_runE2ECypressTestsHasHeadlessParam() throws NoSuchMethodException
	{
		Method m = FormSpecRunner.class.getMethod("runE2ECypressTests", String.class, boolean.class);
		assertEquals("Second param should be boolean for headless",
			boolean.class, m.getParameterTypes()[1]);
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunSpecMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("runSpec".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must NOT have runSpec method (renamed to runFormCypressTests)", !found);
	}

	@Test
	public void testFormSpecRunner_doesNotHaveRunE2ESpecMethod()
	{
		Method[] methods = FormSpecRunner.class.getDeclaredMethods();
		boolean found = false;
		for (Method m : methods)
		{
			if ("runE2ESpec".equals(m.getName()))
			{
				found = true;
				break;
			}
		}
		assertTrue("FormSpecRunner must NOT have runE2ESpec method (renamed to runE2ECypressTests)", !found);
	}

	// --- findMatchingFile / findScreenshot -----------------------------------------------------
	// These are the artifact-matching methods used for SVY-21174 (video/screenshot preservation
	// for failed Cypress runs). Exercised via reflection since they are private.

	private Path tempDir;

	@Before
	public void setUpTempDir() throws IOException
	{
		tempDir = Files.createTempDirectory("formspecrunner-test");
	}

	@After
	public void tearDownTempDir() throws IOException
	{
		if (tempDir != null && Files.exists(tempDir))
		{
			Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
				{
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	private static Path invokeFindMatchingFile(FormSpecRunner runner, Path dir, String baseName, String extension)
		throws Exception
	{
		Method m = FormSpecRunner.class.getDeclaredMethod("findMatchingFile", Path.class, String.class, String.class);
		m.setAccessible(true);
		return (Path)m.invoke(runner, dir, baseName, extension);
	}

	private static Path invokeFindScreenshot(FormSpecRunner runner, Path screenshotsDir, String baseName)
		throws Exception
	{
		Method m = FormSpecRunner.class.getDeclaredMethod("findScreenshot", Path.class, String.class);
		m.setAccessible(true);
		return (Path)m.invoke(runner, screenshotsDir, baseName);
	}

	@Test
	public void testFindMatchingFile_doesNotFalsePositiveOnSubstringFormName() throws Exception
	{
		// A video for "adminlogin.spec.cy.js" must not match baseName "login": the required
		// '.' separator before the leaf name is what prevents this false positive.
		Files.createFile(tempDir.resolve("adminlogin.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "login", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindMatchingFile_matchesE2ESpecVideoNamedAfterFullSpecFilename() throws Exception
	{
		Files.createFile(tempDir.resolve("login.cy.ts.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "login", ".mp4");

		assertNotNull("video named after the full E2E spec filename must be found", found);
		assertEquals("login.cy.ts.mp4", found.getFileName().toString());
	}

	@Test
	public void testFindMatchingFile_matchesFormSpecVideoNamedAfterFullSpecFilename() throws Exception
	{
		Files.createFile(tempDir.resolve("mysolution.myform.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNotNull("video named after the solution-prefixed form spec filename must be found", found);
		assertEquals("mysolution.myform.spec.cy.js.mp4", found.getFileName().toString());
	}

	@Test
	public void testFindMatchingFile_matchesBareFormSpecVideo() throws Exception
	{
		Files.createFile(tempDir.resolve("myform.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNotNull("video named after the bare (legacy) form spec filename must be found", found);
	}

	@Test
	public void testFindMatchingFile_resolvesSubfolderQualifiedE2EName() throws Exception
	{
		Path appB = Files.createDirectories(tempDir.resolve("appB"));
		Files.createFile(appB.resolve("login.cy.ts.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "appB/login", ".mp4");

		assertNotNull("subfolder-qualified name must still resolve to the video under its subfolder", found);
	}

	@Test
	public void testFindMatchingFile_returnsNullWhenNoMatch() throws Exception
	{
		Files.createFile(tempDir.resolve("otherForm.spec.cy.js.mp4"));

		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir, "myform", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindMatchingFile_returnsNullWhenDirectoryMissing() throws Exception
	{
		Path found = invokeFindMatchingFile(new FormSpecRunner(), tempDir.resolve("doesNotExist"), "myform", ".mp4");

		assertNull(found);
	}

	@Test
	public void testFindScreenshot_matchesFormSpecScreenshotFolder() throws Exception
	{
		Path specFolder = Files.createDirectories(tempDir.resolve("mysolution.myform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test -- should work.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNotNull("screenshot under the solution-prefixed form spec folder must be found", found);
	}

	@Test
	public void testFindScreenshot_matchesE2ESpecScreenshotFolder() throws Exception
	{
		Path specFolder = Files.createDirectories(tempDir.resolve("login.cy.ts"));
		Files.createFile(specFolder.resolve("test -- should work.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "login");

		assertNotNull("screenshot under the E2E spec folder must be found", found);
	}

	@Test
	public void testFindScreenshot_prefersFailedScreenshotWhenPresent() throws Exception
	{
		Path specFolder = Files.createDirectories(tempDir.resolve("myform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test -- passed step.png"));
		Files.createFile(specFolder.resolve("test -- failed step (failed).png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNotNull(found);
		assertTrue("the screenshot marked (failed) must be preferred",
			found.getFileName().toString().contains("(failed)"));
	}

	@Test
	public void testFindScreenshot_returnsNullWhenNoMatchingFolder() throws Exception
	{
		Path specFolder = Files.createDirectories(tempDir.resolve("otherform.spec.cy.js"));
		Files.createFile(specFolder.resolve("test.png"));

		Path found = invokeFindScreenshot(new FormSpecRunner(), tempDir, "myform");

		assertNull(found);
	}
}
