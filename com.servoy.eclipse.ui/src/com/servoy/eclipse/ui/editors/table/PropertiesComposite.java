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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.IServerInternal;
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
		refresh();
	}

	/**
	 * @param table
	 */
	public void refresh()
	{
		if (MetaDataUtils.canBeMarkedAsMetaData(table))
		{
			btnMetadataTable.setText("Metadata table");
			btnMetadataTable.setEnabled(true);
		}
		else
		{
			btnMetadataTable.setText("Metadata table (Table can't be a metadata table because it must have a UUID primairy key, a " +
				MetaDataUtils.METADATA_MODIFICATION_COLUMN + " and " + MetaDataUtils.METADATA_DELETION_COLUMN + " date columns)");
			btnMetadataTable.setEnabled(false);
		}
		btnMetadataTable.getParent().layout(true);
		btnHiddenInDeveloper.setSelection(table.isMarkedAsHiddenInDeveloper());
		btnMetadataTable.setSelection(table.isMarkedAsMetaData());
	}

	/**
	 * 
	 */
	public void saveValues()
	{
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
