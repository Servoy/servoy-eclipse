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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.DeleteResourceAction;

import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * Action for deleting the selected resource using the standard eclipse delete resource operation.
 * 
 * @author acostescu
 */
public class DeleteSolutionAction extends Action implements ISelectionChangedListener
{

	private IProject selectedProjects[] = new IProject[0];
	private final DeleteResourceAction deleteAction;

	/**
	 * Creates a new delete solution action that will use the given shell when it needs to display dialogs.
	 * 
	 * @param shell used for interaction with the user.
	 */
	public DeleteSolutionAction(Shell shell)
	{
		deleteAction = new DeleteResourceAction(shell);

		setText("Delete solution");
		setToolTipText("Delete solution");
	}

	@Override
	public void run()
	{
		deleteAction.selectionChanged(new StructuredSelection(selectedProjects));
		deleteAction.run();
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedProjects = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> selit = sel.iterator();
			List<IProject> selected = new ArrayList<IProject>(sel.size());
			while (state && selit.hasNext())
			{
				SimpleUserNode node = selit.next();
				state = (node.getType() == UserNodeType.SOLUTION_ITEM) || (node.getType() == UserNodeType.SOLUTION_ITEM_NOT_ACTIVE_MODULE);
				if (state) selected.add(((ServoyProject)node.getRealObject()).getProject());
			}
			if (state)
			{
				selectedProjects = selected.toArray(new IProject[selected.size()]);
			}
		}
		if (selectedProjects == null)
		{
			selectedProjects = new IProject[0];
		}
		if (selectedProjects.length > 1)
		{
			setText("Delete solutions");
			setToolTipText("Delete solutions");
		}
		else if (selectedProjects.length == 1)
		{
			setText("Delete solution");
			setToolTipText("Delete solution");
		}
		setEnabled(state);
	}

}
