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
package com.servoy.eclipse.designer.editor;

import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.ChangeSupport;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObservable;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Editing support for setting security checkboxes in form editor security page.
 * 
 * @author lvostinar
 */

public class ElementSettingsEditingSupport extends EditingSupport implements IObservable
{
	private final CellEditor editor;
	private final int mask;
	private final ElementSettingsModel model;

	public ElementSettingsEditingSupport(int mask, TableViewer viewer, ElementSettingsModel model)
	{
		super(viewer);
		editor = new CheckboxCellEditor(viewer.getTable());
		this.mask = mask;
		this.model = model;
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

	private final ChangeSupport changeSupport;

	public void addChangeListener(IChangeListener listener)
	{
		changeSupport.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		changeSupport.removeChangeListener(listener);
	}


	@Override
	protected boolean canEdit(Object element)
	{
		if (mask == IRepository.ACCESSIBLE && !model.hasRight((IPersist)element, IRepository.VIEWABLE)) return false;
		return true;
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof IPersist)
		{
			return model.hasRight((IPersist)element, mask);
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof IPersist && value != null)
		{
			IPersist persist = (IPersist)element;
			Boolean right = Boolean.parseBoolean(value.toString());
			model.setRight(right, persist, mask);
			changeSupport.fireEvent(new ChangeEvent(ElementSettingsEditingSupport.this));
			getViewer().update(element, null);
		}

	}

	public void addStaleListener(IStaleListener listener)
	{

	}

	public void dispose()
	{

	}

	public Realm getRealm()
	{
		return Realm.getDefault();
	}

	public boolean isStale()
	{

		return false;
	}

	public void removeStaleListener(IStaleListener listener)
	{

	}

	public void addDisposeListener(IDisposeListener listener)
	{

	}

	public boolean isDisposed()
	{
		return false;
	}

	public void removeDisposeListener(IDisposeListener listener)
	{

	}
}
