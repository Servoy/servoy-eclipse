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

import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.query.ColumnType;

public class ColumnTypeEditingSupport extends EditingSupport
{
	private static final String[] types;
	static
	{
		int length = Column.allDefinedTypes.length;
		types = new String[length + 4];
		for (int i = 0; i < length; i++)
		{
			types[i] = Column.getDisplayTypeString(Column.allDefinedTypes[i]);
		}
		types[length++] = ColumnLabelProvider.UUID_MEDIA_16;
		types[length++] = ColumnLabelProvider.UUID_TEXT_36;
		types[length++] = ColumnLabelProvider.UUID_NATIVE;
		types[length++] = ColumnLabelProvider.VECTOR;

	}

	public class ColumnTypeEditingObservable extends ChangeSupportObservable
	{
		public ColumnTypeEditingObservable(ChangeSupport changeSupport)
		{
			super(changeSupport);
		}

		public ColumnTypeEditingSupport getEditingSupport()
		{
			return ColumnTypeEditingSupport.this;
		}
	}

	private final CellEditor editor;
	private Column column;
	private final TableViewer tv;
	private final ColumnTypeEditingObservable observable;

	public ColumnTypeEditingSupport(TableViewer tv)
	{
		super(tv);
		this.tv = tv;
		editor = new FixedComboBoxCellEditor(tv.getTable(), types, SWT.READ_ONLY);
		Control control = editor.getControl();
		CCombo c = (CCombo)control;
		c.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				editor.deactivate();
			}
		});
		observable = new ColumnTypeEditingObservable(new SimpleChangeSupport());
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			column = pi;
			int length = pi.getConfiguredColumnType().getLength();
			int type;

			int selectedIndex = Integer.parseInt(value.toString());
			if (column.hasFlag(IBaseColumn.NATIVE_COLUMN))
			{
				column.setFlag(IBaseColumn.NATIVE_COLUMN, false);
			}
			if (column.hasFlag(IBaseColumn.VECTOR_COLUMN))
			{
				column.setFlag(IBaseColumn.VECTOR_COLUMN, false);
			}
			if (types[selectedIndex] == ColumnLabelProvider.UUID_MEDIA_16 || types[selectedIndex] == ColumnLabelProvider.UUID_NATIVE)
			{
				type = IColumnTypes.MEDIA;
				length = 16;
				column.setFlag(IBaseColumn.UUID_COLUMN, true);
				column.setFlag(IBaseColumn.NATIVE_COLUMN, types[selectedIndex] == ColumnLabelProvider.UUID_NATIVE);

			}
			else if (types[selectedIndex] == ColumnLabelProvider.UUID_TEXT_36)
			{
				type = IColumnTypes.TEXT;
				length = 36;
				column.setFlag(IBaseColumn.UUID_COLUMN, true);
			}
			else if (types[selectedIndex] == ColumnLabelProvider.VECTOR)
			{
				type = IColumnTypes.MEDIA;
				column.setFlag(IBaseColumn.VECTOR_COLUMN, true);
				column.setFlag(IBaseColumn.NATIVE_COLUMN, true);
				column.setFlag(IBaseColumn.UUID_COLUMN, false);
			}
			else
			{
				type = Column.allDefinedTypes[selectedIndex];

				// if sequence type is uuid generator automaticaly fill MEDIA with length 16 and TEXT with length 36
				if (pi.getSequenceType() == ColumnInfo.UUID_GENERATOR && !pi.getExistInDB())
				{
					if (type == IColumnTypes.TEXT)
					{
						length = 36;
					}
					else if (type == IColumnTypes.MEDIA)
					{
						length = 16;
					}
				}
				else if (type == IColumnTypes.NUMBER || type == IColumnTypes.MEDIA)
				{
					// default create unlimited
					length = 0;
				}
				if (column.hasFlag(IBaseColumn.UUID_COLUMN) && !(type == IColumnTypes.MEDIA || type == IColumnTypes.TEXT))
				{
					column.setFlag(IBaseColumn.UUID_COLUMN, false);
				}
			}
			ColumnType newColumnType = ColumnType.getInstance(type, length, 0);
			if (pi.getColumnInfo() != null) pi.getColumnInfo().setConfiguredColumnType(newColumnType);
			if (!pi.getExistInDB()) pi.updateColumnType(newColumnType);

			getViewer().update(element, null);
			getViewer().refresh();
			//trigger a selection event so that Details, Auto Enter, Validation and Conversion get updated with a correct Column object
			tv.setSelection(tv.getSelection(), false);
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			Column col = (Column)element;
			if (col.hasFlag(IBaseColumn.NATIVE_COLUMN) && col.hasFlag(IBaseColumn.UUID_COLUMN))
			{
				return Integer.valueOf(Column.allDefinedTypes.length + 2);
			}
			if (col.hasFlag(IBaseColumn.NATIVE_COLUMN) && col.hasFlag(IBaseColumn.VECTOR_COLUMN))
			{
				return Integer.valueOf(Column.allDefinedTypes.length + 3);
			}
			if (col.hasFlag(IBaseColumn.UUID_COLUMN))
			{
				int type = Column.mapToDefaultType(col.getConfiguredColumnType().getSqlType());
				if (type == IColumnTypes.MEDIA)
				{
					return Integer.valueOf(Column.allDefinedTypes.length);
				}
				return Integer.valueOf(Column.allDefinedTypes.length + 1);
			}
			int type = Column.mapToDefaultType(col.getConfiguredColumnType().getSqlType());
			for (int i = 0; i < Column.allDefinedTypes.length; i++)
			{
				if (Column.allDefinedTypes[i] == type)
				{
					return Integer.valueOf(i);
				}
			}
			return Integer.valueOf(0);
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

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	public Column getColumn()
	{
		return column;
	}
}