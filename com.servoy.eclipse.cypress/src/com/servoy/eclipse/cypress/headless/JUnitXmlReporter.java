package com.servoy.eclipse.cypress.headless;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Writes a standard JUnit XML report from a list of {@link FormTestResult}s so
 * that CI systems (e.g. the Jenkins JUnit plugin) can display Cypress form test
 * results.
 */
public class JUnitXmlReporter {
	public static void writeReport(Path outputDir, String suiteName, List<FormTestResult> results) throws IOException {
		Files.createDirectories(outputDir);

		int tests = results.size();
		int failures = 0;
		int errors = 0;
		long totalTime = 0;

		for (FormTestResult r : results) {
			if (r.isError()) {
				errors++;
			} else if (!r.isPassed()) {
				failures++;
			}
			totalTime += r.getDurationMs();
		}

		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<testsuite name=\"").append(escapeXml(suiteName)).append("\"");
		xml.append(" tests=\"").append(tests).append("\"");
		xml.append(" failures=\"").append(failures).append("\"");
		xml.append(" errors=\"").append(errors).append("\"");
		xml.append(" time=\"").append(String.format(Locale.ROOT, "%.3f", totalTime / 1000.0)).append("\">\n");

		for (FormTestResult r : results) {
			xml.append("  <testcase name=\"").append(escapeXml(r.getFormName())).append("\"");
			xml.append(" classname=\"cypress.form.").append(escapeXml(r.getFormName())).append("\"");
			xml.append(" time=\"").append(String.format(Locale.ROOT, "%.3f", r.getDurationMs() / 1000.0)).append("\"");

			if (r.isPassed()) {
				xml.append("/>\n");
			} else {
				xml.append(">\n");
				String tag = r.isError() ? "error" : "failure";
				xml.append("    <").append(tag).append(" message=\"").append(escapeXml(r.getSummary())).append("\">\n");
				xml.append(cdata(r.getOutput())).append("\n");
				xml.append("    </").append(tag).append(">\n");
				xml.append("  </testcase>\n");
			}
		}

		xml.append("</testsuite>\n");

		Path reportFile = outputDir.resolve("TEST-" + suiteName + ".xml");
		Files.writeString(reportFile, xml.toString(), StandardCharsets.UTF_8);
	}

	/**
	 * Wraps text in a CDATA section, safely splitting any embedded {@code ]]>}
	 * sequences across two CDATA blocks so the XML remains well-formed.
	 */
	private static String cdata(String text) {
		if (text == null || text.isEmpty()) {
			return "<![CDATA[]]>";
		}
		return "<![CDATA[" + text.replace("]]>", "]]]]><![CDATA[>") + "]]>";
	}

	private static String escapeXml(String text) {
		if (text == null)
			return "";
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&apos;");
	}
}

