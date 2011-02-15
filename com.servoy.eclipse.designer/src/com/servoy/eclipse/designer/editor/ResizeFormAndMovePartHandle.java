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

import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.handles.HandleBounds;

import com.servoy.j2db.persistence.Part;

/** Handle to set form width and at the same time move the part.
 * 
 * @author rgansevles
 *
 */
public class ResizeFormAndMovePartHandle extends PartMoveHandle
{
	public ResizeFormAndMovePartHandle(Part part, GraphicalEditPart owner, int direction)
	{
		super(part, owner, direction, new ResizeFormAndMovePartLocator(owner.getFigure()), Cursors.SIZESE);
	}

	@Override
	protected DragTracker createDragTracker()
	{
		DragTracker tracker = super.createDragTracker();
		if (tracker != null)
		{
			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put(DragFormPartPolicy.PROPERTY_ALLOW_FORM_RESIZE, Boolean.TRUE);
			tracker.setProperties(properties);
		}
		return tracker;
	}

	/**
	 * Locate resize handle.
	 * 
	 * @author rgansevles
	 *
	 */
	static class ResizeFormAndMovePartLocator implements Locator
	{
		private final IFigure reference;

		public ResizeFormAndMovePartLocator(IFigure reference)
		{
			this.reference = reference;
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

			target.setBounds(new Rectangle(figBounds.width - 3, figBounds.y + figBounds.height - 3, 6, 6));
		}
	}
}
