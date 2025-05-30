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


import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;


/**
 * Action to delete global files
 *
 * @author rgansevles
 */

public class DeleteScopeAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public DeleteScopeAction(String text, SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		Activator.getDefault();
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("delete.png"));
		setText(text);
		setToolTipText(text);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		setEnabled(calculateEnabled(event));
	}

	protected boolean calculateEnabled(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		if (sel.size() == 0)
		{
			return false;
		}
		Iterator<SimpleUserNode> nodes = sel.iterator();
		while (nodes.hasNext())
		{
			SimpleUserNode node = nodes.next();
			if (node.getType() != UserNodeType.GLOBALS_ITEM)
			{
				return false;
			}
		}
		return true;
	}

	@Override
	public void run()
	{
		if (!MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?"))
		{
			return;
		}

		for (SimpleUserNode node : viewer.getSelectedTreeNodes())
		{
			SimpleUserNode project = node.getAncestorOfType(ServoyProject.class);
			if (project == null)
			{
				continue;
			}

			Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
			deleteScript(((ServoyProject)project.getRealObject()), pair.getRight());
		}
	}

	public static void deleteScript(ServoyProject project, String scopeName)
	{
		WorkspaceFileAccess wsfa = new WorkspaceFileAccess(project.getProject().getWorkspace());
		String scriptPath = SolutionSerializer.getRelativePath(((project.getSolution())), false) + scopeName +
			SolutionSerializer.JS_FILE_EXTENSION;
		try
		{
			wsfa.delete(scriptPath);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Could not delete scope '" + scopeName + "' from project '" + project.getProject().getName() + "'", e);
		}
	}

}