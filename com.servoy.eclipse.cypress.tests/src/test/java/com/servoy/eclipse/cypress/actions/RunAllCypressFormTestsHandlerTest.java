package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.actions.RunAllCypressFormTestsHandler.TestRunResult;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.cypress.services.FormSpecRunner;

@DisplayName("RunAllCypressFormTestsHandler")
public class RunAllCypressFormTestsHandlerTest {
	private RunAllCypressFormTestsHandler handler;

	@BeforeEach
	void setUp() {
		handler = new RunAllCypressFormTestsHandler();
	}

	@Nested
	@DisplayName("class structure")
	class ClassStructure {
		@Test
		@DisplayName("extends AbstractHandler")
		void extendsAbstractHandler() {
			assertTrue(AbstractHandler.class.isAssignableFrom(RunAllCypressFormTestsHandler.class));
		}

		@Test
		@DisplayName("implements IHandler")
		void implementsIHandler() {
			assertTrue(IHandler.class.isAssignableFrom(RunAllCypressFormTestsHandler.class));
		}

		@Test
		@DisplayName("can be instantiated")
		void canBeInstantiated() {
			assertNotNull(handler);
		}

		@Test
		@DisplayName("has execute method accepting ExecutionEvent")
		void hasExecuteMethod() throws NoSuchMethodException {
			Method execute = RunAllCypressFormTestsHandler.class.getMethod("execute", ExecutionEvent.class);
			assertNotNull(execute);
			assertEquals(Object.class, execute.getReturnType());
		}

		@Test
		@DisplayName("execute method is public")
		void executeMethodIsPublic() throws NoSuchMethodException {
			Method execute = RunAllCypressFormTestsHandler.class.getMethod("execute", ExecutionEvent.class);
			assertTrue(Modifier.isPublic(execute.getModifiers()));
		}

		@Test
		@DisplayName("has runTestsCore method")
		void hasRunTestsCoreMethod() throws NoSuchMethodException {
			Method m = RunAllCypressFormTestsHandler.class.getDeclaredMethod("runTestsCore", List.class,
					FormSpecRunner.class, IProgressMonitor.class);
			assertNotNull(m);
		}

		@Test
		@DisplayName("has isTestPassed static method")
		void hasIsTestPassedMethod() throws NoSuchMethodException {
			Method m = RunAllCypressFormTestsHandler.class.getDeclaredMethod("isTestPassed", String.class);
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
		}

		@Test
		@DisplayName("has formatAggregateResult static method")
		void hasFormatAggregateResultMethod() throws NoSuchMethodException {
			Method m = RunAllCypressFormTestsHandler.class.getDeclaredMethod("formatAggregateResult",
					TestRunResult.class);
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
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
		@DisplayName("handler is not disposed after creation")
		void handlerNotDisposed() {
			assertTrue(handler.isHandled());
		}

		@Test
		@DisplayName("execute returns null when event has no selection context")
		void executeReturnsNullWithoutContext() {
			ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, null);

			Object result = assertDoesNotThrow(() -> handler.execute(event));

			assertNull(result);
		}
	}

	@Nested
	@DisplayName("isTestPassed")
	class IsTestPassed {
		@Test
		@DisplayName("returns true when result contains 'All tests passed'")
		void returnsTrueForPassingResult() {
			assertTrue(RunAllCypressFormTestsHandler.isTestPassed("All tests passed for myForm"));
		}

		@Test
		@DisplayName("returns true when result contains 'All tests passed' anywhere")
		void returnsTrueForPassingResultAnywhere() {
			assertTrue(RunAllCypressFormTestsHandler
					.isTestPassed("**Form Spec Results: myForm**\n\nAll tests passed!\n\nsome output"));
		}

		@Test
		@DisplayName("returns false when result does not contain pass marker")
		void returnsFalseForFailingResult() {
			assertFalse(RunAllCypressFormTestsHandler.isTestPassed("FAILED: formB had 2 errors"));
		}

		@Test
		@DisplayName("returns false for null result")
		void returnsFalseForNull() {
			assertFalse(RunAllCypressFormTestsHandler.isTestPassed(null));
		}

		@Test
		@DisplayName("returns false for empty result")
		void returnsFalseForEmpty() {
			assertFalse(RunAllCypressFormTestsHandler.isTestPassed(""));
		}

		@Test
		@DisplayName("returns false for error result")
		void returnsFalseForError() {
			assertFalse(RunAllCypressFormTestsHandler.isTestPassed("Error: No active Servoy project."));
		}
	}

