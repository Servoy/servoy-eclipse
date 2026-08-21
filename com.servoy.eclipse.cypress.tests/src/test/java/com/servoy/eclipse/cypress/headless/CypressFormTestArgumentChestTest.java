package com.servoy.eclipse.cypress.headless;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CypressFormTestArgumentChest")
public class CypressFormTestArgumentChestTest { // suite-visible

	private static CypressFormTestArgumentChest parse(String... args) {
		return new CypressFormTestArgumentChest(args);
	}

	/** Minimal valid argument set: solution + mandatory export dir. */
	private static CypressFormTestArgumentChest valid(String... extra) {
		String[] base = { "-s", "mySolution", "-o", "out" };
		String[] all = new String[base.length + extra.length];
		System.arraycopy(base, 0, all, 0, base.length);
		System.arraycopy(extra, 0, all, base.length, extra.length);
		return parse(all);
	}

	@Nested
	@DisplayName("validation")
	class Validation {

		@Test
		@DisplayName("no arguments shows help")
		void noArgsShowsHelp() {
			assertTrue(parse().mustShowHelp(), "empty args should request help");
		}

		@Test
		@DisplayName("missing -s marks the chest invalid")
		void missingSolutionInvalid() {
			// -o present but no -s
			CypressFormTestArgumentChest chest = parse("-o", "out");
			assertTrue(chest.isInvalid(), "missing -s must be invalid");
		}

		@Test
		@DisplayName("valid solution + output is not invalid")
		void validNotInvalid() {
			CypressFormTestArgumentChest chest = valid();
			assertAll(() -> assertFalse(chest.isInvalid(), "should be valid"),
					() -> assertFalse(chest.mustShowHelp(), "should not request help"),
					() -> assertEquals("mySolution", chest.getSolutionNames()[0], "solution name"));
		}
	}

	@Nested
	@DisplayName("defaults")
	class Defaults {

		@Test
		@DisplayName("timeout defaults to 120 seconds")
		void timeoutDefault() {
			assertEquals(120, valid().getTimeout());
		}

		@Test
		@DisplayName("forms defaults to an empty list")
		void formsDefault() {
			assertTrue(valid().getForms().isEmpty(), "no -forms means empty");
		}

		@Test
		@DisplayName("generateMissing defaults to false")
		void generateMissingDefault() {
			assertFalse(valid().isGenerateMissing());
		}

		@Test
		@DisplayName("output dir falls back to the mandatory -o export path")
		void outputDirFallsBackToExportPath() {
			CypressFormTestArgumentChest chest = valid();
			assertEquals("out", chest.getOutputDir().toString(), "should use -o value");
		}

		@Test
		@DisplayName("explicit -outputDir overrides the -o export path")
		void outputDirOverride() {
			CypressFormTestArgumentChest chest = valid("-outputDir", "custom-results");
			assertEquals("custom-results", chest.getOutputDir().toString());
		}
	}

	@Nested
	@DisplayName("-forms parsing")
	class FormsParsing {

		@Test
		@DisplayName("splits a comma-separated list")
		void splitsList() {
			CypressFormTestArgumentChest chest = valid("-forms", "a,b,c");
			assertEquals(List.of("a", "b", "c"), chest.getForms());
		}

		@Test
		@DisplayName("trims whitespace around entries")
		void trimsEntries() {
			CypressFormTestArgumentChest chest = valid("-forms", "a, b , c");
			assertEquals(List.of("a", "b", "c"), chest.getForms(), "entries should be trimmed so they match discovery");
		}

		@Test
		@DisplayName("single form yields a one-element list")
		void singleForm() {
			assertEquals(List.of("loginForm"), valid("-forms", "loginForm").getForms());
		}
	}

	@Nested
	@DisplayName("numeric arguments")
	class NumericArgs {

		@Test
		@DisplayName("parses a valid -timeout value")
		void validTimeout() {
			assertEquals(300, valid("-timeout", "300").getTimeout());
		}

		@Test
		@DisplayName("non-numeric -timeout marks the chest invalid")
		void invalidTimeout() {
			CypressFormTestArgumentChest chest = valid("-timeout", "abc");
			assertTrue(chest.isInvalid(), "non-numeric timeout should be invalid");
		}
	}

	@Nested
	@DisplayName("passthrough arguments")
	class Passthrough {

		@Test
		@DisplayName("-generateMissing flag is captured")
		void generateMissingFlag() {
			assertTrue(valid("-generateMissing").isGenerateMissing());
		}

		@Test
		@DisplayName("-cypressArgs value is captured verbatim")
		void cypressArgs() {
			assertEquals("--browser chrome", valid("-cypressArgs", "--browser chrome").getCypressArgs());
		}

		@Test
		@DisplayName("-verbose flag is captured by the base parser")
		void verboseFlag() {
			assertTrue(valid("-verbose").isVerbose());
		}
	}

	@Nested
	@DisplayName("help message")
	class Help {

		@Test
		@DisplayName("documents the runner-specific options and omits -port")
		void helpContent() {
			String help = valid().getHelpMessage();
			assertAll(() -> assertTrue(help.contains("-forms"), "mentions -forms"),
					() -> assertTrue(help.contains("-timeout"), "mentions -timeout"),
					() -> assertTrue(help.contains("-generateMissing"), "mentions -generateMissing"),
					() -> assertTrue(help.contains("-cypressArgs"), "mentions -cypressArgs"),
					() -> assertFalse(help.contains("-port"), "must not advertise removed -port"));
		}
	}
}

