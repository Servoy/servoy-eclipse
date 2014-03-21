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
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.Viewport;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.AutoexposeHelper;
import org.eclipse.gef.GraphicalEditPart;

/**
 * 
 * An implementation of org.eclipse.gef.AutoexposeHelper  that performs autoscrolling of a Viewport figure. This helper is for 
 * use with graphical editparts that contain a viewport figure. This helper will search the editpart and find the viewport. 
 * Autoscroll will occur when the detect location is inside the viewport's bounds, but near its edge. It will continue for as 
 * long as the location continues to meet these criteria. The autoscroll direction is approximated to the nearest orthogonal 
 * or diagonal direction (north, northeast, east, etc.). 
 * 
 * Copied from gef ViewportAutoexposeHelper with some adjustments.
 * 
 * @author asisu
 */

abstract class CustomViewportHelper
{
	protected GraphicalEditPart owner;

	protected CustomViewportHelper(GraphicalEditPart owner)
	{
		this.owner = owner;
	}

	protected Viewport findViewport(GraphicalEditPart part)
	{
		IFigure figure = null;
		Viewport port = null;
		do
		{
			if (figure == null) figure = part.getContentPane();
			else figure = figure.getParent();
			if (figure instanceof Viewport)
			{
				port = (Viewport)figure;
				break;
			}
		}
		while (figure != part.getFigure() && figure != null);
		return port;
	}

}

public class CustomViewportAutoexposeHelper extends CustomViewportHelper implements AutoexposeHelper
{
	/** defines the range where autoscroll is active inside a viewer */
	private static final Insets DEFAULT_EXPOSE_THRESHOLD = new Insets(18);

	/** the last time an auto expose was performed */
	private long lastStepTime = 0;

	/** The insets for this helper. */
	private final Insets threshold;

	private boolean continueIfOutside;

	/**
	 * Constructs a new helper on the given GraphicalEditPart. The editpart must have a <code>Viewport</code> somewhere between its <i>contentsPane</i> and
	 * its <i>figure</i> inclusively.
	 * 
	 * @param owner the GraphicalEditPart that owns the Viewport
	 */
	public CustomViewportAutoexposeHelper(GraphicalEditPart owner)
	{
		super(owner);
		threshold = DEFAULT_EXPOSE_THRESHOLD;
	}

	/**
	 * Constructs a new helper on the given GraphicalEditPart. The editpart must have a <code>Viewport</code> somewhere between its <i>contentsPane</i> and
	 * its <i>figure</i> inclusively.
	 * 
	 * @param owner the GraphicalEditPart that owns the Viewport
	 * @param threshold the Expose Threshold to use when determing whether or not a scroll should occur.
	 */
	public CustomViewportAutoexposeHelper(GraphicalEditPart owner, Insets threshold)
	{
		super(owner);
		this.threshold = threshold;
	}

	public CustomViewportAutoexposeHelper(GraphicalEditPart owner, Insets threshold, boolean continueIfOutside)
	{
		super(owner);
		this.threshold = threshold;
		this.continueIfOutside = continueIfOutside;
	}

	/**
	 * Returns <code>true</code> if the given point is inside the viewport, but near its edge.
	 * 
	 * @see org.eclipse.gef.AutoexposeHelper#detect(org.eclipse.draw2d.geometry.Point)
	 */
	public boolean detect(Point where)
	{
		lastStepTime = 0;
		Viewport port = findViewport(owner);
		Rectangle rect = Rectangle.SINGLETON;
		port.getClientArea(rect);
		port.translateToParent(rect);
		port.translateToAbsolute(rect);
		return rect.contains(where) && !rect.shrink(threshold).contains(where);
	}

	/**
	 * Returns <code>true</code> if the given point is outside the viewport or near its edge. Scrolls the viewport by a calculated (time based) amount in the
	 * current direction.
	 * 
	 * todo: investigate if we should allow auto expose when the pointer is outside the viewport
	 * 
	 * @see org.eclipse.gef.AutoexposeHelper#step(org.eclipse.draw2d.geometry.Point)
	 */
	public boolean step(Point where)
	{
		Viewport port = findViewport(owner);

		Rectangle rect = Rectangle.SINGLETON;
		port.getClientArea(rect);
		port.translateToParent(rect);
		port.translateToAbsolute(rect);

		// note, rect.shrink modified rect
		if (!(continueIfOutside || rect.contains(where)) || rect.shrink(threshold).contains(where)) return false;

		//if (!rect.contains(where) || rect.crop(threshold).contains(where)) return false;

		// set scroll offset (speed factor)
		int scrollOffset = 0;

		// calculate time based scroll offset
		if (lastStepTime == 0) lastStepTime = System.currentTimeMillis();

		long difference = System.currentTimeMillis() - lastStepTime;

		if (difference > 0)
		{
			scrollOffset = ((int)difference / 3);
			lastStepTime = System.currentTimeMillis();
		}

		if (scrollOffset == 0) return true;

		int region = rect.getPosition(where);
		Point loc = port.getViewLocation();

		if ((region & PositionConstants.SOUTH) != 0) loc.y += scrollOffset;
		else if ((region & PositionConstants.NORTH) != 0) loc.y -= scrollOffset;

		if ((region & PositionConstants.EAST) != 0) loc.x += scrollOffset;
		else if ((region & PositionConstants.WEST) != 0) loc.x -= scrollOffset;

		port.setViewLocation(loc);
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "ViewportAutoexposeHelper for: " + owner; //$NON-NLS-1$
	}

}
