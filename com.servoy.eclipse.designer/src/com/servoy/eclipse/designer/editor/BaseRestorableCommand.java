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

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.property.IRestorer;

/**
 * Base command to save state of objects and restore from state in undo().
 * 
 * @author rgansevles
 *
 */
public abstract class BaseRestorableCommand extends Command
{
	private final Object object;
	private Object state;

	public BaseRestorableCommand(String label, Object object)
	{
		super(label);
		this.object = object;
	}

	/**
	 * @return the object
	 */
	public Object getObject()
	{
		return object;
	}

	protected void saveState()
	{
		IRestorer restorable = (IRestorer)Platform.getAdapterManager().getAdapter(object, IRestorer.class);
		state = restorable == null ? null : restorable.getState(object);
	}

	@Override
	public boolean canUndo()
	{
		return state != null;
	}

	@Override
	public void undo()
	{
		IRestorer restorable = (IRestorer)Platform.getAdapterManager().getAdapter(object, IRestorer.class);
		restorable.restoreState(object, state);
		state = null;
		// fire persist change recursively
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, object, true);
	}
}
