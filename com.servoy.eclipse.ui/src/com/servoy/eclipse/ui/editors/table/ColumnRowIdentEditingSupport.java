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
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.DataSourceUtils;

public class ColumnRowIdentEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private String[] rowIdents;
	private final ITable table;

	public ColumnRowIdentEditingSupport(ITable table, TableViewer tv)
	{
		super(tv);
		this.table = table;
		editor = new FixedComboBoxCellEditor(tv.getTable(), getItems(), SWT.READ_ONLY);
	}

	private String[] getItems()
	{
		if (table != null && table.getExistInDB() && DataSourceUtils.getDBServernameTablename(table.getDataSource()) != null)
		{
			rowIdents = new String[IBaseColumn.allDefinedRowIdents.length - 1];
			int j = 0;
			for (int el : IBaseColumn.allDefinedRowIdents)
			{
				if (el != IBaseColumn.PK_COLUMN)
				{
					rowIdents[j++] = Column.getFlagsString(el);
				}
			}
		}
		else
		{
			rowIdents = new String[IBaseColumn.allDefinedRowIdents.length];
			for (int i = 0; i < rowIdents.length; i++)
			{
				rowIdents[i] = Column.getFlagsString(IBaseColumn.allDefinedRowIdents[i]);
			}
		}
		return rowIdents;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			int index = Integer.parseInt(value.toString());
			String rowIdent = rowIdents[index];
			int type = 0;
			for (int element2 : IBaseColumn.allDefinedRowIdents)
			{
				if (Column.getFlagsString(element2).equals(rowIdent))
				{
					type = element2;
					break;
				}
			}
			if (type != 0 && pi.getAllowNull())
			{
				MessageBox dialog = new MessageBox(UIUtils.getActiveShell(), SWT.ICON_WARNING | SWT.OK);
				dialog.setText("Warning");
				dialog.setMessage(
					"Row identifiers should always be not null. \nIf you really need this column to be a row identifier you should make sure the contents of this column is always not null ");
				dialog.open();
			}
			boolean initialAllowNull = pi.getAllowNull();
			pi.setRowIdentType(type);
			if (type != 0 && !pi.getAllowNull() && initialAllowNull) pi.setAllowNull(true); // when a new column is added, force setting Allow Null to true even if Row Ident is set on column
			getViewer().update(element, null);
			pi.flagColumnInfoChanged();
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			int type = pi.getRowIdentType();
			int index = 0;
			for (int i = 0; i < rowIdents.length; i++)
			{
				if (rowIdents[i].equals(Column.getFlagsString(type)))
				{
					index = i;
					break;
				}
			}
			return new Integer(index);
		}
		return null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		((FixedComboBoxCellEditor)editor).setItems(getItems());
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		// only if we have active solution
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null && element instanceof Column && editor != null)
		{
			Column c = (Column)element;
			// for in mem and view foundsets this will always be false..
			if (c.getExistInDB())
			{
				// pk never allowed to change, we do allow for user_ident on nullable column
				return c.getRowIdentType() != IBaseColumn.PK_COLUMN;
			}
			else
			{
				// not possible to add non null columns to existing table
				return true;//!c.getTable().getExistInDB();
			}
		}

		return false;
	}
}