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

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.WebComponent;

/**
 * SVY-21514: the spec generated for a form that extends another form must contain the inherited elements too, because
 * {@link FormSpecGenerator} reads the metadata from the flattened form. Needs the live model (editing flattened solution), so it is a PDE test.
 * <p>
 * Setup: form A has a1, a2, a3; form B extends A and has its own b1 plus an override of a2 (so a1 and a3 are only inherited).
 */
public class FormSpecGeneratorInheritanceIntegrationTest extends TestUtilitiesClass {
	private static final String TEST_SOLUTION = "test_formspec_inherit_suite";
	private static final String SERVOY_RESOURCES = "servoy_resources";
	private static final String FORM_A = "inheritBaseForm";
	private static final String FORM_B = "inheritChildForm";
	// a label, not a button: buttons are listed twice in the legacy spec (visibility block + "buttons are clickable")
	private static final String BUTTON = "bootstrapcomponents-label";

	private FormSpecGenerator gen;
	private String solutionName;

	public FormSpecGeneratorInheritanceIntegrationTest() {
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
		if (solution.getForm(FORM_B) == null) {
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();

			Form formA = solution.createNewForm(validator, null, FORM_A, null, true, new Dimension(640, 480));
			formA.createNewWebComponent("a1", BUTTON);
			WebComponent a2 = formA.createNewWebComponent("a2", BUTTON);
			formA.createNewWebComponent("a3", BUTTON);

			Form formB = solution.createNewForm(validator, null, FORM_B, null, true, new Dimension(640, 480));
			formB.setExtendsID(formA.getUUID().toString());
			formB.createNewWebComponent("b1", BUTTON);
			// override of a2: a child element that extends the parent element
			WebComponent a2Override = formB.createNewWebComponent("a2", BUTTON);
			a2Override.setExtendsID(a2.getUUID().toString());

			activeProject.saveEditingSolutionNodes(new IPersist[] { formA, formB }, true);
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
		for (String form : new String[] { FORM_A, FORM_B }) {
			Files.deleteIfExists(gen.getSpecFilePath(form, solutionName));
			Files.deleteIfExists(gen.getSetupFilePath(form, solutionName));
		}
	}

	private static int count(String text, String part) {
		int count = 0;
		for (int i = text.indexOf(part); i >= 0; i = text.indexOf(part, i + part.length())) {
			count++;
		}
		return count;
	}

	@Test
	public void testSpecOfChildFormContainsOwnOverriddenAndInheritedElements() throws Exception {
		String result = gen.generateSpec(FORM_B);

		Path spec = gen.getSpecFilePath(FORM_B, solutionName);
		assertTrue(Files.exists(spec), "Spec file must be written: " + result);
		String content = Files.readString(spec, StandardCharsets.UTF_8);

		assertEquals(1, count(content, "[data-cy=\"" + FORM_B + ".b1\"]"), "own element b1 must be in the spec once");
		assertEquals(1, count(content, "[data-cy=\"" + FORM_B + ".a2\"]"), "overridden element a2 must be in the spec exactly once (not twice)");
		assertEquals(1, count(content, "[data-cy=\"" + FORM_B + ".a1\"]"), "inherited element a1 (not mentioned in B) must be in the spec");
		assertEquals(1, count(content, "[data-cy=\"" + FORM_B + ".a3\"]"), "inherited element a3 (not mentioned in B) must be in the spec");
		assertTrue(result.contains("4 element assertions"), "the generator must report 4 element assertions: " + result);
	}

	@Test
	public void testSpecOfBaseFormIsNotAffectedByTheChild() throws Exception {
		gen.generateSpec(FORM_A);

		String content = Files.readString(gen.getSpecFilePath(FORM_A, solutionName), StandardCharsets.UTF_8);

		assertEquals(1, count(content, "[data-cy=\"" + FORM_A + ".a1\"]"));
		assertEquals(1, count(content, "[data-cy=\"" + FORM_A + ".a2\"]"));
		assertEquals(1, count(content, "[data-cy=\"" + FORM_A + ".a3\"]"));
		assertEquals(0, count(content, ".b1\""), "the base form must not get the child's element");
	}
}
