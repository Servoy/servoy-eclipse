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
import java.util.Collection;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action to create a new global scope.
 * 
 * @author rgansevles
 */
public class NewScopeAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new "create new variable" action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewScopeAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create new scope");
		setToolTipText(getText());
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("scopes.gif"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.SCOPES_ITEM || type == UserNodeType.SCOPES_ITEM_CALCULATION_MODE;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
		{
			SimpleUserNode project = node.getAncestorOfType(ServoyProject.class);
			if (project == null)
			{
				return;
			}

			String scopeName = askScopeName(viewer.getViewSite().getShell(), "");
			if (scopeName == null)
			{
				return;
			}

			WorkspaceFileAccess wsfa = new WorkspaceFileAccess(((IProjectNature)project.getRealObject()).getProject().getWorkspace());
			String scriptPath = SolutionSerializer.getRelativePath(((((ServoyProject)project.getRealObject()).getSolution())), false) + scopeName +
				SolutionSerializer.JS_FILE_EXTENSION;
			if (!wsfa.exists(scriptPath))
			{
				// file doesn't exist, create the file and its parent directories
				try
				{
					wsfa.setContents(scriptPath, new byte[0]);
				}
				catch (IOException e)
				{
					ServoyLog.logError("Could not create global scope " + scopeName + " in project  " + project, e);
				}
				viewer.refreshTreeCompletely();
			}
		}
	}

	public static String askScopeName(Shell shell, final String initialValue)
	{
		InputDialog nameDialog = new InputDialog(shell, "Create new global scope", "New scope name", initialValue, new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText == null || newText.trim().length() == 0)
				{
					return "";
				}
				if (!IdentDocumentValidator.isJavaIdentifier(newText))
				{
					return "Invalid scope name";
				}
				Collection<String> scopeNames = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getScopeNames();
				for (String scopeName : scopeNames)
				{
					if (scopeName.equals(newText) || (initialValue.equals("") && scopeName.equalsIgnoreCase(newText)) ||
						(newText.equalsIgnoreCase(scopeName) && !initialValue.equalsIgnoreCase(scopeName)))
					{
						return "Scope already exists";
					}
				}
				// ok
				return null;
			}
		});
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			return nameDialog.getValue();
		}
		return null;
	}
}