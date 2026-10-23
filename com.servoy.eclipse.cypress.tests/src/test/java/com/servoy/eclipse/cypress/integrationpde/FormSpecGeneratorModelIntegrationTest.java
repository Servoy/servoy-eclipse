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
package com.servoy.eclipse.cypress.integrationpde;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.cypress.services.FormSpecModel;
import com.servoy.eclipse.cypress.services.FormSpecModel.Effect;
import com.servoy.eclipse.cypress.services.FormSpecModel.EffectKind;
import com.servoy.eclipse.cypress.services.FormSpecModel.Element;
import com.servoy.eclipse.cypress.services.FormSpecModel.Gesture;
import com.servoy.eclipse.cypress.services.FormSpecModel.Handler;
import com.servoy.eclipse.cypress.services.FormSpecModel.Kind;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Solution;

/**
 * Integration tests for {@link FormSpecGenerator#generateSpec(String, FormSpecModel)} (SVY-21517).
 * <p>
 * The model path needs the active solution (it reads the form from the active solution, resolves the spec directory under the workspace root and uses the
 * solution name in the file name), so it must run as a PDE plug-in test. The pure emitter and the model-name mismatch check are covered by the
 * {@code plainjunit} tests {@code FormSpecModelEmitterTest} and {@code FormSpecGeneratorModelTest}.
 */
public class FormSpecGeneratorModelIntegrationTest extends TestUtilitiesClass {
	private static final String TEST_SOLUTION = "test_formspec_model_suite";
	private static final String SERVOY_RESOURCES = "servoy_resources";
	private static final String FORM = "modelSpecForm";

	private FormSpecGenerator gen;
	private String solutionName;

	public FormSpecGeneratorModelIntegrationTest() {
		super(TEST_SOLUTION, SERVOY_RESOURCES);
	}

	@BeforeAll
	public static void deleteProjectsBeforeClass() throws Exception {
		deleteProjects(TEST_SOLUTION, SERVOY_RESOURCES);
		waitForWorkspaceBuildJobs();
	}

	@BeforeEach
	public void setUp() throws Exception {
		assertNotNull(Display.getDefault(), "No Display available - test requires a running Eclipse UI");

		ensureTestSolutionInWorkspace(null, null);
		ensureActiveProject();

		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		assertNotNull(activeProject, "Active project required");
		solutionName = activeProject.getSolution().getName();

		Solution solution = activeProject.getEditingSolution();
		if (solution.getForm(FORM) == null) {
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			Form form = solution.createNewForm(validator, null, FORM, null, true, new Dimension(640, 480));
			activeProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
			waitForWorkspaceBuildJobs();
		}

		gen = new FormSpecGenerator();
		deleteSpecFiles();
	}

	@AfterEach
	public void tearDown() throws Exception {
		deleteSpecFiles();
	}

	private void deleteSpecFiles() throws Exception {
		Files.deleteIfExists(gen.getSpecFilePath(FORM, solutionName));
		Files.deleteIfExists(gen.getSetupFilePath(FORM, solutionName));
	}

	private static FormSpecModel model() {
		Handler click = new Handler("onActionMethodID", "hide", "forms." + FORM, Gesture.CLICK, false, false,
				List.of(new Effect(EffectKind.VISIBLE, "lbl", "false"), new Effect(EffectKind.OFF_FORM, null, "scopes.x.y()")));
		return new FormSpecModel(FORM, List.of(new Element("btn", "bootstrapcomponents-button", Kind.CONTROL, false, List.of(click)),
				new Element("lbl", "bootstrapcomponents-label", Kind.STATIC, false, List.of())));
	}

	private String expectedSpec() {
		return String.join("\n", "describe('" + FORM + "', () => {", "", "  beforeEach(() => {",
				"    cy.visit('solution/" + solutionName + "/index.html?formpreview=" + FORM + "&svy_testmode=true');",
				"    cy.get('[data-cy^=\"" + FORM + ".\"]', { timeout: 30000 }).should('exist');", "  });", "",
				"  it('loads without errors and all elements are visible', () => {",
				"    cy.get('.svy-error, .error-overlay').should('not.exist');",
				"    cy.get('[data-cy=\"" + FORM + ".btn\"]').should('be.visible');", "  });", "",
				"  // btn.onActionMethodID -> forms." + FORM + ".hide", "  it('btn: onActionMethodID (hide)', () => {",
				"    cy.get('[data-cy=\"" + FORM + ".btn\"]').click();",
				"    cy.get('[data-cy=\"" + FORM + ".lbl\"]').should('not.be.visible');",
				"    // OFF-FORM (not asserted): scopes.x.y()", "    cy.get('.svy-error, .error-overlay').should('not.exist');", "  });", "",
				"});") + "\n";
	}

	@Test
	public void testGenerateSpecWithModelWritesEmitterOutputToSolutionPrefixedFile() throws Exception {
		String result = gen.generateSpec(FORM, model());

		Path cySpec = gen.getSpecFilePath(FORM, solutionName);
		assertNotNull(cySpec, "Spec path must not be null with an active solution");
		assertEquals(solutionName + "." + FORM + ".spec.cy.js", cySpec.getFileName().toString(), "Spec file must be solution-prefixed");
		assertTrue(Files.exists(cySpec), "Spec file must be written: " + result);
		assertEquals(expectedSpec(), Files.readString(cySpec, StandardCharsets.UTF_8), "Spec file must equal the emitter output for the model");
	}

	@Test
	public void testGenerateSpecWithModelReportsModelCountsAndCreatedPrefix() throws Exception {
		String result = gen.generateSpec(FORM, model());

		String expectedFirstLine = "Created: jenkins-custom/e2e-test-scripts/cypress/cy-form/" + solutionName + "." + FORM
				+ ".spec.cy.js (1 element visibility assertions, 1 handler cases)";
		assertEquals(expectedFirstLine, result.lines().findFirst().orElse(null), "First result line must report the model counts");
	}

	@Test
	public void testGenerateSpecWithModelWritesSetupFileToo() throws Exception {
		gen.generateSpec(FORM, model());

		Path setup = gen.getSetupFilePath(FORM, solutionName);
		assertTrue(Files.exists(setup), "The .spec.js setup file must also be created in the model path");
		assertTrue(Files.readString(setup, StandardCharsets.UTF_8).contains("function spec_setUp()"), "Setup file must contain spec_setUp");
	}

	@Test
	public void testGenerateSpecWithModelNeverOverwritesExistingSpec() throws Exception {
		gen.generateSpec(FORM, model());
		Path cySpec = gen.getSpecFilePath(FORM, solutionName);
		String before = Files.readString(cySpec, StandardCharsets.UTF_8);

		FormSpecModel other = new FormSpecModel(FORM, List.of());
		String second = gen.generateSpec(FORM, other);

		assertEquals("Spec files already exist: jenkins-custom/e2e-test-scripts/cypress/cy-form/" + solutionName + "." + FORM
				+ ".spec.cy.js and jenkins-custom/e2e-test-scripts/cypress/cy-form-spec/" + solutionName + "." + FORM + ".spec.js", second);
		assertEquals(before, Files.readString(cySpec, StandardCharsets.UTF_8), "Existing spec must be left untouched");
	}

	@Test
	public void testGenerateSpecWithModelForUnknownFormReturnsFormNotFound() {
		String result = gen.generateSpec("noSuchForm_xyz", new FormSpecModel("noSuchForm_xyz", List.of()));

		assertEquals("Error: Form not found: noSuchForm_xyz", result);
	}
}
