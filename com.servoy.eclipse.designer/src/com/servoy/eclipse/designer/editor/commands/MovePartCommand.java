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

import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.Part;

/**
 * Command to apply moving to a Part.
 * 
 * @author rgansevles
 * 
 */
public class MovePartCommand extends Command
{

	/** Stores the new size and location. */
	private final int newHeight;
	/** Stores the old size and location. */
	private int oldHeight;
	/** A request to move/resize an edit part. */
	private final Object requestType;

	/** Object to manipulate. */
	private final Part part;

	/**
	 * Create a command that can move a part.
	 * 
	 * @param part the part to move
	 * @param req the move and resize request
	 * @param newBounds the new size and location
	 * @throws IllegalArgumentException if any of the parameters is null
	 */
	public MovePartCommand(Part part, Object requestType, int newHeight)
	{
		if (part == null || requestType == null)
		{
			throw new IllegalArgumentException();
		}
		this.part = part;
		this.requestType = requestType;
		this.newHeight = newHeight;
		setLabel("move part");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#canExecute()
	 */
	@Override
	public boolean canExecute()
	{
		// make sure the Request is of a type we support:
		return (RequestConstants.REQ_MOVE.equals(requestType) || RequestConstants.REQ_MOVE_CHILDREN.equals(requestType) || RequestConstants.REQ_CLONE.equals(requestType));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#execute()
	 */
	@Override
	public void execute()
	{
		oldHeight = part.getHeight();
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
		apply(newHeight);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.commands.Command#undo()
	 */
	@Override
	public void undo()
	{
		apply(oldHeight);
	}

	private void apply(int height)
	{
		part.setHeight(height);
		ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, part, false);
	}

}
