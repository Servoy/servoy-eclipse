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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.RunInWorkspaceJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;


public class RefreshAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView fPart;
	private IServerInternal selectedServer;

	public RefreshAction(SolutionExplorerView part)
	{
		fPart = part;
		setText(Messages.RefreshAction_refresh);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("refresh.png"));
		setActionDefinitionId("org.eclipse.ui.file.refresh");
//        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IJavaHelpContextIds.CALL_HIERARCHY_REFRESH_ACTION);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedServer = null;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			if (s.size() == 1)
			{
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				if (node.getRealObject() instanceof IServerInternal)
				{
					IServerInternal server = (IServerInternal)node.getRealObject();
					if (server.getConfig().isEnabled() && server.isValid())
					{
						selectedServer = server;
					}
				}
			}
		}
	}

	/**
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		RunInWorkspaceJob job = new RunInWorkspaceJob(new IWorkspaceRunnable()
		{
			public void run(IProgressMonitor monitor) throws CoreException
			{
				try
				{
					AbstractServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel().refreshServoyProjects();
					ServoyProject[] sp = servoyModel.getServoyProjects();
					ServoyResourcesProject[] rp = servoyModel.getResourceProjects();
					monitor.beginTask("Refreshing", sp.length + rp.length + 1);
					servoyModel.getResourceChangesHandlerCounter().increment();
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
					finally
					{
						servoyModel.getResourceChangesHandlerCounter().decrement();
					}
					monitor.subTask("Solution explorer view...");
					fPart.refreshView();
					monitor.worked(1);
					if (selectedServer != null &&
						UIUtils.askQuestion(fPart.getSite().getShell(), "Reload server",
							"Do you want to reload all tables from server: '" + selectedServer.getName() + "'?"))
					{
						try
						{
							selectedServer.reloadTables();
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
				finally
				{
					monitor.done();
				}
			}
		});
		job.setRule(ServoyModel.getWorkspace().getRoot());
		job.setUser(false);
		job.setSystem(true);
		job.schedule();
	}
}
