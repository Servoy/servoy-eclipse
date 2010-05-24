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

import java.util.Comparator;

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;

/**
 * Sort edit parts based on distance from a location.
 * 
 * @author rob
 * 
 */
public class EditpartDistanceComparator implements Comparator<EditPart>
{
	private final Point location;
	private final int sign;

	public EditpartDistanceComparator(Point location, boolean ascending)
	{
		this.location = location;
		this.sign = ascending ? -1 : 1;
	}

	public int compare(EditPart ep1, EditPart ep2)
	{
		if (ep1 instanceof GraphicalEditPart && ep2 instanceof GraphicalEditPart && !ep1.equals(ep2))
		{
			Rectangle bounds1 = ((GraphicalEditPart)ep1).getFigure().getBounds();
			int mid1x = bounds1.x + (bounds1.width / 2);
			int mid1y = bounds1.y + (bounds1.height / 2);
			int dist1 = ((location.x - mid1x) * (location.x - mid1x)) + ((location.y - mid1y) * (location.y - mid1y));
			Rectangle bounds2 = ((GraphicalEditPart)ep2).getFigure().getBounds();
			int mid2x = bounds2.x + (bounds2.width / 2);
			int mid2y = bounds2.y + (bounds2.height / 2);
			int dist2 = ((location.x - mid2x) * (location.x - mid2x)) + ((location.y - mid2y) * (location.y - mid2y));
			return dist1 < dist2 ? sign : dist1 == dist2 ? 1 : (-1 * sign);
		}
		return 0;
	}
}
