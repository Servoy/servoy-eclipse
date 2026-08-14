package com.servoy.eclipse.cypress.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Contract tests that verify the three run_cypress_form_tests launcher scripts
 * (Linux, macOS, Windows) contain the expected application ID, JVM flags,
 * memory settings, and argument-forwarding syntax as specified in SVY-21173.
 * <p>
 * Tests are skipped gracefully when the build repository is not present (e.g.
 * on CI agents that only check out Servoy-Copilot).
 */
@DisplayName("run_cypress_form_tests launcher scripts")
public class RunCypressFormTestsLauncherTest {

	private static final String APP_ID = "com.servoy.eclipse.developer.mcp.cypressFormTestRunner";
	private static final String XMS = "-Xms256m";
	private static final String XMX = "-Xmx2048m";
	private static final String SKIP_CHECKOUT = "-Dservoy.cloud.skipCheckout=true";
	private static final String DISABLE_CHROMIUM = "-Dchromium.integration.eclipse.disable=true";
	private static final String ADD_EXPORTS_BARE = "--add-exports=java.base/sun.security.x509=ALL-UNNAMED";
	private static final String ADD_EXPORTS_WIN = "\"--add-exports=java.base/sun.security.x509=ALL-UNNAMED\"";
	private static final String EQUINOX_MAIN = "org.eclipse.equinox.launcher.Main";
	private static final String JAR_LAUNCHER = "-jar \"%EQUINOXJAR%\"";
	private static final String NO_SPLASH = "-noSplash";
	private static final String CONSOLELOG = "-consolelog";

	/**
	 * Root of the eclipse_build/build/ directory inside the build repository.
	 * Override via {@code -Dcypress.launcher.build.base=<path>} when the build repo
	 * lives somewhere other than the default sibling location.
	 */
	private static final Path BUILD_BASE = Paths.get(System.getProperty("cypress.launcher.build.base",
			"D:\\Eclipse\\202603Workspace\\build\\eclipse_build\\build"));

	private static String read(Path p) throws IOException {
		return Files.readString(p);
	}

	// -------------------------------------------------------------------------
	// Linux
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Linux run_cypress_form_tests.sh")
	class Linux {

		private final Path script = BUILD_BASE.resolve("linux_files/cypress_runner/run_cypress_form_tests.sh");

		@Test
		@DisplayName("script exists at expected location")
		void scriptExists() {
			assumeTrue(Files.exists(script), "Script not present, skipping");
		}

