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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Table;

public class ColumnRowIdentEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private String[] rowIdents;

	public ColumnRowIdentEditingSupport(Table table, TableViewer tv)
	{
		super(tv);
		if (table != null && table.getExistInDB())
		{
			rowIdents = new String[Column.allDefinedRowIdents.length - 1];
			int j = 0;
			for (int element : Column.allDefinedRowIdents)
			{
				if (element != Column.PK_COLUMN)
				{
					rowIdents[j++] = Column.getFlagsString(element);
				}
			}
		}
		else
		{
			rowIdents = new String[Column.allDefinedRowIdents.length];
			for (int i = 0; i < rowIdents.length; i++)
			{
				rowIdents[i] = Column.getFlagsString(Column.allDefinedRowIdents[i]);
			}
		}
		editor = new FixedComboBoxCellEditor(tv.getTable(), rowIdents, SWT.READ_ONLY);
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
			for (int element2 : Column.allDefinedRowIdents)
			{
				if (Column.getFlagsString(element2).equals(rowIdent))
				{
					type = element2;
					break;
				}
			}
			pi.setRowIdentType(type);
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
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		// only if we have active solution
		if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
		{
			if (element instanceof Column && editor != null)
			{
				Column c = (Column)element;
				if (c.getExistInDB())
				{
					if (c.getRowIdentType() == Column.PK_COLUMN)
					{
						return false;// pk not never allowed to change, we do allow or user_ident on null column for views
					}
					else
					{
						return true;
					}
				}
				else
				{
					if (c.getTable().getExistInDB())
					{
						return false;// not possible to add non null columns and
						// null column does not allow user_ident
					}
					else
					{
						return true;
					}
				}
			}
		}

		return false;
	}
}