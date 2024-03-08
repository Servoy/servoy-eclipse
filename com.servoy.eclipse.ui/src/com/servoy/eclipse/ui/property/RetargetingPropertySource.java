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

package com.servoy.eclipse.ui.property;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.Pair;

/**
 * Base properties source for retargeting properties to items in a model.
 *
 * @author rgansevles
 *
 */
public abstract class RetargetingPropertySource implements IPropertySource
{
	private final Object model;
	protected LinkedHashMap<Object, IPropertyDescriptor> propertyDescriptors;
	protected Map<String, IPropertySource> elementPropertySources;
	private final Form context;

	protected RetargetingPropertySource(Object model, Form context)
	{
		this.model = model;
		this.context = context;
	}

	public Object getEditableValue()
	{
		return null;
	}

	public Form getContext()
	{
		return context;
	}

	/**
	 * @return the model
	 */
	protected Object getModel()
	{
		return model;
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		init();
		return propertyDescriptors.values().toArray(new IPropertyDescriptor[propertyDescriptors.size()]);
	}

	protected abstract void fillPropertyDescriptors();

	public void init()
	{
		if (propertyDescriptors == null)
		{
			propertyDescriptors = new LinkedHashMap<Object, IPropertyDescriptor>();// defines order
			elementPropertySources = new HashMap<String, IPropertySource>();

			fillPropertyDescriptors();
		}
	}


	protected void addMethodPropertyDescriptor(IRAGTEST elementPropertySource, String prefix, String propertyName)
	{
		addMethodPropertyDescriptor(elementPropertySource, prefix, propertyName, null);
	}

	protected void addMethodPropertyDescriptor(IRAGTEST elementPropertySource, String prefix, String propertyName, String displayName)
	{
		String id = prefix == null ? propertyName : prefix + '.' + propertyName;
		IPropertyDescriptor propertyDescriptor = elementPropertySource.getPropertyDescriptor(propertyName);
		if (propertyDescriptor != null)
		{
			propertyDescriptors.put(id,
				new DelegatePropertyController<Object, Object>(propertyDescriptor, id,
					displayName == null ? propertyDescriptor.getDisplayName() : displayName));
		}
	}

	private Pair<IPropertySource, String> getElementPropertySource(Object id)
	{
		init();

		String[] split = id.toString().split("\\.");
		if (split.length <= 2) // when prefix is null, property is top-level, like location
		{
			IPropertySource elementPropertySource = elementPropertySources.get(split.length == 1 ? null : split[0] /* prefix */);
			if (elementPropertySource != null)
			{
				return new Pair<IPropertySource, String>(elementPropertySource, split[split.length - 1] /* id */);
			}
		}

		return null;
	}

	public Object getPropertyValue(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			return pair.getLeft().getPropertyValue(pair.getRight());
		}

		return null;
	}

	public boolean isPropertySet(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			return pair.getLeft().isPropertySet(pair.getRight());
		}

		return false;
	}

	public void resetPropertyValue(Object id)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			pair.getLeft().resetPropertyValue(pair.getRight());
		}
	}

	public void setPropertyValue(Object id, Object value)
	{
		Pair<IPropertySource, String> pair = getElementPropertySource(id);
		if (pair != null)
		{
			pair.getLeft().setPropertyValue(pair.getRight(), value);
		}
	}
}
