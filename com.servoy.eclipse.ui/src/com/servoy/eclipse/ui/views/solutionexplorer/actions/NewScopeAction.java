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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.OptionDialog;
import com.servoy.eclipse.model.nature.ServoyDeveloperProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.SolutionMetaData;
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
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("new_scope.png"));
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

			String scopeName = askScopeName(viewer.getViewSite().getShell(), "", (ServoyProject)project.getRealObject());
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
					EditorUtil.openScriptEditor(((ServoyProject)project.getRealObject()).getSolution(), scopeName, true);
				}
				catch (IOException e)
				{
					ServoyLog.logError("Could not create global scope " + scopeName + " in project  " + project, e);
				}
			}
		}
	}

	public static String askScopeName(Shell shell, final String initialValue, final ServoyProject project)
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
				Collection<String> scopeNames = null;
				try
				{
					if (project.getSolution().getSolutionType() == SolutionMetaData.PRE_IMPORT_HOOK ||
						project.getSolution().getSolutionType() == SolutionMetaData.POST_IMPORT_HOOK ||
						project.getProject().hasNature(ServoyDeveloperProject.NATURE_ID))
					{
						scopeNames = project.getSolution().getScopeNames();
					}
					else if (ScriptVariable.GLOBAL_SCOPE.equals(newText))
					{
						scopeNames = project.getGlobalScopenames();
					}
					else
					{
						scopeNames = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getScopeNames();
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
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

	public static ServoyProject askNewProject(Shell shell, final String fileName, final ServoyProject project)
	{
		List<String> modules = getModules(project);
		if (modules.size() == 0) return null;
		Collections.sort(modules);
		String[] moduleNames = modules.toArray(new String[] { });
		final OptionDialog optionDialog = new OptionDialog(shell, "Select destination module", null,
			"Select module where to move scope " + fileName, MessageDialog.INFORMATION, new String[] { "OK", "Cancel" }, 0,
			moduleNames, 0);
		int retval = optionDialog.open();
		String selectedProject = null;
		ServoyProject servoyProject = null;
		if (retval == Window.OK)
		{
			selectedProject = moduleNames[optionDialog.getSelectedOption()];
			if (selectedProject != null)
			{
				servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(selectedProject);
			}
		}

		return servoyProject;
	}

	public static List<String> getModules(ServoyProject project)
	{
		final ServoyProject[] activeModules = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		List<String> modules = new ArrayList<String>();
		for (ServoyProject prj : activeModules)
		{
			if (!prj.getProject().getName().equals(project.getProject().getName()))
			{
				modules.add(prj.getProject().getName());
			}
		}

		return modules;
	}
}