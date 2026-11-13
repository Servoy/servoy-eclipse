package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;

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

	@Nested
	@DisplayName("TypeScript form specs (.spec.cy.ts)")
	class TypeScriptFormSpecs
	{
		@Test
		@DisplayName("hasTest finds a .spec.cy.ts file for the form")
		void hasTestFindsTsSpec() throws IOException
		{
			Files.createFile(tempDir.resolve("myForm.spec.cy.ts"));

			assertTrue(service.hasTest("myForm"));
			assertTrue(service.hasFormTest("myForm"));
		}

		@Test
		@DisplayName("hasTest finds a solution-prefixed .spec.cy.ts file")
		void hasTestFindsSolutionPrefixedTsSpec() throws IOException
		{
			Files.createFile(tempDir.resolve("mySolution.myForm.spec.cy.ts"));

			assertTrue(service.hasTest("myForm"));
		}

		@Test
		@DisplayName("hasTest does not match a different form's .spec.cy.ts file")
		void hasTestIgnoresOtherFormTsSpec() throws IOException
		{
			Files.createFile(tempDir.resolve("mySolution.otherForm.spec.cy.ts"));

			assertFalse(service.hasTest("myForm"));
		}

		@Test
		@DisplayName("discoverAllTestForms lists .js and .ts specs together, extension stripped")
		void discoversBothExtensions() throws IOException
		{
			Files.createFile(tempDir.resolve("formA.spec.cy.js"));
			Files.createFile(tempDir.resolve("formB.spec.cy.ts"));

			List<String> forms = service.discoverAllTestForms();

			assertEquals(2, forms.size());
			assertAll(() -> assertTrue(forms.contains("formA")), () -> assertTrue(forms.contains("formB")));
		}

		@Test
		@DisplayName("discoverAllTestForms lists a form once when it has both a .js and a .ts spec")
void discoversFormOnceWhenBothExtensionsExist() throws IOException
{
	Files.createFile(tempDir.resolve("formA.spec.cy.js"));
	Files.createFile(tempDir.resolve("formA.spec.cy.ts"));

	List<String> forms = service.discoverAllTestForms();

	assertEquals(List.of("formA"), forms);
}

@Test
@DisplayName("hasAnyTest is true when only a .spec.cy.ts file exists")
		void hasAnyTestWithOnlyTsSpec() throws IOException
		{
			Files.createFile(tempDir.resolve("someForm.spec.cy.ts"));

			assertTrue(service.hasAnyTest());
		}
	}

	@Nested
	@DisplayName("FormSpecGenerator extension detection")
	class ExtensionDetection
	{
		@Test
		@DisplayName("defaults to .spec.cy.js when the directory is null or missing")
		void defaultsToJs()
		{
			assertEquals(".spec.cy.js", FormSpecGenerator.detectSpecExtension(null));
			assertEquals(".spec.cy.js", FormSpecGenerator.detectSpecExtension(tempDir.resolve("nonexistent")));
		}

		@Test
		@DisplayName("defaults to .spec.cy.js when the directory has no form specs")
		void defaultsToJsWhenEmpty() throws IOException
		{
			Files.createFile(tempDir.resolve("README.md"));

			assertEquals(".spec.cy.js", FormSpecGenerator.detectSpecExtension(tempDir));
		}

		@Test
		@DisplayName("picks .spec.cy.ts when TypeScript form specs outnumber JavaScript ones")
		void picksTsWhenTsDominates() throws IOException
		{
			Files.createFile(tempDir.resolve("a.spec.cy.ts"));
			Files.createFile(tempDir.resolve("b.spec.cy.ts"));
			Files.createFile(tempDir.resolve("c.spec.cy.js"));

			assertEquals(".spec.cy.ts", FormSpecGenerator.detectSpecExtension(tempDir));
		}

		@Test
		@DisplayName("keeps .spec.cy.js on a tie")
		void keepsJsOnTie() throws IOException
		{
			Files.createFile(tempDir.resolve("a.spec.cy.ts"));
			Files.createFile(tempDir.resolve("b.spec.cy.js"));

			assertEquals(".spec.cy.js", FormSpecGenerator.detectSpecExtension(tempDir));
		}

		@Test
		@DisplayName("detectSpecExtension ignores setup files and run artifacts that only look like form specs")
void ignoresSetupFilesAndArtifacts() throws IOException
{
	Files.createFile(tempDir.resolve("a.spec.ts"));
	Files.createFile(tempDir.resolve("b.spec.ts"));
	Files.createFile(tempDir.resolve("c.spec.cy.ts.mp4"));
	Files.createFile(tempDir.resolve("d.spec.cy.js"));

	assertEquals(".spec.cy.js", FormSpecGenerator.detectSpecExtension(tempDir));
}

@Test
@DisplayName("firstExistingSetup finds a .spec.js and a .spec.ts setup file, .js first")
		void setupLookupFindsBothExtensions() throws IOException
		{
			assertEquals(null, FormSpecGenerator.firstExistingSetup(tempDir, "sol.formA"));

			Files.createFile(tempDir.resolve("sol.formA.spec.ts"));
			assertEquals("sol.formA.spec.ts", FormSpecGenerator.firstExistingSetup(tempDir, "sol.formA").getFileName().toString());

			Files.createFile(tempDir.resolve("sol.formA.spec.js"));
			assertEquals("sol.formA.spec.js", FormSpecGenerator.firstExistingSetup(tempDir, "sol.formA").getFileName().toString());
		}

		@Test
		@DisplayName("firstExisting finds a .spec.cy.ts form spec and does not mistake a setup .spec.ts for it")
		void formSpecLookupIgnoresSetupFiles() throws IOException
		{
			Files.createFile(tempDir.resolve("sol.formA.spec.ts"));
			assertEquals(null, FormSpecGenerator.firstExisting(tempDir, "sol.formA"));

			Files.createFile(tempDir.resolve("sol.formA.spec.cy.ts"));
			assertEquals("sol.formA.spec.cy.ts", FormSpecGenerator.firstExisting(tempDir, "sol.formA").getFileName().toString());
		}

		@Test
		@DisplayName("isFormSpecFileName and stripFormSpecExtension handle both extensions")
		void helpersHandleBothExtensions()
		{
			assertTrue(FormSpecGenerator.isFormSpecFileName("f.spec.cy.js"));
			assertTrue(FormSpecGenerator.isFormSpecFileName("f.spec.cy.ts"));
			assertFalse(FormSpecGenerator.isFormSpecFileName("f.spec.js"));
			assertEquals("my.form", FormSpecGenerator.stripFormSpecExtension("my.form.spec.cy.ts"));
			assertEquals("my.form", FormSpecGenerator.stripFormSpecExtension("my.form.spec.cy.js"));
			assertEquals("f.spec.js", FormSpecGenerator.stripFormSpecExtension("f.spec.js"));
		}
	}

	/**
	 * Exercises the private {@code collectE2ETestNames(Path baseDir, Path scanRoot)} helper
	 * directly via reflection. This is the logic backing {@code discoverAllE2ETests()} and
	 * {@code discoverSolutionE2ETests()}, which cannot be driven end-to-end here because they
	 * resolve their scan directory from the live Eclipse workspace root rather than an injectable
	 * field.
	 */
	@Nested
	@DisplayName("collectE2ETestNames")
	class CollectE2ETestNames
	{
		@SuppressWarnings("unchecked")
		private List<String> invoke(Path baseDir, Path scanRoot) throws Exception
		{
			Method method = CypressTestDiscoveryService.class.getDeclaredMethod("collectE2ETestNames", Path.class,
				Path.class);
			method.setAccessible(true);
			return (List<String>)method.invoke(service, baseDir, scanRoot);
		}

		@Test
		@DisplayName("returns bare name for a spec directly under the base dir")
		void bareNameAtRoot() throws Exception
		{
			Files.createFile(tempDir.resolve("login.cy.ts"));

			List<String> names = invoke(tempDir, tempDir);

			assertEquals(List.of("login"), names);
		}

		@Test
		@DisplayName("returns a subfolder-qualified name for a nested spec")
		void subfolderQualifiedName() throws Exception
		{
			Path appA = Files.createDirectories(tempDir.resolve("appA"));
			Files.createFile(appA.resolve("login.cy.ts"));

			List<String> names = invoke(tempDir, tempDir);

			assertEquals(List.of("appA/login"), names);
		}

		@Test
		@DisplayName("keeps same-named specs in different subfolders distinct")
		void distinguishesSameNameInDifferentSubfolders() throws Exception
		{
			Path appA = Files.createDirectories(tempDir.resolve("appA"));
			Path appB = Files.createDirectories(tempDir.resolve("appB"));
			Files.createFile(appA.resolve("login.cy.ts"));
			Files.createFile(appB.resolve("login.cy.js"));

			List<String> names = invoke(tempDir, tempDir);

			assertEquals(2, names.size());
			assertAll(() -> assertTrue(names.contains("appA/login")),
				() -> assertTrue(names.contains("appB/login")));
		}

		@Test
		@DisplayName("uses forward slashes even on backslash-style path separators")
		void usesForwardSlashSeparator() throws Exception
		{
			Path nested = Files.createDirectories(tempDir.resolve("appA").resolve("sub"));
			Files.createFile(nested.resolve("login.cy.ts"));

			List<String> names = invoke(tempDir, tempDir);

			assertEquals(List.of("appA/sub/login"), names);
			assertFalse(names.get(0).contains("\\"));
		}

		@Test
		@DisplayName("names are relative to baseDir even when scanRoot is a subfolder")
		void namesRelativeToBaseDirWhenScanRootIsSubfolder() throws Exception
		{
			Path solutionDir = Files.createDirectories(tempDir.resolve("mySolution"));
			Files.createFile(solutionDir.resolve("login.cy.ts"));

			List<String> names = invoke(tempDir, solutionDir);

			assertEquals(List.of("mySolution/login"), names);
		}

		@Test
		@DisplayName("ignores non .cy.js/.cy.ts files")
		void ignoresUnrelatedFiles() throws Exception
		{
			Files.createFile(tempDir.resolve("readme.md"));
			Files.createFile(tempDir.resolve("helper.js"));

			List<String> names = invoke(tempDir, tempDir);

			assertTrue(names.isEmpty());
		}
	}
}