	@Nested
	@DisplayName("formatAggregateResult")
	class FormatAggregateResult {
		@Test
		@DisplayName("formats result with all passed")
		void formatsAllPassed() {
			TestRunResult result = new TestRunResult(5, 0, 5, false, List.of());
			String formatted = RunAllCypressFormTestsHandler.formatAggregateResult(result);
			assertEquals("Total: 5 | Passed: 5 | Failed: 0", formatted);
		}

		@Test
		@DisplayName("formats result with mixed pass/fail")
		void formatsMixed() {
			TestRunResult result = new TestRunResult(3, 2, 5, false, List.of());
			String formatted = RunAllCypressFormTestsHandler.formatAggregateResult(result);
			assertEquals("Total: 5 | Passed: 3 | Failed: 2", formatted);
		}

		@Test
		@DisplayName("formats result with all failed")
		void formatsAllFailed() {
			TestRunResult result = new TestRunResult(0, 4, 4, false, List.of());
			String formatted = RunAllCypressFormTestsHandler.formatAggregateResult(result);
			assertEquals("Total: 4 | Passed: 0 | Failed: 4", formatted);
		}

		@Test
		@DisplayName("formats result with single test")
		void formatsSingleTest() {
			TestRunResult result = new TestRunResult(1, 0, 1, false, List.of());
			String formatted = RunAllCypressFormTestsHandler.formatAggregateResult(result);
			assertEquals("Total: 1 | Passed: 1 | Failed: 0", formatted);
		}
	}

	@Nested
	@DisplayName("TestRunResult")
	class TestRunResultTests {
		@Test
		@DisplayName("stores passed count correctly")
		void storesPassedCount() {
			TestRunResult result = new TestRunResult(3, 2, 5, false, List.of("a", "b"));
			assertEquals(3, result.passed);
		}

		@Test
		@DisplayName("stores failed count correctly")
		void storesFailedCount() {
			TestRunResult result = new TestRunResult(3, 2, 5, false, List.of("a", "b"));
			assertEquals(2, result.failed);
		}

		@Test
		@DisplayName("stores total count correctly")
		void storesTotalCount() {
			TestRunResult result = new TestRunResult(3, 2, 5, false, List.of("a", "b"));
			assertEquals(5, result.total);
		}

		@Test
		@DisplayName("stores cancelled flag correctly")
		void storesCancelledFlag() {
			TestRunResult cancelled = new TestRunResult(1, 0, 3, true, List.of("a"));
			assertTrue(cancelled.cancelled);

			TestRunResult notCancelled = new TestRunResult(3, 0, 3, false, List.of());
			assertFalse(notCancelled.cancelled);
		}

		@Test
		@DisplayName("stores results list correctly")
		void storesResultsList() {
			List<String> results = List.of("result1", "result2");
			TestRunResult result = new TestRunResult(2, 0, 2, false, results);
			assertEquals(results, result.results);
		}
	}

	@Nested
	@DisplayName("runTestsCore")
	class RunTestsCore {

		private CypressTestSessionManager sessionManager;

		@BeforeEach
		void injectIsolatedSession() {
			// runTestsCore checks sessionManager.isRunning() so the Stop button can
			// abort mid-run. Inject an isolated session manager (not the global
			// singleton) and start a session so the core loop is exercised.
			sessionManager = CypressTestSessionManager.createForTesting();
			handler.setSessionManager(sessionManager);
		}

		/** Starts a session for the given forms, then runs the core loop. */
		private TestRunResult run(List<String> forms, FormSpecRunner runner, IProgressMonitor monitor) {
			sessionManager.startSession(forms, CypressTestResult.TestType.FORM);
			return handler.runTestsCore(forms, runner, monitor);
		}

