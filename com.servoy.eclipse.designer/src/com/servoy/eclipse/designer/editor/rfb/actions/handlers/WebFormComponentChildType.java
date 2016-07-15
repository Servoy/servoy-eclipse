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
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.UUID;

/**
 *
 * @author jcompagner
 */
public class WebFormComponentChildType extends AbstractBase implements IBasicWebObject
{
	private final JSONObject apiJSON;
	private final PropertyDescription propertyDescription;
	private final String key;

	public WebFormComponentChildType(IBasicWebObject parentWebObject, String key, FlattenedSolution fs)
	{
		super(IRepository.WEBCUSTOMTYPES, parentWebObject, parentWebObject.getID(), UUID.randomUUID());
		this.key = key;
		int index = key.indexOf('.');
		String parentPropertyName = key.substring(0, index);
		JSONObject propertyValue = (JSONObject)parentWebObject.getProperty(parentPropertyName);
		PropertyDescription pd = FormComponentPropertyType.INSTANCE.getPropertyDescription(propertyValue, fs);

		String[] rest = key.substring(index + 1).split("\\.");
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
		apiJSON = propertyValue;
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
		apiJSON.put(propertyName, val);
		getParentComponent().flagChanged();
	}

	@Override
	public Map<String, Object> getPropertiesMap()
	{
		HashMap<String, Object> map = new HashMap<>();
		for (String key : apiJSON.keySet())
		{
			map.put(key, apiJSON.get(key));
		}
		return map;
	}

	@Override
	public Object getProperty(String propertyName)
	{
		return apiJSON.opt(propertyName);
	}

	@Override
	public boolean hasProperty(String propertyName)
	{
		return apiJSON.has(propertyName);
	}

	@Override
	public void clearProperty(String propertyName)
	{
		apiJSON.remove(propertyName);
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
		return apiJSON;
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
		return apiJSON;
	}
}
