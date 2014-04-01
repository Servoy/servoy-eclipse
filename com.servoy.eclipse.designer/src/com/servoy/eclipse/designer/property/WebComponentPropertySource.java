/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.property;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.BeanPropertyHandler;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.component.WebComponentSpec;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;

/**
 * Properties for ngclient components.
 * 
 * @author rgansevles
 *
 */
public class WebComponentPropertySource extends PersistPropertySource
{
	private static final Map<String, PropertyDescriptor> BEAN_PROPERTIES = new HashMap<String, PropertyDescriptor>();
	static
	{
		BeanInfo info;
		try
		{
			info = java.beans.Introspector.getBeanInfo(Bean.class);
			for (PropertyDescriptor desc : info.getPropertyDescriptors())
			{
				if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(desc.getName()) ||
					StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName().equals(desc.getName()))
				{
					BEAN_PROPERTIES.put(desc.getName(), desc);
				}
			}
		}
		catch (IntrospectionException e)
		{
			ServoyLog.logError("Could not load sttaic bean properties", e);
		}
	}

	private final WebComponentSpec webComponentSpec;

	public WebComponentPropertySource(PersistContext persistContext, boolean readonly, WebComponentSpec webComponentSpec)
	{
		super(persistContext, readonly);
		if (!(persistContext.getPersist() instanceof Bean))
		{
			throw new IllegalArgumentException();
		}
		this.webComponentSpec = webComponentSpec;
	}

	@Override
	protected Object getValueObject(FlattenedSolution flattenedEditingSolution, Form form)
	{
		return persistContext.getPersist();
	}

	@Override
	protected IPropertyHandler[] createPropertyHandlers(Object valueObject)
	{
		List<IPropertyHandler> props = new ArrayList<IPropertyHandler>();

		for (PropertyDescription desc : webComponentSpec.getProperties().values())
		{
			PropertyDescriptor beanPropertyDescriptor = BEAN_PROPERTIES.get(desc.getName());
			if (beanPropertyDescriptor != null)
			{
				// handled by bean, not in web component spec
				props.add(new BeanPropertyHandler(beanPropertyDescriptor));
			}
			else
			{
				props.add(new WebComponentPropertyHandler(desc));
			}
		}
		for (PropertyDescription desc : webComponentSpec.getHandlers().values())
		{
			props.add(new WebComponentPropertyHandler(desc));
		}

		return props.toArray(new IPropertyHandler[props.size()]);
	}

	@Override
	public String toString()
	{
		return webComponentSpec.getDisplayName();
	}
}
