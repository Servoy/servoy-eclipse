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

package com.servoy.eclipse.designer.property;

import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyConverter;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.IPropertyHandler;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ServoyFunctionPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;

/**
 * Property handler for web components
 *
 * @author rgansevles
 *
 */
public class WebComponentPropertyHandler implements IPropertyHandler
{
	private final PropertyDescription propertyDescription;

	/**
	 * @param key
	 * @param value
	 */
	public WebComponentPropertyHandler(PropertyDescription propertyDescription)
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
		Object value = null;
		Bean bean = (Bean)obj;
		try
		{
			String json = bean.getBeanXML();
			if (json != null)
			{
				value = new ServoyJSONObject(json, false).opt(getName());
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}

		IPropertyType< ? > type = propertyDescription.getType();
		if (type == FunctionPropertyType.INSTANCE || type == ServoyFunctionPropertyType.INSTANCE || type == ValueListPropertyType.INSTANCE)
		{
			if (value == null) return Integer.valueOf(0);
			if (value instanceof Integer) return value;

			IPersist persist = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).searchPersist(UUID.fromString((String)value));
			if (persist instanceof AbstractBase)
			{
				return new Integer(persist.getID());
			}

			return Integer.valueOf(-1);
		}

		if (value == null)
		{
			// TODO propertyDescription.getDefaultValue() is JSON... I think NGConversions conversion 1 and 3 should be applied here to get something of the same type as propertyDescription.getType().defaultValue()
			// or maybe nothing really has to be returned here but "DEFAULT" cause the designer doesn't really need to show the default value, or does it?
			// anyway, currently it's not ok as propertyDescription.getDefaultValue() is a JSON value (can be primitive though) and propertyDescription.getType().defaultValue() is a sablo type value default (can be pure java, not JSON, can also be primitive)
			if (propertyDescription.getDefaultValue() != null)
			{
				return propertyDescription.getDefaultValue();
			}
			return type.defaultValue();
		}
		else if (type instanceof IPropertyConverter && type != DataproviderPropertyType.INSTANCE)
		{
			if (value instanceof String && ((String)value).startsWith("{"))
			{
				try
				{
					value = ((IPropertyConverter)type).fromJSON(new JSONObject((String)value), null, null);
				}
				catch (Exception e)
				{
					Debug.error("can't parse '" + value + "' to the real type for property converter: " + type, e);
				}
			}
			else
			{
				value = ((IPropertyConverter)type).fromJSON(value, null, null);
			}
		}
		return value;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		Bean bean = (Bean)obj;

		Object convertedValue = value;
		if (propertyDescription.getType() == FunctionPropertyType.INSTANCE || propertyDescription.getType() == ServoyFunctionPropertyType.INSTANCE)
		{
			//  value is methodid
			ITable table = null;
			if (persistContext.getContext() instanceof Form)
			{
				try
				{
					table = ((Form)persistContext.getContext()).getTable();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
			IScriptProvider scriptMethod = ModelUtils.getScriptMethod(bean, persistContext.getContext(), table, ((Integer)value).intValue());
			convertedValue = scriptMethod == null ? null : scriptMethod.getUUID().toString();
		}
		else if (propertyDescription.getType() == ValueListPropertyType.INSTANCE)
		{
			ValueList val = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getValueList(((Integer)value).intValue());
			convertedValue = val.getUUID().toString();
		}
		else if (propertyDescription.getType() instanceof IPropertyConverter && propertyDescription.getType() != DataproviderPropertyType.INSTANCE)
		{
			IPropertyConverter<Object> type = (IPropertyConverter<Object>)propertyDescription.getType();
			JSONStringer writer = new JSONStringer();
			try
			{
				writer.object();
				type.toJSON(writer, getName(), convertedValue, new DataConversion()).toString();
				writer.endObject();
				convertedValue = new JSONObject(writer.toString()).get(getName());
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}

		try
		{
			String json = bean.getBeanXML();
			ServoyJSONObject jsonObject = json == null ? new ServoyJSONObject(true, true) : new ServoyJSONObject(json, false);
			jsonObject.put(getName(), convertedValue);
			bean.setBeanXML(jsonObject.length() == 0 ? null : jsonObject.toString(false));
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}
	}

	public boolean shouldShow(Object obj)
	{
		return true;
	}
}
