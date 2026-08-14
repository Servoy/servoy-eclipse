package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;

@DisplayName("CypressOutputParser")
class CypressOutputParserTest {

	@Nested
	@DisplayName("determineStatus")
	class DetermineStatus {

		@Test
		@DisplayName("returns PASSED when output contains 'All tests passed'")
		void returnsPassed() {
			assertEquals(TestStatus.PASSED, CypressOutputParser.determineStatus("Some output\nAll tests passed\nDone"));
		}

		@Test
		@DisplayName("returns PASSED when 'All tests passed' is the only content")
		void returnsPassedExactMatch() {
			assertEquals(TestStatus.PASSED, CypressOutputParser.determineStatus("All tests passed"));
		}

		@Test
		@DisplayName("returns FAILED when output contains 'Some tests failed'")
		void returnsFailed() {
			assertEquals(TestStatus.FAILED,
					CypressOutputParser.determineStatus("Running...\nSome tests failed\nDetails here"));
		}

		@Test
		@DisplayName("returns FAILED when output contains 'Some tests failed:' with colon")
		void returnsFailedWithColon() {
			assertEquals(TestStatus.FAILED,
					CypressOutputParser.determineStatus("Some tests failed: assertion error in loginForm"));
		}

		@Test
		@DisplayName("returns ERROR when output starts with 'Error:'")
		void returnsErrorForErrorPrefix() {
			assertEquals(TestStatus.ERROR, CypressOutputParser.determineStatus("Error: something went wrong"));
		}

		@Test
		@DisplayName("returns ERROR when output starts with 'Error running spec:'")
		void returnsErrorForRunningSpecPrefix() {
			assertEquals(TestStatus.ERROR,
					CypressOutputParser.determineStatus("Error running spec: timeout after 30s"));
		}

		@Test
		@DisplayName("returns ERROR when output starts with 'Error running E2E spec:'")
		void returnsErrorForE2ESpecPrefix() {
			assertEquals(TestStatus.ERROR,
					CypressOutputParser.determineStatus("Error running E2E spec: file not found"));
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("returns ERROR for null and empty input")
		void returnsErrorForNullAndEmpty(String input) {
			assertEquals(TestStatus.ERROR, CypressOutputParser.determineStatus(input));
		}

		@Test
		@DisplayName("returns ERROR for blank input")
		void returnsErrorForBlank() {
			assertEquals(TestStatus.ERROR, CypressOutputParser.determineStatus("   \n  \t  "));
		}

		@Test
		@DisplayName("returns ERROR for unrecognized output")
		void returnsErrorForUnrecognized() {
			assertEquals(TestStatus.ERROR, CypressOutputParser.determineStatus("random garbage output"));
		}

		@Test
		@DisplayName("PASSED takes priority over ERROR prefix when both present")
		void passedTakesPriorityOverError() {
			assertEquals(TestStatus.PASSED, CypressOutputParser.determineStatus("Error: but also All tests passed"));
		}

		@Test
		@DisplayName("PASSED takes priority over FAILED when both present")
		void passedTakesPriorityOverFailed() {
			assertEquals(TestStatus.PASSED,
					CypressOutputParser.determineStatus("Some tests failed but All tests passed"));
		}

		@Test
		@DisplayName("FAILED takes priority over ERROR prefix in startsWith check")
		void failedTakesPriorityWhenErrorNotAtStart() {
			assertEquals(TestStatus.FAILED, CypressOutputParser.determineStatus("prefix Error: but Some tests failed"));
		}
	}

	@Nested
	@DisplayName("extractErrorSummary")
	class ExtractErrorSummary {

		@Test
		@DisplayName("returns null when status is PASSED")
		void returnsNullForPassed() {
			assertNull(CypressOutputParser.extractErrorSummary("All tests passed", TestStatus.PASSED));
		}

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("returns null for null and empty output when PASSED")
		void returnsNullForNullEmptyPassed(String input) {
			assertNull(CypressOutputParser.extractErrorSummary(input, TestStatus.PASSED));
		}

