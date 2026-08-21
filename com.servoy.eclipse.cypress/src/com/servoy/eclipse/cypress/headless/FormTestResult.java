package com.servoy.eclipse.cypress.headless;

public class FormTestResult {
	private final String formName;
	private final boolean passed;
	private final boolean error;
	private final String output;
	private final String summary;
	private final long durationMs;

	public FormTestResult(String formName, boolean passed, boolean error, String output, String summary,
			long durationMs) {
		this.formName = formName;
		this.passed = passed;
		this.error = error;
		this.output = output;
		this.summary = summary;
		this.durationMs = durationMs;
	}

	public String getFormName() {
		return formName;
	}

	public boolean isPassed() {
		return passed;
	}

	public boolean isError() {
		return error;
	}

	public String getOutput() {
		return output;
	}

	public String getSummary() {
		return summary;
	}

	public long getDurationMs() {
		return durationMs;
	}
}

