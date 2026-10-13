package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.FormSpecGenerator;

public class FormSpecGeneratorSolutionPrefixTest
{
	@Test
	public void testHasSpecExistsWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("specExists", String.class, String.class),
			"FormSpecGenerator must have specExists(String, String) method");
	}

	@Test
	public void testSpecExistsWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals(boolean.class,
			FormSpecGenerator.class.getMethod("specExists", String.class, String.class).getReturnType(),
			"specExists(String,String) must return boolean");
	}

	@Test
	public void testHasGetSpecFilePathWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class),
			"FormSpecGenerator must have getSpecFilePath(String, String) method");
	}

	@Test
	public void testGetSpecFilePathWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals(Path.class,
			FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class).getReturnType(),
			"getSpecFilePath(String,String) must return Path");
	}

	@Test
	public void testHasGetSetupFilePathWithSolutionName() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class),
			"FormSpecGenerator must have getSetupFilePath(String, String) method");
	}

	@Test
	public void testGetSetupFilePathWithSolutionNameReturnType() throws NoSuchMethodException
	{
		assertEquals(Path.class,
			FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class).getReturnType(),
			"getSetupFilePath(String,String) must return Path");
	}

	@Test
	public void testHasFindExistingSpecFile() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class),
			"FormSpecGenerator must have findExistingSpecFile(String, String) method");
	}

	@Test
	public void testFindExistingSpecFileReturnType() throws NoSuchMethodException
	{
		assertEquals(Path.class,
			FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class).getReturnType(),
			"findExistingSpecFile must return Path");
	}

	@Test
	public void testHasFindExistingSetupFile() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class),
			"FormSpecGenerator must have findExistingSetupFile(String, String) method");
	}

	@Test
	public void testFindExistingSetupFileReturnType() throws NoSuchMethodException
	{
		assertEquals(Path.class,
			FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class).getReturnType(),
			"findExistingSetupFile must return Path");
	}

	// NOTE: solution-prefixed path generation (getSpecFilePath/getSetupFilePath
	// with a solutionName) requires a real workspace + active Servoy solution
	// (ResourcesPlugin.getWorkspace()), so the behavioral assertions for those
	// live in FormSpecGeneratorSolutionPrefixIntegrationTest (PDE plug-in test,
	// package ...cypress.integrationpde) instead of here.

	@Test
	public void testFindExistingSpecFileReturnsNullWhenNothingExists()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		Path result = gen.findExistingSpecFile("nonExistentForm", "nonExistentSolution");
		assertNull(result, "findExistingSpecFile must return null when no file exists");
	}

	@Test
	public void testFindExistingSetupFileReturnsNullWhenNothingExists()
	{
		FormSpecGenerator gen = new FormSpecGenerator();
		Path result = gen.findExistingSetupFile("nonExistentForm", "nonExistentSolution");
		assertNull(result, "findExistingSetupFile must return null when no file exists");
	}

	@Test
	public void testLegacySingleArgMethodsStillExist() throws NoSuchMethodException
	{
		assertNotNull(FormSpecGenerator.class.getMethod("oldSpecExists", String.class),
			"Legacy specExists(String) must still exist");
		assertNotNull(FormSpecGenerator.class.getMethod("getOldSpecFilePath", String.class),
			"Legacy getSpecFilePath(String) must still exist");
		assertNotNull(FormSpecGenerator.class.getMethod("getOldSetupFilePath", String.class),
			"Legacy getSetupFilePath(String) must still exist");
	}
}
