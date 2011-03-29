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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
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
	private final PropertyDescriptor propertyDescriptor;
	private final FlattenedSolution flattenedEditingSolution;
	private final Form form;

	private Map<String, IPropertyDescriptor> propertyDescriptors;
	private Map<Object, PropertyDescriptor> beansProperties;

	public BeanSubpropertyPropertySource(ComplexProperty<Object> complexProperty, Object valueObject, PropertyDescriptor propertyDescriptor,
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
			beansProperties = new HashMap<Object, java.beans.PropertyDescriptor>();

			if (info != null)
			{
				for (java.beans.PropertyDescriptor element : PersistPropertySource.sortBeansPropertyDescriptors(info.getPropertyDescriptors()))
				{
					if ((element.getReadMethod() == null) || element.getWriteMethod() == null || element.isExpert() ||
						element.getPropertyType().equals(Object.class) || element.isHidden())
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
						IPropertyDescriptor pd = PersistPropertySource.createPropertyDescriptor(this, element.getName(), null /* persist */, null /* context */,
							readOnly, new PropertyDescriptorWrapper(element, editableValue), element.getDisplayName(), PropertyCategory.Beans,
							flattenedEditingSolution, form);
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

		java.beans.PropertyDescriptor beanPropertyDescriptor = beansProperties.get(id);
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
						beanPropertyDescriptor.getReadMethod().invoke(editableValue, new Object[0]));
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
		Object hint = propertyDescriptor.getValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
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
			return propertyDescriptor.getPropertyType().newInstance();
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
		java.beans.PropertyDescriptor beanPropertyDescriptor = beansProperties.get(id);

		Object editableValue = getEditableValue();
		if (beanPropertyDescriptor != null)
		{
			if (editableValue == null)
			{
				editableValue = createNewValue();
			}

			try
			{
				beanPropertyDescriptor.getWriteMethod().invoke(editableValue,
					new Object[] { PersistPropertySource.convertSetPropertyValue(id, propertyDescriptors.get(id), v) });
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not get property value for id " + id + " on object " + editableValue, e);
			}
		}
		return editableValue;
	}
}
