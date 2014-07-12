/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;

/**
 * Base SelectionProvider adapter.
 * 
 * @author rgansevles
 */
public class SelectionProviderAdapter implements ISelectionProvider
{
	/**
	 * List of selection change listeners (element type: <code>ISelectionChangedListener</code>).
	 *
	 * @see #fireSelectionChanged
	 */
	private final ListenerList selectionChangedListeners = new ListenerList();
	protected ISelection selection;

	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
		selectionChangedListeners.add(listener);
	}

	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
		selectionChangedListeners.remove(listener);
	}

	/**
	 * Notifies any selection changed listeners that the viewer's selection has changed.
	 * Only listeners registered at the time this method is called are notified.
	 *
	 * @param event a selection changed event
	 *
	 * @see ISelectionChangedListener#selectionChanged
	 */
	protected void fireSelectionChanged(final SelectionChangedEvent event)
	{
		Object[] listeners = selectionChangedListeners.getListeners();
		for (Object listener : listeners)
		{
			final ISelectionChangedListener l = (ISelectionChangedListener)listener;
			SafeRunnable.run(new SafeRunnable()
			{
				public void run()
				{
					l.selectionChanged(event);
				}
			});
		}
	}

	public ISelection getSelection()
	{
		return selection == null ? StructuredSelection.EMPTY : selection;
	}

	public void setSelection(ISelection selection)
	{
		ISelection oldSelection = getSelection();
		this.selection = selection;
		ISelection newSelection = getSelection();
		if (oldSelection != newSelection)
		{
			fireSelectionChanged(new SelectionChangedEvent(this, newSelection));
		}
	}
}
