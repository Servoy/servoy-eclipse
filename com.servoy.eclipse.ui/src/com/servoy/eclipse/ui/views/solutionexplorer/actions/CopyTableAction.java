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

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.IdentDocumentValidator;

public class CopyTableAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;
	private String selectedTableName;
	private String selectedServerName;

	public CopyTableAction(Shell shell)
	{
		this.shell = shell;
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("copy_edit.gif")); //$NON-NLS-1$
		setText("Copy table");
		setToolTipText("Copy table");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.TABLE)
			{
				SimpleUserNode userNode = (SimpleUserNode)sel.getFirstElement();
				TableWrapper tableWrapper = (TableWrapper)userNode.getRealObject();

				selectedTableName = tableWrapper.getTableName();
				selectedServerName = tableWrapper.getServerName();
			}
			state = type == UserNodeType.TABLE;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		int index = 0;
		String serverNames[] = ServoyModel.getServerManager().getServerNames(true, true, true, false);
		for (int i = 0; i < serverNames.length; i++)
		{
			if (serverNames[i].equals(selectedServerName))
			{
				index = i;
			}
		}

		// prepare dialog
		final InputComboCheckBoxDialog dialog = new InputComboCheckBoxDialog(shell, "Copy table", "Table name:", selectedTableName, new IInputValidator()
		{
			public String isValid(String newText)
			{
				return IdentDocumentValidator.isJavaIdentifier(newText) ? null : (newText.length() == 0 ? "" : "Invalid table name");
			}

		}, serverNames, "Please select the destination server:", index);
		dialog.setBlockOnOpen(true);
		if (dialog.open() == IDialogConstants.OK_ID)
		{
			boolean copyColumnInfo = dialog.getCheckBoxSelection();
			String targetServerName = dialog.getComboSelectedServer();
			String tableName = dialog.getTextBoxSelection();

			Table selectedTable = null;
			try
			{
				IServerInternal selectedServer = (IServerInternal)ServoyModel.getServerManager().getServer(selectedServerName);
				selectedTable = selectedServer.getTable(selectedTableName);
				copyTable(targetServerName, tableName, copyColumnInfo, selectedTable);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void copyTable(String targetServerName, String tableName, boolean copyColumnInfo, Table selectedTable)
	{
		IServerInternal targetServer = (IServerInternal)ServoyModel.getServerManager().getServer(targetServerName);
		if (tableName != null && targetServerName != null)
		{
			try
			{
				if (targetServer.getTable(tableName) != null)
				{
					MessageDialog.openError(shell, "Error", "Could not copy table:\nA table with the name '" + tableName + "' already exists in server "); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				}
				Table newTable = targetServer.createNewTable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), selectedTable,
					tableName);


				ServoyLog.logInfo("Table created successfully");

				IColumnInfoManager[] cims = ServoyModel.getServerManager().getColumnInfoManagers(targetServer.getName());


				if (copyColumnInfo)
				{
					Iterator<Column> it = newTable.getColumns().iterator();
					while (it.hasNext())
					{
						Column c = it.next();

						if (cims != null)
						{
							cims[0].createNewColumnInfo(c, false); // Use supplied sequence info, don't assume anything!!
							if (selectedTable != null)
							{
								Column templateCol = selectedTable.getColumn(c.getName());
								if (templateCol != null)
								{
									targetServer.duplicateColumnInfo(templateCol.getColumnInfo(), c.getColumnInfo());
									c.setColumnInfo(c.getColumnInfo()); // update some members of the Column if they were changed in column info
								}
							}
						}
					}
				}
				EditorUtil.openTableEditor(newTable);
				//this is necessary to open editor in edit mode;
				if (newTable.getColumnNames().length > 0)
				{
					String columnName = newTable.getColumnNames()[0];
					Column column = newTable.getColumn(columnName);
					column.flagColumnInfoChanged();
				}

			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public static class InputComboCheckBoxDialog extends InputDialog
	{

		private final String comboDescriptionText;
		private final String[] comboOptions;
		private String comboSelection;
		private int comboSelectionIndex;
		protected boolean checkBoxSelection = true;


		public InputComboCheckBoxDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, IInputValidator validator,
			String[] comboOptions, String comboDescriptionText, int initialComboSelection)
		{
			super(parentShell, dialogTitle, dialogMessage, initialValue, validator);
			this.comboOptions = comboOptions;
			this.comboDescriptionText = comboDescriptionText;
			comboSelectionIndex = initialComboSelection;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite area = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = layout.marginWidth = 10;
			area.setLayout(layout);

			Label comboDescription = new Label(area, SWT.NONE);
			comboDescription.setText(comboDescriptionText);

			final Combo combo = new Combo(area, SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(combo);
			combo.setItems(comboOptions);
			combo.select(comboSelectionIndex);
			comboSelection = combo.getText();
			GridData gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			combo.setLayoutData(gd);

			final Button checkBoxButton = new Button(area, SWT.CHECK);
			checkBoxButton.setSelection(checkBoxSelection);
			checkBoxButton.setText("Copy column info");
			GridData checkButtonGridData = new GridData();
			checkButtonGridData.horizontalAlignment = GridData.FILL;
			checkBoxButton.setLayoutData(checkButtonGridData);

			combo.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e)
				{
					comboSelection = combo.getText();
					comboSelectionIndex = combo.getSelectionIndex();
				}
			});

			checkBoxButton.addSelectionListener(new SelectionListener()
			{
				public void widgetDefaultSelected(SelectionEvent e)
				{
					widgetSelected(e);
				}

				public void widgetSelected(SelectionEvent e)
				{
					checkBoxSelection = checkBoxButton.getSelection();
				}
			});
			layout = (GridLayout)((Composite)super.createDialogArea(area)).getLayout();
			layout.marginHeight = layout.marginWidth = 0;

			return area;
		}

		public String getComboSelectedServer()
		{
			return comboSelection;
		}

		public String getTextBoxSelection()
		{
			return this.getValue();
		}

		public boolean getCheckBoxSelection()
		{
			return checkBoxSelection;
		}
	}

}
