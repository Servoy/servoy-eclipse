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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.model.nature.ServoyProject;

/**
 * Integration tests for the solution-prefixed {@link FormSpecGenerator}
 * overloads ({@code getSpecFilePath(formName, solutionName)} and
 * {@code getSetupFilePath(formName, solutionName)}).
 * <p>
 * These methods call into {@code ResourcesPlugin.getWorkspace()} and need a
 * real Servoy solution active in the workspace, so they must run as a PDE
 * plug-in test rather than plain JUnit (see
 * {@code FormSpecGeneratorSolutionPrefixTest} in the {@code plainjunit}
 * package for the method-shape checks that don't need a workspace).
 */
public class FormSpecGeneratorSolutionPrefixIntegrationTest extends TestUtilitiesClass {
	private static final String TEST_SOLUTION = "test_formspec_prefix_suite";
	private static final String SERVOY_RESOURCES = "servoy_resources";

	private FormSpecGenerator gen;
	private String solutionName;

	public FormSpecGeneratorSolutionPrefixIntegrationTest() {
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

		gen = new FormSpecGenerator();
	}

	@Test
	public void testGetSpecFilePathSolutionPrefix() {
		Path specPath = gen.getSpecFilePath("myForm", solutionName);
		assertNotNull(specPath, "getSpecFilePath(formName, solutionName) must not return null with an active solution");
		String fileName = specPath.getFileName().toString();
		assertEquals(solutionName + ".myForm.spec.cy.js", fileName, "Spec file must be solution-prefixed");
	}

	@Test
	public void testGetSetupFilePathSolutionPrefix() {
		Path setupPath = gen.getSetupFilePath("myForm", solutionName);
		assertNotNull(setupPath, "getSetupFilePath(formName, solutionName) must not return null with an active solution");
		String fileName = setupPath.getFileName().toString();
		assertEquals(solutionName + ".myForm.spec.js", fileName, "Setup file must be solution-prefixed");
	}

	@Test
	public void testFindExistingSpecFileReturnsNullWhenNothingExists() {
		Path result = gen.findExistingSpecFile("nonExistentForm", "nonExistentSolution");
		assertNull(result, "findExistingSpecFile must return null when no file exists");
	}

	@Test
	public void testFindExistingSetupFileReturnsNullWhenNothingExists() {
		Path result = gen.findExistingSetupFile("nonExistentForm", "nonExistentSolution");
		assertNull(result, "findExistingSetupFile must return null when no file exists");
	}
}
