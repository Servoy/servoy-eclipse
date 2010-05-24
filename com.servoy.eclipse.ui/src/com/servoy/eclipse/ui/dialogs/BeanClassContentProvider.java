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
package com.servoy.eclipse.ui.dialogs;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.ui.property.IPropertyConverter;
import com.servoy.j2db.plugins.IBeanClassProvider;

/**
 * Content provider class for bean classes.
 * 
 * @author rob
 * 
 */

public class BeanClassContentProvider extends FlatTreeContentProvider
{
	public static final BeanClassContentProvider DEFAULT = new BeanClassContentProvider();

	public static final Object BEANS_DUMMY_INPUT = new Object();

	private Set<BeanInfo> ts;

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement == BEANS_DUMMY_INPUT)
		{
			if (ts == null)
			{
				ts = new TreeSet<BeanInfo>(new Comparator<BeanInfo>()
				{
					public int compare(BeanInfo o1, BeanInfo o2)
					{
						return o1.getBeanDescriptor().getName().compareToIgnoreCase(o2.getBeanDescriptor().getName());
					}
				});
				for (Class< ? > beanClass : ((IBeanClassProvider)Activator.getDefault().getDesignClient().getBeanManager()).getAllBeanClasses())
				{
					try
					{
						BeanInfo bi = Introspector.getBeanInfo(beanClass);
						if (bi != null)
						{
							if (bi.getBeanDescriptor() != null && "InMemDataGrid".equals(bi.getBeanDescriptor().getName())) continue;
							ts.add(bi);
						}
					}
					catch (Throwable e)
					{
						ServoyLog.logError(e);
					}
				}
			}

			return ts.toArray();
		}

		return new Object[0];
	}

	public static class BeanClassConverter implements IPropertyConverter<String, BeanInfo>
	{
		public static final BeanClassConverter CONVERTER = new BeanClassConverter();

		public BeanInfo convertProperty(Object id, String value)
		{
			for (Object bi : BeanClassContentProvider.DEFAULT.getElements(BEANS_DUMMY_INPUT))
			{
				if (bi instanceof BeanInfo && ((BeanInfo)bi).getBeanDescriptor().getBeanClass().getName().equals(value))
				{
					return (BeanInfo)bi;
				}
			}
			return null;
		}

		public String convertValue(Object id, BeanInfo value)
		{
			if (value == null)
			{
				return null;
			}
			return value.getBeanDescriptor().getBeanClass().getName();
		}
	}

}
