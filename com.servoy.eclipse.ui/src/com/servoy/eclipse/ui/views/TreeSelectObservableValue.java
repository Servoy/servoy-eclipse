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
package com.servoy.eclipse.ui.views;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

/**
 * Observable for TreeSelectViewer, can be used to use automatic data binding with a TreeSelectViewer.
 * 
 * @author rgansevles
 */

public class TreeSelectObservableValue extends AbstractObservableValue
{
	private final TreeSelectViewer treeSelectViewer;
	private Object currentValue;
	private final Class type;
	private boolean updating = false;
	private final ISelectionChangedListener selectionChangedListener;

	public TreeSelectObservableValue(TreeSelectViewer treeSelectViewer, Class type)
	{
		this.treeSelectViewer = treeSelectViewer;
		this.type = type;
		treeSelectViewer.getControl().addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent e)
			{
				TreeSelectObservableValue.this.dispose();
			}
		});
		treeSelectViewer.addSelectionChangedListener(selectionChangedListener = new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				if (!updating)
				{
					Object oldValue = currentValue;
					currentValue = doGetValue();
					fireValueChange(Diffs.createValueDiff(oldValue, currentValue));
				}
			}
		});
	}

	public Object getValueType()
	{
		return type;
	}

	@Override
	protected void doSetValue(Object value)
	{
		try
		{
			updating = true;
			treeSelectViewer.setSelection(value == null ? StructuredSelection.EMPTY : new StructuredSelection(value));
			currentValue = value;
		}
		finally
		{
			updating = false;
		}
	}

	@Override
	protected Object doGetValue()
	{
		ISelection selection = TreeSelectObservableValue.this.treeSelectViewer.getSelection();
		if (selection instanceof IStructuredSelection && !((IStructuredSelection)selection).isEmpty())
		{
			return ((IStructuredSelection)selection).getFirstElement();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.databinding.observable.value.AbstractObservableValue#dispose()
	 */
	@Override
	public synchronized void dispose()
	{
		super.dispose();
		if (selectionChangedListener != null && !treeSelectViewer.getControl().isDisposed())
		{
			treeSelectViewer.removeSelectionChangedListener(selectionChangedListener);
		}
	}
}
