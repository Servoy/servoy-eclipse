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

import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.handles.AbstractHandle;

/**
 * Base class for handles in resizing forms/moving parts.
 * 
 *  @since 6.0
 *  
 * @author rgansevles
 *
 */
public abstract class AbstractFormHandle extends AbstractHandle
{
	private final int direction;

	/**
	 * @param owner
	 * @param direction
	 */
	public AbstractFormHandle(GraphicalEditPart owner, int direction)
	{
		this.direction = direction;
		setOwner(owner);
		setCursor((PositionConstants.NORTH_SOUTH & direction) != 0 ? Cursors.SIZENS : Cursors.SIZEWE);
	}

	/**
	 * @return the direction
	 */
	public int getDirection()
	{
		return direction;
	}
}
