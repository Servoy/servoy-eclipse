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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.IdentDocumentValidator;


/**
 * Action to create a new table depending on the selection of a solution view.
 * 
 * @author jblok
 */
public class NewTableAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewTableAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create table");
		setToolTipText("Create table");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.SERVER;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof IServerInternal)
		{
			try
			{
				final IServerInternal s = (IServerInternal)node.getRealObject();
				InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), "Create table", "Supply table name", "", new IInputValidator()
				{
					public String isValid(String newText)
					{
						//check to see if a table with the same name does not already exist.
						try
						{
							if (s.getTable(newText) != null)
							{
								return "A table with the same name already exists"; //$NON-NLS-1$
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
							MessageDialog.openError(UIUtils.getActiveShell(), "Error", e.getMessage()); //$NON-NLS-1$
							return e.getMessage();
						}

						boolean valid = IdentDocumentValidator.isSQLIdentifier(newText);
						return valid ? null : (newText.length() == 0 ? "" : "Invalid table name"); //$NON-NLS-1$//$NON-NLS-2$
					}
				});
				int res = nameDialog.open();
				if (res == Window.OK)
				{
					String name = nameDialog.getValue();
					Table t = s.getTable(name);
					if (t == null)
					{
						IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
						t = s.createNewTable(validator, name);
						EditorUtil.openTableEditor(t);
					}
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
				MessageDialog.openError(UIUtils.getActiveShell(), "Error", e.getMessage());
			}
		}
	}
}
