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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * Action that lets the user choose a resources project to be associated to the selected solution.
 * 
 * @author Andrei Costescu
 */
public class ChangeResourcesProjectAction extends Action implements ISelectionChangedListener
{

	private final Shell shell;
	private ServoyProject selectedSolutionProject;

	/**
	 * Creates a new ChangeResourcesProjectAction action that will use the given shell for the user dialog.
	 * 
	 * @param shell shell used to display a dialog.
	 */
	public ChangeResourcesProjectAction(Shell shell)
	{
		this.shell = shell;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("resources.png"));
		setText(Messages.ChangeResourcesProjectAction_chooseResourcesProject);
		setToolTipText(Messages.ChangeResourcesProjectAction_chooseResourcesProject);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean enabled = true;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			enabled = (s.size() == 1);
			if (enabled)
			{
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				UserNodeType type = node.getType();
				if (((type == UserNodeType.SOLUTION) || (type == UserNodeType.SOLUTION_ITEM) || (type == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE)) &&
					(node.getRealObject() instanceof ServoyProject))
				{
					selectedSolutionProject = (ServoyProject)node.getRealObject();
				}
				else
				{
					enabled = false;
				}
			}
		}
		else
		{
			enabled = false;
		}
		if (!enabled)
		{
			selectedSolutionProject = null;
		}
		setEnabled(enabled);
	}

	@Override
	public void run()
	{
		if (selectedSolutionProject == null) return;
		selectedSolutionProject.showChangeResourceProjectDlg(shell);
	}

}