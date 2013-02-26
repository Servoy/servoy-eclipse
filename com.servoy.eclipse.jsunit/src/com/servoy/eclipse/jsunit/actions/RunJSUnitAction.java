/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.jsunit.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.dltk.launching.ScriptLaunchConfigurationConstants;
import org.eclipse.dltk.testing.DLTKTestingConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.jsunit.SolutionUnitTestTarget;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.jsunit.scriptunit.JSUnitTestingEngine;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This action launches a script unit test run from a selected node in the solex view.
 * @author obuligan
 */
public class RunJSUnitAction implements IObjectActionDelegate
{

	private IStructuredSelection structuredSelection;
	private static int counter = 1;

	public void run(IAction action)
	{
		launchScriptUnitTests();
	}

	private void launchScriptUnitTests()
	{
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
				if (getTestTarget().toString().equals(configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, "")))
				{
					DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);
					return;
				}
				count++;
				if (count > JSUnitLaunchConfigurationDelegate.MAX_CONFIG_INSTANCES) configuration.delete();
			}
			//create a launch configuration copy  with jsunit.js from berilos
			ILaunchConfigurationWorkingCopy workingCopy = type.newInstance(null, "JSunit tests run " + counter++);
			workingCopy.setAttribute(DLTKTestingConstants.ATTR_ENGINE_ID, JSUnitTestingEngine.ENGINE_ID);
			workingCopy.setAttribute(ScriptLaunchConfigurationConstants.ATTR_PROJECT_NAME, "Dummy Solution name");
			workingCopy.setAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, getTestTarget().toString());
			ILaunchConfiguration configuration = workingCopy.doSave();
			//this call will end up in launch method of ServoyJsLaunchConfigurationDelegate
			DebugUITools.launch(configuration, ILaunchManager.DEBUG_MODE);

		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			this.structuredSelection = (IStructuredSelection)selection;
		}
	}

	protected TestTarget getTestTarget()
	{
		if (structuredSelection != null)
		{
			if (structuredSelection.size() == 1)
			{
				Object fe = structuredSelection.getFirstElement();
				if (fe instanceof SolutionUnitTestTarget)
				{
					return ((SolutionUnitTestTarget)fe).getTestTarget();
				}
			}
		}
		return null;
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart)
	{
		// not interested
	}
}