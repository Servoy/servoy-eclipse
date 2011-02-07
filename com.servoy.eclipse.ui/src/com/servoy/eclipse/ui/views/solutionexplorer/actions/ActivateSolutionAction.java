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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;

/**
 * Action for activating a solution.
 */
public class ActivateSolutionAction extends Action implements ISelectionChangedListener
{

	private ServoyProject selectedProject;

	/**
	 * Creates a new activate solution action.
	 */
	public ActivateSolutionAction()
	{
		setText(Messages.SolutionExplorerView_activateSolution);
		setToolTipText(Messages.SolutionExplorerView_activateSolutionTooltip);
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("active_solution.gif"));
	}

	@Override
	public void run()
	{
		if (selectedProject != null)
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(selectedProject, true);
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedProject = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			Object item = sel.getFirstElement();
			if (item instanceof SimpleUserNode)
			{
				Object real = ((SimpleUserNode)item).getRealObject();
				if (real instanceof ServoyProject)
				{
					if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != real)
					{
						selectedProject = (ServoyProject)real;
						state = true;
					}
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public boolean isEnabled()
	{
		return super.isEnabled();
	}

}