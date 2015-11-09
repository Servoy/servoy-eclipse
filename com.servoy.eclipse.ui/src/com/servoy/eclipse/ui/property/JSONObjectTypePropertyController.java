/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Property controller to be used in properties view for custom json objects.
 *
 * @author acostescu
 */
//unfortunately here we can't use JSONObject in generics cause the value can also be JSONObject.NULL which would give classcastexceptions...
public abstract class JSONObjectTypePropertyController extends ObjectTypePropertyController
{

	private static IObjectTextConverter JSONOBJECT_TEXT_CONVERTER = new JSONObjectTextConverter();

	public JSONObjectTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	@Override
	protected String getLabelText(Object element)
	{
		return (!ServoyJSONObject.isJavascriptNull(element) ? "{...}" : "null");
	}

	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return JSONOBJECT_TEXT_CONVERTER;
	}

	public static class JSONObjectTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value.length() > 0 && !"null".equals(value))
			{
				try
				{
					new JSONObject(value);
				}
				catch (JSONException e)
				{
					return "Please use valid JSON object content (eg. '{ \"a\" : 1, \"b\" : \"str\" }'). Error: " +
						e.getMessage().replace("'", "''").replace("{", "'{'").replace("}", "'}'"); // the replace is needed as this string will go through eclipse MessageFormatter which has special meaning for { and }
				}
			}
			return null;
		}

		public Object convertToObject(String value)
		{
			if (value == null || value.trim().length() == 0)
			{
				return null;
			}

			if ("null".equals(value)) return ServoyJSONObject.NULL_FOR_JAVA;

			try
			{
				return new ServoyJSONObject(value, false);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never get here due to validation above in isCorrectString(...)
				return null;
			}
		}

		public String isCorrectObject(Object value)
		{
			if (ServoyJSONObject.isJavascriptNullOrUndefined(value) || (value instanceof JSONObject))
			{
				return null;
			}
			else
			{
				ServoyLog.logWarning("JSON object property contains non JSONObject content", null);
				return "Value is not a JSONObject as expected";
			}
		}

		public String convertToString(Object value)
		{
			if (ServoyJSONObject.isJavascriptUndefined(value))
			{
				return "";
			}
			if (ServoyJSONObject.isJavascriptNull(value))
			{
				return "null";
			}
			try
			{
				return ((JSONObject)value).toString(0).replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never happen
				return "Cannot convert the JSONObject into a string";
			}
		}

	}

	protected abstract class JSONObjectPropertySource extends ObjectPropertySource
	{

		public JSONObjectPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		protected abstract Object getDefaultElementProperty(Object id);

		@Override
		public Object setComplexPropertyValue(Object id, Object v)
		{
			defaultSetProperty(id, v);
			return getEditableValue();
		}

		@Override
		public void defaultSetProperty(Object id, Object value)
		{
			try
			{
				Object newValue = getEditableValue();
				Object val = ServoyJSONObject.adjustJavascriptNULLForOrgJSON(value);
				((JSONObject)newValue).put((String)id, val);

//				defaultElementWasSet(newValue);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}

		@Override
		public Object defaultGetProperty(Object id)
		{
			return ServoyJSONObject.adjustJavascriptNULLForJava(((JSONObject)getEditableValue()).opt((String)id));
		}

		@Override
		public boolean defaultIsPropertySet(Object id)
		{
			return ((JSONObject)getEditableValue()).has((String)id);
		}

		@Override
		public Object resetComplexPropertyValue(Object id)
		{
			IPropertyDescriptor pd = findPD(id);
			PersistPropertySource.adjustPropertyValueAndReset(id, pd, this);
			return ((JSONObject)getEditableValue()).opt((String)id);
		}

		protected IPropertyDescriptor findPD(Object id)
		{
			IPropertyDescriptor[] pds = getPropertyDescriptors();
			for (IPropertyDescriptor pd : pds)
			{
				if (com.servoy.j2db.util.Utils.equalObjects(pd.getId(), id)) return pd;
			}
			return null;
		}

		@Override
		public void defaultResetProperty(Object id)
		{
			defaultSetProperty(id, getDefaultElementProperty(id)); // if id would be of object or array type in which case we would need to create JSONObject or JSONArray, it shouldn't reach this code; it should be intercepted before this by the IPropertySetter method of the controller of those types; so we just use the default value here directly
		}

	}

	@Override
	public void setProperty(ISetterAwarePropertySource propertySource, Object value)
	{
		propertySource.defaultSetProperty(getId(), value);
	}

	@Override
	public Object getProperty(ISetterAwarePropertySource propertySource)
	{
		return propertySource.defaultGetProperty(getId());
	}

	@Override
	public boolean isPropertySet(ISetterAwarePropertySource propertySource)
	{
		return propertySource.defaultIsPropertySet(getId());
	}

}
