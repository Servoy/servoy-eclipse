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

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Command to apply moving to a Part.
 * 
 * @author rgansevles
 * 
 */
public class MovePartCommand extends BaseRestorableCommand
{
	/** Stores the new size and location. */
	private final int newHeight;
	/** A request to move/resize an edit part. */
	private final Object requestType;

	/** Object to manipulate. */
	private final Part part;
	private final IPersist context;

	/**
	 * Create a command that can move a part.
	 * 
	 * @param part the part to move
	 * @param req the move and resize request
	 * @param newHeight the new height
	 * @throws IllegalArgumentException if any of the parameters is null
	 */
	public MovePartCommand(Part part, IPersist context, Object requestType, int newHeight)
	{
		super("move part");
		if (part == null || requestType == null)
		{
			throw new IllegalArgumentException();
		}
		this.part = part;
		this.requestType = requestType;
		this.newHeight = newHeight;
		this.context = context;
	}

	@Override
	public boolean canExecute()
	{
		// make sure the Request is of a type we support:
		return (RequestConstants.REQ_MOVE.equals(requestType) || RequestConstants.REQ_MOVE_CHILDREN.equals(requestType) || RequestConstants.REQ_CLONE.equals(requestType));
	}

	@Override
	public void execute()
	{
		saveState(part);
		new PersistPropertySource(part, context, false).setPersistPropertyValue(StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), new Integer(
			newHeight));
	}
}
