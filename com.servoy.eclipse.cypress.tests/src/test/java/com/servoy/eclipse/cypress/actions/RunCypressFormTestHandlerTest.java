package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.cypress.services.FormSpecRunner;

@DisplayName("RunCypressFormTestHandler")
public class RunCypressFormTestHandlerTest {
	private RunCypressFormTestHandler handler;

	@BeforeEach
	void setUp() {
		handler = new RunCypressFormTestHandler();
	}

	@Nested
	@DisplayName("class structure")
	class ClassStructure {
		@Test
		@DisplayName("extends AbstractHandler")
		void extendsAbstractHandler() {
			assertTrue(AbstractHandler.class.isAssignableFrom(RunCypressFormTestHandler.class));
		}

		@Test
		@DisplayName("implements IHandler")
		void implementsIHandler() {
			assertTrue(IHandler.class.isAssignableFrom(RunCypressFormTestHandler.class));
		}

		@Test
		@DisplayName("can be instantiated")
		void canBeInstantiated() {
			assertNotNull(handler);
		}

		@Test
		@DisplayName("has discoveryService field of correct type")
		void hasDiscoveryServiceField() throws NoSuchFieldException {
			Field field = RunCypressFormTestHandler.class.getDeclaredField("discoveryService");
			assertAll(() -> assertEquals(CypressTestDiscoveryService.class, field.getType()),
					() -> assertTrue(Modifier.isPrivate(field.getModifiers())),
					() -> assertTrue(Modifier.isFinal(field.getModifiers())));
		}

		@Test
		@DisplayName("has execute method accepting ExecutionEvent")
		void hasExecuteMethod() throws NoSuchMethodException {
			Method execute = RunCypressFormTestHandler.class.getMethod("execute", ExecutionEvent.class);
			assertNotNull(execute);
			assertEquals(Object.class, execute.getReturnType());
		}

		@Test
		@DisplayName("has getFormNameFromSelection private method")
		void hasGetFormNameFromSelection() {
			Method[] methods = RunCypressFormTestHandler.class.getDeclaredMethods();
			boolean found = false;
			for (Method m : methods) {
				if ("getFormNameFromSelection".equals(m.getName())) {
					found = true;
					assertEquals(String.class, m.getReturnType());
					break;
				}
			}
			assertTrue(found, "Should have getFormNameFromSelection method");
		}

		@Test
		@DisplayName("has getFormNameFromActiveEditor private method")
		void hasGetFormNameFromActiveEditor() {
			Method[] methods = RunCypressFormTestHandler.class.getDeclaredMethods();
			boolean found = false;
			for (Method m : methods) {
				if ("getFormNameFromActiveEditor".equals(m.getName())) {
					found = true;
					assertEquals(String.class, m.getReturnType());
					break;
				}
			}
			assertTrue(found, "Should have getFormNameFromActiveEditor method");
		}

		@Test
		@DisplayName("has runFormTestCore method")
		void hasRunFormTestCoreMethod() throws NoSuchMethodException {
			Method m = RunCypressFormTestHandler.class.getDeclaredMethod("runFormTestCore",
					String.class, FormSpecRunner.class);
			assertNotNull(m);
			assertEquals(String.class, m.getReturnType());
		}

		@Test
		@DisplayName("has enableTestingMode method")
		void hasEnableTestingModeMethod() throws NoSuchMethodException {
			Method m = RunCypressFormTestHandler.class.getDeclaredMethod("enableTestingMode");
			assertNotNull(m);
		}

		@Test
		@DisplayName("has getDiscoveryService method")
		void hasGetDiscoveryServiceMethod() throws NoSuchMethodException {
			Method m = RunCypressFormTestHandler.class.getDeclaredMethod("getDiscoveryService");
			assertNotNull(m);
			assertEquals(CypressTestDiscoveryService.class, m.getReturnType());
		}
	}

	@Nested
	@DisplayName("handler behavior")
	class HandlerBehavior {
		@Test
		@DisplayName("handler is enabled by default")
		void isEnabledByDefault() {
			assertTrue(handler.isEnabled());
		}

		@Test
		@DisplayName("discoveryService is initialized on construction")
		void discoveryServiceInitialized() throws Exception {
			assertNotNull(handler.getDiscoveryService());
		}

		@Test
		@DisplayName("getDiscoveryService returns non-null")
		void getDiscoveryServiceReturnsNonNull() {
			assertNotNull(handler.getDiscoveryService());
		}
	}

