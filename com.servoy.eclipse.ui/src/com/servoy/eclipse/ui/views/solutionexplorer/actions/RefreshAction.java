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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;


public class RefreshAction extends Action
{
	private final SolutionExplorerView fPart;

	public RefreshAction(SolutionExplorerView part)
	{
		fPart = part;
		setText(Messages.RefreshAction_refresh);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("refresh.gif"));//$NON-NLS-1$
		setActionDefinitionId("org.eclipse.ui.file.refresh"); //$NON-NLS-1$
//        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REFRESH_ACTION);
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		try
		{
			PlatformUI.getWorkbench().getProgressService().run(true, false, new IRunnableWithProgress()
			{
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
				{
					try
					{
						ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel().refreshServoyProjects();
						ServoyProject[] sp = servoyModel.getServoyProjects();
						ServoyResourcesProject[] rp = servoyModel.getResourceProjects();
						monitor.beginTask("Refreshing", sp.length + rp.length + 1);
						try
						{
							for (ServoyProject servoyProject : sp)
							{
								monitor.subTask("Solution ... " + servoyProject.getProject().getName());
								servoyProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
								monitor.worked(1);
							}
							for (ServoyResourcesProject servoyResourcesProject : rp)
							{
								monitor.subTask("Resources project ... " + servoyResourcesProject.getProject().getName());
								servoyResourcesProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
								monitor.worked(1);
							}
						}
						catch (CoreException e)
						{
							ServoyLog.logError("refresh", e);
						}
						monitor.subTask("Solution explorer view...");
						fPart.refreshView();
						monitor.worked(1);
					}
					finally
					{
						monitor.done();
					}
				}
			});
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
	}
}
