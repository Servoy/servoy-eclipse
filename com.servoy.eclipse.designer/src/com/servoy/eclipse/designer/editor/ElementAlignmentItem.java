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

/** 
 * Holds alignment information for moving elements.
 * 
 * @author rgansevles
 *
 */
public class ElementAlignmentItem
{
	public static final String ALIGN_DIRECTION_EAST = "east";
	public static final String ALIGN_DIRECTION_WEST = "west";
	public static final String ALIGN_DIRECTION_NORTH = "north";
	public static final String ALIGN_DIRECTION_SOUTH = "south";

	public static final String ALIGN_TYPE_SIDE = "side";
	public static final String ALIGN_TYPE_DISTANCE_LARGE = "large distance";
	public static final String ALIGN_TYPE_DISTANCE_MEDIUM = "medium distance";
	public static final String ALIGN_TYPE_DISTANCE_SMALL = "small distance";

	public final String alignDirection;
	public final String alignType;
	public final int start;
	public final int end;
	public final int target;
	public final int distance;
	public final boolean anchor;

	public ElementAlignmentItem(String alignDirection, String alignType, int target, int distance, int start, int end, boolean anchor)
	{
		this.alignDirection = alignDirection;
		this.alignType = alignType;
		this.target = target;
		this.distance = distance;
		this.start = start;
		this.end = end;
		this.anchor = anchor;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alignDirection == null) ? 0 : alignDirection.hashCode());
		result = prime * result + ((alignType == null) ? 0 : alignType.hashCode());
		result = prime * result + (anchor ? 1231 : 1237);
		result = prime * result + distance;
		result = prime * result + end;
		result = prime * result + start;
		result = prime * result + target;
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		ElementAlignmentItem other = (ElementAlignmentItem)obj;
		if (alignDirection == null)
		{
			if (other.alignDirection != null) return false;
		}
		else if (!alignDirection.equals(other.alignDirection)) return false;
		if (alignType == null)
		{
			if (other.alignType != null) return false;
		}
		else if (!alignType.equals(other.alignType)) return false;
		if (anchor != other.anchor) return false;
		if (distance != other.distance) return false;
		if (end != other.end) return false;
		if (start != other.start) return false;
		if (target != other.target) return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "ElementAlignmentItem [alignDirection=" + alignDirection + ", alignType=" + alignType + ", start=" + start + ", end=" + end + ", target=" +
			target + ", distance=" + distance + ", anchor=" + anchor + "]";
	}

}
