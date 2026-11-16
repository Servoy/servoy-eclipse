package com.servoy.eclipse.cypress.plainjunit.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.servoy.eclipse.cypress.services.CypressSupportScaffold;

/**
 * SVY-21619: covers {@link CypressSupportScaffold#ensureMeasurementSupport(Path)} and the generated command JS. Acceptance criteria:
 * {@code cy.measureAtViewport(selector, width, height)} exists in a scaffolded {@code cypress/support/commands.js}, is the single place the
 * measure/settle/verify logic lives, yields {@code { innerWidth, containerWidth }}, and is not clobbered when a {@code cy.login()} command already
 * exists in that file; a pre-existing {@code e2e.js} is likewise left alone; the merge is idempotent.
 */
@DisplayName("CypressSupportScaffold")
class CypressSupportScaffoldTest
{
	private static final String MEASURE_SENTINEL = "Cypress.Commands.add('measureAtViewport'";
	private static final String MEASURE_CONTAINER_SENTINEL = "Cypress.Commands.add('measureContainer'";
	private static final String BLOCK_BEGIN = "// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---";
	private static final String LOGIN_SENTINEL = "Cypress.Commands.add('login'";

	private static Path commandsFile(Path scriptsRoot)
	{
		return scriptsRoot.resolve("cypress").resolve("support").resolve("commands.js");
	}

	private static Path e2eFile(Path scriptsRoot)
	{
		return scriptsRoot.resolve("cypress").resolve("support").resolve("e2e.js");
	}

	private static int occurrences(String haystack, String needle)
	{
		int count = 0;
		int idx = 0;
		while ((idx = haystack.indexOf(needle, idx)) >= 0)
		{
			count++;
			idx += needle.length();
		}
		return count;
	}

	@Nested
	@DisplayName("measureCommandsJs content")
	class CommandJsContent
	{
		@Test
		@DisplayName("defines both measureAtViewport and measureContainer custom commands")
		void definesBothCommands()
		{
			String js = CypressSupportScaffold.measureCommandsJs();
			assertTrue(js.contains(MEASURE_SENTINEL), "must define measureAtViewport");
			assertTrue(js.contains(MEASURE_CONTAINER_SENTINEL), "must define measureContainer");
		}

		@Test
		@DisplayName("asserts window.innerWidth and fails loud with both numbers when the viewport is not applied")
		void failsLoudOnUnappliedViewport()
		{
			String js = CypressSupportScaffold.measureCommandsJs();
			assertTrue(js.contains("win.innerWidth"), "must read window.innerWidth");
			assertTrue(js.contains("requested width="), "must name the requested width in the failure message");
			assertTrue(js.contains("window.innerWidth="), "must name the actual innerWidth in the failure message");
		}

		@Test
		@DisplayName("uses a 3-sample stabilize poll and never a blind layout cy.wait")
		void threeSampleStabilizePoll()
		{
			String js = CypressSupportScaffold.measureCommandsJs();
			// the settle step compares three consecutive samples
			assertTrue(js.contains("samples.length >= 3"), "settle must require three samples");
			assertTrue(js.contains("getBoundingClientRect"), "settle must read the element box");
			// the only cy.wait is the 100ms inter-sample poll spacing, explicitly not a layout sleep
			assertTrue(js.contains("cy.wait(100"), "the inter-sample spacing is a 100ms poll");
			assertEquals(1, occurrences(js, "cy.wait("), "there must be exactly one cy.wait (the poll spacing), never a blind layout sleep");
		}

		@Test
		@DisplayName("emits the SVY_MEASURE marker via the svyMeasure cy.task so the Java runner can hoist the numbers from stdout")
		void emitsSvyMeasureMarkerViaTask()
		{
			String js = CypressSupportScaffold.measureCommandsJs();
			// SVY-21619: the marker must travel through cy.task('svyMeasure', ...) - a Node handler console.logs it, and
			// Node stdout IS printed by `cypress run` and captured by the runner, which cy.log is NOT.
			assertTrue(js.contains("cy.task('" + CypressSupportScaffold.SVY_MEASURE_TASK_NAME + "'"),
				"the marker must be routed through the svyMeasure cy.task so it reaches Cypress stdout");
			// the payload the task carries must still be built from the shared SVY_MEASURE marker literal
			assertTrue(js.contains("'" + CypressSupportScaffold.SVY_MEASURE_MARKER + "innerWidth="),
				"the task payload must still be the SVY_MEASURE marker string the runner matches");
			assertTrue(js.contains(CypressSupportScaffold.SVY_MEASURE_MARKER), "must carry the shared SVY_MEASURE marker literal");
		}

