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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyConverterForBrowser;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.websocket.utils.DataConversion;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormatPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGColorPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGConversions.IDesignToFormElement;
import com.servoy.j2db.server.ngclient.property.types.NGDimensionPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGFontPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGInsetsPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGPointPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * Property handler for web components
 *
 * @author rgansevles
 *
 */
public class WebComponentPropertyHandler implements IPropertyHandler
{
	// this map can be filled by an extension point if we support 3rd party types.
	// TODO extension point + maybe use another interface as values - something like IDesignValueConverter - cause this conversion is not related to what the javadoc in IPropertyConverter describes and it can be confusing
	private static final Map<IPropertyType< ? >, IPropertyConverterForBrowser< ? extends Object>> jsonConverters = new HashMap<IPropertyType< ? >, IPropertyConverterForBrowser< ? extends Object>>();

	static
	{
		jsonConverters.put(NGPointPropertyType.NG_INSTANCE, NGPointPropertyType.NG_INSTANCE);
		jsonConverters.put(NGDimensionPropertyType.NG_INSTANCE, NGDimensionPropertyType.NG_INSTANCE);
		jsonConverters.put(NGColorPropertyType.NG_INSTANCE, NGColorPropertyType.NG_INSTANCE);
		jsonConverters.put(NGFontPropertyType.NG_INSTANCE, NGFontPropertyType.NG_INSTANCE);
		jsonConverters.put(NGInsetsPropertyType.NG_INSTANCE, NGInsetsPropertyType.NG_INSTANCE);
		jsonConverters.put(BorderPropertyType.INSTANCE, BorderPropertyType.INSTANCE);
	}

	private final PropertyDescription propertyDescription;

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
		IBasicWebObject bean = (IBasicWebObject)obj;

		JSONObject json = bean.getJson();
		if (json != null)
		{
			value = json.opt(getName());
		}

		IPropertyType< ? > type = propertyDescription.getType();
		if (type instanceof FunctionPropertyType || type instanceof ValueListPropertyType || type instanceof FormPropertyType ||
			type instanceof MediaPropertyType)
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
			if (!(type instanceof IDesignToFormElement) && propertyDescription.getDefaultValue() != null)
			{
				IPropertyConverterForBrowser<Object> converter = (IPropertyConverterForBrowser<Object>)jsonConverters.get(type);
				if (converter != null)
				{
					return converter.fromJSON(propertyDescription.getDefaultValue(), null, propertyDescription, null);
				}
				return propertyDescription.getDefaultValue();
			}
			// FormatPropertyType default is an Object, but the properties view expects string,
			// so just return null as default
			if (type == FormatPropertyType.INSTANCE) return null;
			return type.defaultValue(propertyDescription);
		}
		else
		{
			IPropertyConverterForBrowser<Object> converter = (IPropertyConverterForBrowser<Object>)jsonConverters.get(type);
			if (converter != null)
			{
				if (value instanceof String && ((String)value).startsWith("{"))
				{
					try
					{
						value = converter.fromJSON(new JSONObject((String)value), null, propertyDescription, null);
					}
					catch (Exception e)
					{
						Debug.error("can't parse '" + value + "' to the real type for property converter: " + type, e);
					}
				}
				else
				{
					value = converter.fromJSON(value, null, propertyDescription, null);
				}
			}
		}
		return value;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		IBasicWebObject bean = (IBasicWebObject)obj;

		Object convertedValue = value;
		if (propertyDescription.getType() instanceof FunctionPropertyType)
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
		else if (propertyDescription.getType() instanceof ValueListPropertyType)
		{
			ValueList val = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getValueList(((Integer)value).intValue());
			convertedValue = (val == null) ? null : val.getUUID().toString();
		}
		else if (propertyDescription.getType() instanceof FormPropertyType)
		{
			Form frm = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getForm(((Integer)value).intValue());
			convertedValue = (frm == null) ? null : frm.getUUID().toString();
		}
		else if (propertyDescription.getType() instanceof MediaPropertyType)
		{
			Media media = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).getMedia(((Integer)value).intValue());
			convertedValue = (media == null) ? null : media.getUUID().toString();
		}
		else
		{
			IPropertyConverterForBrowser<Object> converter = (IPropertyConverterForBrowser<Object>)jsonConverters.get(propertyDescription.getType());
			if (converter != null)
			{
				JSONStringer writer = new JSONStringer();
				try
				{
					writer.object();
					converter.toJSON(writer, getName(), convertedValue, propertyDescription, new DataConversion(), null).toString();
					writer.endObject();
					convertedValue = new JSONObject(writer.toString()).get(getName());
				}
				catch (JSONException e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		bean.setJsonSubproperty(getName(), convertedValue);
	}

	public boolean shouldShow(Object obj)
	{
		return true;
	}
}
