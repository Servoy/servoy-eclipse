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
package com.servoy.eclipse.ui.dialogs;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;

import com.servoy.j2db.dataprocessing.SortColumn;


public class SortColumnEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private final boolean ascendingColumn;

	public SortColumnEditingSupport(TableViewer tv, boolean ascendingColumn)
	{
		super(tv);
		editor = new CheckboxCellEditor(tv.getTable());
		this.ascendingColumn = ascendingColumn;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof SortColumn)
		{
			SortColumn sortColumn = (SortColumn)element;
			Boolean sort = Boolean.parseBoolean(value.toString());
			if ((sort.booleanValue() && ascendingColumn) || (!sort.booleanValue() && !ascendingColumn)) sortColumn.setSortOrder(SortColumn.ASCENDING);
			else sortColumn.setSortOrder(SortColumn.DESCENDING);
			getViewer().update(element, null);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof SortColumn)
		{
			SortColumn sortColumn = (SortColumn)element;
			return new Boolean((sortColumn.getSortOrder() == SortColumn.ASCENDING && ascendingColumn) ||
				(sortColumn.getSortOrder() != SortColumn.ASCENDING && !ascendingColumn));
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