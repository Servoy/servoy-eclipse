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
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Locator;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.RelativeLocator;
import org.eclipse.draw2d.geometry.Rectangle;

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
	private final IFigure referenceFigure;

	/**
	 * @param sameHeight
	 * @param childBounds
	 */
	public SameSizeFeedbackFigure(String type, IFigure referenceFigure)
	{
		this.type = type;
		this.referenceFigure = referenceFigure;
		init();
	}

	/**
	 * Initializes the figure.
	 */
	protected void init()
	{
		setPreferredSize(10, 10);

		Locator locator;
		if (SAME_WIDTH.equals(type))
		{
			locator = new RelativeLocator(referenceFigure, 0, 0.2);
		}
		else if (SAME_HEIGHT.equals(type))
		{
			locator = new RelativeLocator(referenceFigure, 0.9, 0);
		}
		else
		{
			return;
		}
		locator.relocate(this);
	}

	@Override
	public void paintFigure(Graphics g)
	{
		Rectangle r = getBounds();
		g.setBackgroundColor(ColorConstants.darkBlue);

		g.setLineWidth(2);

		if (SAME_WIDTH.equals(type))
		{
			g.drawLine(r.x + 2, r.y, r.x + 2, r.y + r.height - 2);
			g.drawLine(r.x + r.width - 2, r.y, r.x + r.width - 2, r.y + r.height - 2);
		}
		else if (SAME_HEIGHT.equals(type))
		{
			g.drawLine(r.x, r.y + 2, r.x + r.width, r.y + 2);
			g.drawLine(r.x, r.y + r.height - 2, r.x + r.width, r.y + r.height - 2);
		}

	}
}
