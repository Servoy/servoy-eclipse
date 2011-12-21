/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.editors.table.RelayEditorProvider;
import com.servoy.eclipse.ui.util.MapEntryValueEditor;
import com.servoy.j2db.dataprocessing.IBaseConverter;
import com.servoy.j2db.dataprocessing.IPropertyDescriptorProvider;
import com.servoy.j2db.persistence.Column;

/**
 * General composite for editing converters.
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public abstract class ConvertersComposite<T extends IBaseConverter> extends Composite
{
	private final ComboViewer convertersCombo;
	private final MapEntryValueEditor tableViewer;

	private final Table table;
	private final Composite tableContainer;
	private final RelayEditorProvider relayEditorProvider;

	private final ListenerList selectionChangedListeners = new ListenerList();
	private boolean adjusting = false;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public ConvertersComposite(Composite parent, int style)
	{
		super(parent, style);

		convertersCombo = new ComboViewer(this);
		Combo combo = convertersCombo.getCombo();
		UIUtils.setDefaultVisibleItemCount(combo);

		convertersCombo.setContentProvider(ArrayContentProvider.getInstance());
		convertersCombo.setLabelProvider(new LabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element instanceof IBaseConverter)
				{
					return ((IBaseConverter)element).getName();
				}
				return String.valueOf(element);
			}
		});

		convertersCombo.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(final SelectionChangedEvent event)
			{
				if (!adjusting)
				{
					Object[] listeners = selectionChangedListeners.getListeners();
					for (int i = 0; i < listeners.length; ++i)
					{
						final ISelectionChangedListener l = (ISelectionChangedListener)listeners[i];
						SafeRunnable.run(new SafeRunnable()
						{
							public void run()
							{
								l.selectionChanged(event);
							}
						});
					}
				}
			}
		});

		tableContainer = new Composite(this, SWT.NONE);
		relayEditorProvider = new RelayEditorProvider();
		tableViewer = new MapEntryValueEditor(tableContainer, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION, relayEditorProvider);
		table = tableViewer.getTable();
		table.setLinesVisible(true);
		table.setHeaderVisible(true);
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING, tableContainer, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE).add(
					GroupLayout.LEADING, combo, GroupLayout.PREFERRED_SIZE, 482, Short.MAX_VALUE)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).addPreferredGap(
				LayoutStyle.RELATED).add(tableContainer, GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE).addContainerGap()));
		setLayout(groupLayout);
	}

	public void addConverterChangedListener(ISelectionChangedListener listener)
	{
		selectionChangedListeners.add(listener);
	}

	public void removeConverterChangedListener(ISelectionChangedListener listener)
	{
		selectionChangedListeners.remove(listener);
	}

	public void addPropertyChangeListener(IChangeListener listener)
	{
		tableViewer.getObservable().addChangeListener(listener);
	}

	public void removePropertyChangeListener(IChangeListener listener)
	{
		tableViewer.getObservable().removeChangeListener(listener);
	}

	protected abstract int[] getSupportedTypes(T converter);

	public void setConverters(int columnType, String selected, Collection<T> converters)
	{
		int mappedType = Column.mapToDefaultType(columnType);
		Object selectedConverter = "none";
		List<Object> input = new ArrayList<Object>();
		input.add(selectedConverter);

		for (T conv : converters)
		{
			boolean match = false;
			int[] conv_types = getSupportedTypes(conv);
			if (conv_types == null)
			{
				match = true;
			}
			else
			{
				for (int element : conv_types)
				{
					if (mappedType == Column.mapToDefaultType(element))
					{
						match = true;
					}
				}
			}
			if (match)
			{
				input.add(conv);
				if (conv.getName().equals(selected))
				{
					selectedConverter = conv;
				}
			}
		}

		adjusting = true;
		try
		{
			convertersCombo.setInput(input);
			convertersCombo.setSelection(new StructuredSelection(selectedConverter));
		}
		finally
		{
			adjusting = false;
		}
	}

	public T getSelectedConverter()
	{
		IStructuredSelection selection = (IStructuredSelection)convertersCombo.getSelection();
		if (selection.getFirstElement() instanceof IBaseConverter)
		{
			return (T)selection.getFirstElement();
		}
		return null; // none
	}

	public void setProperties(Map<String, String> props)
	{
		Map<String, String> newProps = new HashMap<String, String>();

		T conv = getSelectedConverter();
		if (conv != null)
		{
			//make sure it lists all defaults
			if (conv.getDefaultProperties() != null)
			{
				newProps.putAll(conv.getDefaultProperties());
			}

			if (props != null)
			{
				newProps.putAll(props);
			}
		}

		tableViewer.setInput(newProps.entrySet());
		if (newProps.size() > 0) tableViewer.getTable().select(0);

		relayEditorProvider.setPropertyDescriptorProvider(conv instanceof IPropertyDescriptorProvider ? (IPropertyDescriptorProvider)conv : null);
	}

	public Map<String, String> getProperties()
	{
		Map<String, String> props = null;
		for (Entry<String, String> p : (Set<Entry<String, String>>)tableViewer.getInput())
		{
			if (props == null)
			{
				props = new HashMap<String, String>();
			}
			props.put(p.getKey(), p.getValue());
		}
		return props;
	}
}
