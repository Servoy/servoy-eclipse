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

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.IRestorer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
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

	public static final Object REMOVED_OBJECT = new Object();

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

	/**
	 * Restorer of AbstractBase objects.
	 * 
	 * @author rgansevles
	 *
	 */
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

		public Object getRemoveState(Object object)
		{
			return REMOVED_OBJECT;
		}

		public void restoreState(Object object, Object state)
		{
			if (state == REMOVED_OBJECT)
			{
				try
				{
					((IDeveloperRepository)((AbstractBase)object).getRootObject().getRepository()).deleteObject((IPersist)object);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not delete element", e);
				}
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, object, false);
			}
			else
			{
				((AbstractBase)object).copyPropertiesMap((Map<String, Object>)state, true);
			}
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

		public Object getRemoveState(Object object)
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
						elementStates.put(((IPersist)element).getUUID(), restorer.getRemoveState(element));
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
