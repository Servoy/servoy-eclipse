/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.servoy.eclipse.cypress.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Shared scaffolding for the Cypress viewport-measurement support (SVY-21619).
 * <p>
 * Holds the single source of the <code>cy.measureAtViewport</code> / <code>cy.measureContainer</code> custom commands and the selector/marker literals
 * used by every spec generator, and knows how to <b>merge</b> those commands into <code>cypress/support/commands.js</code> idempotently - never
 * clobbering an existing file (which may already hold <code>cy.login()</code> from the auth path), and skipping when the commands are already present.
 * Both the form-spec path ({@link FormSpecGenerator}) and the E2E path (ServoyTestingServer) call this so the measure/settle/verify logic lives in
 * exactly one place.
 */
public final class CypressSupportScaffold
{
	/**
	 * Stable marker prefix that every generated spec logs via <code>cy.log</code> so {@code FormSpecRunner.hoistMeasurementMarkers} can extract the
	 * measured numbers. Kept here as the source of truth; {@code FormSpecRunner.SVY_MEASURE_MARKER} is the same literal and the generators in
	 * {@code com.servoy.eclipse.developer.mcp} reference the identical string (coupling documented on both sides).
	 */
	public static final String SVY_MEASURE_MARKER = "SVY_MEASURE ";

	/**
	 * Name of the Cypress task that carries a {@link #SVY_MEASURE_MARKER} line from the browser-side command to the Node process (SVY-21619). The
	 * task's Node handler {@code console.log}s the line - and <b>Node</b> stdout, unlike {@code cy.log}, IS printed by {@code cypress run} and therefore
	 * captured by the Java runner's merged-stdout scan in {@code FormSpecRunner.hoistMeasurementMarkers}. The marker format is unchanged; only the
	 * channel that gets it onto stdout changed. Must stay in sync with the task registered by {@link #setupNodeEventsBody()}.
	 */
	public static final String SVY_MEASURE_TASK_NAME = "svyMeasure";

	/**
	 * The form's rendered root container selector. {@code .svy-form} is the safe baseline (it is what the getFormLayout read keys off and is present in
	 * every rendered form); kept as a single named constant so it is easy to change if a more specific container is wanted.
	 */
	public static final String ROOT_CONTAINER_SELECTOR = ".svy-form";

	/**
	 * Default viewport height for a width-only {@code viewportWidths} entry. The emitters carry no height of their own, so this reuses the height the
	 * pipeline already uses - the {@code viewportHeight: 1080} written into the scaffolded {@code cypress.config.js} - rather than inventing a new
	 * value (SVY-21619 §8.3). Keep this in sync with that config scaffold.
	 */
	public static final int DEFAULT_VIEWPORT_HEIGHT = 1080;

	/** Idempotency sentinel: if this is already in commands.js, the measurement commands are considered present and are not re-added. */
	private static final String MEASURE_COMMAND_SENTINEL = "Cypress.Commands.add('measureAtViewport'";

	/**
	 * Begin marker of the Servoy-generated measurement block in commands.js (the first line of {@link #measureCommandsJs()}). Used to replace a STALE
	 * block in place (SVY-21619): a block emitted before the svyMeasure task existed still routes the marker through {@code cy.log}, which never reaches
	 * {@code cypress run} stdout, so it must be regenerated - without clobbering surrounding content such as a {@code cy.login()} command.
	 */
	private static final String MEASURE_BLOCK_BEGIN = "// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---";

	/** A stale block is one that holds the sentinel but does NOT route the marker through this task call - the pre-task cy.log-only form. */
	private static final String MEASURE_TASK_CALL = "cy.task('" + SVY_MEASURE_TASK_NAME + "'";

	private CypressSupportScaffold()
	{
	}

