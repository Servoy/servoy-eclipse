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

import java.util.Arrays;

import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.RepositoryException;


/**
 * A command to remove elements from a form. The command can be undone or redone.
 */
public class FormElementDeleteCommand extends Command
{
	/** Objects to remove. */
	private final IPersist[] children;

	/** Object to remove from. */
	private ISupportChilds[] parents;

	/**
	 * Create a command that will remove the element from its parent.
	 * 
	 * @param form the Form containing the child
	 * @param child the element to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public FormElementDeleteCommand(IPersist child)
	{
		this(new IPersist[] { child });
	}

	/**
	 * Create a command that will remove the element from its parent.
	 * 
	 * @param form the Form containing the child
	 * @param children the elements to remove
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public FormElementDeleteCommand(IPersist[] children)
	{
		if (children == null)
		{
			throw new IllegalArgumentException();
		}
		for (IPersist child : children)
		{
			if (child == null || child.getParent() == null)
			{
				throw new IllegalArgumentException();
			}
		}
		this.children = children;
	}

	@Override
	public void execute()
	{
		String label = "delete element";
		if (children.length > 1) label += 's';
		for (IPersist child : children)
		{
			if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
			{
				label += ' ' + ((ISupportName)child).getName();
			}
		}
		setLabel(label);
		redo();
	}

	@Override
	public void redo()
	{
		parents = new ISupportChilds[children.length];
		for (int i = 0; i < children.length; i++)
		{
			// parent may be the form or a tab panel
			parents[i] = children[i].getParent();
			try
			{
				((IDeveloperRepository)children[i].getRootObject().getRepository()).deleteObject(children[i]);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not delete element", e);
			}
		}

		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(children));
	}

	@Override
	public void undo()
	{
		for (int i = children.length - 1; i >= 0; i--)
		{
			try
			{
				((IDeveloperRepository)parents[i].getRootObject().getRepository()).undeleteObject(parents[i], children[i]);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not restore element", e);
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(children));
	}

	public IPersist[] getPersists()
	{
		return children;
	}
}
