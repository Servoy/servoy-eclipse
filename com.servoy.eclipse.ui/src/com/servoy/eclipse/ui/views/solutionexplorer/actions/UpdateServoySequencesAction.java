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

import static com.servoy.j2db.util.Utils.iterate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;

public class UpdateServoySequencesAction extends Action implements ISelectionChangedListener
{
	public List<IServerInternal> selectedServers = null;

	public UpdateServoySequencesAction()
	{
		setText("Update servoy sequences");
		setToolTipText("Update servoy sequences");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedServers = null;
		IStructuredSelection selection = null;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			if (s.size() == 1)
			{
				// is "Servers node" selected?
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				UserNodeType type = node.getType();
				if ((type == UserNodeType.SERVER && ((IServerInternal)node.getRealObject()).getConfig().isEnabled()) ||
					(type == UserNodeType.SERVERS && node.children != null && node.children.length > 0))
				{
					selection = s;
				}
			}
			else if (s.size() > 1)
			{
				boolean ok = true;
				Iterator<SimpleUserNode> it = s.iterator();
				while (it.hasNext() && ok)
				{
					SimpleUserNode serverNode = it.next();
					if (serverNode.getType() != UserNodeType.SERVER || !((IServerInternal)serverNode.getRealObject()).getConfig().isEnabled())
					{
						ok = false;
					}
				}
				if (ok)
				{
					selection = s;
				}
			}
		}
		if (selection != null)
		{
			selectedServers = new ArrayList<>();
			if (selection.size() == 1)
			{
				// is "Servers node" selected?
				SimpleUserNode node = (SimpleUserNode)selection.getFirstElement();
				UserNodeType type = node.getType();
				if (type == UserNodeType.SERVERS)
				{
					for (SimpleUserNode serverNode : node.children)
					{
						selectedServers.add((IServerInternal)serverNode.getRealObject());
					}
				}
				else if (type == UserNodeType.SERVER)
				{
					selectedServers.add((IServerInternal)node.getRealObject());
				}
			}
			else if (selection.size() > 1)
			{
				Iterator<SimpleUserNode> it = selection.iterator();
				while (it.hasNext())
				{
					SimpleUserNode serverNode = it.next();
					if (serverNode.getType() == UserNodeType.SERVER)
					{
						selectedServers.add((IServerInternal)serverNode.getRealObject());
					}
				}
			}
		}
		boolean showLegacyAction = false;
		if (selectedServers != null)
		{
			for (IServerInternal server : selectedServers)
			{
				try
				{
					outer : for (String tableName : server.getTableAndViewNames(true))
					{
						// Do not use uninitialized tables, it may block the UI
						ITable table = server.isTableLoaded(tableName) ? server.getTable(tableName) : null;
						if (table != null)
						{
							for (Column column : iterate(table.getRowIdentColumns()))
							{
								if (column.getColumnInfo() != null && column.getColumnInfo().getAutoEnterType() == ColumnInfo.SEQUENCE_AUTO_ENTER &&
									column.getColumnInfo().getAutoEnterSubType() == ColumnInfo.SERVOY_SEQUENCE)
								{
									// only show if already set
									showLegacyAction = true;
									break outer;
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		setEnabled(selectedServers != null && showLegacyAction);
	}

	@Override
	public void run()
	{
		if (selectedServers == null) return;
		final StringBuffer detail = new StringBuffer();

		WorkspaceJob updateJob = new WorkspaceJob("Updating servoy sequences")
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				for (IServerInternal server : selectedServers)
				{
					if (server.getConfig().isEnabled() && server.isValid())
					{
						detail.append("Updating sequences for all tables of server " + server.getName() + "\n");
						List<String> tables = null;
						try
						{
							tables = server.getTableAndViewNames(true);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
							detail.append("Error getting tables: " + e.getMessage() + "\n");
						}
						if (tables != null)
						{
							for (String tableName : tables)
							{
								try
								{
									ITable table = server.getTable(tableName);
									if (table != null)
									{
										server.syncColumnSequencesWithDB(table);
										detail.append("Table " + tableName + " sequences updated.\n");
									}
								}
								catch (Exception e)
								{
									detail.append("Failed to update sequences for table " + tableName + ": " + e.getMessage() + "\n");
									ServoyLog.logError("Error updating servoy sequences:", e);
								}
							}
						}
					}
				}
				detail.append("Done.");
				Display.getDefault().syncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog dialog = new MessageDialog(UIUtils.getActiveShell(), "Update servoy sequences", null, null,
							MessageDialog.NONE, new String[] { "OK" }, 0)
						{
							@Override
							protected Control createMessageArea(Composite parent)
							{
								final Text text = new Text(getShell(), SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY | SWT.V_SCROLL);
								text.setText(detail.toString());
								GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
								text.setLayoutData(gridData);
								return text;
							}
						};
						dialog.open();
					}
				});
				return Status.OK_STATUS;
			}
		};
		updateJob.setRule(ServoyModel.getWorkspace().getRoot());
		updateJob.setUser(true); // we want the progress to be visible in a dialog, not to stay in the status bar
		updateJob.schedule();

	}
}