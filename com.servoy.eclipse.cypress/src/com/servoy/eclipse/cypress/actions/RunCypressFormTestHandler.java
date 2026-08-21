package com.servoy.eclipse.cypress.actions;

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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.handlers.HandlerUtil;

import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.services.CypressOutputParser;
import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecRunner;
import com.servoy.eclipse.cypress.views.CypressTestResultsView;

public class RunCypressFormTestHandler extends AbstractHandler {
	private final CypressTestDiscoveryService discoveryService = new CypressTestDiscoveryService();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		String formName = getFormNameFromSelection(event);
		if (formName == null) {
			formName = getFormNameFromActiveEditor(event);
		}
		if (formName == null) {
			Display.getDefault()
					.asyncExec(() -> MessageDialog.openError(null, "Cypress Form Test", "No active Servoy project"));
			return null;
		}

		String targetFormName = formName;
		com.servoy.eclipse.cypress.services.CypressTestDiscoveryService.TestType discoveredType = discoveryService
				.getTestType(targetFormName);
		TestType sessionType = (discoveredType == com.servoy.eclipse.cypress.services.CypressTestDiscoveryService.TestType.E2E)
				? TestType.E2E
				: TestType.FORM;

		CypressTestSessionManager sessionManager = CypressTestSessionManager.getInstance();
		sessionManager.startSession(java.util.List.of(targetFormName), sessionType);
		CypressTestResultsView.reveal();

		Job job = new Job("Running Cypress Form Test: " + targetFormName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Running Cypress test for " + targetFormName, IProgressMonitor.UNKNOWN);
				try {
					MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
					console.clearConsole();
					CypressConsoleUtil.showConsole(console);

					enableTestingMode();

					sessionManager.markRunning(targetFormName, sessionType);

					long startTime = System.currentTimeMillis();
					String result = runFormTestCore(targetFormName, new FormSpecRunner());
					long durationMs = System.currentTimeMillis() - startTime;

					TestStatus status = CypressOutputParser.determineStatus(result);
					String errorSummary = CypressOutputParser.extractErrorSummary(result, status);
					sessionManager.updateResult(targetFormName, new CypressTestResult(targetFormName, sessionType,
							status, errorSummary, result, durationMs));

					try (MessageConsoleStream stream = console.newMessageStream()) {
						stream.println(result);
					}
				} catch (Exception e) {
					Display.getDefault().asyncExec(() -> MessageDialog.openError(null, "Cypress Form Test",
							"Error running test: " + e.getMessage()));
				} finally {
					monitor.done();
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();

		return null;
	}

	public String runFormTestCore(String formName, FormSpecRunner runner) {
		if (formName == null || formName.isBlank()) {
			return "Error: No form name specified.";
		}
		com.servoy.eclipse.cypress.services.CypressTestDiscoveryService.TestType testType = discoveryService
				.getTestType(formName);
		if (testType == com.servoy.eclipse.cypress.services.CypressTestDiscoveryService.TestType.E2E) {
			return runner.runE2ECypressTests(formName, false);
		}
		return runner.runFormCypressTests(formName, false);
	}

	public void enableTestingMode() {
		com.servoy.j2db.util.Settings.getInstance().setProperty("servoy.ngclient.testingMode", "true");
	}

	CypressTestDiscoveryService getDiscoveryService() {
		return discoveryService;
	}

	private String getFormNameFromSelection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (!(selection instanceof IStructuredSelection structuredSelection) || structuredSelection.isEmpty()) {
			return null;
		}

		Object element = structuredSelection.getFirstElement();
		Object adapted = Platform.getAdapterManager().getAdapter(element, CypressFormTestTarget.class);
		if (adapted instanceof CypressFormTestTarget target && target.getFormName() != null) {
			return target.getFormName();
		}
		return null;
	}

	private String getFormNameFromActiveEditor(ExecutionEvent event) {
		IEditorPart editor = HandlerUtil.getActiveEditor(event);
		if (editor == null) {
			return null;
		}
		IEditorInput input = editor.getEditorInput();
		if (input instanceof PersistEditorInput persistInput) {
			String name = persistInput.getName();
			if (name != null && discoveryService.hasTest(name)) {
				return name;
			}
		}
		return null;
	}
}
