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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.graphics.Font;

/**
 * A figure for a form part
 */
public class PartFigure extends Figure
{
	private static final int MARGINX = 10;
	private static final int MARGINY = 2;

	private String text = "";

	private Dimension textSize = null;


	@Override
	public void setFont(Font f)
	{
		textSize = null;
		super.setFont(f);
	}


	public Dimension getTextSize()
	{
		if (textSize == null)
		{
			textSize = FigureUtilities.getTextExtents(text, getFont());
		}
		return textSize;
	}

	/**
	 * @see IFigure#getMinimumSize(int, int)
	 */
	@Override
	public Dimension getMinimumSize(int w, int h)
	{
		return new Dimension(getTextSize().width + MARGINX, getTextSize().height + MARGINY);
	}

	/**
	 * @see IFigure#getPreferredSize(int, int)
	 */
	@Override
	public Dimension getPreferredSize(int wHint, int hHint)
	{
		return getMinimumSize(wHint, hHint);
	}

	/**
	 * Returns the text of the part.
	 */
	public String getText()
	{
		return text;
	}

	/**
	 * @see Figure#paintFigure(Graphics)
	 */
	@Override
	protected void paintFigure(Graphics graphics)
	{
		super.paintFigure(graphics);
		Rectangle bnds = getBounds();

		int textWidth = getTextSize().width;
		int textHeight = getTextSize().height;

		graphics.pushState();
		try
		{
			graphics.setBackgroundColor(ColorConstants.buttonDarker);
			graphics.fillRectangle(bnds.x, bnds.y, textWidth + MARGINX, textHeight + MARGINY);
			graphics.drawText(getText(), new Point(bnds.x + (MARGINX / 2), bnds.y + (MARGINY / 2)));

			graphics.drawLine(0, bnds.y, bnds.x, bnds.y);
		}
		finally
		{
			graphics.popState();
		}
	}

	/**
	 * Sets the label's text.
	 * 
	 * @param s the new label text
	 * @since 2.0
	 */
	public void setText(String s)
	{
		//"text" will never be null.
		if (s == null)
		{
			s = "";
		}
		if (text.equals(s))
		{
			return;
		}
		textSize = null;
		text = s;
		revalidate();
		repaint();
	}

}
