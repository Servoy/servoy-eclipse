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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.handles.HandleBounds;

/**
 * Handle for resizing form.
 * 
 * @since 6.0
 *  
 * @author rgansevles
 *
 */
public class FormResizeHandle extends AbstractFormHandle
{
	/**
	 * @param owner
	 * @param direction
	 */
	public FormResizeHandle(GraphicalEditPart owner, int direction)
	{
		super(owner, direction);
		setLocator(new FormHandleLocator(owner.getFigure(), direction));
	}

	@Override
	protected DragTracker createDragTracker()
	{
		return new FormResizeTracker(getOwner(), getDirection());
	}

	/**
	 * Locate form resize handle.
	 * 
	 * @author rgansevles
	 *
	 */
	static class FormHandleLocator implements Locator
	{
		private final IFigure reference;
		private final int direction;

		public FormHandleLocator(IFigure reference, int direction)
		{
			this.reference = reference;
			this.direction = direction;
		}

		public void relocate(IFigure target)
		{
			Rectangle figBounds;
			if (reference instanceof HandleBounds)
			{
				figBounds = ((HandleBounds)reference).getHandleBounds();
			}
			else
			{
				figBounds = reference.getBounds();
			}

			if ((direction & PositionConstants.EAST) != 0)
			{
				target.setBounds(new Rectangle(figBounds.width - 2, figBounds.y, 4, figBounds.height));
			}
			// TODO more directions?
		}

	}
}
