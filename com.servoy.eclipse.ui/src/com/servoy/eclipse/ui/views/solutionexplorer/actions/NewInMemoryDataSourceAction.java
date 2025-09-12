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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.ViewFoundsetServerWrapper;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;


/**
 * Action to create a new table depending on the selection of a solution view.
 *
 * @author jblok
 */
public class NewInMemoryDataSourceAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;
	private final UserNodeType nodeType;

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewInMemoryDataSourceAction(SolutionExplorerView sev, String text, UserNodeType nodeType)
	{
		viewer = sev;
		this.nodeType = nodeType;
		setText(text);
		setToolTipText(text);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			if (node.getRealObject() instanceof IServerInternal)
			{
				IServerInternal s = (IServerInternal)node.getRealObject();
				state = (node.getType() == nodeType);
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getType().equals(nodeType))
		{

			final IServerInternal s = (IServerInternal)node.getRealObject();
			InputDialog nameDialog = new InputDialog(viewer.getViewSite().getShell(), getText(), "Supply datasource name", "", new IInputValidator()
			{
				public String isValid(String newText)
				{
					if (nodeType == UserNodeType.INMEMORY_DATASOURCES && new InMemServerWrapper().getTableNames().contains(newText) ||
						nodeType == UserNodeType.VIEW_FOUNDSETS && new ViewFoundsetServerWrapper().getTableNames().contains(newText))
					{
						return "Name already used";
					}
					boolean valid = (IdentDocumentValidator.isSQLIdentifier(newText) &&
						(!(newText.toUpperCase()).startsWith(DataModelManager.TEMP_UPPERCASE_PREFIX)) &&
						(!(newText.toUpperCase()).startsWith(IServer.SERVOY_UPPERCASE_PREFIX)));
					return valid ? null : (newText.length() == 0 ? "" : "Invalid datasource name");
				}
			});

			int res = nameDialog.open();
			if (res == Window.OK)
			{
				String name = nameDialog.getValue();

				ITable t;
				try
				{
					t = s.getTable(name);
					if (t == null)
					{
						IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
						t = s.createNewTable(validator, name);
						EditorUtil.openTableEditor(t);
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}

			}

		}
	}
}
