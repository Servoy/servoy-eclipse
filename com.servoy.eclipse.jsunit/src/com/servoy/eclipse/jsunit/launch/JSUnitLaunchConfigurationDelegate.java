/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.jsunit.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.dltk.launching.ScriptLaunchConfigurationConstants;
import org.eclipse.dltk.testing.DLTKTestingConstants;
import org.eclipse.dltk.testing.DLTKTestingPlugin;
import org.eclipse.dltk.testing.ITestingEngine;
import org.eclipse.dltk.testing.TestingEngineManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.jsunit.scriptunit.JSUnitTestingEngine;
import com.servoy.eclipse.jsunit.smart.RunClientTests;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;

/**
 * TestTargets are stored in launch configurations.
 * Launches jsunit test configurations . The test configuration is stored in {@link JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE}
 * attribute as a string . TestTarget.fromString(str) deserializes the configuration.
 *
 * @author obuligan
 *
 */
public class JSUnitLaunchConfigurationDelegate extends LaunchConfigurationDelegate
{
	public static final String LAUNCH_CONFIGURATION_TYPE_ID = "com.servoy.eclipse.jsunit.launch";
	public static final String LAUNCH_CONFIG_INSTANCE = "com.servoy.eclipse.jsunit.launch.configurationInstance";
	public static final int MAX_CONFIG_INSTANCES = 10;
	private static ILaunch currentLaunch = null;

	private static int counter = 1;

	public static ILaunch getCurrentLaunch()
	{
		return currentLaunch;
	}

	// mostly preparations needed for DLTK testing view to work as expected
	public static TestTarget prepareForLaunch(ILaunchConfiguration configuration, ILaunch launch) throws CoreException
	{
		currentLaunch = launch;

		DLTKTestingPlugin.getModel().start();
		final ITestingEngine engine = getTestingEngine(configuration);
		//we don't run servoy js as an interpreter in eclipse context so first parameter  == null
		engine.configureLaunch(null, configuration, launch);
		/* DO THE ACTUAL EXECUTION */
		String testTargetStr = configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE,
			TestTarget.activeProjectTarget().convertToString());

		TestTarget target = TestTarget.convertFromString(testTargetStr);

		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		Solution actualActiveSolution = activeProject != null ? activeProject.getSolution() : null;
		final String actualActiveSolutionName = actualActiveSolution != null ? actualActiveSolution.getName() : null;
		if (!target.getActiveSolution().getName().equals(actualActiveSolutionName))
		{
			final TestTarget tt = target;
			UIUtils.runInUI(new Runnable()
			{

				@Override
				public void run()
				{
					Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					UIUtils.showInformation(shell, "Cannot run target", "The currently launched configuration expects the active solution to be : " +
						tt.getActiveSolution().getName() + " instead of " + actualActiveSolutionName + "\n Running current active solution");
				}

			}, false);
			target = TestTarget.activeProjectTarget();
		}

		return target;
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		TestTarget testTarget = prepareForLaunch(configuration, launch);

		new RunClientTests(testTarget, launch, monitor, !"run".equals(mode)).run();
	}

	public static void prepareLaunchConfigForTesting(ILaunchConfigurationWorkingCopy workingCopy)
	{
		workingCopy.setAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, JSUnitTestingEngine.ENGINE_ID);
		workingCopy.setAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, workingCopy.getName());
	}

	public static String generateLaunchConfigName(TestTarget target)
	{
		String ret = "";
		String solutionName = target.getActiveSolution().getName();
		String moduleToTest = target.getModuleToTest() == null ? "" : target.getModuleToTest().getName();

		if (solutionName.equals(moduleToTest))
		{// whole active solution target
			ret = solutionName;
		}
		else if (target.getModuleToTest() != null)
		{
			ret = solutionName + " Module=" + moduleToTest;
		}
		else if (target.getFormToTest() != null)
		{
			ret = solutionName + " Form=" + target.getFormToTest().getName();
		}
		else if (target.getGlobalScopeToTest() != null)
		{
			if (solutionName.equals(target.getGlobalScopeToTest().getLeft().getName()))
			{
				ret = target.getGlobalScopeToTest().getLeft().getName() + " Scope=" + target.getGlobalScopeToTest().getRight();
			}
			else
			{
				ret = solutionName + " Module=" + target.getGlobalScopeToTest().getLeft().getName() + " Scope=" + target.getGlobalScopeToTest().getRight();
			}
		}
		else if (target.getTestMethodToTest() != null)
		{
			ScriptMethod met = target.getTestMethodToTest();
			if (met.getAncestor(IRepository.FORMS) instanceof Form)
			{
				Form form = (Form)met.getAncestor(IRepository.FORMS);
				ret = solutionName + " Form=" + form.getName() + " func=" + target.getTestMethodToTest().getName();
			}
			else
			{
				ret = solutionName + " Scope=" + met.getScopeName() + " func=" + target.getTestMethodToTest().getName();
			}
		}
		return ret.equals("") ? "Dummmy Solution Name" : ret;
	}

	private static ITestingEngine getTestingEngine(ILaunchConfiguration configuration) throws CoreException
	{
		return TestingEngineManager.getEngine(configuration.getAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, (String)null));
	}

}