		@Test
		@DisplayName("returns null for null output when FAILED")
		void returnsNullForNullFailed() {
			assertNull(CypressOutputParser.extractErrorSummary(null, TestStatus.FAILED));
		}

		@Test
		@DisplayName("returns null for blank output when ERROR")
		void returnsNullForBlankError() {
			assertNull(CypressOutputParser.extractErrorSummary("   ", TestStatus.ERROR));
		}

		@Test
		@DisplayName("returns first non-blank line for ERROR status")
		void returnsFirstLineForError() {
			String output = "Error: Connection timeout after 30000ms";
			assertEquals("Error: Connection timeout after 30000ms",
					CypressOutputParser.extractErrorSummary(output, TestStatus.ERROR));
		}

		@Test
		@DisplayName("skips blank lines to find first non-blank line for ERROR")
		void skipsBlankLinesForError() {
			String output = "\n\n  \nError: Actual error message\nMore details";
			assertEquals("Error: Actual error message",
					CypressOutputParser.extractErrorSummary(output, TestStatus.ERROR));
		}

		@Test
		@DisplayName("truncates ERROR summary to 200 characters")
		void truncatesLongErrorSummary() {
			String longLine = "Error: " + "x".repeat(300);
			String result = CypressOutputParser.extractErrorSummary(longLine, TestStatus.ERROR);
			assertAll(() -> assertEquals(203, result.length()), () -> assertEquals("...", result.substring(200)));
		}

		@Test
		@DisplayName("does not truncate ERROR summary at exactly 200 characters")
		void doesNotTruncateAt200() {
			String exactLine = "E".repeat(200);
			String result = CypressOutputParser.extractErrorSummary(exactLine, TestStatus.ERROR);
			assertEquals(200, result.length());
		}

		@Test
		@DisplayName("extracts text after 'Some tests failed:' marker for FAILED")
		void extractsAfterMarkerWithColon() {
			String output = "Running tests...\nSome tests failed:\nAssertionError: Expected 100 but got 0\nMore details";
			assertEquals("AssertionError: Expected 100 but got 0",
					CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED));
		}

		@Test
		@DisplayName("extracts text after 'Some tests failed' marker without colon for FAILED")
		void extractsAfterMarkerWithoutColon() {
			String output = "Running tests...\nSome tests failed\nFirst failure line\nSecond line";
			assertEquals("First failure line", CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED));
		}

