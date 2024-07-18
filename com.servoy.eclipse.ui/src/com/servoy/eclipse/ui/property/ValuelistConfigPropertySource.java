/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2023 Servoy BV

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

import java.util.Arrays;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.property.ConvertorObjectCellEditor.IObjectTextConverter;
import com.servoy.j2db.server.ngclient.property.types.ValuelistConfigTypeSabloValue;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author lvostinar
 *
 */
public class ValuelistConfigPropertySource extends ComplexPropertySourceWithStandardReset<JSONObject>
{
	private static final String FILTER_TYPE = "filterType";
	private static final String FILTER_DESTINATION = "filterDestination";
	private static final String ALLOW_NEW_ENTRIES = "allowNewEntries";

	private static final String[] FILTER_TYPE_ITEMS = new String[] { ValuelistConfigTypeSabloValue.STARTS_WITH, ValuelistConfigTypeSabloValue.CONTAINS };
	private static final String[] FILTER_DESTINATION_ITEMS = new String[] { ValuelistConfigTypeSabloValue.DISPLAY_VALUE, ValuelistConfigTypeSabloValue.DISPLAY_AND_REAL_VALUE };

	private static IObjectTextConverter valuelistConfigTextConverter = new ValuelistConfigTextConverter();
	private static ILabelProvider valuelistConfigLabelProvider;

	public ValuelistConfigPropertySource(ComplexProperty<JSONObject> valuelistConfig)
	{
		super(valuelistConfig);
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		return new IPropertyDescriptor[] { new ComboboxPropertyController<String>(
			"filterType",
			"filterType",
			new ComboboxPropertyModel<String>(
				FILTER_TYPE_ITEMS),
			Messages.LabelUnresolved),

			new ComboboxPropertyController<String>(
				"filterDestination",
				"filterDestination",
				new ComboboxPropertyModel<String>(
					FILTER_DESTINATION_ITEMS),
				Messages.LabelUnresolved),

			new CheckboxPropertyDescriptor(ALLOW_NEW_ENTRIES, ALLOW_NEW_ENTRIES) };

	}

	@Override
	public Object getPropertyValue(Object id)
	{
		JSONObject json = getEditableValue();
		if (FILTER_TYPE.equals(id))
		{
			String type = json != null ? json.optString(FILTER_TYPE, ValuelistConfigTypeSabloValue.STARTS_WITH) : ValuelistConfigTypeSabloValue.STARTS_WITH;
			return Arrays.asList(FILTER_TYPE_ITEMS).indexOf(type);
		}
		if (FILTER_DESTINATION.equals(id))
		{
			String type = json != null ? json.optString(FILTER_DESTINATION, ValuelistConfigTypeSabloValue.DISPLAY_VALUE)
				: ValuelistConfigTypeSabloValue.DISPLAY_VALUE;
			return Arrays.asList(FILTER_DESTINATION_ITEMS).indexOf(type);
		}
		if (ALLOW_NEW_ENTRIES.equals(id))
		{
			return json != null ? json.optBoolean(ALLOW_NEW_ENTRIES, Boolean.TRUE) : Boolean.TRUE;
		}
		return null;
	}

	@Override
	public Object resetComplexPropertyValue(Object id)
	{
		if (ALLOW_NEW_ENTRIES.equals(id))
		{
			return Boolean.TRUE;
		}
		else
		{
			return Integer.valueOf(0);
		}
	}

	@Override
	protected JSONObject setComplexPropertyValue(Object id, Object v)
	{
		JSONObject editJSON = getEditableValue();
		JSONObject json = (editJSON == null) ? new JSONObject() : new ServoyJSONObject(editJSON, ServoyJSONObject.getNames(editJSON), false, false);
		if (ALLOW_NEW_ENTRIES.equals(id))
		{
			json.put(ALLOW_NEW_ENTRIES, v);
		}
		else
		{
			Integer index = (Integer)v;
			if (FILTER_TYPE.equals(id))
			{
				json.put(FILTER_TYPE, FILTER_TYPE_ITEMS[index]);
			}
			if (FILTER_DESTINATION.equals(id))
			{
				json.put(FILTER_DESTINATION, FILTER_DESTINATION_ITEMS[index]);
			}
		}
		return json;
	}

	public static CellEditor createPropertyEditor(Composite parent)
	{
		return new ConvertorObjectCellEditor(parent, valuelistConfigTextConverter);
	}

	public static ILabelProvider getLabelProvider()
	{
		if (valuelistConfigLabelProvider == null)
		{
			valuelistConfigLabelProvider = new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					return valuelistConfigTextConverter.convertToString(element);
				}
			};
		}
		return valuelistConfigLabelProvider;
	}

	public static class ValuelistConfigTextConverter implements IObjectTextConverter
	{

		public String isCorrectString(String value)
		{
			if (value != null && value.trim().length() > 0 && convertToObject(value) == null)
			{
				return "Expecting valid json";
			}
			return null;
		}

		public Object convertToObject(String value)
		{
			if (value == null)
			{
				return null;
			}
			try
			{
				ServoyJSONObject json = new ServoyJSONObject(value, true);
				return json;
			}
			catch (JSONException ex)
			{
				return null;
			}
		}

		public String isCorrectObject(Object value)
		{
			if (value == null || (value instanceof JSONObject))
			{
				return null;
			}
			return "Object is not json";
		}

		public String convertToString(Object value)
		{
			if (value == null)
			{
				return "";
			}
			return ServoyJSONObject.toString((JSONObject)value, true, false, true);
		}

	}
}