	@Nested
	@DisplayName("runFormTestCore")
	class RunFormTestCoreTests {

		@Test
		@DisplayName("returns error for null form name")
		void returnsErrorForNullFormName() {
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					return "All tests passed";
				}
			};

			String result = handler.runFormTestCore(null, mockRunner);

			assertTrue(result.contains("Error"));
		}

		@Test
		@DisplayName("returns error for blank form name")
		void returnsErrorForBlankFormName() {
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					return "All tests passed";
				}
			};

			String result = handler.runFormTestCore("   ", mockRunner);

			assertTrue(result.contains("Error"));
		}

		@Test
		@DisplayName("delegates to runner.runFormCypressTests with headless=false")
		void delegatesToRunnerWithHeadlessFalse() {
			final boolean[] capturedHeadless = { true };
			final String[] capturedFormName = { null };
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					capturedFormName[0] = formName;
					capturedHeadless[0] = headless;
					return "All tests passed for " + formName;
				}
			};

			String result = handler.runFormTestCore("myForm", mockRunner);

			assertAll(
				() -> assertEquals("myForm", capturedFormName[0]),
				() -> assertEquals(false, capturedHeadless[0]),
				() -> assertTrue(result.contains("All tests passed"))
			);
		}

		@Test
		@DisplayName("returns runner result directly")
		void returnsRunnerResultDirectly() {
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					return "FAILED: " + formName + " had 3 errors";
				}
			};

			String result = handler.runFormTestCore("failingForm", mockRunner);

			assertEquals("FAILED: failingForm had 3 errors", result);
		}

		@Test
		@DisplayName("passes correct form name to runner")
		void passesCorrectFormName() {
			final String[] captured = { null };
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					captured[0] = formName;
					return "done";
				}
			};

			handler.runFormTestCore("specificFormName", mockRunner);

			assertEquals("specificFormName", captured[0]);
		}

		@Test
		@DisplayName("handles runner returning error string")
		void handlesRunnerReturningError() {
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					return "Error: No active Servoy project.";
				}
			};

			String result = handler.runFormTestCore("anyForm", mockRunner);

			assertEquals("Error: No active Servoy project.", result);
		}

		@Test
		@DisplayName("handles runner returning empty string")
		void handlesRunnerReturningEmpty() {
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					return "";
				}
			};

			String result = handler.runFormTestCore("anyForm", mockRunner);

			assertEquals("", result);
		}
	}

	@Nested
	@DisplayName("getFormNameFromSelection logic")
	class GetFormNameFromSelectionLogic {
		@Test
		@DisplayName("returns null when event has no application context")
		void returnsNullForNoContext() throws Exception {
			ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, null);
			Method method = RunCypressFormTestHandler.class.getDeclaredMethod("getFormNameFromSelection",
					ExecutionEvent.class);
			method.setAccessible(true);

			Object result = method.invoke(handler, event);

			assertNull(result);
		}
	}

	@Nested
	@DisplayName("getFormNameFromActiveEditor logic")
	class GetFormNameFromActiveEditorLogic {
		@Test
		@DisplayName("returns null when event has no application context")
		void returnsNullForNoContext() throws Exception {
			ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, null);
			Method method = RunCypressFormTestHandler.class.getDeclaredMethod("getFormNameFromActiveEditor",
					ExecutionEvent.class);
			method.setAccessible(true);

			Object result = method.invoke(handler, event);

			assertNull(result);
		}
	}

	@Nested
	@DisplayName("discoveryService integration")
	class DiscoveryServiceIntegration {
		private Path tempDir;

		@BeforeEach
		void setUpTempDir() throws Exception {
			tempDir = Files.createTempDirectory("cypress-handler-test");
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

			Field serviceField = RunCypressFormTestHandler.class.getDeclaredField("discoveryService");
			serviceField.setAccessible(true);
			serviceField.set(handler, mockService);
		}

		@Test
		@DisplayName("discoveryService can detect form with test file")
		void discoveryServiceDetectsTest() throws Exception {
			Files.createFile(tempDir.resolve("loginForm.spec.cy.js"));

			assertTrue(handler.getDiscoveryService().hasTest("loginForm"));
		}

		@Test
		@DisplayName("execute returns null without workbench context")
		void executeReturnsNullWithoutContext() {
			ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, null);

			Object result = assertDoesNotThrow(() -> handler.execute(event));

			assertNull(result);
		}
	}
}

