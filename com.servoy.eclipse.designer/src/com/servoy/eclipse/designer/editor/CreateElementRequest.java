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

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.commands.ICommandWrapper;
import com.servoy.eclipse.designer.editor.commands.ISupportModels;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;

/**
 * CreateRequest for elements from form editor palette or drag-and-drop.
 * 
 * @author rgansevles
 *
 */
public class CreateElementRequest extends CreateRequest
{
	private boolean resizable = false;

	public CreateElementRequest(Object type)
	{
		super(type);
	}

	/**
	 * @param factory
	 */
	public CreateElementRequest(CreationFactory factory)
	{
		setFactory(factory);
	}

	@Override
	public CreationFactory getFactory()
	{
		return super.getFactory();
	}

	public Command chainSetFactoryObjectCommand(Command command)
	{
		Command innerCommand = command;
		while (!(innerCommand instanceof ISupportModels) && innerCommand instanceof ICommandWrapper)
		{
			innerCommand = ((ICommandWrapper)innerCommand).getCommand();
		}
		if (innerCommand instanceof ISupportModels && getFactory() instanceof RequestTypeCreationFactory)
		{
			final ISupportModels createCommand = (ISupportModels)innerCommand;
			return command.chain(new Command()
			{
				@Override
				public void execute()
				{
					Object[] models = createCommand.getModels();
					if (models != null && models.length > 0)
					{
						((RequestTypeCreationFactory)getFactory()).setNewObject(models[0]);
					}
				}
			});
		}
		return command;
	}

	/**
	 * @param resizable
	 */
	public void setResizable(boolean resizable)
	{
		this.resizable = resizable;
	}

	/**
	 * @return the resizable
	 */
	public boolean isResizable()
	{
		return resizable;
	}
}
