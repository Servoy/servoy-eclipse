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

import com.servoy.j2db.persistence.Column;

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
			Column pi = (Column)element;
			Boolean allowNull = Boolean.parseBoolean(value.toString());
			pi.setAllowNull(allowNull);
			getViewer().update(element, null);
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
		if (element instanceof Column && editor != null)
		{
			Column c = (Column)element;
			if (c.getExistInDB())
			{
				return false;
			}
			else
			{
				if (c.getTable().getExistInDB())
				{
					return false; // how whould you fill the not null column for all existing rows?
				}
				else
				{
					return true;
				}
			}
		}
		return false;
	}
}