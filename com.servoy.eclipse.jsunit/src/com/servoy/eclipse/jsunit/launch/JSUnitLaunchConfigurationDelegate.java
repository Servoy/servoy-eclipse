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
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.dltk.testing.DLTKTestingConstants;
import org.eclipse.dltk.testing.DLTKTestingPlugin;
import org.eclipse.dltk.testing.ITestingEngine;
import org.eclipse.dltk.testing.TestingEngineManager;

import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.jsunit.scriptunit.RunJSUnitTests;

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
	private static ILaunch currentLaunch = null;

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

	private ITestingEngine getTestingEngine(ILaunchConfiguration configuration) throws CoreException
	{
		return TestingEngineManager.getEngine(configuration.getAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, (String)null));
	}


}
