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
package com.servoy.eclipse.ui.property;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Holder for property value with sub-properties (like Dimension, Point, ..)..
 *
 * @author rgansevles
 */

public class ComplexProperty<T> implements IAdaptable
{

	private T value;

	/**
	 * @param value
	 */
	public ComplexProperty(T value)
	{
		this.value = value;
	}

	public T getValue()
	{
		return value;
	}

	public void setValue(T value)
	{
		this.value = value;
	}

	public Object getAdapter(Class type)
	{
		if (type == IPropertySource.class) return getPropertySource();
		return null;
	}

	protected IPropertySource getPropertySource()
	{
		return null;
	}

	/**
	 * Convert from base object to ComplexProperty wrapper.
	 *
	 * @author rgansevles
	 *
	 * @param <T>
	 */
	public static abstract class ComplexPropertyConverter<T> implements IPropertyConverter<T, Object>
	{

		public abstract Object convertProperty(Object id, T value);

		public T convertValue(Object id, Object value)
		{
			if (value instanceof ComplexProperty)
			{
				return ((ComplexProperty<T>)value).getValue();
			}
			// this happens when complex value is edited directly in the cell-editor, an not via one of the child-nodes.
			return (T)value;
		}
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + value + "]";
	}
}
