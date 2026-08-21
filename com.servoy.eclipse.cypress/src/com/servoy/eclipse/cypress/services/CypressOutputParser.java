package com.servoy.eclipse.cypress.services;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;

public final class CypressOutputParser {
	private CypressOutputParser() {
	}

	public static TestStatus determineStatus(String rawOutput) {
		if (rawOutput == null || rawOutput.isBlank()) {
			return TestStatus.ERROR;
		}
		if (rawOutput.contains("All tests passed")) {
			return TestStatus.PASSED;
		}
		if (rawOutput.startsWith("Error:") || rawOutput.startsWith("Error running spec:")
				|| rawOutput.startsWith("Error running E2E spec:")) {
			return TestStatus.ERROR;
		}
		if (rawOutput.contains("Some tests failed")) {
			if (rawOutput.contains("An uncaught error was detected outside of a test")
					|| rawOutput.contains("TypeError:") || rawOutput.contains("ReferenceError:")
					|| rawOutput.contains("SyntaxError:")
					|| rawOutput.contains("Can't run because no spec files were found")) {
				return TestStatus.ERROR;
			}
			return TestStatus.FAILED;
		}
		return TestStatus.ERROR;
	}

	public static String extractErrorSummary(String rawOutput, TestStatus status) {
		if (status == TestStatus.PASSED || rawOutput == null || rawOutput.isBlank()) {
			return null;
		}
		if (status == TestStatus.ERROR) {
			String errorLine = rawOutput.lines().filter(l -> !l.isBlank()).filter(l -> !l.startsWith("Warning:"))
					.filter(l -> !l.startsWith("1.") && !l.startsWith("2.")).filter(l -> !l.startsWith("Learn more:"))
					.filter(l -> l.contains("Error") || l.contains("TypeError") || l.contains("Can't run")
							|| l.startsWith(">") || l.contains("not a function"))
					.findFirst().orElse(null);
			if (errorLine != null) {
				return errorLine.length() > 200 ? errorLine.substring(0, 200) + "..." : errorLine.trim();
			}
			String firstLine = rawOutput.lines().filter(l -> !l.isBlank()).findFirst().orElse(rawOutput);
			return firstLine.length() > 200 ? firstLine.substring(0, 200) + "..." : firstLine;
		}
		String assertion = rawOutput.lines().map(String::strip)
				.filter(l -> !l.isBlank())
				.filter(l -> !isSeparatorLine(l))
				.filter(l -> !l.startsWith("Warning:") && !l.startsWith("Learn more:"))
				.filter(l -> l.contains("AssertionError") || l.contains("CypressError")
						|| l.contains("Error:") || l.startsWith("Timed out")
						|| l.contains("expected") || l.contains("Expected") || l.contains("to exist"))
				.findFirst().orElse(null);
		if (assertion != null) {
			return assertion.length() > 200 ? assertion.substring(0, 200) + "..." : assertion;
		}
		return "Test failed (see details pane)";
	}

	private static boolean isSeparatorLine(String line) {
		String t = line.strip();
		return !t.isEmpty() && t.chars().allMatch(c -> c == '=' || c == '-' || c == '_'
				|| c == '\u2500' || c == '\u2502' || c == '\u250C' || c == '\u2510'
				|| c == '\u2514' || c == '\u2518' || c == ' ');
	}

	public static String extractVideoPath(String rawOutput) {
		return extractMarkerPath(rawOutput, FormSpecRunner.PRESERVED_VIDEO_MARKER);
	}

	public static String extractScreenshotPath(String rawOutput) {
		return extractMarkerPath(rawOutput, FormSpecRunner.PRESERVED_SCREENSHOT_MARKER);
	}

	private static String extractMarkerPath(String rawOutput, String marker) {
		if (rawOutput == null)
			return null;
		int idx = rawOutput.indexOf(marker);
		if (idx < 0)
			return null;
		int start = idx + marker.length();
		int end = rawOutput.indexOf('\n', start);
		if (end < 0)
			end = rawOutput.length();
		String path = rawOutput.substring(start, end).trim();
		if (path.isEmpty())
			return null;
		return new java.io.File(path).exists() ? path : null;
	}
}
