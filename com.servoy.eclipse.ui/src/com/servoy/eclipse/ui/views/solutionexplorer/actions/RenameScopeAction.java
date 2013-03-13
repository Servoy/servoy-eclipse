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

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

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
 * Action to create a new global scope.
 * 
 * @author rgansevles
 */
public class RenameScopeAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "create new variable" action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public RenameScopeAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Rename scope");
		setToolTipText(getText());
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("scopes.gif"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		setEnabled(sel.size() == 1 && ((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.GLOBALS_ITEM);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
		{
			SimpleUserNode project = node.getAncestorOfType(IProjectNature.class);
			if (project == null)
			{
				return;
			}

			Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
			String oldname = pair.getRight();

			String scopeName = NewScopeAction.askScopeName(viewer.getViewSite().getShell(), oldname, (ServoyProject)project.getRealObject());
			if (scopeName == null || scopeName.equals(oldname))
			{
				return;
			}

			Solution solution = (((ServoyProject)project.getRealObject()).getSolution());

			WorkspaceFileAccess wsfa = new WorkspaceFileAccess(((IProjectNature)project.getRealObject()).getProject().getWorkspace());
			String oldScriptPath = SolutionSerializer.getRelativePath(solution, false) + oldname + SolutionSerializer.JS_FILE_EXTENSION;
			String newScriptPath = SolutionSerializer.getRelativePath(solution, false) + scopeName + SolutionSerializer.JS_FILE_EXTENSION;
			// if the file isn't there, create it here so that the formatter sees the js file.
			if (!wsfa.exists(newScriptPath))
			{
				// file doesn't exist, create the file and its parent directories
				try
				{
					wsfa.move(oldScriptPath, newScriptPath);
				}
				catch (IOException e)
				{
					ServoyLog.logError("Could not rename global scope " + scopeName + " in project  " + project, e);
				}
			}
		}
	}
}