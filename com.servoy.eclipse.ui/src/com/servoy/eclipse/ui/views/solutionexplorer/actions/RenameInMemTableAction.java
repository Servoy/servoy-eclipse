/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author emera
 */
public class RenameInMemTableAction extends AbstractInMemTableAction
{
	private HashMap<String, String> names;

	public RenameInMemTableAction(Shell shell)
	{
		super(shell, "rename", "Renaming");
		setText("Rename in memory datasource");
		setToolTipText("Rename in memory datasource");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#doAction(com.servoy.j2db.persistence.IServer,
	 * com.servoy.j2db.persistence.ITable)
	 */
	@Override
	protected void doAction(IServer server, ITable table) throws SQLException, RepositoryException
	{
		((MemServer)server).renameTable(table, names.get(table.getName()));

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#doAction(com.servoy.j2db.persistence.IServer,
	 * com.servoy.j2db.persistence.ITable, java.util.ArrayList)
	 */
	@Override
	protected void doAction(IServer server, ITable table, ArrayList<String> userSelection) throws RepositoryException
	{
		try
		{
			((MemServer)server).renameTable(table, names.get(table.getName()), userSelection);
		}
		catch (SQLException e)
		{
			ServoyLog.logError(e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#run()
	 */
	@Override
	public void run()
	{
		names = new HashMap<String, String>();
		Iterator<IDataSourceWrapper> it = selection.keySet().iterator();
		while (it.hasNext())
		{
			final IDataSourceWrapper selectedTable = it.next();
			InputDialog nameDialog = new InputDialog(Display.getDefault().getActiveShell(), "Rename in memory datasource", "Supply a new datasource name",
				selectedTable.getTableName(), new IInputValidator()
				{
					public String isValid(String newText)
					{
						if (new InMemServerWrapper().getTableNames().contains(newText))
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
				names.put(selectedTable.getTableName(), nameDialog.getValue());
			}
		}
		if (names.size() > 0) super.run();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AbstractInMemTableAction#confirm()
	 */
	@Override
	protected boolean confirm()
	{
		return true;
	}
}
