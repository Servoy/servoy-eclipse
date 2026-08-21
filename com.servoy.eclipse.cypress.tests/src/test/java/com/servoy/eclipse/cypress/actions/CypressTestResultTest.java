package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;

@DisplayName("CypressTestResult")
class CypressTestResultTest {

	@Nested
	@DisplayName("record construction")
	class RecordConstruction {

		@Test
		@DisplayName("stores all fields correctly")
		void storesAllFields() {
			CypressTestResult result = new CypressTestResult("loginForm", TestType.FORM, TestStatus.PASSED, null,
					"All tests passed", 1500);

			assertAll(() -> assertEquals("loginForm", result.testName()),
					() -> assertEquals(TestType.FORM, result.testType()),
					() -> assertEquals(TestStatus.PASSED, result.status()), () -> assertNull(result.errorSummary()),
					() -> assertEquals("All tests passed", result.rawOutput()),
					() -> assertEquals(1500, result.durationMs()));
		}

		@Test
		@DisplayName("stores error summary for failed test")
		void storesErrorSummary() {
			CypressTestResult result = new CypressTestResult("orderForm", TestType.FORM, TestStatus.FAILED,
					"Expected '.total' to have text '100'", "Some tests failed:\nExpected '.total' to have text '100'",
					3100);

			assertAll(() -> assertEquals(TestStatus.FAILED, result.status()),
					() -> assertEquals("Expected '.total' to have text '100'", result.errorSummary()));
		}

		@Test
		@DisplayName("allows null rawOutput and errorSummary")
		void allowsNullFields() {
			CypressTestResult result = new CypressTestResult("pendingForm", TestType.E2E, TestStatus.PENDING, null,
					null, 0);

			assertAll(() -> assertNull(result.errorSummary()), () -> assertNull(result.rawOutput()),
					() -> assertEquals(0, result.durationMs()));
		}

		@Test
		@DisplayName("supports E2E test type")
		void supportsE2EType() {
			CypressTestResult result = new CypressTestResult("e2eTest", TestType.E2E, TestStatus.RUNNING, null, null,
					0);

			assertEquals(TestType.E2E, result.testType());
		}
	}

	@Nested
	@DisplayName("TestStatus enum")
	class TestStatusEnum {

		@Test
		@DisplayName("has exactly 5 values")
		void hasFiveValues() {
			assertEquals(5, TestStatus.values().length);
		}

		@ParameterizedTest
		@EnumSource(TestStatus.class)
		@DisplayName("all enum values are accessible")
		void allValuesAccessible(TestStatus status) {
			assertEquals(status, TestStatus.valueOf(status.name()));
		}

		@Test
		@DisplayName("values are in expected order")
		void valuesInOrder() {
			TestStatus[] values = TestStatus.values();
			assertAll(() -> assertEquals(TestStatus.PENDING, values[0]),
					() -> assertEquals(TestStatus.RUNNING, values[1]), () -> assertEquals(TestStatus.PASSED, values[2]),
					() -> assertEquals(TestStatus.FAILED, values[3]), () -> assertEquals(TestStatus.ERROR, values[4]));
		}
	}

	@Nested
	@DisplayName("TestType enum")
	class TestTypeEnum {

		@Test
		@DisplayName("has exactly 2 values")
		void hasTwoValues() {
			assertEquals(2, TestType.values().length);
		}

		@ParameterizedTest
		@EnumSource(TestType.class)
		@DisplayName("all enum values are accessible")
		void allValuesAccessible(TestType type) {
			assertEquals(type, TestType.valueOf(type.name()));
		}

		@Test
		@DisplayName("values are FORM and E2E")
		void valuesAreFormAndE2E() {
			TestType[] values = TestType.values();
			assertAll(() -> assertEquals(TestType.FORM, values[0]), () -> assertEquals(TestType.E2E, values[1]));
		}
	}

	@Nested
	@DisplayName("record equality")
	class RecordEquality {

		@Test
		@DisplayName("two records with same values are equal")
		void equalRecords() {
			CypressTestResult r1 = new CypressTestResult("form", TestType.FORM, TestStatus.PASSED, null, "ok", 100);
			CypressTestResult r2 = new CypressTestResult("form", TestType.FORM, TestStatus.PASSED, null, "ok", 100);

			assertEquals(r1, r2);
		}

		@Test
		@DisplayName("toString contains test name")
		void toStringContainsName() {
			CypressTestResult result = new CypressTestResult("myForm", TestType.FORM, TestStatus.PASSED, null, "ok",
					100);

			String str = result.toString();
			assertAll(() -> assertEquals(true, str.contains("myForm")),
					() -> assertEquals(true, str.contains("PASSED")));
		}
	}

	@Nested
	@DisplayName("media fields")
	class MediaFields {

		@Test
		@DisplayName("convenience constructor leaves video and screenshot paths null")
		void convenienceConstructorNullMedia() {
			CypressTestResult result = new CypressTestResult("form", TestType.FORM, TestStatus.FAILED, "err", "out",
					100);

			assertAll(() -> assertNull(result.videoPath()), () -> assertNull(result.screenshotPath()),
					() -> assertFalse(result.hasVideo()), () -> assertFalse(result.hasScreenshot()));
		}

		@Test
		@DisplayName("full constructor stores video and screenshot paths")
		void fullConstructorStoresMedia() {
			CypressTestResult result = new CypressTestResult("form", TestType.E2E, TestStatus.FAILED, "err", "out",
					100, "C:\\videos\\form.mp4", "C:\\shots\\form.png");

			assertAll(() -> assertEquals("C:\\videos\\form.mp4", result.videoPath()),
					() -> assertEquals("C:\\shots\\form.png", result.screenshotPath()),
					() -> assertTrue(result.hasVideo()), () -> assertTrue(result.hasScreenshot()));
		}

		@Test
		@DisplayName("hasVideo is false for null and blank path")
		void hasVideoFalseForNullBlank() {
			CypressTestResult nullVideo = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					null, null);
			CypressTestResult blankVideo = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					"   ", null);

			assertAll(() -> assertFalse(nullVideo.hasVideo()), () -> assertFalse(blankVideo.hasVideo()));
		}

		@Test
		@DisplayName("hasScreenshot is false for null and blank path")
		void hasScreenshotFalseForNullBlank() {
			CypressTestResult nullShot = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					null, null);
			CypressTestResult blankShot = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					null, "  ");

			assertAll(() -> assertFalse(nullShot.hasScreenshot()), () -> assertFalse(blankShot.hasScreenshot()));
		}

		@Test
		@DisplayName("hasVideo true independent of hasScreenshot")
		void mediaFlagsIndependent() {
			CypressTestResult videoOnly = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					"v.mp4", null);
			CypressTestResult shotOnly = new CypressTestResult("f", TestType.E2E, TestStatus.FAILED, null, null, 0,
					null, "s.png");

			assertAll(() -> assertTrue(videoOnly.hasVideo()), () -> assertFalse(videoOnly.hasScreenshot()),
					() -> assertFalse(shotOnly.hasVideo()), () -> assertTrue(shotOnly.hasScreenshot()));
		}
	}
}

