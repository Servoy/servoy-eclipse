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
package com.servoy.eclipse.ui.util;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.eclipse.ui.editors.table.ColumnsSorter;

/**
 * Editor for name-value pairs in a table view editor.
 * 
 * @author jblok
 */

public class MapEntryValueEditor extends TableViewer
{
	private final Observable observable;
	private static final int CI_NAME = 0;
	private static final int CI_VALUE = 1;
	private final ChangeSupport changeSupport;


	public MapEntryValueEditor(Composite comp, int flags)
	{
		this(comp, flags, null);
	}

	public MapEntryValueEditor(Composite comp, int flags, IRelayEditorProvider cellEditorProvider)
	{
		super(comp, flags);

		observable = new Observable();

		createTableColumns(comp, cellEditorProvider);

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

	}

	/**
	 * @return
	 */
	public IObservable getObservable()
	{
		return observable;
	}


	private void createTableColumns(Composite parent, IRelayEditorProvider cellEditorProvider)
	{
		TableColumn nameColumn = new TableColumn(getTable(), SWT.LEFT, CI_NAME);
		nameColumn.setText("Name"); //$NON-NLS-1$

		TableColumn valueColumn = new TableColumn(getTable(), SWT.LEFT, CI_VALUE);
		valueColumn.setText("Value"); //$NON-NLS-1$
		TableViewerColumn lengthViewerColumn = new TableViewerColumn(this, valueColumn);
		lengthViewerColumn.setEditingSupport(new ValueEditingSupport(this, cellEditorProvider));

		setLabelProvider(new MapEntryLabelProvider(cellEditorProvider));
		setContentProvider(new IStructuredContentProvider()
		{
			Set<Map.Entry<String, String>> content = null;

			@SuppressWarnings("unchecked")
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{
				if (newInput instanceof Set)
				{
					content = (Set<Map.Entry<String, String>>)newInput;
				}
				else if (newInput == null)
				{
					content = new HashSet<Map.Entry<String, String>>();
				}
				else
				{
					throw new IllegalArgumentException("Can only work with Set<Map.Entry>, normally retieved from .entrySet() on Map's"); //$NON-NLS-1$
				}
			}

			public Object[] getElements(Object inputElement)
			{
				if (content != null)
				{
					return content.toArray();
				}
				return null;
			}

			public void dispose()
			{
			}
		});
		ViewerSorter sorter = new ColumnsSorter(this, new TableColumn[] { nameColumn, valueColumn, }, new Comparator[] { null, null, null, null, null });
		setSorter(sorter);
		final TableColumnLayout layout = new TableColumnLayout();
		parent.setLayout(layout);
		layout.setColumnData(nameColumn, new ColumnWeightData(1, 50, true));
		layout.setColumnData(valueColumn, new ColumnWeightData(1, 50, true));
	}

	private class Observable extends AbstractObservable
	{

		/**
		 * @param realm
		 */
		public Observable()
		{
			super(Realm.getDefault());
		}


		/**
		 * @see org.eclipse.core.databinding.observable.IObservable#isStale()
		 */
		public boolean isStale()
		{
			return false;
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

	}

	class MapEntryLabelProvider extends ColumnLabelProvider
	{

		private final IRelayEditorProvider relayLabelProvider;

		/**
		 * @param relayLabelProvider
		 */
		public MapEntryLabelProvider(IRelayEditorProvider relayLabelProvider)
		{
			this.relayLabelProvider = relayLabelProvider;
		}

		@Override
		public String getColumnText(Object element, int columnIndex)
		{
			if (element instanceof Map.Entry< ? , ? >)
			{
				@SuppressWarnings("unchecked")
				Map.Entry<String, String> pi = (Map.Entry<String, String>)element;
				switch (columnIndex)
				{
					case CI_NAME :
						if (relayLabelProvider != null)
						{
							return relayLabelProvider.getLabel(pi.getKey());
						}
						else
						{
							return pi.getKey().toString();
						}
					case CI_VALUE :
						return (pi.getValue() != null ? pi.getValue().toString() : ""); //$NON-NLS-1$
					default :
						return "?"; //$NON-NLS-1$
				}
			}
			return null;
		}

	}

	class ValueEditingSupport extends EditingSupport
	{
		private final CellEditor editor;
		private final IRelayEditorProvider relayEditorProvider;
		private final TableViewer tv;

		public ValueEditingSupport(TableViewer tv, IRelayEditorProvider cellEditorProvider)
		{
			super(tv);
			this.tv = tv;
			this.relayEditorProvider = cellEditorProvider;
			editor = new TextCellEditor(tv.getTable());
		}

		@Override
		protected void setValue(Object element, Object value)
		{
			if (element instanceof Map.Entry< ? , ? >)
			{
				Object realValue = value;
				@SuppressWarnings("unchecked")
				Map.Entry<String, String> pi = (Map.Entry<String, String>)element;
				if (relayEditorProvider != null)
				{
					realValue = relayEditorProvider.convertSetValue(pi.getKey(), value);
				}
				pi.setValue((realValue != null ? realValue.toString() : "")); //$NON-NLS-1$
				changeSupport.fireEvent(new ChangeEvent(observable));
				getViewer().update(element, null);
			}
		}

		@Override
		protected Object getValue(Object element)
		{
			if (element instanceof Map.Entry< ? , ? >)
			{
				@SuppressWarnings("unchecked")
				Map.Entry<String, String> pi = (Map.Entry<String, String>)element;
				Object value = pi.getValue();
				if (relayEditorProvider != null)
				{
					value = relayEditorProvider.convertGetValue(pi.getKey(), value);
				}
				return value;
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected CellEditor getCellEditor(Object element)
		{
			CellEditor ce = editor;
			if (relayEditorProvider != null && element instanceof Map.Entry< ? , ? >)
			{
				ce = relayEditorProvider.getCellEditor(((Map.Entry<String, String>)element).getKey(), tv);
				if (ce == null) ce = editor;
			}
			return ce;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}
	}

	public interface IRelayEditorProvider
	{
		CellEditor getCellEditor(String key, TableViewer parent);

		/**
		 * @param key
		 */
		String getLabel(String key);

		/**
		 * @param element
		 * @param value
		 */
		Object convertGetValue(String key, Object value);

		/**
		 * @param element
		 * @param value
		 */
		Object convertSetValue(String key, Object value);

	}
}
