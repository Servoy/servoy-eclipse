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


import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * This is the layout manager for an XY layout with Flow of those figures that don't have a specified location. It will layout the figures that have a constraint in their XY
 * location. It will then go through the figures without a constraint (identified by x,y == PREFERRED_LOC) and find a spot for them. Creation date: (5/25/00 3:20:21 PM)
 * 
 */
public class FormLayout extends XYLayout
{
	protected static int fMarginWidth = 10; // Margin width when auto positioning figures	
	protected static int fMarginHeight = 10; // Margin height when auto positioning figures

	public static final int PREFERRED_SIZE = -1; // This is hard-code in GEF XYLayout and XYLayoutEditPolicy for default. If changed then we need to change here.
	public static final int PREFERRED_LOC = Integer.MIN_VALUE;

	public FormLayout()
	{
	}

	protected void layoutIfRequired(IFigure f)
	{
		Iterator<IFigure> itr = constraints.keySet().iterator();
		while (itr.hasNext())
		{
			// See if any of the components have a preferred size or don't know their location.
			IFigure child = itr.next();
			Rectangle constraint = (Rectangle)getConstraint(child);
			if (constraint == null) continue;
			// The width and height must be not set or the x/y must be unpositioned to require a layout
			if (constraint.width == PREFERRED_SIZE || constraint.height == PREFERRED_SIZE || (constraint.x == PREFERRED_LOC && constraint.y == PREFERRED_LOC))
			{
				layout(f);
				return;
			}
		}
	}

	/**
	 * The preferred size is actually calculated within the layout so that the unpositioned and default sized children can be taken into account.
	 */
	@Override
	protected Dimension calculatePreferredSize(IFigure f, int wHint, int hHint)
	{
		// Under some circumstances we are asked to calculate preferred  size
		// when our figures have no x and y ( it is still PREFERRED_LOC ) representing
		// preferredSize
		// An example is a free form that has a number of unannotated objects none of which
		// have explicit bounds set on them
		// If so then we must lay the figures out so that we know what size they will occupy
		layoutIfRequired(f);

		Rectangle rect = new Rectangle();
		Rectangle workingRect = new Rectangle();
		Iterator children = f.getChildren().iterator();
		while (children.hasNext())
		{
			IFigure child = (IFigure)children.next();
			Rectangle r = (Rectangle)constraints.get(child);
			if (r == null) continue;
			// For a figure that has no annotation the preferredX and Y will be left as unset
			// however the actual X and Y may be set.
			workingRect.x = (r.x == PREFERRED_LOC) ? child.getBounds().x : r.x;
			workingRect.y = (r.y == PREFERRED_LOC) ? child.getBounds().y : r.y;

			if (r.width == PREFERRED_SIZE || r.height == PREFERRED_SIZE)
			{
				Dimension prefSize = child.getPreferredSize();
				workingRect.width = (r.width == PREFERRED_SIZE) ? prefSize.width : r.width;
				workingRect.height = (r.height == PREFERRED_SIZE) ? prefSize.height : r.height;
			}
			rect.union(r);
		}
		Dimension d = rect.getSize();
		Insets insets = f.getInsets();
		return d.expand(insets.getWidth(), insets.getHeight()).union(getBorderPreferredSize(f));
	}

