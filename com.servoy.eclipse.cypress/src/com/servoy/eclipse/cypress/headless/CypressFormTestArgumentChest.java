package com.servoy.eclipse.cypress.headless;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;

/**
 * Command-line arguments for the headless Cypress form test runner.
 * <p>
 * Extends {@link AbstractArgumentChest} so the standard Servoy exporter
 * arguments ({@code -s}, {@code -o}, {@code -data}, {@code -as}, {@code -p},
 * {@code -pl}, {@code -verbose}) are parsed by the proven base parser, and the
 * runner can reuse the headless bootstrap in {@code AbstractWorkspaceExporter}
 * (application server start, project import, Servoy model initialization).
 * <p>
 * Runner-specific arguments: {@code -forms}, {@code -timeout},
 * {@code -outputDir}, {@code -generateMissing}, {@code -cypressArgs}.
 * <p>
 * The embedded Tomcat port is <b>not</b> a runner argument: it is resolved from
 * the Servoy application server's {@code server.xml}. To use a different port
 * on a CI agent, point the platform at a custom config via the standard
 * {@code -Dwebserver-file=<server.xml>} system property.
 */
public class CypressFormTestArgumentChest extends AbstractArgumentChest {
	private static final int DEFAULT_TIMEOUT = 120;
	private static final String DEFAULT_OUTPUT_DIR = "./test-results";

	// License argument keys - identical to the WAR exporter's WarArgumentChest so
	// the same CLI syntax works for both products.
	private static final String LICENSE = "license";
	private static final String LICENSE_NAME_SUFFIX = ".company_name";
	private static final String LICENSE_CODE_SUFFIX = ".code";
	private static final String LICENSE_NR_SUFFIX = ".licenses";

	private int timeout = DEFAULT_TIMEOUT;
	private List<String> forms = Collections.emptyList();
	private Path outputDir;
	private boolean generateMissing;
	private String cypressArgs;
	private List<License> licenses = Collections.emptyList();

	private String[] rawArgs;

	public CypressFormTestArgumentChest(String[] args) {
		super();
		this.rawArgs = args;
		initialize(args);
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap) {
		if (argsMap.containsKey("timeout")) {
			String value = parseArg("timeout", "Timeout value was not specified after '-timeout' argument.", argsMap,
					false);
			if (value != null)
				timeout = parsePositiveInt(value, "-timeout", DEFAULT_TIMEOUT);
		}
		String formsArg = parseArg("forms", "Form list was not specified after '-forms' argument.", argsMap, false);
		if (formsArg != null)
			forms = Arrays.asList(formsArg.trim().split("\\s*,\\s*"));

		String outputDirArg = parseArg("outputDir", "Output directory was not specified after '-outputDir' argument.",
				argsMap, false);
		if (outputDirArg != null)
			outputDir = Paths.get(outputDirArg);

		if (argsMap.containsKey("generateMissing"))
			generateMissing = true;

		// Read -cypressArgs straight from the raw argument array. The base parser
		// joins all args with spaces and re-splits on " -", which mangles any value
		// that itself contains a space followed by a dash (e.g. "--browser chrome").
		cypressArgs = parseRawArg("-cypressArgs");

		licenses = parseLicensesArg(argsMap);
	}

	/**
	 * Parses the client license argument(s), mirroring the WAR exporter's scheme so
	 * the same CLI syntax works for both products:
	 *
	 * <pre>
	 * single:   -license.company_name "ACME" -license.code XXXX-XXXX-XXXX -license.licenses SERVER
	 * multiple: -license.1.company_name .. -license.1.code .. -license.1.licenses ..
	 *           -license.2.company_name .. (etc.)
	 * </pre>
	 *
	 * The parsed licenses are activated at runtime via
	 * {@code ApplicationServerRegistry.get().checkClientLicense(...)} - the exact
	 * call the WAR exporter and the setup pipeline wizard use - so NG client
	 * registration is allowed and forms can render. Without a license the embedded
	 * server falls back to the non-commercial user limit, which the concurrent
	 * form-preview clients quickly exhaust ("No more licenses available").
	 */
	private List<License> parseLicensesArg(HashMap<String, String> argsMap) {
		List<License> result = new ArrayList<>();
		if (argsMap.containsKey(LICENSE + LICENSE_NAME_SUFFIX)) {
			License license = buildLicense(argsMap, LICENSE);
			if (license != null)
				result.add(license);
		} else if (argsMap.containsKey(LICENSE + ".1" + LICENSE_NAME_SUFFIX)) {
			int i = 1;
			while (argsMap.containsKey(LICENSE + "." + i + LICENSE_NAME_SUFFIX)) {
				License license = buildLicense(argsMap, LICENSE + "." + i);
				if (license != null)
					result.add(license);
				i++;
			}
		}
		return result;
	}

