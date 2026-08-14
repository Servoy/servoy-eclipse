package com.servoy.eclipse.cypress.headless;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JUnitXmlReporter")
public class JUnitXmlReporterTest { // suite-visible

	private Path dir;

	@BeforeEach
	void setUp() throws IOException {
		dir = Files.createTempDirectory("cypress-report-test");
	}

	@AfterEach
	void tearDown() throws IOException {
		if (dir != null && Files.exists(dir)) {
			try (Stream<Path> paths = Files.walk(dir)) {
				paths.sorted(Comparator.reverseOrder()).forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException ignored) {
						// best-effort cleanup
					}
				});
			}
		}
	}

	private static FormTestResult passed(String form) {
		return new FormTestResult(form, true, false, "All tests passed", "All tests passed", 1200);
	}

	private static FormTestResult failed(String form, String output) {
		return new FormTestResult(form, false, false, output, "Some tests failed for " + form, 800);
	}

	private static FormTestResult errored(String form, String output) {
		return new FormTestResult(form, false, true, output, "Error: boom", 50);
	}

	private String writeAndRead(List<FormTestResult> results) throws Exception {
		JUnitXmlReporter.writeReport(dir, "cypress-form-tests", results);
		Path report = dir.resolve("TEST-cypress-form-tests.xml");
		assertTrue(Files.exists(report), "report file should be created");
		return Files.readString(report, StandardCharsets.UTF_8);
	}

	@Nested
	@DisplayName("suite-level attributes")
	class SuiteAttributes {

		@Test
		@DisplayName("counts tests, failures and errors and aggregates time")
		void countsAndTime() throws Exception {
			String xml = writeAndRead(List.of(passed("a"), failed("b", "boom"), errored("c", "Error: x")));
			assertAll(() -> assertTrue(xml.contains("tests=\"3\""), "tests=3"),
					() -> assertTrue(xml.contains("failures=\"1\""), "failures=1"),
					() -> assertTrue(xml.contains("errors=\"1\""), "errors=1"),
					// 1200 + 800 + 50 = 2050ms => 2.050s
					() -> assertTrue(xml.contains("time=\"2.050\""), "aggregated time"));
		}

		@Test
		@DisplayName("writes a well-formed empty testsuite for no results")
		void emptySuite() throws Exception {
			String xml = writeAndRead(List.of());
			assertAll(() -> assertTrue(xml.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), "xml decl"),
					() -> assertTrue(xml.contains("tests=\"0\""), "tests=0"),
					() -> assertTrue(xml.contains("failures=\"0\""), "failures=0"),
					() -> assertTrue(xml.contains("errors=\"0\""), "errors=0"),
					() -> assertTrue(xml.contains("</testsuite>"), "closing tag"));
		}
	}

	@Nested
	@DisplayName("testcase rendering")
	class TestCaseRendering {

		@Test
		@DisplayName("passed test renders a self-closing testcase without failure/error children")
		void passedSelfClosing() throws Exception {
			String xml = writeAndRead(List.of(passed("myForm")));
			assertAll(() -> assertTrue(xml.contains("name=\"myForm\""), "name attr"),
					() -> assertTrue(xml.contains("classname=\"cypress.form.myForm\""), "classname attr"),
					() -> assertTrue(xml.contains("/>"), "self-closing"),
					() -> assertFalse(xml.contains("<failure"), "no failure element"),
					() -> assertFalse(xml.contains("<error"), "no error element"));
		}

		@Test
		@DisplayName("failed test renders a <failure> element")
		void failedElement() throws Exception {
			String xml = writeAndRead(List.of(failed("loginForm", "assertion failed")));
			assertAll(() -> assertTrue(xml.contains("<failure message="), "failure element"),
					() -> assertFalse(xml.contains("<error message="), "not error"));
		}

		@Test
		@DisplayName("errored test renders an <error> element")
		void errorElement() throws Exception {
			String xml = writeAndRead(List.of(errored("brokenForm", "Error: no spec files")));
			assertTrue(xml.contains("<error message="), "error element");
		}
	}

	@Nested
	@DisplayName("XML safety")
	class XmlSafety {

		@Test
		@DisplayName("escapes special characters in attribute values")
		void escapesAttributes() throws Exception {
			FormTestResult r = new FormTestResult("a<b>&\"c", false, false, "out", "sum & <tag> \"q\"", 10);
			String xml = writeAndRead(List.of(r));
			assertAll(() -> assertTrue(xml.contains("&lt;b&gt;"), "escaped <>"),
					() -> assertTrue(xml.contains("&amp;"), "escaped &"),
					() -> assertTrue(xml.contains("&quot;c"), "escaped quote"),
					() -> assertFalse(xml.contains("a<b>&\"c"), "raw unescaped name must not appear"));
		}

		@Test
		@DisplayName("splits embedded ]]> so the CDATA section stays well-formed")
		void splitsCdata() throws Exception {
			String malicious = "before ]]> after";
			String xml = writeAndRead(List.of(failed("f", malicious)));
			assertAll(
					// the raw ]]> must be split into ]]]]><![CDATA[>
					() -> assertTrue(xml.contains("]]]]><![CDATA[>"), "CDATA split marker present"),
					() -> assertFalse(xml.contains("before ]]> after"), "raw ]]> not left intact"),
					() -> assertTrue(xml.contains("<![CDATA["), "opens CDATA"));
		}

		@Test
		@DisplayName("wraps null output in an empty CDATA section")
		void nullOutputCdata() throws Exception {
			FormTestResult r = new FormTestResult("f", false, false, null, "sum", 10);
			String xml = writeAndRead(List.of(r));
			assertTrue(xml.contains("<![CDATA[]]>"), "empty CDATA for null output");
		}
	}
}

