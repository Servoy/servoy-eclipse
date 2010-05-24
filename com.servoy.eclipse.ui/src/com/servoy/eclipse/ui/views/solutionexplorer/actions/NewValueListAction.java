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
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.IdentDocumentValidator;

/**
 * Action to create a new valuelist depending on the selection of a solution view.
 * 
 * @author Johan Compagner
 */
public class NewValueListAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewValueListAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("newvaluelist.gif")); //$NON-NLS-1$
		setText("Create valuelist");
		setToolTipText("Create valuelist");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.VALUELISTS;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IPersist)
		{

			Solution realSolution = (Solution)((IPersist)node.getRealObject()).getRootObject();

			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(realSolution.getName());
			Solution editingSolution = servoyProject.getEditingSolution();
			if (editingSolution == null)
			{
				return;
			}
			String name = askValueListName(viewer.getViewSite().getShell());
			if (name != null)
			{
				createValueList(name, editingSolution);
			}
		}
	}

	public static String askValueListName(Shell shell)
	{
		InputDialog nameDialog = new InputDialog(shell, "Create value list", "Supply value list name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
				return valid ? null : (newText.length() == 0 ? "" : "Invalid value list name");
			}
		});
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			String name = nameDialog.getValue();
			return name;
		}
		return null;
	}

	public static ValueList createValueList(String valueListName, Solution editingSolution)
	{
		try
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList vl = editingSolution.getValueList(valueListName);
			if (vl == null)
			{
				IValidateName validator = servoyModel.getNameValidator();
				vl = editingSolution.createNewValueList(validator, valueListName);
				vl.setAddEmptyValue(ValueList.EMPTY_VALUE_NEVER);
			}
			EditorUtil.openValueListEditor(vl);
			return vl;
		}
		catch (RepositoryException e)
		{
			MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Error", e.getMessage());
		}
		return null;
	}
}
