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

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.query.ColumnType;

public class ColumnLengthEditingSupport extends EditingSupport
{
	private final VerifyingTextCellEditor editor;

	public ColumnLengthEditingSupport(TableViewer tv)
	{
		super(tv);
		editor = new VerifyingTextCellEditor(tv.getTable());
		editor.addVerifyListener(DocumentValidatorVerifyListener.NUMBER_VERIFIER);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column column)
		{
			int length = 0;
			try
			{
				if (value != null && value.toString().length() > 0)
				{
					length = Integer.parseInt(value.toString());
				}
			}
			catch (NumberFormatException e)
			{
				ServoyLog.logError(e);
			}

			ColumnType columnType = column.getConfiguredColumnType();
			ColumnType newColumnType = ColumnType.getInstance(columnType.getSqlType(), length, columnType.getScale());
			column.getColumnInfo().setConfiguredColumnType(newColumnType);
			if (!column.getExistInDB()) column.updateColumnType(newColumnType);
			getViewer().update(column, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = ((Column)element);
			return Integer.valueOf(pi.getConfiguredColumnType().getLength()).toString();
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
		if (element instanceof Column && editor != null && !((Column)element).getExistInDB())
		{
			Column pi = ((Column)element);
			int defType = pi.getConfiguredColumnType().getSqlType();
			return defType != IColumnTypes.INTEGER && defType != IColumnTypes.DATETIME;
		}
		return false;
	}
}