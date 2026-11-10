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
package com.servoy.eclipse.ui.tests.integrationpde.quickfix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.base.persistence.constants.IPartConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.quickfix.ClearFormCssPositionQuickFix;
import com.servoy.eclipse.core.quickfix.ServoyQuickFixGenerator;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyBuilderUtils;
import com.servoy.eclipse.model.builder.ServoyFormBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.UUID;

/**
 * SVY-21700: PDE integration tests for the "Form ... uses CSS position layout
 * but has no body part" marker quick-fix.
 * <p>
 * The three behaviours under test all need a live workspace, OSGi container and
 * {@code ServoyModel} (a form is created in and resolved from a
 * {@code ServoyProject} editing solution, the project builder's
 * {@code ServoyFormBuilder.addFormMarkers} is run against it, and the quick-fix
 * saves the mutated form back through {@code saveEditingSolutionNodes}), so
 * this is an {@code integrationpde} test, not a plain one.
 * <ul>
 * <li>{@link #builderMarkerCarriesFormUuidAndSolutionName()} guards the builder
 * change in {@code ServoyFormBuilder.addFormMarkers} (SVY-21700 §3.1): the
 * css-no-body marker now carries a {@code Uuid} attribute equal to the form's
 * UUID plus the existing {@code SolutionName}, and the
 * {@code cssPositionNoBodyPart} flag attribute used to discriminate it.</li>
 * <li>{@link #quickFixResolvesAndClearsUseCssPosition()} guards the generator
 * branch and the quick-fix class (§3.2-§3.3): exactly one
 * {@link ClearFormCssPositionQuickFix} is offered and running it clears
 * {@code useCssPosition} on the exact flagged form and saves the change.</li>
 * <li>{@link #quickFixNotOfferedForUnrelatedFormProblemMarker()} guards the
 * flag-based discrimination (§3.3): another {@code formProblem}-type marker
 * that does not carry the {@code cssPositionNoBodyPart} flag is offered no
 * {@code ClearFormCssPositionQuickFix}.</li>
 * </ul>
 */
public class ClearFormCssPositionQuickFixIntegrationTest extends TestUtilitiesClass {
	private static final String TEST_SOLUTION = "test_svy21700_quickfix";
	private static final String SERVOY_RESOURCES = "servoy_resources_svy21700";
	private static final String MESSAGE_DUPLICATE_PART = "has multiple parts";

	public ClearFormCssPositionQuickFixIntegrationTest() {
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
	}

	private ServoyProject activeProject() {
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		assertNotNull(activeProject, "Active project required");
		return activeProject;
	}

	/**
	 * Creates a non-responsive, {@code useCssPosition==true}, body-less form in the
	 * editing solution, saves it, runs {@code ServoyFormBuilder.addFormMarkers}
	 * against it (the exact call the project builder makes) and returns the single
	 * css-no-body marker it raised.
	 */
	private IMarker createBodylessCssPositionFormAndBuildMarker(String formName) throws Exception {
		ServoyProject activeProject = activeProject();
		Solution editingSolution = activeProject.getEditingSolution();

		Form form = editingSolution.getForm(formName);
		if (form == null) {
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			form = editingSolution.createNewForm(validator, null, formName, null, true, new Dimension(640, 480));
		}
		// a freshly created form has no parts (no body); make it an explicit
		// non-responsive css-position form
		form.setResponsiveLayout(false);
		form.setUseCssPosition(Boolean.TRUE);
		activeProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
		waitForWorkspaceBuildJobs();

		assertFalse(form.isResponsiveLayout(), "Test form must be non-responsive to trigger the marker");
		assertTrue(Boolean.TRUE.equals(form.getUseCssPosition()),
				"Test form must have useCssPosition==true to trigger the marker");
		assertFalse(form.hasPart(IPartConstants.BODY), "Test form must have no body part to trigger the marker");

		// run exactly the builder step that raises the marker
		ServoyFormBuilder.deleteMarkers(form);
		ServoyFormBuilder.addFormMarkers(activeProject, form, new HashSet<UUID>(), new HashMap<Form, Boolean>());
		waitForWorkspaceBuildJobs();

		IResource markerResource = ServoyBuilderUtils.getPersistResource(form);
		assertTrue(markerResource.exists(), "Form resource must exist so markers can be read from it");

		IMarker[] markers = markerResource.findMarkers(ServoyBuilder.PROJECT_FORM_MARKER_TYPE, true,
				IResource.DEPTH_INFINITE);
		IMarker cssNoBodyMarker = null;
		for (IMarker marker : markers) {
			if (marker.getAttribute(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, false)) {
				assertNotNull(form.getName());
				cssNoBodyMarker = marker;
				break;
			}
		}
		assertNotNull(cssNoBodyMarker,
				"The css-position-no-body-part marker must be raised for a non-responsive, useCssPosition, body-less form");
		return cssNoBodyMarker;
	}