	private License buildLicense(HashMap<String, String> argsMap, String prefix) {
		String name = argsMap.get(prefix + LICENSE_NAME_SUFFIX);
		String code = argsMap.get(prefix + LICENSE_CODE_SUFFIX);
		String nrLicenses = argsMap.get(prefix + LICENSE_NR_SUFFIX);
		if (checkLicensePart(name, prefix + LICENSE_NAME_SUFFIX)
				&& checkLicensePart(code, prefix + LICENSE_CODE_SUFFIX)
				&& checkLicensePart(nrLicenses, prefix + LICENSE_NR_SUFFIX)) {
			return new License(name, code, nrLicenses);
		}
		return null;
	}

	private boolean checkLicensePart(String value, String key) {
		if (value == null || value.isBlank()) {
			info("Missing value for license argument '-" + key + "'.", com.servoy.j2db.util.ILogLevel.ERROR);
			markInvalid();
			return false;
		}
		return true;
	}

	/**
	 * Returns the single argument that follows {@code flag} in the raw argument
	 * array, preserving it verbatim (spaces and dashes included), or {@code null}
	 * if the flag is absent or has no following value.
	 */
	private String parseRawArg(String flag) {
		if (rawArgs == null)
			return null;
		for (int i = 0; i < rawArgs.length - 1; i++) {
			if (flag.equals(rawArgs[i]))
				return rawArgs[i + 1];
		}
		return null;
	}

	private int parsePositiveInt(String value, String argName, int fallback) {
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException e) {
			info("Invalid numeric value for " + argName + ": '" + value + "'", com.servoy.j2db.util.ILogLevel.ERROR);
			markInvalid();
			return fallback;
		}
	}

	@Override
	public String getHelpMessage() {
		return "Servoy Cypress Form Test Runner. Runs Cypress form tests headlessly for CI.\n" + getHelpMessageCore()
				+ "        -timeout <seconds> ... per-test timeout. Default: " + DEFAULT_TIMEOUT + "\n"
				+ "        -forms <list> ... comma-separated form names to test. Default: all discovered\n"
				+ "        -outputDir <path> ... output directory for JUnit XML. Default: " + DEFAULT_OUTPUT_DIR + "\n"
				+ "        -generateMissing ... auto-generate specs for forms without them\n"
				+ "        -cypressArgs <args> ... extra arguments passed to Cypress\n"
				+ "        -license.company_name <name> -license.code <code> -license.licenses <count|SERVER>\n"
				+ "             ... client license so NG form clients can register (same syntax as the WAR\n"
				+ "             exporter; use -license.<i>.* for multiple licenses). If omitted, the server's\n"
				+ "             own servoy.properties license is used.\n" + getHelpMessageExitCodes();
	}

	public int getTimeout() {
		return timeout;
	}

	public List<String> getForms() {
		return forms;
	}

	public Path getOutputDir() {
		if (outputDir != null)
			return outputDir;
		// fall back to the mandatory -o export file path, then to the default
		String exportPath = getExportFilePath();
		return (exportPath != null && !exportPath.isBlank()) ? Paths.get(exportPath) : Paths.get(DEFAULT_OUTPUT_DIR);
	}

	public boolean isGenerateMissing() {
		return generateMissing;
	}

	public String getCypressArgs() {
		return cypressArgs;
	}

	/**
	 * Client licenses parsed from the command line, to be activated via
	 * {@code checkClientLicense} before running tests. Empty when none supplied.
	 */
	public List<License> getLicenses() {
		return licenses;
	}
}