		private FormSpecRunner createMockRunner(String passPrefix) {
			return new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					if (formName.startsWith(passPrefix)) {
						return "All tests passed for " + formName;
					}
					return "FAILED: " + formName + " had errors";
				}
			};
		}

		@Test
		@DisplayName("returns all passed when all tests succeed")
		void allTestsPass() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passFormA", "passFormB", "passFormC");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(3, result.passed), () -> assertEquals(0, result.failed),
					() -> assertEquals(3, result.total), () -> assertFalse(result.cancelled),
					() -> assertEquals(3, result.results.size()));
		}

		@Test
		@DisplayName("returns all failed when all tests fail")
		void allTestsFail() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("failFormA", "failFormB");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(0, result.passed), () -> assertEquals(2, result.failed),
					() -> assertEquals(2, result.total), () -> assertFalse(result.cancelled));
		}

		@Test
		@DisplayName("returns mixed results when some pass and some fail")
		void mixedResults() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passFormA", "failFormB", "passFormC", "failFormD", "failFormE");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(2, result.passed), () -> assertEquals(3, result.failed),
					() -> assertEquals(5, result.total), () -> assertFalse(result.cancelled));
		}

		@Test
		@DisplayName("returns empty result for empty test list")
		void emptyTestList() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of();

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(0, result.passed), () -> assertEquals(0, result.failed),
					() -> assertEquals(0, result.total), () -> assertFalse(result.cancelled),
					() -> assertTrue(result.results.isEmpty()));
		}

		@Test
		@DisplayName("stores individual results in order")
		void storesResultsInOrder() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passA", "failB", "passC");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertTrue(result.results.get(0).contains("All tests passed")),
					() -> assertTrue(result.results.get(1).contains("FAILED")),
					() -> assertTrue(result.results.get(2).contains("All tests passed")));
		}

		@Test
		@DisplayName("works with null monitor")
		void worksWithNullMonitor() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passA", "failB");

			TestRunResult result = run(forms, mockRunner, null);

			assertAll(() -> assertEquals(1, result.passed), () -> assertEquals(1, result.failed),
					() -> assertFalse(result.cancelled));
		}

		@Test
		@DisplayName("detects cancellation from monitor")
		void detectsCancellation() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passA", "passB", "passC", "passD");

			IProgressMonitor cancellingMonitor = new NullProgressMonitor() {
				private int callCount = 0;

				@Override
				public boolean isCanceled() {
					callCount++;
					return callCount > 2; // cancel after 2nd form
				}
			};

			TestRunResult result = run(forms, mockRunner, cancellingMonitor);

			assertTrue(result.cancelled);
			assertTrue(result.results.size() < forms.size());
		}

		@Test
		@DisplayName("single test pass")
		void singleTestPass() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("passOnly");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(1, result.passed), () -> assertEquals(0, result.failed),
					() -> assertEquals(1, result.total), () -> assertFalse(result.cancelled));
		}

		@Test
		@DisplayName("single test fail")
		void singleTestFail() {
			FormSpecRunner mockRunner = createMockRunner("pass");
			List<String> forms = List.of("failOnly");

			TestRunResult result = run(forms, mockRunner, new NullProgressMonitor());

			assertAll(() -> assertEquals(0, result.passed), () -> assertEquals(1, result.failed),
					() -> assertEquals(1, result.total), () -> assertFalse(result.cancelled));
		}

		@Test
		@DisplayName("headless flag is passed as true")
		void headlessFlagPassedAsTrue() {
			final boolean[] capturedHeadless = { false };
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					capturedHeadless[0] = headless;
					return "All tests passed";
				}
			};

			run(List.of("form1"), mockRunner, new NullProgressMonitor());

			assertTrue(capturedHeadless[0]);
		}

		@Test
		@DisplayName("form names are passed correctly to runner")
		void formNamesPassedCorrectly() {
			final java.util.List<String> capturedNames = new java.util.ArrayList<>();
			FormSpecRunner mockRunner = new FormSpecRunner() {
				@Override
				public String runFormCypressTests(String formName, boolean headless) {
					capturedNames.add(formName);
					return "All tests passed";
				}
			};

			List<String> forms = List.of("loginForm", "dashboardForm", "settingsForm");
			run(forms, mockRunner, new NullProgressMonitor());

			assertEquals(forms, capturedNames);
		}
	}

	@Nested
	@DisplayName("CypressFormTestTarget integration")
	class TargetIntegration {
		@Test
		@DisplayName("SolutionLevelTestTarget getTestFormNames delegates to discoveryService")
		void solutionLevelTargetDelegatesToDiscovery() throws Exception {
			Path tempDir = Files.createTempDirectory("cypress-all-handler-test");
			try {
				Files.createFile(tempDir.resolve("formA.spec.cy.js"));
				Files.createFile(tempDir.resolve("formB.spec.cy.js"));
				Files.createFile(tempDir.resolve("formC.spec.cy.js"));

				CypressTestAdapterFactory factory = new CypressTestAdapterFactory();
				CypressTestDiscoveryService mockService = createMockService(tempDir);
				Field serviceField = CypressTestAdapterFactory.class.getDeclaredField("discoveryService");
				serviceField.setAccessible(true);
				serviceField.set(factory, mockService);

				Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
				Class<?> solutionClass = null;
				for (Class<?> c : innerClasses) {
					if (c.getSimpleName().equals("SolutionLevelTestTarget")) {
						solutionClass = c;
						break;
					}
				}
				assertNotNull(solutionClass);
				var ctor = solutionClass.getDeclaredConstructors()[0];
				ctor.setAccessible(true);
				CypressFormTestTarget target = (CypressFormTestTarget) ctor.newInstance(factory);

				List<String> testForms = target.getTestFormNames();

				assertAll(() -> assertEquals(3, testForms.size()), () -> assertTrue(testForms.contains("formA")),
						() -> assertTrue(testForms.contains("formB")), () -> assertTrue(testForms.contains("formC")));
			} finally {
				deleteTempDir(tempDir);
			}
		}

		@Test
		@DisplayName("SolutionLevelTestTarget returns empty list when no tests exist")
		void solutionLevelTargetReturnsEmptyWhenNoTests() throws Exception {
			Path tempDir = Files.createTempDirectory("cypress-all-handler-empty");
			try {
				CypressTestAdapterFactory factory = new CypressTestAdapterFactory();
				CypressTestDiscoveryService mockService = createMockService(tempDir);
				Field serviceField = CypressTestAdapterFactory.class.getDeclaredField("discoveryService");
				serviceField.setAccessible(true);
				serviceField.set(factory, mockService);

				Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
				Class<?> solutionClass = null;
				for (Class<?> c : innerClasses) {
					if (c.getSimpleName().equals("SolutionLevelTestTarget")) {
						solutionClass = c;
						break;
					}
				}
				assertNotNull(solutionClass);
				var ctor = solutionClass.getDeclaredConstructors()[0];
				ctor.setAccessible(true);
				CypressFormTestTarget target = (CypressFormTestTarget) ctor.newInstance(factory);

				List<String> testForms = target.getTestFormNames();

				assertTrue(testForms.isEmpty());
			} finally {
				deleteTempDir(tempDir);
			}
		}

		@Test
		@DisplayName("aggregate result counting pattern works correctly")
		void aggregateResultCountingLogic() {
			List<String> results = List.of("All tests passed for formA", "FAILED: formB had 2 errors",
					"All tests passed for formC", "All tests passed for formD", "FAILED: formE timeout");

			int passed = 0;
			int failed = 0;
			for (String result : results) {
				if (RunAllCypressFormTestsHandler.isTestPassed(result)) {
					passed++;
				} else {
					failed++;
				}
			}

			int finalPassed = passed;
			int finalFailed = failed;
			assertAll(() -> assertEquals(3, finalPassed), () -> assertEquals(2, finalFailed),
					() -> assertEquals(results.size(), finalPassed + finalFailed));
		}

		private CypressTestDiscoveryService createMockService(Path formsDir) throws Exception {
			CypressTestDiscoveryService service = new CypressTestDiscoveryService();
			FormSpecGenerator mockGenerator = new FormSpecGenerator() {
				@Override
				public Path getFormSpecDir() {
					return formsDir;
				}
			};
			Field genField = CypressTestDiscoveryService.class.getDeclaredField("specGenerator");
			genField.setAccessible(true);
			genField.set(service, mockGenerator);
			return service;
		}

		private void deleteTempDir(Path dir) throws IOException {
			Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
					Files.delete(d);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	@Nested
	@DisplayName("CypressConsoleUtil")
	class ConsoleUtilStructure {
		@Test
		@DisplayName("CypressConsoleUtil has findOrCreateConsole method")
		void hasFindOrCreateConsoleMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getMethod("findOrCreateConsole");
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
		}

		@Test
		@DisplayName("CypressConsoleUtil has showConsole method")
		void hasShowConsoleMethod() throws NoSuchMethodException {
			Method m = CypressConsoleUtil.class.getMethod("showConsole", org.eclipse.ui.console.MessageConsole.class);
			assertNotNull(m);
			assertTrue(Modifier.isStatic(m.getModifiers()));
		}

		@Test
		@DisplayName("CypressConsoleUtil is final utility class")
		void isFinalUtilityClass() {
			assertTrue(Modifier.isFinal(CypressConsoleUtil.class.getModifiers()));
		}
	}
}

