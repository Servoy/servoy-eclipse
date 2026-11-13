/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.ui.actions.DeleteResourceAction;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Deletes the selected developer solution using the standard Eclipse delete-resource operation (the same dialog modules use for Delete, offering
 * "Delete project contents on disk"). The standard delete dialog is shown first; only when a delete actually proceeds (the user confirmed, not cancelled)
 * is the developer solution un-referenced from the active solution so no dangling reference is left behind. Cancelling the dialog leaves everything untouched.
 *
 * @author Servoy
 */
public class DeleteDevSolutionAction extends Action implements ISelectionChangedListener
{
	private ServoyProject selectedProject = null;
	private final DeleteResourceAction deleteAction;
	private final SolutionExplorerView viewer;

	public DeleteDevSolutionAction(IShellProvider shellProvider, SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		deleteAction = new DeleteResourceAction(shellProvider);
		setText("Delete developer solution");
		setToolTipText("Delete developer solution");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("delete.png"));
	}

	@Override
	public void run()
	{
		if (selectedProject == null) return;
		IProject project = selectedProject.getProject();
		deleteAction.selectionChanged(new StructuredSelection(new IProject[] { project }));
		deleteAction.run();
		// the standard delete runs in a background job; only treat it as "really deleting" when that job was started
		// (or the project is already gone). If the user cancelled the dialog, nothing started and the project still
		// exists - leave the active solution's reference untouched.
		boolean deleting = Job.getJobManager().find(IDEWorkbenchMessages.DeleteResourceAction_jobName).length > 0 || !project.exists();
		if (deleting)
		{
			ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			if (activeProject != null)
			{
				// un-reference the deleted developer solution so the active solution keeps no dangling reference
				activeProject.removeDeveloperProject(project);
			}
			if (viewer != null) viewer.refreshTreeCompletely();
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		selectedProject = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.DEVELOPER_SOLUTION) && (node.getRealObject() instanceof ServoyProject);
			if (state) selectedProject = (ServoyProject)node.getRealObject();
		}
		setEnabled(state);
	}
}
