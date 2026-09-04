package com.servoy.eclipse.cypress.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.services.CypressOutputParser;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecRunner;
import com.servoy.eclipse.cypress.views.CypressTestResultsView;

public class RunAllCypressFormTestsHandler extends AbstractHandler {

	private CypressTestSessionManager sessionManager = CypressTestSessionManager.getInstance();

	void setSessionManager(CypressTestSessionManager sessionManager) {
		this.sessionManager = sessionManager;
	}

	public static class TestRunResult {
		final int passed;
		final int failed;
		final int total;
		final boolean cancelled;
		final List<String> results;

		TestRunResult(int passed, int failed, int total, boolean cancelled, List<String> results) {
			this.passed = passed;
			this.failed = failed;
			this.total = total;
			this.cancelled = cancelled;
			this.results = results;
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection structuredSelection) || structuredSelection.isEmpty()) {
			return null;
		}

		Object element = structuredSelection.getFirstElement();
		Object adapted = Platform.getAdapterManager().getAdapter(element, CypressFormTestTarget.class);
		if (!(adapted instanceof CypressFormTestTarget target) || !target.isSolutionLevel()) {
			return null;
		}

		Job job = new Job("Running All Cypress Form Tests") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<String> testForms = target.getTestFormNames();
				if (testForms.isEmpty()) {
					Display.getDefault().asyncExec(() -> MessageDialog.openInformation(null, "Cypress Form Tests",
							"No Cypress form tests found."));
					return Status.OK_STATUS;
				}

				monitor.beginTask("Running Cypress form tests", testForms.size());

				com.servoy.j2db.util.Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");

				sessionManager.startSession(testForms, TestType.FORM);
				CypressTestResultsView.reveal();

				MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
				console.clearConsole();
				CypressConsoleUtil.showConsole(console);

				FormSpecRunner runner = new FormSpecRunner();
				sessionManager.setActiveRunner(runner);

				try (MessageConsoleStream stream = console.newMessageStream()) {
					stream.println("Running " + testForms.size() + " Cypress form test(s)...\n");

					TestRunResult result = runTestsCore(testForms, runner, monitor);

					for (String line : result.results) {
						stream.println(line);
						stream.println("---\n");
					}

					if (result.cancelled) {
						stream.println("\nTest run cancelled.");
						return Status.CANCEL_STATUS;
					}

					stream.println("\n=== Aggregate Results ===");
					stream.println(formatAggregateResult(result));
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> MessageDialog.openError(null, "Cypress Form Tests",
							"Error running tests: " + e.getMessage()));
				} finally {
					sessionManager.clearActiveRunner(runner);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}

	public void runAllFormTests() {
		runFormTests(null);
	}

	public void runFormTests(List<String> explicitForms) {
		Job job = new Job("Running Cypress Form Tests") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<String> testForms = explicitForms != null ? explicitForms
						: new CypressTestDiscoveryService().discoverAllTestForms();
				if (testForms.isEmpty()) {
					Display.getDefault().asyncExec(() -> MessageDialog.openInformation(null, "Cypress Form Tests",
							"No Cypress form tests found."));
					return Status.OK_STATUS;
				}

				monitor.beginTask("Running Cypress form tests", testForms.size());

				com.servoy.j2db.util.Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");

				sessionManager.startSession(testForms, TestType.FORM);
				CypressTestResultsView.reveal();

				MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
				console.clearConsole();
				CypressConsoleUtil.showConsole(console);

				FormSpecRunner runner = new FormSpecRunner();
				sessionManager.setActiveRunner(runner);

				try (MessageConsoleStream stream = console.newMessageStream()) {
					stream.println("Running " + testForms.size() + " Cypress form test(s)...\n");

					TestRunResult result = runTestsCore(testForms, runner, monitor);

					for (String line : result.results) {
						stream.println(line);
						stream.println("---\n");
					}

					if (result.cancelled) {
						stream.println("\nTest run cancelled.");
						return Status.CANCEL_STATUS;
					}

					stream.println("\n=== Aggregate Results ===");
					stream.println(formatAggregateResult(result));
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> MessageDialog.openError(null, "Cypress Form Tests",
							"Error running tests: " + e.getMessage()));
				} finally {
					sessionManager.clearActiveRunner(runner);
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	public TestRunResult runTestsCore(List<String> testForms, FormSpecRunner runner, IProgressMonitor monitor) {
		int passed = 0;
		int failed = 0;
		List<String> results = new ArrayList<>();

		for (String formName : testForms) {
			if (monitor != null && monitor.isCanceled()) {
				return new TestRunResult(passed, failed, testForms.size(), true, results);
			}

			if (!sessionManager.isRunning()) {
				return new TestRunResult(passed, failed, testForms.size(), true, results);
			}

			if (monitor != null) {
				monitor.subTask("Testing: " + formName);
			}

			sessionManager.markRunning(formName, TestType.FORM);

			long startTime = System.currentTimeMillis();
			String result = runner.runFormCypressTests(formName, true);
			long durationMs = System.currentTimeMillis() - startTime;

			results.add(result);

			TestStatus status = CypressOutputParser.determineStatus(result);
			String errorSummary = CypressOutputParser.extractErrorSummary(result, status);
			String videoPath = CypressOutputParser.extractVideoPath(result);
			String screenshotPath = CypressOutputParser.extractScreenshotPath(result);
			sessionManager.updateResult(formName, new CypressTestResult(formName, TestType.FORM, status, errorSummary,
					result, durationMs, videoPath, screenshotPath));

			if (isTestPassed(result)) {
				passed++;
			} else {
				failed++;
			}

			if (monitor != null) {
				monitor.worked(1);
			}
		}

		return new TestRunResult(passed, failed, testForms.size(), false, results);
	}

	public static boolean isTestPassed(String result) {
		return result != null && result.contains("All tests passed");
	}

	public static String formatAggregateResult(TestRunResult result) {
		return "Total: " + result.total + " | Passed: " + result.passed + " | Failed: " + result.failed;
	}
}