	/**
	 * Try to find a white space for a box of this size outside of the given rectangles, but within the bounding rectangle (expand bounding rectangle if required). A rectangle that
	 * has an x/y = MIN_VALUE is not to be considered for calculations since it has not yet been positioned. Creation date: (2/22/00 4:21:13 PM)
	 */
	protected void findWhiteSpaceFor(int rIndex, Rectangle[] rects, Rectangle boundingRect)
	{
		int requestedWidth = rects[rIndex].width + 2 * fMarginWidth; // Increase by double margin width (one on each side)
		int requestedHeight = rects[rIndex].height + 2 * fMarginHeight;
		int leftMost = boundingRect.x;
		int topMost = boundingRect.y;
		int rightMost = boundingRect.x + boundingRect.width;
		if (requestedWidth < boundingRect.width)
		{
			// The requested size is less than the bounding rectangle, see where on the right
			// it can fit, moving down as needed.

			int highestBottom = topMost; // How high up to do search. Start at the the top.
			// Perform search by width, then by height. Start at upper left and see if any intersections.
			// If there aren't any, good, we found a spot. If there are, move right beyond all of the
			// intersections and try again. If we get all of the way over, then move back to the highest
			// bottom we found among all of the intersections and to the far left and try again, moving
			// down the page until we find something.
			while (true)
			{
				int x = leftMost;
				int y = highestBottom;
				highestBottom = Integer.MAX_VALUE;
				while (x + requestedWidth < rightMost)
				{
					boolean intersects = false;
					Rectangle tryRect = new Rectangle(x, y, requestedWidth, requestedHeight);
					for (Rectangle rect : rects)
					{
						if (rect == null) continue;
						if (rect.x != PREFERRED_LOC && rect.y != PREFERRED_LOC && rect.intersects(tryRect))
						{
							intersects = true;
							x = Math.max(x, rect.x + rect.width);
							highestBottom = Math.min(highestBottom, rect.y + rect.height);
						}
					}
					if (!intersects)
					{
						// We got a space for it.
						rects[rIndex].x = tryRect.x + fMarginWidth; // Move right by margin amount.
						rects[rIndex].y = tryRect.y + fMarginHeight; // Move down by margin amount.
						return;
					}
					// There was an intersection, x is now just right of the rightmost intersection.
					// Loop will try searching here now.
				}
				// We gone all of the way to the right and couldn't find anything. Now move down
				// to just below the height intersect bottom. Loop will try searching here.
			}
		}
		else
		{
			// The requested width is larger than the bounding area, so go down and find first
			// y that we can use that doesn't intersect anything.
			int y = topMost;
			while (true)
			{
				boolean intersects = false;
				Rectangle tryRect = new Rectangle(leftMost, y, requestedWidth, requestedHeight);
				for (Rectangle rect : rects)
				{
					if (rect == null) continue;
					if (rect.x != PREFERRED_LOC && rect.y != PREFERRED_LOC && rect.intersects(tryRect))
					{
						intersects = true;
						y = Math.max(y, rect.y + rect.height);
					}
				}
				if (!intersects)
				{
					// We got a space for it.
					rects[rIndex].x = tryRect.x + fMarginWidth; // Move right by margin amount.
					rects[rIndex].y = tryRect.y + fMarginHeight; // Move down by margin amount.
					return;
				}
				// It intersects something in this row, move below the lowest of the intersections
				// Loop will try again.
			}
		}
	}

	/**
	 * Layout the figures according to their constraint. If they don't have one, then at the end find a spot for them.
	 */
	@Override
	public void layout(IFigure containerFigure)
	{
		List children = containerFigure.getChildren();
		Rectangle newBounds[] = new Rectangle[children.size()];
		Rectangle containerRectangle = null;
		Point offset = getOrigin(containerFigure);

		// For Freeform XY layout assume that the constraint is a rectangle.
		// Loop through children, setting the displayBox to the constraint.
		for (int i = 0; i < children.size(); i++)
		{
			IFigure f = (IFigure)children.get(i);
			Rectangle bounds = (Rectangle)getConstraint(f);
			if (bounds == null) continue;
			bounds = new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height); // So we don't modify the one still in the figure.
			if (bounds.width == PREFERRED_SIZE || bounds.height == PREFERRED_SIZE)
			{
				Dimension prefSize = f.getPreferredSize();
				if (bounds.width == PREFERRED_SIZE) bounds.width = prefSize.width;
				if (bounds.height == PREFERRED_SIZE) bounds.height = prefSize.height;
			}
			if (bounds.x != PREFERRED_LOC && bounds.y != PREFERRED_LOC)
			{
				bounds.translate(offset); // Adjust from relative to absolute (including inset).
				containerRectangle = containerRectangle != null ? containerRectangle.union(bounds) : bounds.getCopy().setLocation(Math.min(0, bounds.x), Math.min(0, bounds.y)); // Container area should hold all assigned figures.
			}
			newBounds[i] = bounds;
		}

		// Now loop through and apply the bounds, and find an area for any not set.
		if (containerRectangle != null) containerRectangle.setSize(Math.max(containerRectangle.width, 1000), Math.max(containerRectangle.height, 1000));
		else containerRectangle = new Rectangle();
		for (int i = 0; i < newBounds.length; i++)
		{
			if (newBounds[i] == null) continue;
			if (newBounds[i].x == PREFERRED_LOC && newBounds[i].y == PREFERRED_LOC)
			{
				// Find a spot for it.
				findWhiteSpaceFor(i, newBounds, containerRectangle);
			}
			IFigure f = (IFigure)children.get(i);
			Rectangle newBound = newBounds[i];
			f.setBounds(newBound);
		}
	}

	@Override
	public Object getConstraint(IFigure f)
	{
		return f.getBounds();
	}

	@Override
	public Point getOrigin(IFigure parent)
	{
		return new Point();
	}
}
