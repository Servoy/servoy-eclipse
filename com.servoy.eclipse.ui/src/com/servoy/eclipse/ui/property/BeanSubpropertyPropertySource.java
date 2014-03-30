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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistPropertySource.PropertyDescriptorWrapper;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataui.PropertyEditorHint;
import com.servoy.j2db.dataui.PropertyEditorOption;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Property source for custom sub-properties of a bean.
 * <p>
 * For example, a bean groups a number of settings in 1 object, the settings can be collapsed in Properties View.
 * 
 * @author rgansevles
 */

public class BeanSubpropertyPropertySource extends ComplexPropertySource<Object>
{
	private final Object valueObject;
	private final BeanPropertyHandler propertyDescriptor;
	private final FlattenedSolution flattenedEditingSolution;
	private final Form form;

	private Map<String, IPropertyDescriptor> propertyDescriptors;
	private Map<Object, BeanPropertyHandler> beansProperties;

	public BeanSubpropertyPropertySource(ComplexProperty<Object> complexProperty, Object valueObject, BeanPropertyHandler propertyDescriptor,
		FlattenedSolution flattenedEditingSolution, Form form)
	{
		super(complexProperty);
		this.valueObject = valueObject;
		this.propertyDescriptor = propertyDescriptor;
		this.flattenedEditingSolution = flattenedEditingSolution;
		this.form = form;
	}

	private void init()
	{
		if (propertyDescriptors == null)
		{
			java.beans.BeanInfo info = null;
			try
			{
				info = java.beans.Introspector.getBeanInfo(propertyDescriptor.getPropertyType());
			}
			catch (java.beans.IntrospectionException e)
			{
				ServoyLog.logError(e);
			}

			propertyDescriptors = new LinkedHashMap<String, IPropertyDescriptor>();
			beansProperties = new HashMap<Object, BeanPropertyHandler>();

			if (info != null)
			{
				for (BeanPropertyHandler element : PersistPropertySource.sortBeansPropertyDescriptors(createPropertyHandlers(info.getPropertyDescriptors())))
				{
					if (!element.isProperty())
					{
						continue;
					}

					try
					{
						Object editableValue = getEditableValue();
						if (editableValue == null)
						{
							editableValue = createNewValue();
						}
						IPropertyDescriptor pd = PersistPropertySource.createPropertyDescriptor(this, element.getName(), null /* persistContext */, readOnly,
							new PropertyDescriptorWrapper(element, editableValue), element.getDisplayName(), flattenedEditingSolution, form);
						if (pd != null)
						{
							beansProperties.put(element.getName(), element);
							propertyDescriptors.put(element.getName(), pd);
						}
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}

	private static BeanPropertyHandler[] createPropertyHandlers(java.beans.PropertyDescriptor[] descs)
	{
		if (descs == null)
		{
			return new BeanPropertyHandler[0];
		}
		BeanPropertyHandler[] handlers = new BeanPropertyHandler[descs.length];
		for (int i = 0; i < descs.length; i++)
		{
			handlers[i] = new BeanPropertyHandler(descs[i]);
		}
		return handlers;
	}

	@Override
	public IPropertyDescriptor[] createPropertyDescriptors()
	{
		init();
		return PropertyController.applySequencePropertyComparator(propertyDescriptors.values().toArray(new IPropertyDescriptor[propertyDescriptors.size()]));
	}

	@Override
	public Object getPropertyValue(Object id)
	{
		init();

		BeanPropertyHandler beanPropertyDescriptor = beansProperties.get(id);
		if (beanPropertyDescriptor != null)
		{
			Object editableValue = getEditableValue();
			if (editableValue == null)
			{
				editableValue = createNewValue();
			}

			if (editableValue != null)
			{
				try
				{
					return PersistPropertySource.convertGetPropertyValue(id, propertyDescriptors.get(id),
						beanPropertyDescriptor.getValue(editableValue, null /* persistContext */));
				}
				catch (Exception e)
				{
					ServoyLog.logError("Could not get property value for id " + id + " on object " + editableValue, e);
				}
			}
		}
		return null;
	}

	/**
	 * @return
	 */
	private Object createNewValue()
	{
		Object hint = propertyDescriptor.getAttributeValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
		if (hint instanceof PropertyEditorHint)
		{
			Object factoryMethodName = ((PropertyEditorHint)hint).getOption(PropertyEditorOption.subPropertyFactoryMethod);
			if (factoryMethodName instanceof String)
			{
				// a factory method is configured
				Object newValue = null;
				if (valueObject != null)
				{
					try
					{
						Method method = valueObject.getClass().getMethod((String)factoryMethodName, ((Class[])null));
						newValue = method.invoke(valueObject, ((Object[])null));
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				return newValue;
			}
		}

		// try default constructor for type.
		try
		{
			int modifiers = propertyDescriptor.getPropertyType().getModifiers();
			if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & Modifier.ABSTRACT) == 0)
			{
				return propertyDescriptor.getPropertyType().newInstance();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	@Override
	protected Object setComplexPropertyValue(Object id, Object v)
	{
		init();
		BeanPropertyHandler beanPropertyDescriptor = beansProperties.get(id);

		Object editableValue = getEditableValue();
		if (beanPropertyDescriptor != null)
		{
			if (editableValue == null)
			{
				editableValue = createNewValue();
			}

			try
			{
				beanPropertyDescriptor.setValue(editableValue, PersistPropertySource.convertSetPropertyValue(id, propertyDescriptors.get(id), v), null /* persistContext */);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not get property value for id " + id + " on object " + editableValue, e);
			}
		}
		return editableValue;
	}
}
