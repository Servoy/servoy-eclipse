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
package com.servoy.eclipse.ui.dialogs;

import java.sql.Types;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManager;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Table;

public class I18NServerTableDialog extends Dialog
{
	private static final String SELECTION_NONE = "<none>"; //$NON-NLS-1$

	private String selectedServerName;
	private String selectedTableName;

	private Combo defaultI18NServer;
	private Text defaultI18NTable;

	public I18NServerTableDialog(Shell parentShell, String selectedServerName, String proposedTableName)
	{
		super(parentShell);
		this.selectedServerName = selectedServerName;
		this.selectedTableName = proposedTableName;
		setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		getShell().setText("Create new I18N table"); //$NON-NLS-1$

		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setLayout(new GridLayout());

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		composite.setLayout(gridLayout);

		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 2;

		Label labelDefaultI18NServer = new Label(composite, SWT.NONE);
		labelDefaultI18NServer.setText("I18N server"); //$NON-NLS-1$

		defaultI18NServer = new Combo(composite, SWT.NULL | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(defaultI18NServer);
		defaultI18NServer.setLayoutData(gridData);

		Label labelDefaultI18NTable = new Label(composite, SWT.NONE);
		labelDefaultI18NTable.setText("New I18N table"); //$NON-NLS-1$

		defaultI18NTable = new Text(composite, SWT.BORDER);
		defaultI18NTable.setText(selectedTableName);
		defaultI18NTable.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label labelAdvice = new Label(composite, SWT.NONE);
		gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = 3;
		gridData.verticalIndent = 20;
		labelAdvice.setLayoutData(gridData);
		labelAdvice.setText("If you leave the table name empty, a default name will be used.");

		// Initialize server names list.
		String currentServerName = selectedServerName;
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IServerManager sm = ServoyModel.getServerManager();
		String[] serverNames = sm.getServerNames(true, true, true, false);
		defaultI18NServer.removeAll();
		defaultI18NServer.add(SELECTION_NONE);
		int selectedServerIndex = 0;
		for (int i = 0; i < serverNames.length; i++)
		{
			defaultI18NServer.add(serverNames[i]);
			if (serverNames[i].equals(currentServerName)) selectedServerIndex = i + 1;
		}
		defaultI18NServer.select(selectedServerIndex);

		return composite;
	}

	@Override
	public boolean close()
	{
		selectedServerName = defaultI18NServer.getText();
		selectedTableName = defaultI18NTable.getText();
		return super.close();
	}

	public String getSelectedServerName()
	{
		return selectedServerName;
	}

	public String getSelectedTableName()
	{
		return selectedTableName;
	}

	/**
	 * Creates a new I18N table in the specified server. The table name can be also specified. If a table name is specified, it is used exactly as provided. If
	 * no table name is specified, then a default table name will be used.
	 * 
	 * @param serverName The name of the server where to create the new I18N table.
	 * @param tableName The name of the new I18N table.
	 * @param shell A shell used for displaying message boxes.
	 * @return Returns the table name that was used for the new I18N table. This will be the same table name as that which was provided, if a table name was
	 *         provided. Otherwise this will be a default table name picked automatically.
	 */
	public static Table createDefaultMessagesTable(String serverName, String tableName, Shell shell)
	{
		if ((serverName == null) || serverName.equals(SELECTION_NONE) || (serverName.trim().length() == 0))
		{
			MessageBox msg = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
			msg.setText("No server selected"); //$NON-NLS-1$
			msg.setMessage("Please select the server where the I18N table should be created."); //$NON-NLS-1$
			msg.open();
			return null;
		}
		else
		{
			String adjustedTableName = tableName;
			if (adjustedTableName.equals(SELECTION_NONE) || adjustedTableName.trim().length() == 0)
			{
				String prefix = "i18n_messages"; //$NON-NLS-1$
				adjustedTableName = prefix;
				try
				{
					IServerInternal srv = (IServerInternal)ServoyModel.getServerManager().getServer(serverName);
					if (srv.checkIfTableExistsInDatabase(adjustedTableName))
					{
						int counter = 0;
						do
						{
							counter++;
							adjustedTableName = prefix + "_" + counter; //$NON-NLS-1$
						}
						while ((counter < 20) && (srv.checkIfTableExistsInDatabase(adjustedTableName)));
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError("Failed to propose a default name for the I18N table on server '" + serverName + "'.", ex); //$NON-NLS-1$//$NON-NLS-2$
				}
			}

			try
			{
				IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(serverName);
				Table table = server.getTable(adjustedTableName);
				if (table != null)
				{
					MessageBox msg = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
					msg.setText("Table already exists"); //$NON-NLS-1$
					msg.setMessage("A table named '" + adjustedTableName + "' already exists on server '" + serverName + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					msg.open();
					return null;
				}
				else
				{
					// Create the table in the repository.
					IValidateName validator = DummyValidator.INSTANCE;
					table = server.createNewTable(validator, adjustedTableName);
					Column column = table.createNewColumn(validator, "message_id", Types.INTEGER, 0, 0, false, true); //$NON-NLS-1$
					column.setSequenceType(ColumnInfo.SERVOY_SEQUENCE);
					table.createNewColumn(validator, "message_key", Types.VARCHAR, 150, 0, false); //$NON-NLS-1$ 
					table.createNewColumn(validator, "message_language", Types.VARCHAR, 5, 0, true); //$NON-NLS-1$
					table.createNewColumn(validator, "message_value", Types.VARCHAR, 2000, 0, true); //$NON-NLS-1$
					server.syncTableObjWithDB(table, false, false, null);

					// Store the I18N server/table names. In case the user later pushes the Cancel button, the server/table name
					// will remain changed anyway, because the user pushed the Create button.
					//storeI18NServerAndTable();
					MessageBox msg = new MessageBox(shell, SWT.ICON_INFORMATION | SWT.OK);
					msg.setText("I18N table successfully created"); //$NON-NLS-1$
					msg.setMessage("I18N table named '" + adjustedTableName + "' was created on server '" + serverName + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					msg.open();

					return table;
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Failed to create I18N table '" + adjustedTableName + "' on server '" + serverName + "'.", ex); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				MessageBox msg = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
				msg.setText("Could not create I18N table"); //$NON-NLS-1$
				msg.setMessage("An error occured while creating table named '" + adjustedTableName + "' on server '" + serverName + "'."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				msg.open();

				return null;
			}
		}
	}
}
