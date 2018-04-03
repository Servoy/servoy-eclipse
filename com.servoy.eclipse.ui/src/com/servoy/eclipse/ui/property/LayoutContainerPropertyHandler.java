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
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.LayoutContainer;

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
		return ((LayoutContainer)obj).getAttribute(getName());
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