	@Test
	@DisplayName("the css-no-body marker carries the form UUID, solution name and discriminator flag (SVY-21700 §3.1)")
	public void builderMarkerCarriesFormUuidAndSolutionName() throws Exception {
		String formName = "svy21700_uuidForm";
		IMarker marker = createBodylessCssPositionFormAndBuildMarker(formName);

		Form form = activeProject().getEditingSolution().getForm(formName);
		assertNotNull(form, "Form must be resolvable from the editing solution");

		assertEquals(form.getUUID().toString(), marker.getAttribute("Uuid", null),
				"The css-no-body marker's Uuid attribute must equal the flagged form's UUID");
		assertEquals(activeProject().getSolution().getName(), marker.getAttribute("SolutionName", null),
				"The css-no-body marker must keep its SolutionName attribute");
		assertTrue(marker.getAttribute(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, false),
				"The css-no-body marker must carry the cssPositionNoBodyPart flag used to offer the quick-fix");
	}

	@Test
	@DisplayName("the generator offers exactly one ClearFormCssPositionQuickFix and running it clears and saves useCssPosition (SVY-21700 §3.2-§3.3)")
	public void quickFixResolvesAndClearsUseCssPosition() throws Exception {
		String formName = "svy21700_clearForm";
		IMarker marker = createBodylessCssPositionFormAndBuildMarker(formName);

		org.eclipse.ui.IMarkerResolution[] resolutions = new ServoyQuickFixGenerator().getResolutions(marker);
		assertNotNull(resolutions, "getResolutions must not return null for the css-no-body marker");

		int clearFixCount = 0;
		ClearFormCssPositionQuickFix clearFix = null;
		for (org.eclipse.ui.IMarkerResolution resolution : resolutions) {
			if (resolution instanceof ClearFormCssPositionQuickFix) {
				clearFixCount++;
				clearFix = (ClearFormCssPositionQuickFix) resolution;
			}
		}
		assertEquals(1, clearFixCount,
				"Exactly one ClearFormCssPositionQuickFix must be offered for the css-no-body marker");

		// precondition: the flag is set before the fix runs
		Form before = activeProject().getEditingSolution().getForm(formName);
		assertTrue(Boolean.TRUE.equals(before.getUseCssPosition()),
				"useCssPosition must be true before the quick-fix runs");

		clearFix.run(marker);
		waitForWorkspaceBuildJobs();

		// re-resolve the form from the editing solution by UUID (as the quick-fix does)
		// to confirm the saved state
		UUID formUuid = before.getUUID();
		IPersist resolved = AbstractRepository.searchPersist(activeProject().getEditingSolution(), formUuid);
		assertTrue(resolved instanceof Form, "The flagged persist must still resolve to a Form after the fix");
		Form after = (Form) resolved;

		assertFalse(Boolean.TRUE.equals(after.getUseCssPosition()),
				"After the quick-fix the form's useCssPosition must be cleared (not TRUE)");
		assertFalse(after.isResponsiveLayout(),
				"The quick-fix must not change responsiveLayout (it clears useCssPosition only)");
	}

	@Test
	@DisplayName("the generator offers no ClearFormCssPositionQuickFix for an unrelated formProblem marker (SVY-21700 §3.3 discrimination)")
	public void quickFixNotOfferedForUnrelatedFormProblemMarker() throws Exception {
		ServoyProject activeProject = activeProject();
		Solution editingSolution = activeProject.getEditingSolution();

		String formName = "svy21700_unrelatedForm";
		Form form = editingSolution.getForm(formName);
		if (form == null) {
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			form = editingSolution.createNewForm(validator, null, formName, null, true, new Dimension(640, 480));
			activeProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
			waitForWorkspaceBuildJobs();
		}

		// synthesize a PROJECT_FORM_MARKER_TYPE marker that is NOT the css-no-body one:
		// same shared type and the same Uuid/SolutionName attributes, but WITHOUT the
		// cssPositionNoBodyPart flag (that flag is the discriminator the quick-fix
		// uses).
		IResource markerResource = ServoyBuilderUtils.getPersistResource(form);
		assertTrue(markerResource.exists(), "Form resource must exist so a marker can be created on it");

		IMarker unrelatedMarker = markerResource.createMarker(ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
		unrelatedMarker.setAttribute(IMarker.MESSAGE, "Form \"" + formName + "\" " + MESSAGE_DUPLICATE_PART + ".");
		unrelatedMarker.setAttribute("Uuid", form.getUUID().toString());
		unrelatedMarker.setAttribute("SolutionName", activeProject.getSolution().getName());

		// sanity: the unrelated marker does NOT carry the css-no-body flag
		assertFalse(unrelatedMarker.getAttribute(ServoyBuilder.FORM_CSS_POSITION_NO_BODY_PART_ATTRIBUTE, false),
				"Test setup error: the unrelated marker must not carry the cssPositionNoBodyPart flag");

		org.eclipse.ui.IMarkerResolution[] resolutions = new ServoyQuickFixGenerator().getResolutions(unrelatedMarker);
		assertNotNull(resolutions, "getResolutions must not return null");

		boolean offersClearFix = Arrays.stream(resolutions).anyMatch(r -> r instanceof ClearFormCssPositionQuickFix);
		assertFalse(offersClearFix,
				"No ClearFormCssPositionQuickFix must be offered for an unrelated formProblem marker");

		unrelatedMarker.delete();
	}
}
