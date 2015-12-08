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

import org.json.JSONArray;
import org.json.JSONException;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Property controller to be used in properties view for custom json arrays.
 *
 * @author acostescu
 */
// unfortunately here we can't use JSONArray in generics even if we would make ArrayTypePropertyController generic because the value can also be JSONObject.NULL which would give classcastexceptions...
public abstract class JSONArrayTypePropertyController extends ArrayTypePropertyController implements ICanHandleJSONNullValues
{

	private static IObjectTextConverter JSONARRAY_TEXT_CONVERTER = new JSONArrayTextConverter();

	public JSONArrayTypePropertyController(Object id, String displayName)
	{
		super(id, displayName);
	}

	protected abstract Object getValueForReset();

	@Override
	protected String getLabelText(Object element)
	{
		return (!ServoyJSONObject.isJavascriptNull(element) ? (((JSONArray)element).length() == 0 ? "[]" : "[...]") : "null");
	}

	@Override
	protected IObjectTextConverter getMainObjectTextConverter()
	{
		return JSONARRAY_TEXT_CONVERTER;
	}

	@Override
	protected boolean isNotSet(Object value)
	{
		return ServoyJSONObject.isJavascriptNullOrUndefined(value);
	}

	@Override
	protected Object createEmptyPropertyValue()
	{
		ServoyJSONArray v = new ServoyJSONArray();
		v.put(getNewElementInitialValue()); // for convenience; usually when ppl want to use an array property they also want to add elements to it... not just have an empty array
		return v;
	}

	@Override
	protected Object insertElementAtIndex(int i, Object elementValue, Object oldMainValue)
	{
		return ServoyJSONArray.insertAtIndexInJSONArray((JSONArray)oldMainValue, i, elementValue);
	}

	public abstract class JSONArrayPropertySource extends ArrayPropertySource
	{

		public JSONArrayPropertySource(ComplexProperty<Object> complexProperty)
		{
			super(complexProperty);
		}

		protected abstract void defaultElementWasSet(Object newMainValue);

		protected abstract Object getDefaultElementProperty(Object id);

		@Override
		protected Object setComplexElementValueImpl(int idx, Object v)
		{
			defaultSetProperty(getIdFromIndex(idx), v);
			return getEditableValue();
		}

		@Override
		protected ServoyJSONArray deleteElementAtIndex(final int idx)
		{
			return ServoyJSONArray.removeIndexFromJSONArray((JSONArray)getEditableValue(), idx);
		}

		@Override
		protected Object insertNewElementAfterIndex(int idx)
		{
			return ServoyJSONArray.insertAtIndexInJSONArray((JSONArray)getEditableValue(), idx + 1, getNewElementInitialValue());
		}

		@Override
		protected void defaultSetElement(Object value, final int idx)
		{
			try
			{
				Object newValue = getEditableValue();
				Object val = ServoyJSONObject.adjustJavascriptNULLForOrgJSON(value);
				((JSONArray)newValue).put(idx, val);

				defaultElementWasSet(newValue);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}

		@Override
		protected Object defaultGetElement(final int idx)
		{
			return ServoyJSONObject.adjustJavascriptNULLForJava(((JSONArray)getEditableValue()).opt(idx));
		}

		@Override
		protected boolean defaultIsElementSet(final int idx)
		{
			return ((JSONArray)getEditableValue()).length() > idx;
		}

		@Override
		protected Object resetComplexElementValue(Object id, final int idx)
		{
			PersistPropertySource.adjustPropertyValueAndReset(id, getPropertyDescriptors()[idx], this);
			return PersistPropertySource.adjustPropertyValueToGet(id, getPropertyDescriptors()[idx], this);
		}

		@Override
		public void defaultResetProperty(Object id)
		{
			defaultSetProperty(id, getDefaultElementProperty(id)); // if id would be of object or array type in which case we would need to create JSONObject or JSONArray, it shouldn't reach this code; it should be intercepted before this by the IPropertySetter method of the controller of those types; so we just use the default value here directly
		}

	}

	public static class JSONArrayTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value.length() > 0 && !"null".equals(value))
			{
				try
				{
					new JSONArray(value);
				}
				catch (JSONException e)
				{
					return "Please use valid JSON array content (eg. '[ \"a\", \"b\" ]'). Error: " +
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
			if ("null".equals(value)) return ServoyJSONObject.NULL_FOR_JAVA; // temporary value that shouldn't reach the real JSONObject, but it is meant to not equal null (java-wise), cause JSONObject.NULL.equals(null) is true and then properties view cannot make the distinction correctly

			try
			{
				return new ServoyJSONArray(value);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never get here due to validation above in isCorrectString(...)
				return null;
			}
		}

		public String isCorrectObject(Object value)
		{
			if (ServoyJSONObject.isJavascriptNullOrUndefined(value) || (value instanceof JSONArray))
			{
				return null;
			}
			else
			{
				ServoyLog.logWarning("JSON array property contains non JSONArray content", null);
				return "Value is not a JSONArray as expected";
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
				return ((JSONArray)value).toString(0).replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ');
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e); // should never happen
				return "Cannot convert the JSONArray into a string";
			}
		}

	}

	@Override
	public void resetPropertyValue(ISetterAwarePropertySource propertySource)
	{
		Object defValue = getValueForReset();
		JSONArray toSet = null;
		if (defValue instanceof String)
		{
			try
			{
				toSet = new ServoyJSONArray((String)defValue);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}
		}
		else if (defValue instanceof JSONArray) toSet = (JSONArray)defValue;
		propertySource.setPropertyValue(getId(), toSet);
	}

}