		@Test
		@DisplayName("yields { innerWidth, containerWidth } so callers can chain assertions")
		void yieldsMeasurements()
		{
			String js = CypressSupportScaffold.measureCommandsJs();
			assertTrue(js.contains("innerWidth: innerWidth") && js.contains("containerWidth: cw"),
				"the command must yield an object with innerWidth and containerWidth");
		}

		@Test
		@DisplayName("the shared marker constant is exactly 'SVY_MEASURE '")
		void markerConstantValue()
		{
			assertEquals("SVY_MEASURE ", CypressSupportScaffold.SVY_MEASURE_MARKER);
		}

		@Test
		@DisplayName("the svyMeasure task constant is exactly 'svyMeasure'")
		void taskNameConstantValue()
		{
			assertEquals("svyMeasure", CypressSupportScaffold.SVY_MEASURE_TASK_NAME);
		}

		@Test
		@DisplayName("the setupNodeEvents declaration registers the svyMeasure task whose Node handler console.logs the line and returns config")
		void setupNodeEventsRegistersTask()
		{
			String decl = CypressSupportScaffold.setupNodeEventsDeclaration();
			// SVY-21619: both config scaffolds splice this in so cy.task('svyMeasure', ...) reaches a Node handler
			// whose console.log IS printed by `cypress run` (cy.log is not) and captured by the Java runner.
			assertTrue(decl.contains("setupNodeEvents(on, config)"), "must be a setupNodeEvents(on, config) declaration");
			assertTrue(decl.contains("on('task'"), "must register a Cypress task");
			assertTrue(decl.contains(CypressSupportScaffold.SVY_MEASURE_TASK_NAME + "(line)"),
				"must register the svyMeasure task taking the marker line");
			assertTrue(decl.contains("console.log(line)"), "the Node handler must console.log the line onto stdout");
			assertTrue(decl.contains("return config"), "setupNodeEvents must return config as Cypress expects");
		}

		@Test
		@DisplayName("the root container selector is the .svy-form baseline and the default height is 1080")
		void selectorAndHeightConstants()
		{
			assertEquals(".svy-form", CypressSupportScaffold.ROOT_CONTAINER_SELECTOR);
			assertEquals(1080, CypressSupportScaffold.DEFAULT_VIEWPORT_HEIGHT);
		}
	}

	@Nested
	@DisplayName("ensureMeasurementSupport")
	class EnsureSupport
	{
		@Test
		@DisplayName("creates commands.js with the measurement commands and e2e.js importing it when neither exists")
		void createsBothFilesWhenMissing(@TempDir Path scriptsRoot) throws IOException
		{
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			Path commands = commandsFile(scriptsRoot);
			Path e2e = e2eFile(scriptsRoot);
			assertTrue(Files.exists(commands), "commands.js must be created");
			assertTrue(Files.exists(e2e), "e2e.js must be created");

			String commandsContent = Files.readString(commands, StandardCharsets.UTF_8);
			assertTrue(commandsContent.contains(MEASURE_SENTINEL), "commands.js must hold measureAtViewport");
			assertTrue(commandsContent.contains(MEASURE_CONTAINER_SENTINEL), "commands.js must hold measureContainer");

			assertTrue(Files.readString(e2e, StandardCharsets.UTF_8).contains("import './commands'"), "e2e.js must import './commands'");
		}

		@Test
		@DisplayName("a second call does not duplicate the measurement command (idempotent)")
		void idempotentSecondCall(@TempDir Path scriptsRoot) throws IOException
		{
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String commandsContent = Files.readString(commandsFile(scriptsRoot), StandardCharsets.UTF_8);
			assertEquals(1, occurrences(commandsContent, MEASURE_SENTINEL), "the measureAtViewport command must appear exactly once after two calls");
		}

