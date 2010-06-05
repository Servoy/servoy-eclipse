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
package com.servoy.eclipse.designer.editor.commands;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.RepositoryException;


/**
 * A command to remove an element from a form. The command can be undone or redone.
 */
public class FormElementDeleteCommand extends Command
{
	/** Object to remove. */
	private final IPersist child;

	/** Object to remove from. */
	private ISupportChilds parent;

	/**
	 * Create a command that will remove the element from its parent.
	 * 
	 * @param form the Form containing the child
	 * @param child the element to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public FormElementDeleteCommand(IPersist child)
	{
		if (child == null || child.getParent() == null)
		{
			throw new IllegalArgumentException();
		}
		this.child = child;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
	public void execute()
	{
		String label = "delete element";
		if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
		{
			label += " " + ((ISupportName)child).getName();
		}
		setLabel(label);
		redo();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#redo()
	 */
	@Override
	public void redo()
	{
		// parent may be the form or a tab panel
		parent = child.getParent();
		try
		{
			((IDeveloperRepository)child.getRootObject().getRepository()).deleteObject(child);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not delete element", e);
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, child, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
	public void undo()
	{
		try
		{
			((IDeveloperRepository)parent.getRootObject().getRepository()).undeleteObject(parent, child);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not restore element", e);
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, child, false);
	}

	public IPersist getPersist()
	{
		return child;
	}
}
