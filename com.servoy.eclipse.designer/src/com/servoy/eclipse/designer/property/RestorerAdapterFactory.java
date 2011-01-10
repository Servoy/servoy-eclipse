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
package com.servoy.eclipse.designer.property;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IAdapterFactory;

import com.servoy.eclipse.ui.property.IRestorer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * Factory for adapters for saving state of elements in the form editor.
 * 
 * @author rgansevles
 */
public class RestorerAdapterFactory implements IAdapterFactory
{
	private static Class[] ADAPTERS = new Class[] { IRestorer.class };

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

	public Object getAdapter(Object obj, Class key)
	{
		if (key == IRestorer.class)
		{
			return getRestorer(obj);
		}

		return null;
	}

	static IRestorer getRestorer(Object obj)
	{
		if (obj instanceof AbstractBase)
		{
			return AbstractBaseRestorer.INSTANCE;
		}
		if (obj instanceof FormElementGroup)
		{
			return FormElementGroupRestorer.INSTANCE;
		}

		return null;
	}

	static class AbstractBaseRestorer implements IRestorer
	{

		public static final AbstractBaseRestorer INSTANCE = new AbstractBaseRestorer();

		private AbstractBaseRestorer()
		{
		}

		public Object getState(Object object)
		{
			return ((AbstractBase)object).getPropertiesMap();
		}

		public void restoreState(Object object, Object state)
		{
			Map<String, Object> saved = (Map<String, Object>)state;
			AbstractBase base = ((AbstractBase)object);
			Map<String, Object> propertiesMap = base.getPropertiesMap();
			for (String key : propertiesMap.keySet())
			{
				if (!saved.containsKey(key))
				{
					base.clearProperty(key);
				}
			}
			base.copyPropertiesMap(saved, true);
		}
	}


	static class FormElementGroupRestorer implements IRestorer
	{
		public static final FormElementGroupRestorer INSTANCE = new FormElementGroupRestorer();

		private FormElementGroupRestorer()
		{
		}

		public Object getState(Object object)
		{
			FormElementGroup group = (FormElementGroup)object;

			Map<UUID, Object> elementStates = new HashMap<UUID, Object>();
			Iterator<IFormElement> elements = group.getElements();
			while (elements.hasNext())
			{
				IFormElement element = elements.next();
				if (element instanceof IPersist)
				{
					IRestorer restorer = getRestorer(element);
					if (restorer != null)
					{
						elementStates.put(((IPersist)element).getUUID(), restorer.getState(element));
					}
				}
			}

			return new Pair<String, Map<UUID, Object>>(group.getGroupID(), elementStates);
		}

		public void restoreState(Object object, Object state)
		{
			FormElementGroup group = (FormElementGroup)object;
			Pair<String, Map<UUID, Object>> saved = (Pair<String, Map<UUID, Object>>)state;

			Map<UUID, Object> elementStates = saved.getRight();
			Iterator<IFormElement> elements = group.getElements();
			while (elements.hasNext())
			{
				IFormElement element = elements.next();
				if (element instanceof IPersist)
				{
					IRestorer restorer = getRestorer(element);
					if (restorer != null)
					{
						restorer.restoreState(element, elementStates.get(((IPersist)element).getUUID()));
					}
				}
			}
			group.setGroupID(saved.getLeft());
		}
	}
}
