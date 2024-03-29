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
package com.servoy.eclipse.ui.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.eclipse.core.databinding.BindingException;
import org.eclipse.core.databinding.observable.value.AbstractObservableValue;
import org.eclipse.core.databinding.observable.value.IObservableValue;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Observer immutable objects, the setters will create a new instance.
 *
 * @author rgansevles
 *
 */
public class ImmutableObjectObservable<T>
{
	private T object;
	private final Class< ? >[] constructorTypes;
	private final String[] constructorProperties;
	private final String[] ignoredProperties;

	public ImmutableObjectObservable(T object, Class< ? >[] constructorTypes, String[] constructorProperties)
	{
		this(object, constructorTypes, constructorProperties, null);
	}

	public ImmutableObjectObservable(T object, Class< ? >[] constructorTypes, String[] constructorProperties, String[] ignoredProperties)
	{
		this.object = object;
		this.constructorTypes = constructorTypes;
		this.constructorProperties = constructorProperties;
		this.ignoredProperties = ignoredProperties;
	}

	public IObservableValue observePropertyValue(String property)
	{
		return new ObserveImmutableObjectPropertyValue(getPropertyDescriptor(object.getClass(), property));
	}

	public T getObject()
	{
		return object;
	}

	public void setPropertyValue(String property, Object value)
	{
		new ObserveImmutableObjectPropertyValue(getPropertyDescriptor(object.getClass(), property)).setValue(value);
	}

	private static PropertyDescriptor getPropertyDescriptor(Class beanClass, String propertyName)
	{
		BeanInfo beanInfo;
		try
		{
			beanInfo = Introspector.getBeanInfo(beanClass);
		}
		catch (IntrospectionException e)
		{
			// cannot introspect, give up
			return null;
		}
		PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
		for (PropertyDescriptor descriptor : propertyDescriptors)
		{
			if (descriptor.getName().equals(propertyName))
			{
				return descriptor;
			}
		}
		throw new BindingException("Could not find property with name " + propertyName + " in class " + beanClass);
	}

	private static Object getPropertyValue(Object object, PropertyDescriptor propertyDescriptor)
	{
		try
		{
			Method readMethod = propertyDescriptor.getReadMethod();
			if (readMethod == null)
			{
				throw new BindingException(propertyDescriptor.getName() + " property does not have a read method.");
			}
			if (!readMethod.isAccessible())
			{
				readMethod.setAccessible(true);
			}
			return readMethod.invoke(object, (Object[])null);
		}
		catch (InvocationTargetException e)
		{
			/*
			 * InvocationTargetException wraps any exception thrown by the invoked method.
			 */
			throw new RuntimeException(e.getCause());
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			return null;
		}
	}

	class ObserveImmutableObjectPropertyValue extends AbstractObservableValue
	{

		private final PropertyDescriptor propertyDescriptor;

		public ObserveImmutableObjectPropertyValue(PropertyDescriptor propertyDescriptor)
		{
			this.propertyDescriptor = propertyDescriptor;
		}

		public Object getValueType()
		{
			return propertyDescriptor.getPropertyType();
		}

		@Override
		protected Object doGetValue()
		{
			return getPropertyValue(object, propertyDescriptor);
		}

		@Override
		protected void doSetValue(Object value)
		{
			try
			{
				Constructor<T> constructor = (Constructor<T>)object.getClass().getConstructor(constructorTypes);
				Object[] initargs = new Object[constructorProperties.length];
				for (int i = 0; i < initargs.length; i++)
				{
					String constructorProperty = constructorProperties[i];
					if (ignoredProperties != null && Arrays.asList(ignoredProperties).contains(constructorProperty))
					{
						initargs[i] = getDefaultValue(constructorTypes[i]);
					}
					else
					{
						if ("this".equals(constructorProperty))
						{
							initargs[i] = object;
						}
						else if (propertyDescriptor.getName().equals(constructorProperty))
						{
							initargs[i] = value;
						}
						else
						{
							initargs[i] = getPropertyValue(object, getPropertyDescriptor(object.getClass(), constructorProperty));
						}
					}
				}
				object = constructor.newInstance(initargs);
			}
			catch (InvocationTargetException e)
			{
				/*
				 * InvocationTargetException wraps any exception thrown by the invoked method.
				 */
				throw new RuntimeException(e.getCause());
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}

		private Object getDefaultValue(Class< ? > cls)
		{
			if (cls == int.class) return Integer.valueOf(0);
			if (cls == byte.class) return Byte.valueOf((byte)0);
			if (cls == short.class) return Short.valueOf((short)0);
			if (cls == long.class) return Long.valueOf(0);
			if (cls == float.class) return Float.valueOf(0);
			if (cls == double.class) return Double.valueOf(0);
			if (cls == boolean.class) return Boolean.FALSE;
			if (cls == char.class) return Character.valueOf((char)0);
			return null;
		}
	}
}
