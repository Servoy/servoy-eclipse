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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class ColumnAllowNullEditingSupport extends EditingSupport
{
	private final CellEditor editor;

	public ColumnAllowNullEditingSupport(TableViewer tv)
	{
		super(tv);
		editor = new CheckboxCellEditor(tv.getTable());
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column column = (Column)element;
			Boolean allowNull = Boolean.parseBoolean(value.toString());
			boolean canChange = true;
			if ((column.getTable() instanceof Table) && column.getTable().getExistInDB())
			{
				if (org.eclipse.jface.dialogs.MessageDialog.openConfirm(new Shell(), "Allow null modification",
					"In order to change allow null for columns in existing table we have to drop and recreate the table. " +
						"All data in table will be lost. This could be a problem if this table is already in production and it has data, then we are not able to update that column. " +
						"Can we drop the existing table ? (save action will recreate it)"))
				{
					IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
					IServerInternal server = (IServerInternal)serverManager.getServer(column.getTable().getServerName(), true, true);
					try
					{
						server.dropTable((Table)column.getTable());
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					canChange = false;
				}
			}
			if (canChange)
			{
				column.setAllowNull(allowNull);
				getViewer().update(element, null);
				column.getTable().fireIColumnChanged(column);
			}
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			return new Boolean(pi.getAllowNull());
		}
		return null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return true;
	}
}