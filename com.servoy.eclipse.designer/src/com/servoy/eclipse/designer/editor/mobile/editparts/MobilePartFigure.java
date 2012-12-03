/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
package com.servoy.eclipse.designer.editor.mobile.editparts;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.handles.HandleBounds;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * Figure for header or footer part in mobile form editor.
 * 
 * @author rgansevles
 * 
 */
public class MobilePartFigure extends Figure implements HandleBounds
{
	public static Color HEADER_COLOR = ColorResource.INSTANCE.getColor(new RGB(110, 150, 190));// TODO: use theme
	private final int partType;

	/**
	 * Construct an empty group figure.
	 * @param partType 
	 */
	public MobilePartFigure(int partType)
	{
		this.partType = partType;
		setOpaque(true);
		setBackgroundColor(HEADER_COLOR);
	}

	/**
	 * @return the partType
	 */
	public int getPartType()
	{
		return partType;
	}

	public Rectangle getHandleBounds()
	{
		// just show the line as rectangle
		return getBounds();
	}
}
