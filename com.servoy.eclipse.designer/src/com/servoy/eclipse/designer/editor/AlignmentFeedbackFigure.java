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
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.util.Settings;

/** 
 * Figure for feedback in alignment when moving elements.
 * Consists of pairs of points, each 2 points is shown as a line.
 * 
 * @author rgansevles
 *
 */

public class AlignmentFeedbackFigure extends Polygon
{
	protected final ElementAlignmentItem elementAlignmentItem;
	private final DesignerPreferences designerPreferences;
	private final Rectangle referenceBounds;

	public AlignmentFeedbackFigure(ElementAlignmentItem item, Rectangle referenceBounds)
	{
		this.elementAlignmentItem = item;
		this.referenceBounds = referenceBounds;
		designerPreferences = new DesignerPreferences(Settings.getInstance());
		setLineStyle(Graphics.LINE_CUSTOM);
		setLineDash(new float[] { 5, 2 });
		setForegroundColor(ColorResource.INSTANCE.getColor(designerPreferences.getAlignmentGuideColor()));
		createPoints();
	}

	public void addLine(boolean horizontal, int target, int start, int end)
	{
		if (horizontal)
		{
			addPoint(new Point(start, target));
			if (referenceBounds != null && (target == referenceBounds.y || target == referenceBounds.y + referenceBounds.height) && start < referenceBounds.x &&
				end > referenceBounds.x + referenceBounds.width)
			{
				// do not draw over reference box
				addPoint(new Point(referenceBounds.x, target));
				addPoint(new Point(referenceBounds.x + referenceBounds.width, target));
			}
			addPoint(new Point(end, target));
		}
		else
		{
			addPoint(new Point(target, start));
			if (referenceBounds != null && (target == referenceBounds.x || target == referenceBounds.x + referenceBounds.width) && start < referenceBounds.y &&
				end > referenceBounds.y + referenceBounds.height)
			{
				// do not draw over reference box
				addPoint(new Point(target, referenceBounds.y));
				addPoint(new Point(target, referenceBounds.y + referenceBounds.height));
			}
			addPoint(new Point(target, end));
		}
	}

	@Override
	protected void fillShape(Graphics graphics)
	{
		// no fill
	}


	protected void createPoints()
	{
		if (ElementAlignmentItem.ALIGN_TYPE_SIDE.equals(elementAlignmentItem.alignType))
		{
			createSideAlignmentFeedbackLines();
		}
		else if (ElementAlignmentItem.ALIGN_TYPE_INDENT.equals(elementAlignmentItem.alignType))
		{
			createIndentAlignmentFeedbackLines();
		}
		else if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE.equals(elementAlignmentItem.alignType) ||
			ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM.equals(elementAlignmentItem.alignType) ||
			ElementAlignmentItem.ALIGN_TYPE_DISTANCE_SMALL.equals(elementAlignmentItem.alignType))
		{
			createDistanceAlignmentFeedbackLines();
		}
	}

	protected void createSideAlignmentFeedbackLines()
	{
		boolean horizontal = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(elementAlignmentItem.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(elementAlignmentItem.alignDirection);

		addLine(horizontal, elementAlignmentItem.target, elementAlignmentItem.start - 5, elementAlignmentItem.end + 5);
	}

	protected void createIndentAlignmentFeedbackLines()
	{
		int indent = designerPreferences.getAlignmentIndent();

		addLine(false, elementAlignmentItem.target, elementAlignmentItem.start - 5, elementAlignmentItem.end + 50);
		addLine(false, elementAlignmentItem.target - indent, elementAlignmentItem.start - 50, elementAlignmentItem.end + 5);
		// horizontal line confuses with other horizontal feedback // addLine(true, elementAlignmentItem.end, elementAlignmentItem.target - indent - 10, elementAlignmentItem.target + 50);
	}

	protected void createDistanceAlignmentFeedbackLines()
	{
		boolean horizontal = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(elementAlignmentItem.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(elementAlignmentItem.alignDirection);
		int sign = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(elementAlignmentItem.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(elementAlignmentItem.alignDirection) ? -1 : 1;

		// snapped-to line
		addLine(horizontal, elementAlignmentItem.target, elementAlignmentItem.start - 5, elementAlignmentItem.end + 5);

		int[] alignmentDistances = new DesignerPreferences(Settings.getInstance()).getAlignmentDistances();
		int mediumDiff = 0;
		int smallDiff = 0;
		if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE.equals(elementAlignmentItem.alignType))
		{
			mediumDiff = alignmentDistances[2 /* large */] - alignmentDistances[1/* medium */];
			smallDiff = alignmentDistances[2/* large */] - alignmentDistances[0/* small */];
		}
		else if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM.equals(elementAlignmentItem.alignType))
		{
			smallDiff = alignmentDistances[1/* medium */] - alignmentDistances[0/* small */];
		}

		if (mediumDiff > 0)
		{
			// add line for medium distance
			addLine(horizontal, elementAlignmentItem.target + sign * mediumDiff, elementAlignmentItem.start - 5, elementAlignmentItem.end + 5);
		}
		if (smallDiff > 0)
		{
			// add line for small distance
			addLine(horizontal, elementAlignmentItem.target + sign * smallDiff, elementAlignmentItem.start - 5, elementAlignmentItem.end + 5);
		}
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
