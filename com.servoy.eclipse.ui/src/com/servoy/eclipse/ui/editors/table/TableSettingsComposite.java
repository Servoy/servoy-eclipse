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
package com.servoy.eclipse.ui.editors.table;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;

public class TableSettingsComposite extends Group
{

	private final Button readButton;

	private final Button insertButton;

	private final Button updateButton;

	private final Button deleteButton;

	private final Button trackingButton;

	private final Button trackingSelectButton;

	private final TableEditor tableEditor;

	private String currentGroup;

	private final Map<String, Integer> securityInfo;

	public TableSettingsComposite(Composite parent, int style, TableEditor te)
	{
		super(parent, style);
		setLayout(new FormLayout());
		setText("Table Settings");
		tableEditor = te;
		securityInfo = new HashMap<String, Integer>();

		final Button selectAllButton = new Button(this, SWT.CHECK);
		final FormData fd_selectAllButton = new FormData();
		fd_selectAllButton.left = new FormAttachment(0, 20);
		fd_selectAllButton.top = new FormAttachment(0, 9);
		fd_selectAllButton.right = new FormAttachment(100, 0);
		selectAllButton.setLayoutData(fd_selectAllButton);
		selectAllButton.setText("Select all");


		readButton = new Button(this, SWT.CHECK);
		final FormData fd_readButton = new FormData();
		fd_readButton.left = new FormAttachment(0, 20);
		fd_readButton.top = new FormAttachment(0, 34);
		fd_readButton.right = new FormAttachment(100, 0);
		readButton.setLayoutData(fd_readButton);
		readButton.setText("Read");

		insertButton = new Button(this, SWT.CHECK);
		final FormData fd_insertButton = new FormData();
		fd_insertButton.left = new FormAttachment(0, 20);
		fd_insertButton.top = new FormAttachment(0, 59);
		fd_insertButton.right = new FormAttachment(100, 0);
		insertButton.setLayoutData(fd_insertButton);
		insertButton.setText("Insert");

		updateButton = new Button(this, SWT.CHECK);
		final FormData fd_updateButton = new FormData();
		fd_updateButton.left = new FormAttachment(0, 20);
		fd_updateButton.top = new FormAttachment(0, 84);
		fd_updateButton.right = new FormAttachment(100, 0);
		updateButton.setLayoutData(fd_updateButton);
		updateButton.setText("Update");

		deleteButton = new Button(this, SWT.CHECK);
		final FormData fd_deleteButton = new FormData();
		fd_deleteButton.left = new FormAttachment(0, 20);
		fd_deleteButton.top = new FormAttachment(0, 109);
		fd_deleteButton.right = new FormAttachment(100, 0);
		deleteButton.setLayoutData(fd_deleteButton);
		deleteButton.setText("Delete");

		trackingButton = new Button(this, SWT.CHECK);
		final FormData fd_trackingButton = new FormData();
		fd_trackingButton.left = new FormAttachment(0, 20);
		fd_trackingButton.top = new FormAttachment(0, 134);
		fd_trackingButton.right = new FormAttachment(100, 0);
		trackingButton.setLayoutData(fd_trackingButton);
		trackingButton.setText("Tracking(Insert/Update/Delete)");

		trackingSelectButton = new Button(this, SWT.CHECK);
		final FormData fd_trackingSelectButton = new FormData();
		fd_trackingSelectButton.left = new FormAttachment(0, 20);
		fd_trackingSelectButton.top = new FormAttachment(0, 159);
		fd_trackingSelectButton.right = new FormAttachment(100, 0);
		trackingSelectButton.setLayoutData(fd_trackingSelectButton);
		trackingSelectButton.setText("Tracking(Select)");

		SelectionAdapter selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				tableEditor.flagModified();
				addGroupAccess();
			}
		};
		selectAllButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (selectAllButton.getSelection())
				{
					selectAllButton.setText("Deslect all");
				}
				else
				{
					selectAllButton.setText("Select all");
				}
				readButton.setSelection(selectAllButton.getSelection());
				updateButton.setSelection(selectAllButton.getSelection());
				insertButton.setSelection(selectAllButton.getSelection());
				deleteButton.setSelection(selectAllButton.getSelection());
				tableEditor.flagModified();
				addGroupAccess();
			}
		});
		readButton.addSelectionListener(selectionListener);
		updateButton.addSelectionListener(selectionListener);
		insertButton.addSelectionListener(selectionListener);
		deleteButton.addSelectionListener(selectionListener);
		trackingButton.addSelectionListener(selectionListener);
		trackingSelectButton.addSelectionListener(selectionListener);

	}


	public void setValues(String group, boolean implicitSecurityNoRights)
	{
		currentGroup = group;
		if (currentGroup != null)
		{
			if (this.securityInfo.containsKey(currentGroup))
			{
				setRights(securityInfo.get(currentGroup));
			}
			else
			{
				List<SecurityInfo> securityInfo = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getSecurityInfos(group,
					tableEditor.getTable());
				if (securityInfo == null || securityInfo.size() == 0)
				{
					setRights(implicitSecurityNoRights ? IRepository.IMPLICIT_TABLE_NO_ACCESS : IRepository.IMPLICIT_TABLE_ACCESS);
				}
				else
				{
					int i_access = securityInfo.get(0).access;
					setRights(i_access);
				}
			}
		}
	}

	private void setRights(int i_access)
	{
		readButton.setSelection(((i_access & IRepository.READ) != 0));
		insertButton.setSelection(((i_access & IRepository.INSERT) != 0));
		updateButton.setSelection(((i_access & IRepository.UPDATE) != 0));
		deleteButton.setSelection(((i_access & IRepository.DELETE) != 0));
		trackingButton.setSelection(((i_access & IRepository.TRACKING) != 0));
		trackingSelectButton.setSelection(((i_access & IRepository.TRACKING_VIEWS) != 0));
	}


	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	private void addGroupAccess()
	{
		int access = 0;
		if (readButton.getSelection())
		{
			access += IRepository.READ;
		}
		if (insertButton.getSelection())
		{
			access += IRepository.INSERT;
		}
		if (updateButton.getSelection())
		{
			access += IRepository.UPDATE;
		}
		if (deleteButton.getSelection())
		{
			access += IRepository.DELETE;
		}
		if (trackingButton.getSelection())
		{
			access += IRepository.TRACKING;
		}
		if (trackingSelectButton.getSelection())
		{
			access += IRepository.TRACKING_VIEWS;
		}
		if (currentGroup != null)
		{
			if (securityInfo.containsKey(currentGroup)) securityInfo.remove(currentGroup);
			securityInfo.put(currentGroup, new Integer(access));
		}
	}

	public void saveValues()
	{
		try
		{
			Iterator<String> groups = securityInfo.keySet().iterator();
			while (groups.hasNext())
			{
				String group = groups.next();
				Integer access = securityInfo.get(group);
				ITable t = tableEditor.getTable();
				EclipseUserManager userManager = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager();
				int writeMode = userManager.getWriteMode();
				try
				{
					userManager.setWriteMode(WorkspaceUserManager.WRITE_MODE_MANUAL);
					for (String columnName : t.getColumnNames())
					{
						userManager.setTableSecurityAccess(ApplicationServerRegistry.get().getClientId(), group, access, t.getServerName(), t.getName(),
							columnName);
					}
					userManager.writeSecurityInfo(t.getServerName(), t.getName(), false);
				}
				finally
				{
					userManager.setWriteMode(writeMode);
				}
			}
			securityInfo.clear();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}
}
