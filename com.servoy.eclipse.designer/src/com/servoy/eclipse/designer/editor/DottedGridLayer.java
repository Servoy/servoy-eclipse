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
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editparts.GridLayer;
import org.eclipse.swt.graphics.Color;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.ColorResource;
import com.servoy.j2db.persistence.Form;

/**
 * A Grid that shows dots in stead of lines and is printed on a limited area.
 * 
 * @author rgansevles
 * 
 */
public class DottedGridLayer extends GridLayer
{
	private final VisualFormEditor editorPart;

	public DottedGridLayer(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	@Override
	protected void paintGrid(Graphics g)
	{
		Form flattenedForm = editorPart.getFlattenedForm();
		if (flattenedForm == null) return;

		java.awt.Dimension size = flattenedForm.getSize();
		if (size == null) return;

		DesignerPreferences designerPreferences = new DesignerPreferences();
		g.pushState();
		try
		{
			g.setClip(new Rectangle(origin.x, origin.y, size.width, size.height));
			Color color = ColorResource.INSTANCE.getColor(designerPreferences.getGridColor());
			g.setForegroundColor(color);
			g.setBackgroundColor(color);
			int gridSize = designerPreferences.getGridSize();
			paintGridDots(g, origin, gridSize, gridSize, designerPreferences.getGridPointSize());
		}
		finally
		{
			g.popState();
		}
	}

	/**
	 * Helper method to paint a grid. Painting is optimized as it is restricted to the Graphics' clip.
	 * 
	 * @param g The Graphics object to be used for painting
	 * @param f The figure in which the grid is to be painted
	 * @param origin Any point where the grid points are expected to intersect
	 * @param distanceX Distance between vertical grid points; if 0 or less, vertical grid points will not be drawn
	 * @param distanceY Distance between horizontal grid pints; if 0 or less, horizontal grid lines will not be drawn
	 * 
	 */
	public static void paintGridDots(Graphics g, org.eclipse.draw2d.geometry.Point origin, int distanceX, int distanceY, int pointSize)
	{
		if (distanceX > 0 && distanceY > 0 && pointSize > 0)
		{
			Rectangle clip = g.getClip(Rectangle.SINGLETON);
			if (origin.x >= clip.x)
			{
				while (origin.x - distanceX >= clip.x)
				{
					origin.x -= distanceX;
				}
			}
			else
			{
				while (origin.x < clip.x)
				{
					origin.x += distanceX;
				}
			}
			for (int i = origin.x; i < clip.x + clip.width; i += distanceX)
			{
				for (int j = 0; j < clip.height; j += distanceY)
				{
					if (pointSize == 1)
					{
						// uses foreground color
						g.drawPoint(i, j + clip.y);
					}
					else if (pointSize == 2)
					{
						// uses foreground color
						g.drawRectangle(i, j + clip.y, 1, 1);
					}
					else
					{
						// uses background color
						g.fillOval(i - (pointSize / 2), j + clip.y - (pointSize / 2), pointSize, pointSize);
					}
				}
			}
		}
	}
}