		@Test
		@DisplayName("returns 'Assertion failure' when no non-blank line after marker")
		void returnsDefaultWhenNoLineAfterMarker() {
			String output = "Some tests failed:\n\n   \n";
			assertEquals("Assertion failure", CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED));
		}

		@Test
		@DisplayName("returns 'Test failed' when FAILED but no known marker present")
		void returnsTestFailedWhenNoMarker() {
			String output = "Something went wrong unexpectedly";
			assertEquals("Test failed", CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED));
		}

		@Test
		@DisplayName("skips Cypress box-drawing separator lines for FAILED")
		void skipsSeparatorLines() {
			String output = "Some tests failed\n"
					+ "====================================================================\n"
					+ "  1) my suite\n"
					+ "  AssertionError: expected '.total' to have text '100' but got '0'\n";
			String result = CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED);
			assertEquals("AssertionError: expected '.total' to have text '100' but got '0'", result);
		}

		@Test
		@DisplayName("skips dashed separator lines for FAILED")
		void skipsDashedSeparators() {
			String output = "Some tests failed\n--------------------\nCypressError: Timed out retrying\n";
			String result = CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED);
			assertEquals("CypressError: Timed out retrying", result);
		}

		@Test
		@DisplayName("finds 'Timed out' line for FAILED")
		void findsTimedOutLine() {
			String output = "Some tests failed\nRunning...\nTimed out retrying after 4000ms: expected to find element\n";
			String result = CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED);
			assertEquals("Timed out retrying after 4000ms: expected to find element", result);
		}

		@Test
		@DisplayName("returns fallback message when no assertion-like line found for FAILED")
		void returnsFallbackForFailed() {
			String output = "Some tests failed\nrandom noise line\nanother line\n";
			String result = CypressOutputParser.extractErrorSummary(output, TestStatus.FAILED);
			assertEquals("Test failed (see details pane)", result);
		}
	}

	@Nested
	@DisplayName("determineStatus - crash detection")
	class DetermineStatusCrash {

		@Test
		@DisplayName("returns ERROR when uncaught error outside a test even with 'Some tests failed'")
		void uncaughtErrorIsError() {
			String output = "Some tests failed\nAn uncaught error was detected outside of a test\n";
			assertEquals(TestStatus.ERROR, CypressOutputParser.determineStatus(output));
		}

		@Test
		@DisplayName("returns ERROR for TypeError with 'Some tests failed'")
		void typeErrorIsError() {
			assertEquals(TestStatus.ERROR,
					CypressOutputParser.determineStatus("Some tests failed\nTypeError: x is not a function"));
		}

		@Test
		@DisplayName("returns ERROR for 'Can't run because no spec files were found'")
		void noSpecFilesIsError() {
			assertEquals(TestStatus.ERROR,
					CypressOutputParser.determineStatus("Some tests failed\nCan't run because no spec files were found"));
		}
	}

	@Nested
	@DisplayName("extractVideoPath / extractScreenshotPath")
	class ExtractMediaPaths {

		@ParameterizedTest
		@NullAndEmptySource
		@DisplayName("return null for null/empty output")
		void nullForNullEmpty(String input) {
			assertAll(() -> assertNull(CypressOutputParser.extractVideoPath(input)),
					() -> assertNull(CypressOutputParser.extractScreenshotPath(input)));
		}

		@Test
		@DisplayName("return null when marker absent")
		void nullWhenMarkerAbsent() {
			assertAll(() -> assertNull(CypressOutputParser.extractVideoPath("no markers here")),
					() -> assertNull(CypressOutputParser.extractScreenshotPath("no markers here")));
		}

		@Test
		@DisplayName("return null when marker present but file does not exist")
		void nullWhenFileMissing() {
			String output = "output\n" + FormSpecRunner.PRESERVED_VIDEO_MARKER
					+ "C:\\does\\not\\exist\\video.mp4\n";
			assertNull(CypressOutputParser.extractVideoPath(output));
		}

		@Test
		@DisplayName("extract existing video path from marker")
		void extractsExistingVideo(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp) throws Exception {
			java.nio.file.Path video = tmp.resolve("video.mp4");
			java.nio.file.Files.writeString(video, "fake");
			String output = "run output\n" + FormSpecRunner.PRESERVED_VIDEO_MARKER + video + "\n";
			assertEquals(video.toString(), CypressOutputParser.extractVideoPath(output));
		}

		@Test
		@DisplayName("extract existing screenshot path from marker")
		void extractsExistingScreenshot(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp) throws Exception {
			java.nio.file.Path shot = tmp.resolve("screenshot.png");
			java.nio.file.Files.writeString(shot, "fake");
			String output = "run output\n" + FormSpecRunner.PRESERVED_SCREENSHOT_MARKER + shot + "\n";
			assertEquals(shot.toString(), CypressOutputParser.extractScreenshotPath(output));
		}

		@Test
		@DisplayName("video and screenshot markers are read independently")
		void independentMarkers(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp) throws Exception {
			java.nio.file.Path video = tmp.resolve("v.mp4");
			java.nio.file.Path shot = tmp.resolve("s.png");
			java.nio.file.Files.writeString(video, "v");
			java.nio.file.Files.writeString(shot, "s");
			String output = "out\n" + FormSpecRunner.PRESERVED_VIDEO_MARKER + video + "\n"
					+ FormSpecRunner.PRESERVED_SCREENSHOT_MARKER + shot + "\n";
			assertAll(() -> assertEquals(video.toString(), CypressOutputParser.extractVideoPath(output)),
					() -> assertEquals(shot.toString(), CypressOutputParser.extractScreenshotPath(output)));
		}
	}
}

