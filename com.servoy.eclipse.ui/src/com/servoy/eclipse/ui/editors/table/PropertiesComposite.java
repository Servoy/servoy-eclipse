/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.editors.table;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * @author jcompagner
 * @since 6.1
 */
public class PropertiesComposite extends Composite
{

	private final Button btnHiddenInDeveloper;
	private final Button btnMetadataTable;
	private final Table table;
	private final Button createMetaDataColumnsButton;

	/**
	 * @param parent
	 * @param style
	 * @param tableEditor 
	 */
	public PropertiesComposite(Composite parent, int style, final TableEditor tableEditor)
	{
		super(parent, style);
		setLayout(new FormLayout());
		this.table = tableEditor.getTable();

		SelectionListener listener = new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (btnHiddenInDeveloper.getSelection() != table.isMarkedAsHiddenInDeveloper() || btnMetadataTable.getSelection() != table.isMarkedAsMetaData())
				{
					tableEditor.flagModified(false);
				}
				else
				{
					tableEditor.flagModified(true);
				}
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		};
		btnHiddenInDeveloper = new Button(this, SWT.CHECK);
		btnHiddenInDeveloper.addSelectionListener(listener);
		FormData fd_btnHiddenInDeveloper = new FormData();
		fd_btnHiddenInDeveloper.top = new FormAttachment(0, 10);
		fd_btnHiddenInDeveloper.left = new FormAttachment(0, 10);
		btnHiddenInDeveloper.setLayoutData(fd_btnHiddenInDeveloper);
		btnHiddenInDeveloper.setText("Hidden in developer");

		btnMetadataTable = new Button(this, SWT.CHECK);
		btnMetadataTable.addSelectionListener(listener);
		FormData fd_btnMetadataTable = new FormData();
		fd_btnMetadataTable.top = new FormAttachment(btnHiddenInDeveloper, 6);
		fd_btnMetadataTable.left = new FormAttachment(0, 10);
		btnMetadataTable.setLayoutData(fd_btnMetadataTable);

		createMetaDataColumnsButton = new Button(this, SWT.NONE);
		FormData fd_button = new FormData();
		fd_button.top = new FormAttachment(btnMetadataTable, 6);
		fd_button.left = new FormAttachment(0, 37);
		createMetaDataColumnsButton.setLayoutData(fd_button);
		createMetaDataColumnsButton.setText("add metadata date columns");
		createMetaDataColumnsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				try
				{
					if (table.getColumn(MetaDataUtils.METADATA_MODIFICATION_COLUMN) == null)
					{
						tableEditor.getColumnComposite().addColumn(table, MetaDataUtils.METADATA_MODIFICATION_COLUMN, IColumnTypes.DATETIME, 0);
					}
					if (table.getColumn(MetaDataUtils.METADATA_DELETION_COLUMN) == null)
					{
						tableEditor.getColumnComposite().addColumn(table, MetaDataUtils.METADATA_DELETION_COLUMN, IColumnTypes.DATETIME, 0);
					}
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logError(ex);
					UIUtils.reportWarning("Error creating columns", "Error creating columns: " + ex.getMessage());
				}
			}
		});
		refresh();
	}

	public void refresh()
	{
		if (MetaDataUtils.canBeMarkedAsMetaData(table))
		{
			btnMetadataTable.setText("Metadata table");
			btnMetadataTable.setEnabled(true);
		}
		else
		{
			btnMetadataTable.setText("Metadata table (Table can't be a metadata table because it must have a UUID primary key, a " +
				MetaDataUtils.METADATA_MODIFICATION_COLUMN + " and a " + MetaDataUtils.METADATA_DELETION_COLUMN + " date column)");
			btnMetadataTable.setEnabled(false);
		}
		btnMetadataTable.getParent().layout(true);
		btnHiddenInDeveloper.setSelection(table.isMarkedAsHiddenInDeveloper());
		btnMetadataTable.setSelection(table.isMarkedAsMetaData());

		boolean canMakeMetaDataColumns = true;
		for (Column column : table.getRowIdentColumns())
		{
			if (column.getColumnInfo() == null || !column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
			{
				canMakeMetaDataColumns = false;
			}
		}

		canMakeMetaDataColumns &= ((table.getColumn(MetaDataUtils.METADATA_MODIFICATION_COLUMN) == null) || (table.getColumn(MetaDataUtils.METADATA_DELETION_COLUMN) == null));
		createMetaDataColumnsButton.setEnabled(canMakeMetaDataColumns);

	}

	/**
	 * 
	 */
	public void saveValues()
	{
		if (table.isMarkedAsMetaData() && !btnMetadataTable.getSelection())
		{
			IFile dataFile = ServoyModelFinder.getServoyModel().getDataModelManager().getMetaDataFile(table.getDataSource());
			if (dataFile != null && dataFile.exists())
			{
				String wscontents = null;
				try
				{
					wscontents = new WorkspaceFileAccess(ServoyModel.getWorkspace()).getUTF8Contents(dataFile.getFullPath().toString());
				}
				catch (IOException e1)
				{
					ServoyLog.logError(e1);
				}
				boolean deleteFile = false;
				if (wscontents != null && wscontents.length() > 0)
				{
					if (UIUtils.askConfirmation(getShell(), "Unmark metadata table",
						"Are you sure you want to unmark table as metadata table? This will also delete data file."))
					{
						deleteFile = true;
					}
					else
					{
						btnMetadataTable.setSelection(true);
					}
				}
				else
				{
					deleteFile = true;
				}
				if (deleteFile)
				{
					try
					{
						dataFile.delete(true, null);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}

		}
		table.setMarkedAsMetaData(btnMetadataTable.getSelection());
		IServerInternal server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(table.getServerName());
		if (server != null)
		{
			server.setTableMarkedAsHiddenInDeveloper(table.getName(), btnHiddenInDeveloper.getSelection());
		}
		else
		{
			table.setMarkedAsHiddenInDeveloperInternal(btnHiddenInDeveloper.getSelection());
		}
	}
}
