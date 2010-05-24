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

import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnTypes;

public class ColumnTypeEditingSupport extends EditingSupport
{
	private final CellEditor editor;

	public ColumnTypeEditingSupport(TableViewer tv)
	{
		super(tv);
		String[] types = new String[Column.allDefinedTypes.length];
		for (int i = 0; i < types.length; i++)
		{
			types[i] = Column.getDisplayTypeString(Column.allDefinedTypes[i]);
		}
		editor = new FixedComboBoxCellEditor(tv.getTable(), types, SWT.READ_ONLY);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			int index = Integer.parseInt(value.toString());
			int type = Column.allDefinedTypes[index];
			pi.setType(type);
			int defaultType = Column.mapToDefaultType(type);
			if (defaultType == IColumnTypes.NUMBER || defaultType == IColumnTypes.MEDIA)
			{
				pi.setLenght(0);// default create unlimited
			}
			getViewer().update(element, null);
			getViewer().refresh();
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			int type = pi.getType();
			int index = 0;
			for (int i = 0; i < Column.allDefinedTypes.length; i++)
			{
				if (Column.allDefinedTypes[i] == type)
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
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		if (element instanceof Column && editor != null)
		{
			return !((Column)element).getExistInDB();
		}
		return false;
	}
}