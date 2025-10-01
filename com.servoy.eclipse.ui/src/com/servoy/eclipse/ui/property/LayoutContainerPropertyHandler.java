/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.IYieldingType;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.StringPropertyType;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.util.Debug;

/**
 * Property handler for layout components
 *
 * @author rgansevles
 *
 */
public class LayoutContainerPropertyHandler implements IPropertyHandler
{

	private final PropertyDescription propertyDescription;

	public LayoutContainerPropertyHandler(PropertyDescription propertyDescription)
	{
		this.propertyDescription = propertyDescription;
	}

	@Override
	public String getName()
	{
		return propertyDescription.getName();
	}

	@Override
	public boolean isProperty()
	{
		return true;
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		return propertyDescription;
	}

	@Override
	public String getDisplayName()
	{
		return getName();
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return true;
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		LayoutContainer layoutContainer = (LayoutContainer)obj;
		Object value = layoutContainer.getAttribute(getName());
		try
		{
			IPropertyType< ? > type = propertyDescription.getType();
			if (type instanceof IYieldingType) type = ((IYieldingType< ? , ? >)type).getPossibleYieldType();
			if (value == null && !layoutContainer.hasProperty(getName()) && propertyDescription.hasDefault()) // default values for persist mapped properties are already handled by LayoutContainer, so value will not be null here for those
			{
				// if null is coming from parent, return it
				if (layoutContainer.getExtendsID() != null) return value;
				Object defaultValue = propertyDescription.getDefaultValue();
				if (propertyDescription.getType() instanceof IDesignValueConverter)
				{
					return ((IDesignValueConverter< ? >)propertyDescription.getType()).fromDesignValue(defaultValue, propertyDescription,
						persistContext.getPersist());
				}
				return defaultValue;
			}
			if (value != null && type instanceof StringPropertyType)
			{
				value = value.toString();
			}
		}
		catch (Exception e)
		{
			Debug.log("illegal value in layoutcontainer, ignoring it: " + value);
		}
		return value;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		((LayoutContainer)obj).putAttribute(getName(), (String)value);
	}

	public boolean shouldShow(PersistContext persistContext)
	{
		return true;
	}
}
