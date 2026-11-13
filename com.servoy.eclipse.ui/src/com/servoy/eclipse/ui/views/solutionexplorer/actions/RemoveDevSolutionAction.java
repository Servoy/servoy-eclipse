/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;


public class RemoveDevSolutionAction extends Action implements ISelectionChangedListener
{
	private ServoyProject selectedProject = null;
	private final SolutionExplorerView viewer;

	public RemoveDevSolutionAction(Shell shell, SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Remove developer solution");
		setToolTipText("Remove the developer solution from the active solution (the solution itself will not be deleted)");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("remove_module.png"));
	}

	@Override
	public void run()
	{
		if (selectedProject == null) return;
		ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject == null) return;
		activeProject.removeDeveloperProject(selectedProject.getProject());
		if (viewer != null) viewer.refreshTreeCompletely();
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
