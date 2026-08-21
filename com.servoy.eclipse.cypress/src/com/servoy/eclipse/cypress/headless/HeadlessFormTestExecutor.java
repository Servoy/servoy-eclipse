package com.servoy.eclipse.cypress.headless;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.cypress.services.FormSpecRunner;

/**
 * Orchestrates headless discovery and execution of Cypress form tests.
 * <p>
 * Resolves the list of forms to test (explicit {@code -forms} list or all
 * discovered), optionally generates missing specs, then runs each spec via
 * {@link FormSpecRunner} honouring the configured per-test timeout and any
 * extra Cypress arguments.
 */
public class HeadlessFormTestExecutor {
	private final FormSpecRunner specRunner;
	private final FormSpecGenerator specGenerator;
	private final CypressTestDiscoveryService discoveryService;
	private final CypressFormTestArgumentChest arguments;
	private final Consumer<String> output;
	private final Consumer<String> verboseOutput;
	private volatile boolean cancelled;

	public HeadlessFormTestExecutor(CypressFormTestArgumentChest arguments, Consumer<String> output,
			Consumer<String> verboseOutput) {
		this.arguments = arguments;
		this.output = output;
		this.verboseOutput = verboseOutput;
		this.specGenerator = new FormSpecGenerator();
		this.specRunner = new FormSpecRunner();
		this.discoveryService = new CypressTestDiscoveryService();
	}

	public List<FormTestResult> execute() {
		List<String> testForms = resolveTestForms();
		if (testForms.isEmpty()) {
			output.accept("No form tests found.");
			return List.of();
		}

		output.accept("Discovered " + testForms.size() + " form test(s) to run.");

		if (arguments.isGenerateMissing()) {
			generateMissingSpecs(testForms);
		}

		List<FormTestResult> results = new ArrayList<>();
		for (String formName : testForms) {
			if (cancelled) {
				output.accept("Test run cancelled.");
				break;
			}

			output.accept("Running: " + formName);
			long startTime = System.currentTimeMillis();
			String testOutput = specRunner.runFormCypressTests(formName, true, arguments.getTimeout(),
					arguments.getCypressArgs());
			long durationMs = System.currentTimeMillis() - startTime;

			boolean error = isTestError(testOutput);
			boolean passed = !error && isTestPassed(testOutput);
			String summary = extractSummary(testOutput, formName);

			results.add(new FormTestResult(formName, passed, error, testOutput, summary, durationMs));

			String status = passed ? "PASSED" : (error ? "ERROR" : "FAILED");
			output.accept("  " + status + " (" + durationMs + "ms)");
		}

		return results;
	}

	public void cancel() {
		cancelled = true;
		specRunner.cancel();
	}

	private List<String> resolveTestForms() {
		if (!arguments.getForms().isEmpty()) {
			return arguments.getForms();
		}
		return discoveryService.discoverAllTestForms();
	}

	private void generateMissingSpecs(List<String> testForms) {
		for (String formName : testForms) {
			if (!discoveryService.hasFormTest(formName)) {
				verboseOutput.accept("Generating spec for: " + formName);
				specGenerator.generateSpec(formName);
			}
		}
	}

	private static boolean isTestPassed(String result) {
		return result != null && result.contains("All tests passed");
	}

	private static boolean isTestError(String result) {
		return result != null && result.startsWith("Error");
	}

	private static String extractSummary(String output, String formName) {
		if (output == null)
			return "No output";
		if (isTestError(output))
			return output.lines().findFirst().orElse("Error");
		if (isTestPassed(output))
			return "All tests passed";
		return "Some tests failed for " + formName;
	}
}

