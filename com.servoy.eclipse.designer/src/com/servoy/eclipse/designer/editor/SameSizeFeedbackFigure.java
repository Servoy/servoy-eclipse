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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.eclipse.designer.util.AbsoluteLocator;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * Feedback figure for elements that have the same width or height
 * 
 * @author rgansevles
 *
 */
public class SameSizeFeedbackFigure extends Figure
{
	public static final String SAME_WIDTH = "same width";
	public static final String SAME_HEIGHT = "same height";

	protected static final Border TOOL_TIP_BORDER = new MarginBorder(0, 2, 0, 2);

	private final String type;

	/**
	 * @param sameHeight
	 * @param childBounds
	 */
	public SameSizeFeedbackFigure(String type)
	{
		this.type = type;
		init();
	}

	/**
	 * @return the type
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Get the appropriate locator.
	 */
	public static Locator getLocator(String type, IFigure referenceFigure)
	{
		if (SAME_WIDTH.equals(type))
		{
			return new AbsoluteLocator(referenceFigure, true, 2, true, 3);
		}
		if (SAME_HEIGHT.equals(type))
		{
			return new AbsoluteLocator(referenceFigure, false, -4, true, 1);
		}
		return null;
	}

	/**
	 * Initializes the figure.
	 */
	protected void init()
	{
		if (SAME_WIDTH.equals(type))
		{
			setPreferredSize(5, 10);
		}
		else if (SAME_HEIGHT.equals(type))
		{
			setPreferredSize(10, 5);
		}
	}

	@Override
	public void paintFigure(Graphics g)
	{
		Rectangle r = getBounds();
		DesignerPreferences dp = new DesignerPreferences();
		g.setBackgroundColor(ColorResource.INSTANCE.getColor(dp.getSameHeightWidthIndicatorColor()));
		g.setForegroundColor(ColorResource.INSTANCE.getColor(dp.getSameHeightWidthIndicatorColor()));

		g.setLineWidth(2);

		if (SAME_WIDTH.equals(type))
		{
			g.drawLine(r.x + r.width - 2, r.y, r.x + r.width - 2, r.y + r.height - 2);
		}
		else if (SAME_HEIGHT.equals(type))
		{
			g.drawLine(r.x, r.y + r.height - 2, r.x + r.width, r.y + r.height - 2);
		}

	}
}