		@Test
		@DisplayName("contains correct application ID")
		void applicationId() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(APP_ID), "must reference cypress test runner application ID");
		}

		@Test
		@DisplayName("contains -Xms256m")
		void xms() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMS), "must set minimum heap to 256 MiB");
		}

		@Test
		@DisplayName("contains -Xmx2048m")
		void xmx() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMX), "must set maximum heap to 2048 MiB");
		}

		@Test
		@DisplayName("contains -Dservoy.cloud.skipCheckout=true")
		void skipCheckout() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(SKIP_CHECKOUT), "must include skipCheckout flag");
		}

		@Test
		@DisplayName("contains -Dchromium.integration.eclipse.disable=true")
		void disableChromium() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(DISABLE_CHROMIUM),
					"must disable chromium integration for headless execution");
		}

		@Test
		@DisplayName("contains bare --add-exports flag (no quoting)")
		void addExports() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(ADD_EXPORTS_BARE), "must include add-exports for Servoy TLS on Java 17+");
		}

		@Test
		@DisplayName("contains -noSplash")
		void noSplash() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(NO_SPLASH), "must suppress Eclipse splash screen");
		}

		@Test
		@DisplayName("contains -consolelog")
		void consolelog() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(CONSOLELOG), "must redirect Eclipse log to stdout for CI capture");
		}

		@Test
		@DisplayName("forwards all arguments via \"$@\"")
		void argumentForwarding() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains("\"$@\""), "must forward all arguments verbatim via \"$@\"");
		}

		@Test
		@DisplayName("does NOT use macOS Contents/Home JRE path")
		void noContentsHome() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertFalse(read(script).contains("Contents/Home"),
					"Linux must use jre/bin/java directly, not the macOS bundle path");
		}

		@Test
		@DisplayName("uses -cp org.eclipse.equinox.launcher.Main launcher style (not -jar)")
		void launcherStyle() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(EQUINOX_MAIN),
					"Linux must use -cp ... org.eclipse.equinox.launcher.Main, not -jar style");
		}
	}

	// -------------------------------------------------------------------------
	// macOS
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("macOS run_cypress_form_tests.sh")
	class MacOS {

		private final Path script = BUILD_BASE.resolve("macosx_files/cypress_runner/run_cypress_form_tests.sh");

		@Test
		@DisplayName("script exists at expected location")
		void scriptExists() {
			assumeTrue(Files.exists(script), "Script not present, skipping");
		}

		@Test
		@DisplayName("contains correct application ID")
		void applicationId() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(APP_ID), "must reference cypress test runner application ID");
		}

		@Test
		@DisplayName("contains -Xms256m")
		void xms() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMS), "must set minimum heap to 256 MiB");
		}

		@Test
		@DisplayName("contains -Xmx2048m")
		void xmx() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMX), "must set maximum heap to 2048 MiB");
		}

		@Test
		@DisplayName("contains -Dservoy.cloud.skipCheckout=true")
		void skipCheckout() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(SKIP_CHECKOUT), "must include skipCheckout flag");
		}

		@Test
		@DisplayName("contains -Dchromium.integration.eclipse.disable=true")
		void disableChromium() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(DISABLE_CHROMIUM),
					"must disable chromium integration for headless execution");
		}

		@Test
		@DisplayName("contains bare --add-exports flag (no quoting)")
		void addExports() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(ADD_EXPORTS_BARE), "must include add-exports for Servoy TLS on Java 17+");
		}

		@Test
		@DisplayName("contains -noSplash")
		void noSplash() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(NO_SPLASH), "must suppress Eclipse splash screen");
		}

		@Test
		@DisplayName("contains -consolelog")
		void consolelog() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(CONSOLELOG), "must redirect Eclipse log to stdout for CI capture");
		}

		@Test
		@DisplayName("forwards all arguments via \"$@\"")
		void argumentForwarding() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains("\"$@\""), "must forward all arguments verbatim via \"$@\"");
		}

		@Test
		@DisplayName("uses macOS Contents/Home JRE path")
		void contentsHomeJrePath() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains("Contents/Home"),
					"macOS must use jre/Contents/Home/bin/java (bundle JRE layout)");
		}

		@Test
		@DisplayName("uses -cp org.eclipse.equinox.launcher.Main launcher style (not -jar)")
		void launcherStyle() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(EQUINOX_MAIN),
					"macOS must use -cp ... org.eclipse.equinox.launcher.Main, not -jar style");
		}
	}

	// -------------------------------------------------------------------------
	// Windows
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Windows run_cypress_form_tests.bat")
	class Windows {

		private final Path script = BUILD_BASE.resolve("windows_files/cypress_runner/run_cypress_form_tests.bat");

		@Test
		@DisplayName("script exists at expected location")
		void scriptExists() {
			assumeTrue(Files.exists(script), "Script not present, skipping");
		}

		@Test
		@DisplayName("contains correct application ID")
		void applicationId() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(APP_ID), "must reference cypress test runner application ID");
		}

		@Test
		@DisplayName("contains -Xms256m")
		void xms() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMS), "must set minimum heap to 256 MiB");
		}

		@Test
		@DisplayName("contains -Xmx2048m")
		void xmx() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(XMX), "must set maximum heap to 2048 MiB");
		}

		@Test
		@DisplayName("contains -Dservoy.cloud.skipCheckout=true")
		void skipCheckout() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(SKIP_CHECKOUT), "must include skipCheckout flag");
		}

		@Test
		@DisplayName("contains -Dchromium.integration.eclipse.disable=true")
		void disableChromium() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(DISABLE_CHROMIUM),
					"must disable chromium integration for headless execution");
		}

		@Test
		@DisplayName("--add-exports flag is double-quoted with full module path")
		void addExportsDoubleQuoted() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(ADD_EXPORTS_WIN),
					"Windows must double-quote the full --add-exports value to prevent cmd.exe from misparsing the '=' sign");
		}

		@Test
		@DisplayName("uses -jar launcher style (not -cp)")
		void launcherStyle() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(JAR_LAUNCHER),
					"Windows must use -jar \"%EQUINOXJAR%\" style launcher, not -cp");
		}

		@Test
		@DisplayName("contains -noSplash")
		void noSplash() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(NO_SPLASH), "must suppress Eclipse splash screen");
		}

		@Test
		@DisplayName("contains -consolelog")
		void consolelog() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains(CONSOLELOG), "must redirect Eclipse log to stdout for CI capture");
		}

		@Test
		@DisplayName("forwards all arguments via %*")
		void argumentForwarding() throws IOException {
			assumeTrue(Files.exists(script), "Script not present, skipping");
			assertTrue(read(script).contains("%*"), "must forward all arguments verbatim via %*");
		}
	}
}

