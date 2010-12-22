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
package com.servoy.eclipse.ui.property;

import java.beans.PropertyEditor;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;

/**
 * Convert bean properties to/from string when the bean supports get/setAsText.
 * 
 * @author rgansevles
 * 
 */
public class BeanAsTextPropertyConverter implements IPropertyConverter<Object, String>
{
	private final PropertyEditor propertyEditor;

	public BeanAsTextPropertyConverter(PropertyEditor propertyEditor)
	{
		this.propertyEditor = propertyEditor;
	}

	public String convertProperty(Object id, Object value)
	{
		try
		{
			propertyEditor.setValue(value);
			return propertyEditor.getAsText();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error using property editor " + id, e);
			return Messages.LabelError;
		}
	}

	public Object convertValue(Object id, String value)
	{
		if (value == Messages.LabelError)
		{
			return null;
		}
		try
		{
			propertyEditor.setAsText(value);
			return propertyEditor.getValue();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error converting property value " + id, e);
			return null;
		}
	}
}
