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

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.handles.HandleBounds;

import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.persistence.Part;

/**
 * Handle for moving parts in form editor.
 * 
 * @since 6.0
 *  
 * @author rgansevles
 *
 */
public class PartMoveHandle extends AbstractFormHandle
{
	private final Part part;

	/**
	 * @param owner
	 */
	public PartMoveHandle(Part part, GraphicalEditPart owner)
	{
		super(owner, PositionConstants.NORTH);
		this.part = part;
		setLocator(new PartHandleLocator(part, owner.getFigure()));
	}

	@Override
	protected DragTracker createDragTracker()
	{
		return new PartMoveTracker(part, getOwner(), getDirection());
	}

	@Override
	protected void paintFigure(Graphics graphics)
	{
		if (isOpaque())
		{
			Rectangle bnds = getBounds();
			graphics.setLineStyle(Graphics.LINE_DOT);
			setForegroundColor(ColorResource.INSTANCE.getColor(DragFormBoundsFigure.PART_HANDLE_RGB));
			graphics.drawLine(bnds.x, bnds.y + (bnds.height / 2), bnds.x + bnds.width, bnds.y + (bnds.height / 2));
		}
	}

	/**
	 * Locate the part move handler.
	 * 
	 * @author rgansevles
	 *
	 */
	static class PartHandleLocator implements Locator
	{
		private final IFigure reference;
		private final Part part;

		public PartHandleLocator(Part part, IFigure reference)
		{
			this.part = part;
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

			int height = part.getHeight();
			target.setBounds(new Rectangle(figBounds.x, height - 2, figBounds.width, 4));
		}

	}
}
