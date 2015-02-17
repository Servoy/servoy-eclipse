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
import org.eclipse.swt.widgets.Label;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;

public class TableSettingsComposite extends Group
{

	private final Button implicitButton;

	private final Button explicitButton;

	private final Label implicitLabel;

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

		implicitButton = new Button(this, SWT.RADIO);
		FormData fd_implicitButton;
		fd_implicitButton = new FormData();
		implicitButton.setLayoutData(fd_implicitButton);
		implicitButton.setText("Implicit:");

		explicitButton = new Button(this, SWT.RADIO);
		final FormData fd_explicitButton = new FormData();
		fd_explicitButton.right = new FormAttachment(100, 0);
		fd_explicitButton.left = new FormAttachment(0, 9);
		fd_explicitButton.top = new FormAttachment(0, 84);
		fd_explicitButton.bottom = new FormAttachment(0, 104);
		explicitButton.setLayoutData(fd_explicitButton);
		explicitButton.setText("Explicit:");

		implicitLabel = new Label(this, SWT.WRAP);
		fd_implicitButton.top = new FormAttachment(implicitLabel, -20, SWT.TOP);
		fd_implicitButton.bottom = new FormAttachment(implicitLabel, 0, SWT.TOP);
		fd_implicitButton.left = new FormAttachment(0, 9);
		final FormData fd_implicitLabel = new FormData();
		fd_implicitLabel.bottom = new FormAttachment(explicitButton, -5, SWT.TOP);
		fd_implicitLabel.right = new FormAttachment(100, 0);
		fd_implicitLabel.left = new FormAttachment(0, 24);
		fd_implicitLabel.top = new FormAttachment(0, 21);
		implicitLabel.setLayoutData(fd_implicitLabel);
		implicitLabel.setText("Default security (read, insert, update, and delete) unless overridden by another module.");

		readButton = new Button(this, SWT.CHECK);
		//fd_implicitButton.right = new FormAttachment(readButton, 0, SWT.RIGHT);
		fd_implicitButton.right = new FormAttachment(100, 0);
		final FormData fd_readButton = new FormData();
		fd_readButton.left = new FormAttachment(0, 20);
		fd_readButton.top = new FormAttachment(0, 109);
		fd_readButton.bottom = new FormAttachment(0, 129);
		fd_readButton.right = new FormAttachment(100, 0);
		readButton.setLayoutData(fd_readButton);
		readButton.setText("Read");

		insertButton = new Button(this, SWT.CHECK);
		final FormData fd_insertButton = new FormData();
		fd_insertButton.right = new FormAttachment(100, 0);
		fd_insertButton.left = new FormAttachment(0, 20);
		fd_insertButton.top = new FormAttachment(0, 134);
		fd_insertButton.bottom = new FormAttachment(0, 154);
		insertButton.setLayoutData(fd_insertButton);
		insertButton.setText("Insert");

		updateButton = new Button(this, SWT.CHECK);
		final FormData fd_updateButton = new FormData();
		fd_updateButton.left = new FormAttachment(0, 20);
		fd_updateButton.right = new FormAttachment(100, 0);
		fd_updateButton.bottom = new FormAttachment(0, 179);
		fd_updateButton.top = new FormAttachment(0, 159);
		updateButton.setLayoutData(fd_updateButton);
		updateButton.setText("Update");

		deleteButton = new Button(this, SWT.CHECK);
		final FormData fd_deleteButton = new FormData();
		fd_deleteButton.left = new FormAttachment(0, 20);
		fd_deleteButton.right = new FormAttachment(100, 0);
		fd_deleteButton.bottom = new FormAttachment(0, 204);
		fd_deleteButton.top = new FormAttachment(0, 184);
		deleteButton.setLayoutData(fd_deleteButton);
		deleteButton.setText("Delete");

		trackingButton = new Button(this, SWT.CHECK);
		final FormData fd_trackingButton = new FormData();
		fd_trackingButton.left = new FormAttachment(0, 20);
		fd_trackingButton.right = new FormAttachment(100, 0);
		fd_trackingButton.bottom = new FormAttachment(0, 229);
		fd_trackingButton.top = new FormAttachment(0, 209);
		trackingButton.setLayoutData(fd_trackingButton);
		trackingButton.setText("Tracking(Insert/Update/Delete)");

