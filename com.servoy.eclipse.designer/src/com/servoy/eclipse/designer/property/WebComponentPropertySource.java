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

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.BeanPropertyHandler;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.WebFormComponent;

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

	private final PropertyDescription propertyDescription;

	public WebComponentPropertySource(PersistContext persistContext, boolean readonly, PropertyDescription propertyDescription)
	{
		super(persistContext, readonly);
		if (!(persistContext.getPersist() instanceof Bean))
		{
			throw new IllegalArgumentException();
		}
		this.propertyDescription = propertyDescription;
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

		for (PropertyDescription desc : propertyDescription.getProperties().values())
		{
			Object scope = desc.getTag(WebFormComponent.TAG_SCOPE);
			if ("private".equals(scope) || "runtime".equals(scope))
			{
				// only show design properties
				continue;
			}

			PropertyDescriptor beanPropertyDescriptor = BEAN_PROPERTIES.get(desc.getName());
			if (beanPropertyDescriptor != null)
			{
				// handled by bean, not in web component spec
				props.add(new BeanPropertyHandler(beanPropertyDescriptor));
			}
			else
			{
				List<Object> values = desc.getValues();
				if (values != null && values.size() > 0 && !desc.getName().equals(StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName()))
				{
					ValuesConfig config = new ValuesConfig();
					if (!(values.get(0) instanceof JSONObject))
					{
						config.setValues(values.toArray(new Object[0]));
					}
					else
					{
						List<String> displayValues = new ArrayList<String>();
						List<Object> realValues = new ArrayList<Object>();
						for (Object jsonObject : values)
						{
							if (jsonObject instanceof JSONObject && ((JSONObject)jsonObject).keys().hasNext())
							{
								String key = (String)((JSONObject)jsonObject).keys().next();
								displayValues.add(key);
								realValues.add(((JSONObject)jsonObject).opt(key));
							}
						}
						config.setValues(realValues.toArray(new Object[realValues.size()]), displayValues.toArray(new String[displayValues.size()]));
					}
					if (desc.getDefaultValue() != null)
					{
						config.addDefault(desc.getDefaultValue(), null);
					}
					props.add(new WebComponentPropertyHandler(new PropertyDescription(desc.getName(), ValuesPropertyType.INSTANCE, config,
						desc.getDefaultValue(), null, null, false)));
				}
				else
				{
					props.add(new WebComponentPropertyHandler(desc));
				}
			}
		}

		if (propertyDescription instanceof WebComponentSpecification)
		{
			for (PropertyDescription desc : ((WebComponentSpecification)propertyDescription).getHandlers().values())
			{
				props.add(new WebComponentPropertyHandler(desc));
			}
		}

		return props.toArray(new IPropertyHandler[props.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.servoy.eclipse.ui.property.PersistPropertySource#createPropertyCategory(com.servoy.eclipse.ui.property.PersistPropertySource.PropertyDescriptorWrapper
	 * )
	 * 
	 * Properties from spec should be dispayed under "Component" category except for handlers and BEAN_PROPERTIES. Properties found with reflection are handled
	 * by the super class (they go under "Properties").
	 */
	@Override
	protected PropertyCategory createPropertyCategory(PropertyDescriptorWrapper propertyDescriptor)
	{
		if (propertyDescription instanceof WebComponentSpecification && BEAN_PROPERTIES.containsKey(propertyDescriptor.propertyDescriptor.getName())) return super.createPropertyCategory(propertyDescriptor);
		if (propertyDescription instanceof WebComponentSpecification &&
			((WebComponentSpecification)propertyDescription).getHandlers().containsKey(propertyDescriptor.propertyDescriptor.getName())) return PropertyCategory.Events;
		if (propertyDescription.getProperties().containsKey(propertyDescriptor.propertyDescriptor.getName())) return PropertyCategory.Component;
		return super.createPropertyCategory(propertyDescriptor);
	}

	@Override
	public String toString()
	{
		if (propertyDescription instanceof WebComponentSpecification) return ((WebComponentSpecification)propertyDescription).getDisplayName() + " - " +
			((Bean)persistContext.getPersist()).getName();
		return propertyDescription.getName();
	}
}
