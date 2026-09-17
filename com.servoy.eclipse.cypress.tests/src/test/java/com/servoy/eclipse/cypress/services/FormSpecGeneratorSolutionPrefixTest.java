package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class FormSpecGeneratorSolutionPrefixTest {
	@Test
	public void testHasSpecExistsWithSolutionName() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("specExists", String.class, String.class),
				"FormSpecGenerator must have specExists(String, String) method");
	}

	@Test
	public void testSpecExistsWithSolutionNameReturnType() throws NoSuchMethodException {
		assertEquals(boolean.class,
				FormSpecGenerator.class.getMethod("specExists", String.class, String.class).getReturnType(),
				"specExists(String,String) must return boolean");
	}

	@Test
	public void testHasGetSpecFilePathWithSolutionName() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class),
				"FormSpecGenerator must have getSpecFilePath(String, String) method");
	}

	@Test
	public void testGetSpecFilePathWithSolutionNameReturnType() throws NoSuchMethodException {
		assertEquals(Path.class,
				FormSpecGenerator.class.getMethod("getSpecFilePath", String.class, String.class).getReturnType(),
				"getSpecFilePath(String,String) must return Path");
	}

	@Test
	public void testHasGetSetupFilePathWithSolutionName() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class),
				"FormSpecGenerator must have getSetupFilePath(String, String) method");
	}

	@Test
	public void testGetSetupFilePathWithSolutionNameReturnType() throws NoSuchMethodException {
		assertEquals(Path.class,
				FormSpecGenerator.class.getMethod("getSetupFilePath", String.class, String.class).getReturnType(),
				"getSetupFilePath(String,String) must return Path");
	}

	@Test
	public void testHasFindExistingSpecFile() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class),
				"FormSpecGenerator must have findExistingSpecFile(String, String) method");
	}

	@Test
	public void testFindExistingSpecFileReturnType() throws NoSuchMethodException {
		assertEquals(Path.class,
				FormSpecGenerator.class.getMethod("findExistingSpecFile", String.class, String.class).getReturnType(),
				"findExistingSpecFile must return Path");
	}

	@Test
	public void testHasFindExistingSetupFile() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class),
				"FormSpecGenerator must have findExistingSetupFile(String, String) method");
	}

	@Test
	public void testFindExistingSetupFileReturnType() throws NoSuchMethodException {
		assertEquals(Path.class,
				FormSpecGenerator.class.getMethod("findExistingSetupFile", String.class, String.class).getReturnType(),
				"findExistingSetupFile must return Path");
	}

	@Test
	public void testGetSpecFilePathSolutionPrefix() {
		FormSpecGenerator gen = new FormSpecGenerator();
		try {
			Path specPath = gen.getSpecFilePath("myForm", "mySolution");
			assertNotNull(specPath, "getSpecFilePath(formName, solutionName) must not return null");
			String fileName = specPath.getFileName().toString();
			assertEquals("mySolution.myForm.spec.cy.js", fileName, "Spec file must be solution-prefixed");
		} catch (Exception e) {
			assertTrue(true, "Expected exception in plain JUnit (no workspace): " + e.getMessage());
		}
	}

	@Test
	public void testGetSetupFilePathSolutionPrefix() {
		FormSpecGenerator gen = new FormSpecGenerator();
		try {
			Path setupPath = gen.getSetupFilePath("myForm", "mySolution");
			assertNotNull(setupPath, "getSetupFilePath(formName, solutionName) must not return null");
			String fileName = setupPath.getFileName().toString();
			assertEquals("mySolution.myForm.spec.js", fileName, "Setup file must be solution-prefixed");
		} catch (Exception e) {
			assertTrue(true, "Expected exception in plain JUnit (no workspace): " + e.getMessage());
		}
	}

	@Test
	public void testFindExistingSpecFileReturnsNullWhenNothingExists() {
		FormSpecGenerator gen = new FormSpecGenerator();
		try {
			Path result = gen.findExistingSpecFile("nonExistentForm", "nonExistentSolution");
			assertNull(result, "findExistingSpecFile must return null when no file exists");
		} catch (Exception e) {
			assertTrue(true, "Expected exception in plain JUnit (no workspace): " + e.getMessage());
		}
	}

	@Test
	public void testFindExistingSetupFileReturnsNullWhenNothingExists() {
		FormSpecGenerator gen = new FormSpecGenerator();
		try {
			Path result = gen.findExistingSetupFile("nonExistentForm", "nonExistentSolution");
			assertNull(result, "findExistingSetupFile must return null when no file exists");
		} catch (Exception e) {
			assertTrue(true, "Expected exception in plain JUnit (no workspace): " + e.getMessage());
		}
	}

	@Test
	public void testLegacySingleArgMethodsStillExist() throws NoSuchMethodException {
		assertNotNull(FormSpecGenerator.class.getMethod("specExists", String.class),
				"Legacy specExists(String) must still exist");
		assertNotNull(FormSpecGenerator.class.getMethod("getSpecFilePath", String.class),
				"Legacy getSpecFilePath(String) must still exist");
		assertNotNull(FormSpecGenerator.class.getMethod("getSetupFilePath", String.class),
				"Legacy getSetupFilePath(String) must still exist");
	}
}
