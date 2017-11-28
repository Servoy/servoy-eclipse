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

import java.util.Collection;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;

public class FlagTenantColumnAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	public FlagTenantColumnAction(SolutionExplorerView sev)
	{
		viewer = sev;
		setText("Flag Tenant Columns");
		setToolTipText("Flag Tenant Columns");
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
			InputDialog columnNameDialog = new InputDialog(viewer.getSite().getShell(), "Flag tenant columns", "Specify column name", "", null);
			columnNameDialog.setBlockOnOpen(true);
			columnNameDialog.open();
			if (columnNameDialog.getReturnCode() == Window.OK && !columnNameDialog.getValue().isEmpty())
			{
				String columnName = columnNameDialog.getValue();
				try
				{
					final IServerInternal s = (IServerInternal)node.getRealObject();
					List<String> tableNames = s.getTableNames(true);
					for (String tableName : tableNames)
					{
						boolean changed = false;
						ITable table = s.getTable(tableName);
						Collection<Column> columns = table.getColumns();
						for (Column column : columns)
						{
							if (column.getName().equals(columnName) || column.getDataProviderID().equals(columnName))
							{
								column.setFlag(IBaseColumn.TENANT_COLUMN, true);
								changed = true;
							}
						}
						if (changed) s.updateAllColumnInfo(table);
					}

				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}
}
