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

import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;

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
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
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
	private boolean canHandleJSONNull = false;

	public WebComponentPropertyHandler(PropertyDescription propertyDescription)
	{
		this.propertyDescription = propertyDescription;
	}

	public void setCanHandleJSON(boolean canHandleJSONNull)
	{
		this.canHandleJSONNull = canHandleJSONNull;
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
		IBasicWebObject bean = (IBasicWebObject)obj;
		Object value = bean.getProperty(getName());
		try
		{

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
		}
		catch (Exception e)
		{
			Debug.log("illegal value in bean, ignoring it: " + value);
		}
		return canHandleJSONNull ? value : ServoyJSONObject.jsonNullToNull(value);
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

		bean.setProperty(getName(), convertedValue);
	}

	public boolean shouldShow(Object obj)
	{
		return true;
	}
}
