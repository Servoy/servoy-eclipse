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

package com.servoy.eclipse.designer.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.IModelSavePropertySource;
import com.servoy.eclipse.ui.property.IRestorer;

/**
 * Base command to save state of objects and restore from state in undo().
 *
 * @author rgansevles
 *
 */
public abstract class BaseRestorableCommand extends Command
{
	private Map<Object, Object> states;
	private static String REMOVE_PROPERTY = "{}";

	public BaseRestorableCommand(String label)
	{
		super(label);
	}

	public static IRestorer getRestorer(Object object)
	{
		if (object == null)
		{
			return null;
		}
		return Platform.getAdapterManager().getAdapter(object, IRestorer.class);
	}

	public static Object getState(Object object)
	{
		if (object == null)
		{
			return null;
		}
		IRestorer restorer = Platform.getAdapterManager().getAdapter(object, IRestorer.class);
		if (restorer == null)
		{
			return null;
		}
		return restorer.getState(object);
	}

	public static Object getRemovedState(Object object)
	{
		if (object == null)
		{
			return null;
		}
		IRestorer restorer = Platform.getAdapterManager().getAdapter(object, IRestorer.class);
		if (restorer == null)
		{
			return null;
		}
		return restorer.getRemoveState(object);
	}

	protected void saveState(Object object)
	{
		if (states != null && states.containsKey(object))
		{
			// already saved
			return;
		}
		save(object, getState(object));
	}

	protected void save(Object object, Object state)
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, object, true);
		if (states != null && states.containsKey(object))
		{
			// already saved
			return;
		}

		if (state != null)
		{
			if (states == null)
			{
				states = new HashMap<Object, Object>();
			}
			states.put(object, state);
		}
	}

	/**
	 * Set a property in the propertySource.
	 * Save state for undo().
	 *
	 * @param propertySource
	 * @param propertyName
	 * @param location
	 */
	protected void setPropertyValue(IModelSavePropertySource propertySource, Object id, Object value)
	{
		Object modelBefore = propertySource.getSaveModel();
		Object stateBefore = getState(modelBefore);
		propertySource.setPropertyValue(id, value);
		Object modelAfter = propertySource.getSaveModel();
		if (modelBefore == modelAfter)
		{
			save(modelBefore, stateBefore);
		}
		else
		{
			// model of propertySource changed during setPropertyValue,
			save(modelAfter, getRemovedState(modelAfter));
		}
	}

	@Override
	public void undo()
	{
		if (states != null)
		{
			for (Entry<Object, Object> entry : states.entrySet())
			{
				Object object = entry.getKey();
				IRestorer restorable = Platform.getAdapterManager().getAdapter(object, IRestorer.class);
				if (entry.getValue().toString().equals(REMOVE_PROPERTY))
				{
					((WebFormComponentChildType)object).removeProperty();
				}
				else
				{
					restorable.restoreState(object, entry.getValue());
				}

				// fire persist change recursively
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, object, true);
			}
			states = null;
		}
	}
}
