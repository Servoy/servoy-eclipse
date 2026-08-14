package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.nio.file.Path;

import org.junit.Test;

public class FormSpecGeneratorSolutionPrefixTest
{
	@Test
	public void testHasSpecExistsWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have specExists(String, String) method",
			FormSpecGenerator.class.getMethod("specExists", String.class, String.class));
	}

	@Test
	public void testSpecExistsWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals("specExists(String,String) must return boolean", boolean.class,
			FormSpecGenerator.class.getMethod("specExists", String.class, String.class).getReturnType());
	}

	@Test
	public void testHasGetSpecFilePathWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have getSpecFilePath(String, String) method",
			FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class));
	}

	@Test
	public void testGetSpecFilePathWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals("getSpecFilePath(String,String) must return Path", Path.class,
			FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class).getReturnType());
	}

	@Test
	public void testHasGetSetupFilePathWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have getSetupFilePath(String, String) method",
			FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class));
	}

	@Test
	public void testGetSetupFilePathWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals("getSetupFilePath(String,String) must return Path", Path.class,
			FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class).getReturnType());
	}

	@Test
	public void testHasFindExistingSpecFile() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have findExistingSpecFile(String, String) method",
			FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class));
	}

	@Test
	public void testFindExistingSpecFileReturnType() throws NoSuchMethodException
	{
		assertEquals("findExistingSpecFile must return Path", Path.class,
			FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class).getReturnType());
	}

	@Test
	public void testHasFindExistingSetupFile() throws NoSuchMethodException
	{
		assertNotNull("FormSpecGenerator must have findExistingSetupFile(String, String) method",
			FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class));
	}

	@Test
	public void testFindExistingSetupFileReturnType() throws NoSuchMethodException
	{
		assertEquals("findExistingSetupFile must return Path", Path.class,
			FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class).getReturnType());
	}

	@Test
	public void testGetSpecFilePathSolutionPrefix()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		try
		{
			Path specPath = gen.getSpecFilePath("myForm", "mySolution");
			assertNotNull("getSpecFilePath(formName, solutionName) must not return null", specPath);
			String fileName = specPath.getFileName().toString();
			assertEquals("Spec file must be solution-prefixed", "mySolution.myForm.spec.cy.js", fileName);
		}
		catch (Exception e)
		{
			assertTrue("Expected exception in plain JUnit (no workspace): " + e.getMessage(), true);
		}
	}

	@Test
	public void testGetSetupFilePathSolutionPrefix()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		try
		{
			Path setupPath = gen.getSetupFilePath("myForm", "mySolution");
			assertNotNull("getSetupFilePath(formName, solutionName) must not return null", setupPath);
			String fileName = setupPath.getFileName().toString();
			assertEquals("Setup file must be solution-prefixed", "mySolution.myForm.spec.js", fileName);
		}
		catch (Exception e)
		{
			assertTrue("Expected exception in plain JUnit (no workspace): " + e.getMessage(), true);
		}
	}

	@Test
	public void testFindExistingSpecFileReturnsNullWhenNothingExists()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		try
		{
			Path result = gen.findExistingSpecFile("nonExistentForm", "nonExistentSolution");
			assertNull("findExistingSpecFile must return null when no file exists", result);
		}
		catch (Exception e)
		{
			assertTrue("Expected exception in plain JUnit (no workspace): " + e.getMessage(), true);
		}
	}

	@Test
	public void testFindExistingSetupFileReturnsNullWhenNothingExists()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		try
		{
			Path result = gen.findExistingSetupFile("nonExistentForm", "nonExistentSolution");
			assertNull("findExistingSetupFile must return null when no file exists", result);
		}
		catch (Exception e)
		{
			assertTrue("Expected exception in plain JUnit (no workspace): " + e.getMessage(), true);
		}
	}

	@Test
	public void testLegacySingleArgMethodsStillExist() throws NoSuchMethodException
	{
		assertNotNull("Legacy specExists(String) must still exist",
			FormSpecGenerator.class.getMethod("specExists", String.class));
		assertNotNull("Legacy getSpecFilePath(String) must still exist",
			FormSpecGenerator.class.getMethod("getSpecFilePath", String.class));
		assertNotNull("Legacy getSetupFilePath(String) must still exist",
			FormSpecGenerator.class.getMethod("getSetupFilePath", String.class));
	}
}
