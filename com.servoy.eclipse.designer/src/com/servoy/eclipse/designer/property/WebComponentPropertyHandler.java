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

import java.awt.Color;

import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONException;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.ColorPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ColorPropertyController;
import com.servoy.eclipse.ui.property.ColorPropertyController.PropertyColorConverter;
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

		if (propertyDescription.getType() == ColorPropertyType.INSTANCE)
		{
			return ColorPropertyController.PROPERTY_COLOR_CONVERTER.convertValue(null, (String)value);
		}
		else if (propertyDescription.getType() == FunctionPropertyType.INSTANCE)
		{
			if (value == null) return Integer.valueOf(0);

			IPersist func = ModelUtils.getEditingFlattenedSolution(bean, persistContext.getContext()).searchPersist(UUID.fromString((String)value));
			if (func instanceof AbstractBase)
			{
				return new Integer(func.getID());
			}

			return Integer.valueOf(-1);
		}

		if (value == null)
		{
			if (propertyDescription.getDefaultValue() != null)
			{
				return propertyDescription.getDefaultValue();
			}
			return propertyDescription.getType().defaultValue();
		}
		return value;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		Bean bean = (Bean)obj;

		Object convertedValue = value;
		if (propertyDescription.getType() == ColorPropertyType.INSTANCE)
		{
			convertedValue = PropertyColorConverter.getColorString((Color)value);
		}
		else if (propertyDescription.getType() == FunctionPropertyType.INSTANCE)
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
