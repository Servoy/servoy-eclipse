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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

import com.servoy.eclipse.jsunit.SolutionUnitTestTarget;
import com.servoy.eclipse.jsunit.launch.JSUnitLaunchConfigurationDelegate;
import com.servoy.eclipse.jsunit.runner.TestTarget;

/**
 * This action launches a script unit test run from a selected node in the solex view.
 * @author obuligan
 */
public class RunJSUnitAction implements IObjectActionDelegate, IWorkbenchWindowActionDelegate
{

	private IStructuredSelection structuredSelection;

	public void run(IAction action)
	{

		JSUnitLaunchConfigurationDelegate.launchTestTarget(getTestTarget());
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


	@Override
	public void dispose()
	{
		structuredSelection = null;
	}

	@Override
	public void init(IWorkbenchWindow window)
	{
		// not interested
	}
}