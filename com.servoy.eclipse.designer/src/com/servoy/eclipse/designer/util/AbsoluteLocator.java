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

package com.servoy.eclipse.designer.util;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Locates a figure to some distance from the reference figure bounds.
 * 
 * @author rgansevles
 *
 */
public class AbsoluteLocator implements Locator
{
	private final IFigure reference;
	private final int xdelta;
	private final int ydelta;
	private final boolean left;
	private final boolean top;

	/**
	 * 
	 * @param reference
	 * @param left from left-or-right
	 * @param xdelta
	 * @param top from top-or-bottom
	 * @param ydelta
	 */
	public AbsoluteLocator(IFigure reference, boolean left, int xdelta, boolean top, int ydelta)
	{
		this.reference = reference;
		this.left = left;
		this.xdelta = xdelta;
		this.top = top;
		this.ydelta = ydelta;
	}

	public void relocate(IFigure target)
	{
		Rectangle targetBounds = new PrecisionRectangle(reference.getBounds().getResized(-1, -1));
		reference.translateToAbsolute(targetBounds);
		target.translateToRelative(targetBounds);
		targetBounds.resize(1, 1);

		Dimension targetSize = target.getPreferredSize();

		targetBounds.x += xdelta;
		if (!left)
		{
			targetBounds.x += targetBounds.width;
			if (xdelta < 0) targetBounds.x -= targetSize.width;
		}
		targetBounds.y += ydelta;
		if (!top)
		{
			targetBounds.y += targetBounds.height;
			if (ydelta < 0) targetBounds.y -= targetSize.height;
		}

		targetBounds.setSize(targetSize);
		target.setBounds(targetBounds);
	}
}
