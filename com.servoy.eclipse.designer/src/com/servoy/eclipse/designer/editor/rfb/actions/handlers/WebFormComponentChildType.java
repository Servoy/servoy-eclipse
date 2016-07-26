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
package com.servoy.eclipse.designer.editor.rfb.actions.handlers;


import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IDesignValueConverter;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.UUID;

/**
 *
 * @author jcompagner
 */
public class WebFormComponentChildType extends AbstractBase implements IBasicWebObject
{
	private final PropertyDescription propertyDescription;
	private final String key;
	private final String parentPropertyName;
	private final String[] rest;
	private final FlattenedSolution fs;

	public WebFormComponentChildType(IBasicWebObject parentWebObject, String key, FlattenedSolution fs)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, parentWebObject.getID(), UUID.randomUUID());
		this.key = key;
		this.fs = fs;
		int index = key.indexOf('.');
		this.parentPropertyName = key.substring(0, index);
		JSONObject propertyValue = (JSONObject)parentWebObject.getProperty(parentPropertyName);
		PropertyDescription pd = FormComponentPropertyType.INSTANCE.getPropertyDescription(propertyValue, fs);

		this.rest = key.substring(index + 1).split("\\.");
		for (String propertyName : rest)
		{
			pd = pd.getProperty(propertyName);
			// TODO check for nested FormComponents by checkign the type of the pd.
			JSONObject value = propertyValue.optJSONObject(propertyName);
			if (value == null)
			{
				value = new JSONObject();
				propertyValue.put(propertyName, value);
			}
			propertyValue = value;
		}
		propertyDescription = pd;
	}

	/**
	 * @return the key
	 */
	public String getKey()
	{
		return key;
	}

	public PropertyDescription getPropertyDescription()
	{
		return propertyDescription;
	}

	@Override
	public void setProperty(String propertyName, Object val)
	{
		PropertyDescription pd = propertyDescription.getProperty(propertyName);
		if (pd != null && pd.getType() instanceof IDesignValueConverter< ? >)
		{
			getJson().put(propertyName, ((IDesignValueConverter< ? >)pd.getType()).toDesignValue(val, pd));
		}
		else getJson().put(propertyName, convertFromJavaType(propertyName, val));
		getParentComponent().flagChanged();
	}

	private Object convertToJavaType(String propertyName, Object val)
	{
		Object value = val;
		PropertyDescription pd = propertyDescription.getProperty(propertyName);
		if (value != null && pd != null && getConverter(pd) != null)
		{
			value = getConverter(pd).fromDesignValue(value, pd);
		}
		return (val != JSONObject.NULL) ? value : null;
	}

	private Object convertFromJavaType(String propertyName, Object value)
	{
		PropertyDescription pd = propertyDescription.getProperty(propertyName);
		if (pd != null && getConverter(pd) != null)
		{
			return getConverter(pd).toDesignValue(value, pd);
		}
		return value;
	}

	private IDesignValueConverter< ? > getConverter(PropertyDescription pd)
	{
		return (pd.getType() instanceof IDesignValueConverter< ? >) ? (IDesignValueConverter< ? >)pd.getType() : null;
	}

	@Override
	public Map<String, Object> getPropertiesMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		JSONObject json = getJson();
		for (String property : json.keySet())
		{
			map.put(property, json.get(property));
		}
		return map;
	}

	@Override
	public Object getProperty(String propertyName)
	{
		return convertToJavaType(propertyName, getJson().opt(propertyName));
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		return getJson().has(propertyName);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		getJson().remove(propertyName);
		getParentComponent().flagChanged();
	}

	@Override
	public void setName(String arg)
	{
	}

	@Override
	public String getName()
	{
		return null;
	}

	@Override
	public void setTypeName(String arg)
	{
	}

	@Override
	public String getTypeName()
	{
		return null;
	}

	@Override
	public JSONObject getFlattenedJson()
	{
		return getJson();
	}

	@Override
	public IBasicWebComponent getParentComponent()
	{
		return (IBasicWebComponent)getParent();
	}

	@Override
	public void updateJSON()
	{
	}

	@Override
	public JSONObject getJson()
	{
		JSONObject propertyValue = (JSONObject)((IBasicWebObject)getParent()).getProperty(parentPropertyName);
		for (String propertyName : rest)
		{
			// TODO check for nested FormComponents by checkign the type of the pd.
			JSONObject value = propertyValue.optJSONObject(propertyName);
			if (value == null)
			{
				value = new JSONObject();
				propertyValue.put(propertyName, value);
			}
			propertyValue = value;
		}
		return propertyValue;
	}
}
