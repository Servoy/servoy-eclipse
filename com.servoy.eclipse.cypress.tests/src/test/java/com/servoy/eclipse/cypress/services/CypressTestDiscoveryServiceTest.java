package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CypressTestDiscoveryService")
public class CypressTestDiscoveryServiceTest
{
	private Path tempDir;
	private CypressTestDiscoveryService service;

	@BeforeEach
	void setUp() throws Exception
	{
		tempDir = Files.createTempDirectory("cypress-discovery-test");
		service = new CypressTestDiscoveryService();
		injectMockSpecGenerator(tempDir);
	}

	@AfterEach
	void tearDown() throws Exception
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

	private void injectMockSpecGenerator(Path formsDir) throws Exception
	{
		FormSpecGenerator mockGenerator = new FormSpecGenerator()
		{
			@Override
			public Path getFormSpecDir()
			{
				return formsDir;
			}
		};
		Field field = CypressTestDiscoveryService.class.getDeclaredField("specGenerator");
		field.setAccessible(true);
		field.set(service, mockGenerator);
	}

	@Nested
	@DisplayName("hasTest")
	class HasTest
	{
		@Test
		@DisplayName("returns true when .spec.cy.js file exists for form")
		void returnsTrueWhenSpecFileExists() throws IOException
		{
			Files.createFile(tempDir.resolve("myForm.spec.cy.js"));

			assertTrue(service.hasTest("myForm"));
		}

		@Test
		@DisplayName("returns false when no .spec.cy.js file exists for form")
		void returnsFalseWhenNoSpecFile()
		{
			assertFalse(service.hasTest("nonExistentForm"));
		}

		@Test
		@DisplayName("returns false when directory contains unrelated files")
		void returnsFalseForUnrelatedFiles() throws IOException
		{
			Files.createFile(tempDir.resolve("myForm.js"));
			Files.createFile(tempDir.resolve("myForm.spec.js"));

			assertFalse(service.hasTest("myForm"));
		}

		@ParameterizedTest
		@ValueSource(strings = { "formA", "formB", "my_form_123" })
		@DisplayName("correctly resolves different form names")
		void resolvesVariousFormNames(String formName) throws IOException
		{
			Files.createFile(tempDir.resolve(formName + ".spec.cy.js"));

			assertTrue(service.hasTest(formName));
		}

		@Test
		@DisplayName("returns false when directory is null")
		void returnsFalseWhenDirIsNull() throws Exception
		{
			injectMockSpecGenerator(null);

			assertFalse(service.hasTest("anyForm"));
		}

		@Test
		@DisplayName("returns false when directory does not exist")
		void returnsFalseWhenDirDoesNotExist() throws Exception
		{
			injectMockSpecGenerator(tempDir.resolve("nonexistent"));

			assertFalse(service.hasTest("anyForm"));
		}
	}

	@Nested
	@DisplayName("discoverAllTestForms")
	class DiscoverAllTestForms
	{
		@Test
		@DisplayName("returns empty list when no spec files exist")
		void returnsEmptyListWhenNoFiles()
		{
			List<String> forms = service.discoverAllTestForms();

			assertNotNull(forms);
			assertTrue(forms.isEmpty());
		}

		@Test
		@DisplayName("discovers single form test")
		void discoversSingleForm() throws IOException
		{
			Files.createFile(tempDir.resolve("loginForm.spec.cy.js"));

			List<String> forms = service.discoverAllTestForms();

			assertEquals(1, forms.size());
			assertEquals("loginForm", forms.get(0));
		}

		@Test
		@DisplayName("discovers multiple form tests")
		void discoversMultipleForms() throws IOException
		{
			Files.createFile(tempDir.resolve("formA.spec.cy.js"));
			Files.createFile(tempDir.resolve("formB.spec.cy.js"));
			Files.createFile(tempDir.resolve("formC.spec.cy.js"));

			List<String> forms = service.discoverAllTestForms();

			assertEquals(3, forms.size());
			assertAll(
				() -> assertTrue(forms.contains("formA")),
				() -> assertTrue(forms.contains("formB")),
				() -> assertTrue(forms.contains("formC")));
		}

		@Test
		@DisplayName("ignores non-spec files in directory")
		void ignoresNonSpecFiles() throws IOException
		{
			Files.createFile(tempDir.resolve("myForm.spec.cy.js"));
			Files.createFile(tempDir.resolve("helper.js"));
			Files.createFile(tempDir.resolve("myForm.spec.js"));
			Files.createFile(tempDir.resolve("README.md"));

			List<String> forms = service.discoverAllTestForms();

			assertEquals(1, forms.size());
			assertEquals("myForm", forms.get(0));
		}

		@Test
		@DisplayName("returns empty list when directory is null")
		void returnsEmptyWhenDirIsNull() throws Exception
		{
			injectMockSpecGenerator(null);

			List<String> forms = service.discoverAllTestForms();

			assertNotNull(forms);
			assertTrue(forms.isEmpty());
		}

		@Test
		@DisplayName("returns empty list when directory does not exist")
		void returnsEmptyWhenDirDoesNotExist() throws Exception
		{
			injectMockSpecGenerator(tempDir.resolve("nonexistent"));

			List<String> forms = service.discoverAllTestForms();

			assertNotNull(forms);
			assertTrue(forms.isEmpty());
		}

		@Test
		@DisplayName("extracts form name correctly by stripping .spec.cy.js extension")
		void extractsFormNameCorrectly() throws IOException
		{
			Files.createFile(tempDir.resolve("my.complex.form.spec.cy.js"));

			List<String> forms = service.discoverAllTestForms();

			assertEquals(1, forms.size());
			assertEquals("my.complex.form", forms.get(0));
		}
	}

	@Nested
	@DisplayName("hasAnyTest")
	class HasAnyTest
	{
		@Test
		@DisplayName("returns true when at least one spec file exists")
		void returnsTrueWhenSpecExists() throws IOException
		{
			Files.createFile(tempDir.resolve("someForm.spec.cy.js"));

			assertTrue(service.hasAnyTest());
		}

		@Test
		@DisplayName("returns false when no spec files exist")
		void returnsFalseWhenNoSpecFiles()
		{
			assertFalse(service.hasAnyTest());
		}

		@Test
		@DisplayName("returns false when only non-spec files exist")
		void returnsFalseForNonSpecFiles() throws IOException
		{
			Files.createFile(tempDir.resolve("helper.js"));
			Files.createFile(tempDir.resolve("myForm.spec.js"));

			assertFalse(service.hasAnyTest());
		}

		@Test
		@DisplayName("returns false when directory is null")
		void returnsFalseWhenDirIsNull() throws Exception
		{
			injectMockSpecGenerator(null);

			assertFalse(service.hasAnyTest());
		}

		@Test
		@DisplayName("returns false when directory does not exist")
		void returnsFalseWhenDirDoesNotExist() throws Exception
		{
			injectMockSpecGenerator(tempDir.resolve("nonexistent"));

			assertFalse(service.hasAnyTest());
		}

		@Test
		@DisplayName("returns true even with many non-spec files plus one spec")
		void returnsTrueWithMixedFiles() throws IOException
		{
			Files.createFile(tempDir.resolve("a.js"));
			Files.createFile(tempDir.resolve("b.txt"));
			Files.createFile(tempDir.resolve("c.spec.js"));
			Files.createFile(tempDir.resolve("oneForm.spec.cy.js"));

			assertTrue(service.hasAnyTest());
		}
	}
}