		@Test
		@DisplayName("a pre-existing commands.js holding cy.login() keeps login and gets the measurement commands appended")
		void doesNotClobberExistingLoginCommand(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			Files.writeString(commands, "// existing auth support\nCypress.Commands.add('login', (u, p) => { cy.visit('/'); });\n",
				StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String content = Files.readString(commands, StandardCharsets.UTF_8);
			assertTrue(content.contains(LOGIN_SENTINEL), "the pre-existing cy.login() command must be preserved");
			assertTrue(content.contains("cy.visit('/')"), "the body of the pre-existing login command must be preserved");
			assertTrue(content.contains(MEASURE_SENTINEL), "the measurement command must be appended");
		}

		@Test
		@DisplayName("a pre-existing commands.js that already holds the measurement command is left byte-for-byte unchanged")
		void leavesExistingMeasureCommandUntouched(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			String original = "// hand-tuned\n" + CypressSupportScaffold.measureCommandsJs();
			Files.writeString(commands, original, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			assertEquals(original, Files.readString(commands, StandardCharsets.UTF_8),
				"when the sentinel is already present nothing must be appended");
		}

		@Test
		@DisplayName("a STALE cy.log-only measurement block is rewritten to the svyMeasure task form, keeping a preceding cy.login() intact")
		void rewritesStaleCyLogOnlyBlock(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			// Simulate a pre-SVY-21619 fix block: holds the measureAtViewport sentinel but emits the marker via cy.log only
			// (no cy.task). It is preceded by a cy.login() command that must survive the rewrite.
			String stale = "Cypress.Commands.add('login', (u, p) => { cy.visit('/'); });\n\n" +
				"// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---\n" +
				"Cypress.Commands.add('measureAtViewport', (selector, width, height) => {\n" +
				"  cy.log('SVY_MEASURE innerWidth=' + 1 + ' container=' + selector);\n" +
				"});\n" +
				"Cypress.Commands.add('measureContainer', (selector) => {\n" +
				"  cy.log('SVY_MEASURE innerWidth=' + 1 + ' container=' + selector);\n" +
				"});\n";
			Files.writeString(commands, stale, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String content = Files.readString(commands, StandardCharsets.UTF_8);
			assertTrue(content.contains(LOGIN_SENTINEL), "the preceding cy.login() command must be preserved");
			assertTrue(content.contains("cy.visit('/')"), "the body of the preceding login command must be preserved");
			assertTrue(content.contains("cy.task('" + CypressSupportScaffold.SVY_MEASURE_TASK_NAME + "'"),
				"the stale block must be rewritten to route the marker through the svyMeasure task");
			assertEquals(1, occurrences(content, MEASURE_SENTINEL), "the rewritten block must hold exactly one measureAtViewport command");
			assertEquals(1, occurrences(content, "// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---"),
				"the stale block must be replaced in place, not duplicated");
		}

		@Test
		@DisplayName("a second call after a stale rewrite is idempotent (sentinel + task present)")
		void idempotentAfterStaleRewrite(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			Files.writeString(commands,
				"// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---\n" +
					"Cypress.Commands.add('measureAtViewport', (selector, width, height) => { cy.log('SVY_MEASURE x'); });\n",
				StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);
			String afterFirst = Files.readString(commands, StandardCharsets.UTF_8);
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);
			String afterSecond = Files.readString(commands, StandardCharsets.UTF_8);

			assertTrue(afterFirst.contains("cy.task('" + CypressSupportScaffold.SVY_MEASURE_TASK_NAME + "'"), "stale block rewritten on first call");
			assertEquals(afterFirst, afterSecond, "a second call after the rewrite must leave the file unchanged (task now present)");
		}

		@Test
		@DisplayName("a pre-existing e2e.js is not clobbered")
		void doesNotClobberExistingE2eJs(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path e2e = supportDir.resolve("e2e.js");
			String original = "import './commands';\nimport './my-extra-support';\n";
			Files.writeString(e2e, original, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			assertEquals(original, Files.readString(e2e, StandardCharsets.UTF_8), "an existing e2e.js must be left exactly as it was");
		}

		@Test
		@DisplayName("the appended merge never duplicates login nor drops the measure command")
		void mergeIsCleanAlongsideLogin(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			Files.writeString(commands, "Cypress.Commands.add('login', () => {});\n", StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String content = Files.readString(commands, StandardCharsets.UTF_8);
			assertEquals(1, occurrences(content, LOGIN_SENTINEL), "login must stay single");
			assertEquals(1, occurrences(content, MEASURE_SENTINEL), "measureAtViewport must be appended exactly once across repeated calls");
		}

		@Test
		@DisplayName("a file with measureContainer but not the sentinel is still treated via the measureAtViewport sentinel only")
		void sentinelIsMeasureAtViewport(@TempDir Path scriptsRoot) throws IOException
		{
			// The idempotency sentinel is the measureAtViewport add; confirm a fresh scaffold writes both commands.
			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);
			String content = Files.readString(commandsFile(scriptsRoot), StandardCharsets.UTF_8);
			assertFalse(content.isBlank());
			assertTrue(content.contains(MEASURE_SENTINEL) && content.contains(MEASURE_CONTAINER_SENTINEL));
		}

		@Test
		@DisplayName("a pre-existing commands.js without a trailing newline gets a line break before the appended block")
		void appendsAfterMissingTrailingNewline(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			String original = "Cypress.Commands.add('login', () => {});";
			Files.writeString(commands, original, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String content = Files.readString(commands, StandardCharsets.UTF_8);
			assertTrue(content.startsWith(original + "\n"), "the appended block must start on its own line, not glued to the last command");
			assertEquals(1, occurrences(content, LOGIN_SENTINEL), "login must stay single");
			assertEquals(1, occurrences(content, MEASURE_SENTINEL), "measureAtViewport must be appended exactly once");
		}

		@Test
		@DisplayName("a stale block glued to the preceding command is separated from it by a line break when rewritten")
		void staleBlockGluedToPrecedingCommand(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			String stale = "Cypress.Commands.add('login', () => {});" + BLOCK_BEGIN + "\n" +
				"Cypress.Commands.add('measureAtViewport', (s, w, h) => { cy.log('SVY_MEASURE x'); });\n";
			Files.writeString(commands, stale, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			String content = Files.readString(commands, StandardCharsets.UTF_8);
			assertTrue(content.contains("});\n" + BLOCK_BEGIN), "the rewritten block must start on its own line");
			assertEquals(1, occurrences(content, LOGIN_SENTINEL), "the preceding login command must be preserved exactly once");
			assertTrue(content.contains("cy.task('" + CypressSupportScaffold.SVY_MEASURE_TASK_NAME + "'"), "the block must be rewritten to the task form");
		}

		@Test
		@DisplayName("a hand-written measureAtViewport without the Servoy block marker is never rewritten")
		void handWrittenCommandWithoutBlockMarkerIsLeftAlone(@TempDir Path scriptsRoot) throws IOException
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);
			Path commands = supportDir.resolve("commands.js");
			String original = "Cypress.Commands.add('measureAtViewport', (s, w, h) => { cy.log('mine'); });\n";
			Files.writeString(commands, original, StandardCharsets.UTF_8);

			CypressSupportScaffold.ensureMeasurementSupport(scriptsRoot);

			assertEquals(original, Files.readString(commands, StandardCharsets.UTF_8),
				"without the Servoy block marker the file is the user's own and must not be touched");
		}

		@Test
		@DisplayName("an unusable scripts root is logged and swallowed, never thrown")
		void unusableScriptsRootIsSwallowed(@TempDir Path dir) throws IOException
		{
			Path notADirectory = dir.resolve("file.txt");
			Files.writeString(notADirectory, "x", StandardCharsets.UTF_8);

			assertDoesNotThrow(() -> CypressSupportScaffold.ensureMeasurementSupport(notADirectory),
				"scaffolding is non-fatal: a failure must never block spec generation");
			assertFalse(Files.exists(notADirectory.resolve("cypress")), "nothing can be created below a regular file");
		}
	}
}