	/**
	 * The <code>cy.measureAtViewport</code> and <code>cy.measureContainer</code> custom command definitions as a self-contained JS block. The settle
	 * step polls the element's bounding box 3x 100ms apart and resolves only once the width is unchanged across the samples (no blind
	 * <code>cy.wait</code>), within Cypress's default command timeout; it fails with a clear message if the box never stabilizes.
	 */
	public static String measureCommandsJs()
	{
		// NOTE: the SVY_MEASURE marker literal below MUST stay in sync with SVY_MEASURE_MARKER above (and FormSpecRunner.SVY_MEASURE_MARKER).
		// SVY-21619: the marker is emitted through cy.task('svyMeasure', ...) whose Node handler console.logs it, because cypress run prints Node
		// stdout (which the Java runner captures) but NOT cy.log. The cy.log is kept as a harmless in-GUI breadcrumb only.
		return "// --- SVY-21619 viewport measurement commands (added by Servoy Copilot) ---\n" +
			"// Polls the element box until it is stable across 3 samples (no blind cy.wait), then emits a\n" +
			"// 'SVY_MEASURE ...' line (via the svyMeasure task -> Node console.log -> cypress run stdout) the\n" +
			"// Java runner hoists into a 'Measured layout' summary.\n" +
			"function svyStabilizeAndMeasure(selector) {\n" +
			"  const samples = [];\n" +
			"  const read = () => {\n" +
			"    const el = Cypress.$(selector)[0];\n" +
			"    if (!el) {\n" +
			"      throw new Error('measure: selector not found: ' + selector);\n" +
			"    }\n" +
			"    return el.getBoundingClientRect().width;\n" +
			"  };\n" +
			"  const poll = (remaining) => {\n" +
			"    const w = read();\n" +
			"    samples.push(w);\n" +
			"    const haveThree = samples.length >= 3;\n" +
			"    const stable = haveThree && samples[samples.length - 1] === samples[samples.length - 2] &&\n" +
			"      samples[samples.length - 2] === samples[samples.length - 3];\n" +
			"    if (stable) {\n" +
			"      return cy.wrap(w, { log: false });\n" +
			"    }\n" +
			"    if (remaining <= 0) {\n" +
			"      throw new Error('measure: container width never stabilized for ' + selector +\n" +
			"        ' (last samples: ' + samples.slice(-3).join(', ') + ')');\n" +
			"    }\n" +
			"    // 100ms between samples; this is a settle poll, not a fixed sleep for layout.\n" +
			"    return cy.wait(100, { log: false }).then(() => poll(remaining - 1));\n" +
			"  };\n" +
			"  // give enough polls to cover Cypress's default command timeout at 100ms spacing\n" +
			"  return poll(100);\n" +
			"}\n" +
			"\n" +
			"Cypress.Commands.add('measureAtViewport', (selector, width, height) => {\n" +
			"  cy.viewport(width, height);\n" +
			"  cy.window().then((win) => {\n" +
			"    const actual = win.innerWidth;\n" +
			"    if (actual !== width) {\n" +
			"      throw new Error('viewport not applied: requested width=' + width + ' but window.innerWidth=' + actual);\n" +
			"    }\n" +
			"  });\n" +
			"  return svyStabilizeAndMeasure(selector).then((cw) => {\n" +
			"    return cy.window().then((win) => {\n" +
			"      const innerWidth = win.innerWidth;\n" +
			"      const marker = '" + SVY_MEASURE_MARKER + "innerWidth=' + innerWidth + ' container=' + selector +\n" +
			"        ' containerWidth=' + cw + ' requested=' + width;\n" +
			"      // Route the marker to Node stdout (captured by the Java runner); cy.log alone is NOT printed by cypress run.\n" +
			"      cy.task('" + SVY_MEASURE_TASK_NAME + "', marker);\n" +
			"      cy.log(marker);\n" +
			"      return cy.wrap({ innerWidth: innerWidth, containerWidth: cw }, { log: false });\n" +
			"    });\n" +
			"  });\n" +
			"});\n" +
			"\n" +
			"// Measure-only variant: no viewport change and no innerWidth assertion; omits 'requested='.\n" +
			"Cypress.Commands.add('measureContainer', (selector) => {\n" +
			"  return svyStabilizeAndMeasure(selector).then((cw) => {\n" +
			"    return cy.window().then((win) => {\n" +
			"      const innerWidth = win.innerWidth;\n" +
			"      const marker = '" + SVY_MEASURE_MARKER + "innerWidth=' + innerWidth + ' container=' + selector +\n" +
			"        ' containerWidth=' + cw;\n" +
			"      // Route the marker to Node stdout (captured by the Java runner); cy.log alone is NOT printed by cypress run.\n" +
			"      cy.task('" + SVY_MEASURE_TASK_NAME + "', marker);\n" +
			"      cy.log(marker);\n" +
			"      return cy.wrap({ innerWidth: innerWidth, containerWidth: cw }, { log: false });\n" +
			"    });\n" +
			"  });\n" +
			"});\n";
	}

