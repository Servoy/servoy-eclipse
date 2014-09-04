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

import java.beans.PropertyDescriptor;

import javax.swing.border.Border;

import org.eclipse.ui.views.properties.IPropertySource;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.BytePropertyType;
import org.sablo.specification.property.types.ColorPropertyType;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.DoublePropertyType;
import org.sablo.specification.property.types.FloatPropertyType;
import org.sablo.specification.property.types.FontPropertyType;
import org.sablo.specification.property.types.InsetsPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.LongPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;

/**
 * Base class for property handlers base on java beans/introspection.
 *
 * @author rgansevles
 *
 */
public class BasePropertyHandler implements IPropertyHandler
{
	// null type: use property controller internally
	public static final PropertyDescription ANCHORS_DESCRIPTION = new PropertyDescription("anchors", null, new AnchorPropertyController("anchors",
		RepositoryHelper.getDisplayName("anchors", GraphicalComponent.class)));

	protected final PropertyDescriptor propertyDescriptor;

	public BasePropertyHandler(java.beans.PropertyDescriptor propertyDescriptor)
	{
		this.propertyDescriptor = propertyDescriptor;
	}

	@Override
	public String getName()
	{
		return propertyDescriptor.getName();
	}

	@Override
	public boolean isProperty()
	{
		return propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null && !propertyDescriptor.isExpert() &&
			!propertyDescriptor.getPropertyType().equals(Object.class) && !propertyDescriptor.isHidden();
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		// Some properties apply to both persists and beans

		String name = propertyDescriptor.getName();

		// name based
		if (name.equals("anchors"))
		{
			return ANCHORS_DESCRIPTION;
		}

		// type based
		Class< ? > clazz = propertyDescriptor.getPropertyType();

		if (clazz == java.awt.Dimension.class)
		{
			return new PropertyDescription(name, TypesRegistry.getType(DimensionPropertyType.TYPE_NAME));
		}

		if (clazz == java.awt.Point.class)
		{
			return new PropertyDescription(name, TypesRegistry.getType(PointPropertyType.TYPE_NAME));
		}

		if (clazz == java.awt.Insets.class)
		{
			return new PropertyDescription(name, TypesRegistry.getType(InsetsPropertyType.TYPE_NAME));
		}

		if (clazz == java.awt.Color.class)
		{
			return new PropertyDescription(name, TypesRegistry.getType(ColorPropertyType.TYPE_NAME));
		}

		if (clazz == java.awt.Font.class)
		{
			return new PropertyDescription(name, TypesRegistry.getType(FontPropertyType.TYPE_NAME), Boolean.FALSE);
		}

		if (clazz == Border.class)
		{
			return new PropertyDescription(name, BorderPropertyType.INSTANCE, Boolean.FALSE);
		}

		if (clazz == boolean.class || clazz == Boolean.class)
		{
			return new PropertyDescription(name, BooleanPropertyType.INSTANCE);
		}

		if (clazz == String.class)
		{
			return new PropertyDescription(name, StringPropertyType.INSTANCE);
		}

		if (clazz == byte.class || clazz == Byte.class)
		{
			return new PropertyDescription(name, BytePropertyType.INSTANCE);
		}

		if (clazz == double.class || clazz == Double.class)
		{
			return new PropertyDescription(name, DoublePropertyType.INSTANCE);
		}

		if (clazz == float.class || clazz == Float.class)
		{
			return new PropertyDescription(name, FloatPropertyType.INSTANCE);
		}

		if (clazz == int.class || clazz == Integer.class)
		{
			return new PropertyDescription(name, IntPropertyType.INSTANCE);
		}

		if (clazz == long.class || clazz == Long.class)
		{
			return new PropertyDescription(name, LongPropertyType.INSTANCE);
		}

		if (clazz == short.class || clazz == Short.class)
		{
			return new PropertyDescription(name, IntPropertyType.INSTANCE);
		}

		return null;
	}

	@Override
	public String getDisplayName()
	{
		return propertyDescriptor.getDisplayName();
	}

	public Class< ? > getPropertyType()
	{
		return propertyDescriptor.getPropertyType();
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return AnnotationManagerReflection.getInstance().hasSupportForClientType(propertyDescriptor.getReadMethod(), obj.getClass(), csp, ClientSupport.Default);
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		try
		{
			return propertyDescriptor.getReadMethod().invoke(obj, new Object[0]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		try
		{
			propertyDescriptor.getWriteMethod().invoke(obj, new Object[] { value });
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public boolean shouldShow(Object obj)
	{
		try
		{
			String name = getName();
			// check for content spec element.
			IPersist persist = (IPersist)obj;
			EclipseRepository repository = (EclipseRepository)persist.getRootObject().getRepository();
			Element element = repository.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), name);

			int dispType = -1;
			if (obj instanceof Field)
			{
				dispType = ((Field)persist).getDisplayType();
			}

			if (!RepositoryHelper.shouldShow(name, element, persist.getClass(), dispType))
			{
				return false;
			}

			if (name.equals("labelFor") && persist instanceof GraphicalComponent)
			{
				GraphicalComponent gc = (GraphicalComponent)persist;
				if (ComponentFactory.isButton(gc))
				{
					//if it's a button, then we only show the property if it has a value (probably to be cleared via quickfix)
					return gc.getLabelFor() != null && !gc.getLabelFor().equals("");
				}
				else return true;
			}
			if (name.endsWith("printSliding") && !(persist.getParent() instanceof Form))
			{
				return false;//if not directly on form it can not slide
			}
			if ((name.equals("onTabChangeMethodID") || name.equals("scrollTabs")) && persist instanceof TabPanel &&
				(((TabPanel)persist).getTabOrientation() == TabPanel.SPLIT_HORIZONTAL || ((TabPanel)persist).getTabOrientation() == TabPanel.SPLIT_VERTICAL))
			{
				return false; // not applicable for splitpanes
			}

			if (name.equals("loginFormID") && persist instanceof Solution && ((Solution)persist).getLoginFormID() <= 0)
			{
				if (((Solution)persist).getLoginSolutionName() != null)
				{
					// there is a login solution, do not show the deprecated login form setting
					return false;
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

		return true;
	}
}
