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

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.query.ColumnType;

public class ColumnTypeEditingSupport extends EditingSupport
{
	public class ColumnTypeEditingObservable extends AbstractObservable
	{

		public ColumnTypeEditingObservable(Realm realm)
		{
			super(realm);
		}

		@Override
		public void addChangeListener(IChangeListener listener)
		{
			changeSupport.addChangeListener(listener);
		}

		@Override
		public void removeChangeListener(IChangeListener listener)
		{
			changeSupport.removeChangeListener(listener);
		}

		public boolean isStale()
		{
			return false;
		}

		public ColumnTypeEditingSupport getEditingSupport()
		{
			return ColumnTypeEditingSupport.this;
		}
	}

	private final CellEditor editor;
	private final ChangeSupport changeSupport;
	private final IObservable observable;
	private Column column;

	public ColumnTypeEditingSupport(TableViewer tv)
	{
		super(tv);
		String[] types = new String[Column.allDefinedTypes.length];
		for (int i = 0; i < types.length; i++)
		{
			types[i] = Column.getDisplayTypeString(Column.allDefinedTypes[i]);
		}
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
		changeSupport = new ChangeSupport(Realm.getDefault())
		{
			@Override
			protected void lastListenerRemoved()
			{
			}

			@Override
			protected void firstListenerAdded()
			{
			}
		};
		observable = new ColumnTypeEditingObservable(Realm.getDefault());
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof Column)
		{
			Column pi = (Column)element;
			column = pi;
			int type = Column.allDefinedTypes[Integer.parseInt(value.toString())];

			int length = pi.getConfiguredColumnType().getLength();

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
			pi.getColumnInfo().setConfiguredColumnType(ColumnType.getInstance(type, length, 0));
			if (!pi.getExistInDB()) pi.updateColumnType(type, length, 0);

			getViewer().update(element, null);
			getViewer().refresh();
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof Column)
		{
			int type = Column.mapToDefaultType(((Column)element).getConfiguredColumnType().getSqlType());
			int index = 0;
			for (int i = 0; i < Column.allDefinedTypes.length; i++)
			{
				if (Column.allDefinedTypes[i] == type)
				{
					index = i;
					break;
				}
			}
			return Integer.valueOf(index);
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
		changeSupport.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		changeSupport.removeChangeListener(listener);
	}

	public Column getColumn()
	{
		return column;
	}
}