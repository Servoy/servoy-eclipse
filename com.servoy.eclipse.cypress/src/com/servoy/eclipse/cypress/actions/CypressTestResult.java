package com.servoy.eclipse.cypress.actions;

public record CypressTestResult(String testName, TestType testType, TestStatus status, String errorSummary,
		String rawOutput, long durationMs, String videoPath, String screenshotPath) {
	public enum TestStatus {
		PENDING, RUNNING, PASSED, FAILED, ERROR
	}

	public enum TestType {
		FORM, E2E
	}

	/** Convenience constructor without media paths. */
	public CypressTestResult(String testName, TestType testType, TestStatus status, String errorSummary,
			String rawOutput, long durationMs) {
		this(testName, testType, status, errorSummary, rawOutput, durationMs, null, null);
	}

	public boolean hasVideo() {
		return videoPath != null && !videoPath.isBlank();
	}

	public boolean hasScreenshot() {
		return screenshotPath != null && !screenshotPath.isBlank();
	}
}
