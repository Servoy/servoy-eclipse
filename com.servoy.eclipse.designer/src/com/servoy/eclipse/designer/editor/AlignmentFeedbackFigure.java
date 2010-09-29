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
import org.eclipse.draw2d.Polygon;
import org.eclipse.draw2d.geometry.Point;

/** 
 * Figure for feedback in alignment when moving elements.
 * Consists of pairs of points, each 2 points is shown as a line.
 * 
 * @author rgansevles
 *
 */

public class AlignmentFeedbackFigure extends Polygon
{

	public void addLine(boolean horizontal, int target, int start, int end)
	{
		if (horizontal)
		{
			addPoint(new Point(start, target));
			addPoint(new Point(end, target));
		}
		else
		{
			addPoint(new Point(target, start));
			addPoint(new Point(target, end));
		}
	}

	@Override
	protected void fillShape(Graphics graphics)
	{
		// no fill
	}

	@Override
	protected void outlineShape(Graphics graphics)
	{
		int[] pointInts = getPoints().toIntArray();
		for (int i = 0; i + 3 < pointInts.length; i += 4)
		{
			graphics.drawLine(pointInts[i], pointInts[i + 1], pointInts[i + 2], pointInts[i + 3]);
		}
	}
}
