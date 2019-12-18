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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.InputAndListDialog;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action to create a new valuelist depending on the selection of a solution view.
 *
 * @author jcompagner
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

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("newvaluelist.png"));
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

			Pair<String, String> name = askValueListName(viewer.getViewSite().getShell(), realSolution.getName());
			if (name != null)
			{
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(name.getRight());
				Solution editingSolution = servoyProject.getEditingSolution();
				if (editingSolution == null)
				{
					return;
				}
				createValueList(name.getLeft(), editingSolution);
			}
		}
	}

	public static Pair<String, String> askValueListName(Shell shell, String solutionName)
	{
		ServoyProject[] projects = ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		String[] names = new String[projects.length];
		for (int i = 0; i < projects.length; i++)
		{
			names[i] = projects[i].getProject().getName();
		}
		InputAndListDialog nameDialog = new InputAndListDialog(shell, "Create value list", "Supply value list name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				String message = null;
				if (newText.length() == 0)
				{
					message = "";
				}
				else if (!IdentDocumentValidator.isJavaIdentifier(newText))
				{
					message = "Invalid value list name";
				}
				else
				{
					try
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(newText, -1,
							new ValidatorSearchContext(this, IRepository.VALUELISTS), false);
					}
					catch (RepositoryException e)
					{
						message = e.getMessage();
					}
				}
				return message;
			}
		}, names, solutionName, "Solution");
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			return new Pair<String, String>(nameDialog.getValue(), nameDialog.getExtendedValue());
		}
		return null;
	}

	public static ValueList createValueList(String valueListName, Solution editingSolution)
	{
		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList vl = editingSolution.getValueList(valueListName);
			if (vl == null)
			{
				IValidateName validator = servoyModel.getNameValidator();
				vl = editingSolution.createNewValueList(validator, valueListName);
				vl.setAddEmptyValue(IValueListConstants.EMPTY_VALUE_NEVER);
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
