package com.servoy.eclipse.cypress.actions;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.services.CypressOutputParser;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecRunner;
import com.servoy.eclipse.cypress.views.CypressTestResultsView;

public class RunAllE2ETestsHandler {
	private final CypressTestDiscoveryService discoveryService = new CypressTestDiscoveryService();
	private final boolean solutionOnly;
	private final List<String> explicitTestNames;

	public RunAllE2ETestsHandler() {
		this(false);
	}

	public RunAllE2ETestsHandler(boolean solutionOnly) {
		this.solutionOnly = solutionOnly;
		this.explicitTestNames = null;
	}

	public RunAllE2ETestsHandler(List<String> testNames) {
		this.solutionOnly = false;
		this.explicitTestNames = testNames;
	}

	public void execute() {
		Job job = new Job(explicitTestNames != null ? "Re-running Cypress E2E Tests"
				: (solutionOnly ? "Running Solution Cypress E2E Tests" : "Running All Cypress E2E Tests")) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<String> testNames = explicitTestNames != null ? explicitTestNames
						: (solutionOnly ? discoveryService.discoverSolutionE2ETests()
								: discoveryService.discoverAllE2ETests());
				if (testNames.isEmpty()) {
					Display.getDefault().asyncExec(() -> MessageDialog.openInformation(null, "Cypress E2E Tests",
							"No Cypress E2E tests found."));
					return Status.OK_STATUS;
				}

				monitor.beginTask("Running Cypress E2E tests", testNames.size());

				com.servoy.j2db.util.Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");

				CypressTestSessionManager sessionManager = CypressTestSessionManager.getInstance();
				sessionManager.startSession(testNames, TestType.E2E);
				CypressTestResultsView.reveal();

				MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
				console.clearConsole();
				CypressConsoleUtil.showConsole(console);

				FormSpecRunner runner = new FormSpecRunner();
				sessionManager.setActiveRunner(runner);

				try (MessageConsoleStream stream = console.newMessageStream()) {
					stream.println("Running " + testNames.size() + " Cypress E2E test(s)...\n");

					for (String testName : testNames) {
						if (monitor.isCanceled() || !sessionManager.isRunning()) {
							stream.println("\nTest run cancelled.");
							return Status.CANCEL_STATUS;
						}

						monitor.subTask("Testing: " + testName);
						sessionManager.markRunning(testName, TestType.E2E);

						long startTime = System.currentTimeMillis();
						String result = runner.runE2ECypressTests(testName, true);
						long durationMs = System.currentTimeMillis() - startTime;

						TestStatus status = CypressOutputParser.determineStatus(result);
						String errorSummary = CypressOutputParser.extractErrorSummary(result, status);
						String videoPath = CypressOutputParser.extractVideoPath(result);
						String screenshotPath = CypressOutputParser.extractScreenshotPath(result);
						sessionManager.updateResult(testName, new CypressTestResult(testName, TestType.E2E, status,
								errorSummary, result, durationMs, videoPath, screenshotPath));

						stream.println(result);
						stream.println("---\n");
						monitor.worked(1);
					}
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> MessageDialog.openError(null, "Cypress E2E Tests",
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
}
