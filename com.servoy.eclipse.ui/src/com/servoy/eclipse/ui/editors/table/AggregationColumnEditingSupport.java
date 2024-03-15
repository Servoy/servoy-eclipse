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

import java.util.Iterator;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.ITable;

public class AggregationColumnEditingSupport extends EditingSupport
{
	private final ITable table;
	private final CellEditor editor;
	private String[] columns;
	private final ChangeSupportObservable observable;

	public AggregationColumnEditingSupport(ITable table, TreeViewer tv)
	{
		super(tv);
		this.table = table;
		updateColumns();
		observable = new ChangeSupportObservable(new SimpleChangeSupport());
		editor = new ComboBoxCellEditor(tv.getTree(), columns, SWT.READ_ONLY);
	}

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof AggregateVariable && table != null)
		{
			AggregateVariable aggregateVariable = (AggregateVariable)element;
			int index = Integer.parseInt(value.toString());
			IColumn column = table.getColumn(columns[index]);
			if (!column.getDataProviderID().equals(aggregateVariable.getDataProviderIDToAggregate()))
			{
				aggregateVariable.setDataProviderIDToAggregate(column.getDataProviderID());
				observable.fireChangeEvent();
				getViewer().update(element, null);
			}
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof AggregateVariable)
		{
			AggregateVariable aggregateVariable = (AggregateVariable)element;
			String dataProviderId = aggregateVariable.getDataProviderIDToAggregate();
			int index = 0;
			try
			{
				Iterator<Column> it = EditorUtil.getTableColumns(table, true);
				int i = 0;
				while (it.hasNext())
				{
					Column column = it.next();
					if (column.getDataProviderID().equals(dataProviderId))
					{
						index = i;
						break;
					}
					i++;
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			return new Integer(index);
		}
		return null;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		updateColumns();
		CCombo combo = (CCombo)editor.getControl();
		combo.setItems(columns);
		return editor;
	}

	private void updateColumns()
	{
		columns = new String[table.getColumnCount()];
		try
		{
			Iterator<Column> it = EditorUtil.getTableColumns(table, true);
			int i = 0;
			while (it.hasNext())
			{
				columns[i++] = it.next().getName();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	protected boolean canEdit(Object element)
	{
		if (element instanceof AggregateVariable) return true;
		else return false;
	}
}
