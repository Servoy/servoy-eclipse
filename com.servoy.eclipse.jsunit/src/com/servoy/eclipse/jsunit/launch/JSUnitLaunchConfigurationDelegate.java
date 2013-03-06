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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.dltk.launching.ScriptLaunchConfigurationConstants;
import org.eclipse.dltk.testing.DLTKTestingConstants;
import org.eclipse.dltk.testing.DLTKTestingPlugin;
import org.eclipse.dltk.testing.ITestingEngine;
import org.eclipse.dltk.testing.TestingEngineManager;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.jsunit.scriptunit.JSUnitTestingEngine;
import com.servoy.eclipse.jsunit.scriptunit.RunJSUnitTests;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;

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


	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException
	{
		currentLaunch = launch;
		DLTKTestingPlugin.getModel().start();
		final ITestingEngine engine = getTestingEngine(configuration);
		//we don't run servoy js as an interpreter in eclipse context so first parameter  == null 
		engine.configureLaunch(null, configuration, launch);
		/* DO THE ACTUAL EXECUTION */
		String testTargetStr = configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, (String)null);
		TestTarget testTarget = TestTarget.fromString(testTargetStr);
		new RunJSUnitTests(testTarget, launch).run();
	}

	public static void launchTestTarget(TestTarget target)
	{
		if (target == null)
		{
			// use currently active solution
			target = new TestTarget(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution());
		}
		try
		{
			ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType type = manager.getLaunchConfigurationType(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIGURATION_TYPE_ID);
			ILaunchConfiguration[] configurations;
			configurations = manager.getLaunchConfigurations(type);
			int count = 0;
			for (ILaunchConfiguration configuration : configurations)
			{
				//was this target already launched before? if yes reuse the launch configuration
				if (target.toString().equals(configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, "")))
				{
					DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
					return;
				}
				count++;
				if (count > JSUnitLaunchConfigurationDelegate.MAX_CONFIG_INSTANCES) configuration.delete();
			}

			//create a launch configuration copy 
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, generateLaunchConfigName(target));
			workingCopy.setAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, JSUnitTestingEngine.ENGINE_ID);

			workingCopy.setAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, generateLaunchConfigName(target));
			workingCopy.setAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, target.toString());
			ILaunchConfiguration configuration = workingCopy.doSave();
			//this call will end up in launch method of ServoyJsLaunchConfigurationDelegate
			DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
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

	private ITestingEngine getTestingEngine(ILaunchConfiguration configuration) throws CoreException
	{
		return TestingEngineManager.getEngine(configuration.getAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, (String)null));
	}


}
