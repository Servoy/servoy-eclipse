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
package com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.eclipse.ngclient.ui.CopySourceFolderAction;
import com.servoy.eclipse.ngclient.ui.NodeFolderCreatorJob;

/**
 * Base class for integration tests that activate Servoy solutions.
 * <p>
 * Uses JUnit 5 {@code @BeforeAll}/{@code @AfterAll}. The lifecycle methods must
 * remain {@code static} and {@code public} — {@code AbstractIntegrationTestBaseTest}
 * relies on calling them directly.
 * </p>
 * <p>
 * Disables {@link NodeFolderCreatorJob} before the test class runs and restores
 * the default (enabled) state in teardown. Test classes that genuinely need the
 * node copy (e.g. Cypress form-testing integration tests) should override
 * {@code disableNodeFolderCreatorJob()} and call
 * {@link NodeFolderCreatorJob#setDisabled(boolean) setDisabled(false)} instead.
 * </p>
 * 
 * NOTE: this class is copied/duplicated into other repos. with name AbstractTitaniumIntegrationTestCopy; it is duplicated in order to avoid cross repo
 * dependencies on test projects which would require more configuration for Jenkins (one more feature, update site etc.).
 * Keep this original and all the copies in sync pls.
 */
public abstract class AbstractTitaniumIntegrationTest extends TestUtilitiesClass {
	
	private static boolean titaniumBuildAlreadyTriggeredForThisClass = false;

	public AbstractTitaniumIntegrationTest(String testSolutionName, String servoyResourcesProjectName) {
		super(testSolutionName, servoyResourcesProjectName);
	}

	/**
	 * Disables the node folder copy/npm cycle before any test in this class runs.
	 * Override in subclasses that require the node folder to be set up.
	 */
	@BeforeAll
	public static void adjustTitaniumBuildJobEnablementForThisClass() throws Exception {
		Activator.setNodeExtractionAndTitaniumBuildDisabled(true);
	}

	/**
	 * Restores the default (jobs disabled) state after all tests in this class have run,
	 * so subsequent test classes are unaffected.
	 */
	@AfterAll
	public static void restoreTitaniumBuildJobEnablementToDefault() {
		Activator.setNodeExtractionAndTitaniumBuildDisabled(true);
		waitForTitaniumuildJobs(); // just to make sure we are not in the middle of a titanium build when continuing with next test class
	}
	
	@Override
	public void ensureActiveProject() throws Exception
	{
		IDeveloperServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();

		ServoyProject active = model.getActiveProject();
		boolean wasActive = (active != null && testSolutionName.equals(active.getProject().getName()));
			
		super.ensureActiveProject();
		
		if (!Activator.isNodeExtractionAndTitaniumBuildDisabled())
		{
			if (wasActive && !titaniumBuildAlreadyTriggeredForThisClass) {
				// if the solution happened to be already active
				// on the first setUp that calls ensureActiveProject (titaniumBuildAlreadyTriggeredForThisClass)
				// then do trigger a check for a titanium build - as if previously running testclasses did no build it
				// and Activator.isNodeExtractionAndTitaniumBuildDisabled() used to be false, it might not have been built at all
				buildTitaniumIfNeeded();
			} else waitForTitaniumuildJobs();
			  // else it was already built once for this class or it will be built now automatically by
			  // the solution activation; when all build jobs are enabled while current class is running tests it should auto-build titanium if needed from now on
			titaniumBuildAlreadyTriggeredForThisClass = true;
		}
	}
	
	protected void buildTitaniumIfNeeded() {
		long x = System.currentTimeMillis();
		System.out.println("*** " + this.getClass().getName() + " buildTitaniumOnce starting");

		CopySourceFolderAction.startTitaniumNGBuild(CopySourceFolderAction.NORMAL_BUILD);
		
		System.out.println("*** buildTitaniumOnce took: " + String.format( "%.2f", ((System.currentTimeMillis() - x) / 1000d)) + " s");
	
		waitForTitaniumuildJobs();
		waitForWorkspaceBuildJobs();
	}

}
