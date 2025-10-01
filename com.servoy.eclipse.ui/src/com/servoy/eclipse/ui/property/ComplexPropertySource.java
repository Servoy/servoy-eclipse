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

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Base IPropertySource implementation for properties with sub-properties (like Dimension, Point, ..).
 *
 * @author rgansevles
 */

public abstract class ComplexPropertySource<T> implements IPropertySource
{
	private final ComplexProperty<T> complexProperty;
	protected boolean readOnly = false;

	public ComplexPropertySource(ComplexProperty<T> complexProperty)
	{
		this.complexProperty = complexProperty;
	}

	public T getEditableValue()
	{
		return complexProperty == null ? null : complexProperty.getValue();
	}

	public void setReadonly(boolean readOnly)
	{
		this.readOnly = readOnly;
	}

	public final IPropertyDescriptor[] getPropertyDescriptors()
	{
		if (readOnly)
		{
			return new IPropertyDescriptor[0];
		}
		return createPropertyDescriptors();
	}

	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		return new IPropertyDescriptor[0];
	}


	public final void setPropertyValue(Object id, Object v)
	{
		complexProperty.setValue(setComplexPropertyValue(id, v));
	}

	protected T setComplexPropertyValue(@SuppressWarnings("unused") Object id, @SuppressWarnings("unused") Object v)
	{
		return null;
	}

	public Object getPropertyValue(Object id)
	{
		return null;
	}

	public boolean isPropertySet(Object id)
	{
		if (complexProperty != null)
		{
			IPropertySource propertySource = complexProperty.getPropertySource();
			if (propertySource != null)
			{
				IPropertyDescriptor[] properties = propertySource.getPropertyDescriptors();
				for (IPropertyDescriptor prop : properties)
				{
					if (prop.getId().equals(id)) return true;
				}
			}
		}
		return false;
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "[" + complexProperty + "]";
	}

}