	/**
	 * The body to splice into a Cypress config's <code>setupNodeEvents(on, config) { ... }</code> so the {@link #SVY_MEASURE_TASK_NAME} task is
	 * registered (SVY-21619). The task's handler runs in the <b>Node</b> process and {@code console.log}s the received line verbatim on its own line -
	 * and Node stdout IS printed by {@code cypress run} and captured by the Java runner, which {@code cy.log} is not. The returned snippet ends by
	 * returning {@code config} (as Cypress expects from setupNodeEvents) and is indented for a two-space config block. The caller is responsible for
	 * wrapping it in the <code>setupNodeEvents(on, config) { ... }</code> declaration.
	 *
	 * @return the JS statements that register the svyMeasure task and return config
	 */
	public static String setupNodeEventsBody()
	{
		return "      on('task', {\n" +
			"        " + SVY_MEASURE_TASK_NAME + "(line) {\n" +
			"          // Node stdout is printed by `cypress run` and captured by the Java runner (cy.log is not).\n" +
			"          console.log(line);\n" +
			"          return null;\n" +
			"        },\n" +
			"      });\n" +
			"      return config;\n";
	}

	/**
	 * A full <code>setupNodeEvents(on, config) { ... }</code> declaration wrapping {@link #setupNodeEventsBody()}, indented to sit inside a two-space
	 * <code>e2e:</code> block of a generated <code>cypress.config.js</code>. Both config scaffolds (FormSpecRunner and ServoyTestingServer) emit this
	 * identical text so the svyMeasure task is registered the same way everywhere (SVY-21619).
	 *
	 * @return the setupNodeEvents declaration, ending in a newline
	 */
	public static String setupNodeEventsDeclaration()
	{
		return "    setupNodeEvents(on, config) {\n" + setupNodeEventsBody() + "    },\n";
	}

	/**
	 * Ensures <code>cypress/support/commands.js</code> and <code>e2e.js</code> exist under the given scripts root and that the measurement commands are
	 * present, merging them in idempotently:
	 * <ul>
	 * <li>creates <code>commands.js</code> (with the measurement commands) when missing,</li>
	 * <li>appends the measurement commands to an existing <code>commands.js</code> only when the sentinel is not already there - never overwriting an
	 * existing <code>cy.login()</code> or other content,</li>
	 * <li>replaces a STALE measurement block in place (SVY-21619): a block emitted before the svyMeasure task existed still routes the marker through
	 * {@code cy.log} only, which never reaches {@code cypress run} stdout - it is rewritten to the current task-based form, keeping any content before it
	 * (such as a {@code cy.login()} command) intact,</li>
	 * <li>creates <code>e2e.js</code> (<code>import './commands';</code>) when missing.</li>
	 * </ul>
	 * Non-fatal: failures are logged and swallowed so generation is never blocked by scaffolding.
	 *
	 * @param scriptsRoot the e2e-test-scripts root (the folder that contains the <code>cypress</code> directory)
	 */
	public static void ensureMeasurementSupport(Path scriptsRoot)
	{
		try
		{
			Path supportDir = scriptsRoot.resolve("cypress").resolve("support");
			Files.createDirectories(supportDir);

			Path commandsFile = supportDir.resolve("commands.js");
			if (!Files.exists(commandsFile))
			{
				Files.writeString(commandsFile,
					"// Add custom Cypress commands here.\n// See: https://docs.cypress.io/api/cypress-api/custom-commands\n\n" + measureCommandsJs(),
					StandardCharsets.UTF_8);
			}
			else
			{
				String existing = Files.readString(commandsFile, StandardCharsets.UTF_8);
				if (!existing.contains(MEASURE_COMMAND_SENTINEL))
				{
					StringBuilder merged = new StringBuilder(existing);
					if (!existing.endsWith("\n"))
					{
						merged.append("\n");
					}
					merged.append("\n").append(measureCommandsJs());
					Files.writeString(commandsFile, merged.toString(), StandardCharsets.UTF_8);
				}
				else if (!existing.contains(MEASURE_TASK_CALL) && existing.contains(MEASURE_BLOCK_BEGIN))
				{
					// SVY-21619: the measurement block is present but STALE - it logs the SVY_MEASURE marker via cy.log
					// only, which never reaches `cypress run` stdout. Replace the Servoy-generated block (from its begin
					// marker to the end of the file, where it is always appended) with the current task-based block,
					// keeping any content before it (e.g. a cy.login() command) untouched.
					int begin = existing.indexOf(MEASURE_BLOCK_BEGIN);
					String prefix = existing.substring(0, begin);
					if (!prefix.isEmpty() && !prefix.endsWith("\n"))
					{
						prefix = prefix + "\n";
					}
					Files.writeString(commandsFile, prefix + measureCommandsJs(), StandardCharsets.UTF_8);
				}
			}

			Path e2eJsFile = supportDir.resolve("e2e.js");
			if (!Files.exists(e2eJsFile))
			{
				Files.writeString(e2eJsFile, "import './commands';\n", StandardCharsets.UTF_8);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError("Could not scaffold Cypress measurement support in " + scriptsRoot, e);
		}
	}
}
