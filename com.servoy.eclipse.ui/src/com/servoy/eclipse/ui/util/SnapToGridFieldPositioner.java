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
package com.servoy.eclipse.ui.util;

import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;

/**
 * Determine next fields position using snap-to-grid when needed.
 * 
 * @author rgansevles
 * 
 */
public class SnapToGridFieldPositioner extends DefaultFieldPositioner
{
	private final DesignerPreferences designerPreferences;

	public SnapToGridFieldPositioner(DesignerPreferences designerPreferences)
	{
		this.designerPreferences = designerPreferences;
	}

	@Override
	public Point getNextLocation(Point location)
	{
		Point loc = location == null ? getDefaultLocation() : location;
		loc = loc == null ? new Point(0, 0) : loc;

		int gridSize = designerPreferences.getGridSize();
		boolean gridSnapTo = getGridSnapTo();
		if (gridSize > 0 && gridSnapTo)
		{
			// snap 'm
			loc = new Point(loc.x - (int)Math.IEEEremainder(loc.x, gridSize), loc.y - (int)Math.IEEEremainder(loc.y, gridSize));
		}

		// move a bit for next location if there has been no mouse click
		int copyPasteOffset = gridSnapTo && gridSize > designerPreferences.getCopyPasteOffset() ? gridSize : designerPreferences.getCopyPasteOffset();
		setDefaultLocation(new Point(loc.x + copyPasteOffset, loc.y + copyPasteOffset));

		return loc;
	}

	/**
	 * Override if you want to determine snap-to-grid from elsewhere.
	 * 
	 * @return
	 */
	protected boolean getGridSnapTo()
	{
		return designerPreferences.getGridSnapTo();
	}
}
