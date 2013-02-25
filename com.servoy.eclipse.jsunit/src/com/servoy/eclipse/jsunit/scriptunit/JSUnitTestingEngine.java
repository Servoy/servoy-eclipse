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

package com.servoy.eclipse.jsunit.scriptunit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.environment.IEnvironment;
import org.eclipse.dltk.launching.InterpreterConfig;
import org.eclipse.dltk.testing.AbstractTestingEngine;
import org.eclipse.dltk.testing.ITestRunnerUI;

/**
 * @author obuligan
 *
 */
public class JSUnitTestingEngine extends AbstractTestingEngine
{

	public static String ENGINE_ID = "com.servoy.eclipse.jsunit.scriptunit.JSUnitTestingEngine";

	@Override
	public void configureLaunch(InterpreterConfig config, ILaunchConfiguration configuration, ILaunch launch) throws CoreException
	{
		super.configureLaunch(null, configuration, launch);
	}


	@Override
	public String getMainScriptPath(ILaunchConfiguration configuration, IEnvironment scriptEnvironment) throws CoreException
	{
		return null;
	}

	@Override
	public IStatus validateContainer(IModelElement element)
	{
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus validateSourceModule(ISourceModule module)
	{
		return Status.CANCEL_STATUS;
	}

	@Override
	public Object getAdapter(Class adapter)
	{
		return null;
	}


	@Override
	public ITestRunnerUI getTestRunnerUI(IScriptProject project, ILaunchConfiguration configuration)
	{
		return new JSUnitTestRunnerUI(this, project);
		//return super.getTestRunnerUI(project, configuration);
	}

}