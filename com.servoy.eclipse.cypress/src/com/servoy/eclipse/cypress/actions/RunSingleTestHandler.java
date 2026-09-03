package com.servoy.eclipse.cypress.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.services.CypressOutputParser;
import com.servoy.eclipse.cypress.services.FormSpecRunner;
import com.servoy.eclipse.cypress.views.CypressTestResultsView;

public class RunSingleTestHandler {

	public void execute(String testName, TestType testType) {
		Job job = new Job("Running Cypress Test: " + testName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Running: " + testName, 1);

				com.servoy.j2db.util.Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");

				CypressTestSessionManager sessionManager = CypressTestSessionManager.getInstance();
				sessionManager.startSession(java.util.List.of(testName), testType);
				CypressTestResultsView.reveal();

				MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
				console.clearConsole();
				CypressConsoleUtil.showConsole(console);

				FormSpecRunner runner = new FormSpecRunner();
				sessionManager.setActiveRunner(runner);

				try (MessageConsoleStream stream = console.newMessageStream()) {
					stream.println("Running Cypress test: " + testName + "\n");

					sessionManager.markRunning(testName, testType);

					long startTime = System.currentTimeMillis();
					String result;
					if (testType == TestType.E2E) {
						result = runner.runE2ECypressTests(testName, true);
					} else {
						result = runner.runFormCypressTests(testName, true);
					}
					long durationMs = System.currentTimeMillis() - startTime;

					TestStatus status = CypressOutputParser.determineStatus(result);
					String errorSummary = CypressOutputParser.extractErrorSummary(result, status);
					String videoPath = CypressOutputParser.extractVideoPath(result);
					String screenshotPath = CypressOutputParser.extractScreenshotPath(result);
					sessionManager.updateResult(testName, new CypressTestResult(testName, testType, status,
							errorSummary, result, durationMs, videoPath, screenshotPath));

					stream.println(result);
					monitor.worked(1);
				} catch (Exception e) {
					sessionManager.updateResult(testName,
							new CypressTestResult(testName, testType, TestStatus.ERROR, e.getMessage(), null, 0));
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
}
