package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import com.servoy.j2db.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;

@DisplayName("CypressEditorInputPropertyTester")
public class CypressEditorInputPropertyTesterTest {
	private CypressEditorInputPropertyTester tester;
	private Path tempDir;

	@BeforeEach
	void setUp() throws Exception {
		tester = new CypressEditorInputPropertyTester();
		tempDir = Files.createTempDirectory("cypress-editor-tester");
		injectMockDiscoveryService(tempDir);
	}

	@AfterEach
	void tearDown() throws Exception {
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

	private void injectMockDiscoveryService(Path formsDir) throws Exception {
		CypressTestDiscoveryService mockService = new CypressTestDiscoveryService();
		FormSpecGenerator mockGenerator = new FormSpecGenerator() {
			@Override
			public Path getFormSpecDir() {
				return formsDir;
			}
		};
		Field genField = CypressTestDiscoveryService.class.getDeclaredField("specGenerator");
		genField.setAccessible(true);
		genField.set(mockService, mockGenerator);

		Field serviceField = CypressEditorInputPropertyTester.class.getDeclaredField("discoveryService");
		serviceField.setAccessible(true);
		serviceField.set(tester, mockService);
	}

	@Nested
	@DisplayName("valid PersistEditorInput with existing test")
	class ValidInputWithTest {
		@Test
		@DisplayName("returns true when form has cypress test file")
		void returnsTrueWhenTestExists() throws IOException {
			Files.createFile(tempDir.resolve("loginForm.spec.cy.js"));
			PersistEditorInput input = new PersistEditorInput("loginForm", "mySolution", UUID.randomUUID());

			assertTrue(tester.test(input, "cypressTestExists", new Object[0], null));
		}

		@Test
		@DisplayName("returns true for different form names")
		void returnsTrueForVariousForms() throws IOException {
			Files.createFile(tempDir.resolve("dashboardForm.spec.cy.js"));
			PersistEditorInput input = new PersistEditorInput("dashboardForm", "sol", UUID.randomUUID());

			assertTrue(tester.test(input, "cypressTestExists", new Object[0], null));
		}
	}

	@Nested
	@DisplayName("valid PersistEditorInput without test")
	class ValidInputWithoutTest {
		@Test
		@DisplayName("returns false when form has no cypress test file")
		void returnsFalseWhenNoTestFile() {
			PersistEditorInput input = new PersistEditorInput("noTestForm", "mySolution", UUID.randomUUID());

			assertFalse(tester.test(input, "cypressTestExists", new Object[0], null));
		}

		@Test
		@DisplayName("returns false when only non-spec files exist")
		void returnsFalseForNonSpecFiles() throws IOException {
			Files.createFile(tempDir.resolve("myForm.js"));
			Files.createFile(tempDir.resolve("myForm.spec.js"));
			PersistEditorInput input = new PersistEditorInput("myForm", "sol", UUID.randomUUID());

			assertFalse(tester.test(input, "cypressTestExists", new Object[0], null));
		}
	}

	@Nested
	@DisplayName("invalid inputs")
	class InvalidInputs {
		@Test
		@DisplayName("returns false for null receiver")
		void returnsFalseForNull() {
			assertFalse(tester.test(null, "cypressTestExists", new Object[0], null));
		}

		@Test
		@DisplayName("returns false for non-PersistEditorInput receiver")
		void returnsFalseForWrongType() {
			assertAll(() -> assertFalse(tester.test("a string", "cypressTestExists", new Object[0], null)),
					() -> assertFalse(tester.test(Integer.valueOf(1), "cypressTestExists", new Object[0], null)),
					() -> assertFalse(tester.test(new Object(), "cypressTestExists", new Object[0], null)));
		}

		@ParameterizedTest
		@ValueSource(strings = { "unknownProperty", "hasTest", "", "cypressTest" })
		@DisplayName("returns false for unknown property names")
		void returnsFalseForUnknownProperty(String property) {
			PersistEditorInput input = new PersistEditorInput("testForm", "sol", UUID.randomUUID());

			assertFalse(tester.test(input, property, new Object[0], null));
		}

		@Test
		@DisplayName("returns false when PersistEditorInput name is null")
		void returnsFalseWhenNameIsNull() {
			PersistEditorInput input = new PersistEditorInput(null, "sol", UUID.randomUUID());

			assertFalse(tester.test(input, "cypressTestExists", new Object[0], null));
		}
	}
}

