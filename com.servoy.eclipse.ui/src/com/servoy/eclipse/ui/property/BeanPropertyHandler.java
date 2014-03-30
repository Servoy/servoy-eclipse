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

import java.awt.Component;
import java.awt.Dimension;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.Bean;


/**
 * Property handler for classic (component) beans.
 * 
 * @author rgansevles
 *
 */
public class BeanPropertyHandler extends BasePropertyHandler
{
	public BeanPropertyHandler(java.beans.PropertyDescriptor propertyDescriptor)
	{
		super(propertyDescriptor);
	}

	public Class< ? > getPropertyEditorClass()
	{
		return propertyDescriptor.getPropertyEditorClass();
	}

	public Object getAttributeValue(String propertyEditorHint)
	{
		return propertyDescriptor.getValue(propertyEditorHint);
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		super.setValue(obj, value, persistContext);

		if (obj instanceof Bean)
		{
			// size, location and name are set on persist, not on bean instance
			if ("size".equals(propertyDescriptor.getName()) && (value == null || value instanceof Dimension))
			{
				Object beanDesignInstance = ModelUtils.getEditingFlattenedSolution((Bean)obj).getBeanDesignInstance((Bean)obj);
				if (beanDesignInstance instanceof Component)
				{
					((Component)beanDesignInstance).setSize((Dimension)value);
				}
			}
		}
		else
		{
			// obj is bean instance
			if (persistContext != null) // null for subproperties
			{
				ComponentFactory.updateBeanWithItsXML((Bean)persistContext.getPersist(), obj);
			}
		}
	}
}