		trackingSelectButton = new Button(this, SWT.CHECK);
		final FormData fd_trackingSelectButton = new FormData();
		fd_trackingSelectButton.left = new FormAttachment(0, 20);
		fd_trackingSelectButton.right = new FormAttachment(100, 0);
		fd_trackingSelectButton.bottom = new FormAttachment(0, 254);
		fd_trackingSelectButton.top = new FormAttachment(0, 234);
		trackingSelectButton.setLayoutData(fd_trackingSelectButton);
		trackingSelectButton.setText("Tracking(Select)");

		setDefaultValues();
		implicitButton.setEnabled(false);
		explicitButton.setEnabled(false);
		implicitLabel.setEnabled(false);

		SelectionAdapter selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				tableEditor.flagModified();
				addGroupAccess();
			}
		};
		implicitButton.addSelectionListener(selectionListener);
		explicitButton.addSelectionListener(selectionListener);
		readButton.addSelectionListener(selectionListener);
		updateButton.addSelectionListener(selectionListener);
		insertButton.addSelectionListener(selectionListener);
		deleteButton.addSelectionListener(selectionListener);
		trackingButton.addSelectionListener(selectionListener);
		trackingSelectButton.addSelectionListener(selectionListener);

	}

	private void setDefaultValues()
	{
		explicitButton.setSelection(false);
		implicitButton.setSelection(true);
		readButton.setSelection(true);
		insertButton.setSelection(true);
		updateButton.setSelection(true);
		deleteButton.setSelection(true);
		trackingButton.setSelection(false);
		trackingSelectButton.setSelection(false);
		enableControls(false);
	}

	public void enableControls(boolean enable)
	{
		readButton.setEnabled(enable);
		insertButton.setEnabled(enable);
		updateButton.setEnabled(enable);
		deleteButton.setEnabled(enable);
		trackingButton.setEnabled(enable);
		trackingSelectButton.setEnabled(enable);
		if (enable) enableTracking();
	}

	public void setValues(String group)
	{
		implicitButton.setEnabled(true);
		explicitButton.setEnabled(true);
		implicitLabel.setEnabled(true);
		currentGroup = group;
		if (currentGroup != null)
		{
			if (this.securityInfo.containsKey(currentGroup))
			{
				int i_access = securityInfo.get(currentGroup);
				if (i_access == IRepository.IMPLICIT_TABLE_ACCESS)
				{
					setDefaultValues();
				}
				else
				{
					setRights(i_access);
				}
			}
			else
			{
				List<SecurityInfo> securityInfo = ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager().getSecurityInfos(group,
					tableEditor.getTable());
				if (securityInfo == null || securityInfo.size() == 0)
				{
					setDefaultValues();
				}
				else
				{
					int i_access = securityInfo.get(0).access;
					setRights(i_access);
				}
			}
		}
		else
		{
			setDefaultValues();
		}
	}

	private void setRights(int i_access)
	{
		enableControls(true);
		implicitButton.setSelection(false);
		explicitButton.setSelection(true);
		readButton.setSelection(((i_access & IRepository.READ) != 0));
		insertButton.setSelection(((i_access & IRepository.INSERT) != 0));
		updateButton.setSelection(((i_access & IRepository.UPDATE) != 0));
		deleteButton.setSelection(((i_access & IRepository.DELETE) != 0));
		trackingButton.setSelection(((i_access & IRepository.TRACKING) != 0));
		trackingSelectButton.setSelection(((i_access & IRepository.TRACKING_VIEWS) != 0));
	}

	private void enableTracking()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IServer s = ServoyModel.getServerManager().getLogServer();
		if (s != null)
		{
			try
			{
				ITable log = s.getTable("log");
				if (log != null)
				{
					trackingButton.setEnabled(true);
					trackingSelectButton.setEnabled(true);
					return;
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}

		}
		trackingButton.setEnabled(false);
		trackingSelectButton.setEnabled(false);
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	private void addGroupAccess()
	{
		int access = 0;
		if (implicitButton.getSelection())
		{
			access = IRepository.READ + IRepository.INSERT + IRepository.UPDATE + IRepository.DELETE;
			enableControls(false);
		}
		else
		{
			enableControls(true);
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
				Table t = tableEditor.getTable();
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
