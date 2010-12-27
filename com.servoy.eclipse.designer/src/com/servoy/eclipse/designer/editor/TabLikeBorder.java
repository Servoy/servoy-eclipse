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
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Border for Tabs in form editor.
 * 
 * @author rgansevles
 *
 */
public class TabLikeBorder extends MarginBorder
{

	private static final Insets DEFAULT_INSETS = new Insets(2, 2, 2, 0);

	/**
	 * @see org.eclipse.draw2d.Border#getInsets(IFigure)
	 */
	@Override
	public Insets getInsets(IFigure figure)
	{
		return insets;
	}

	public TabLikeBorder()
	{
		this(DEFAULT_INSETS);
	}

	public TabLikeBorder(Insets insets)
	{
		super(insets);
	}

	public TabLikeBorder(int t, int l, int b, int r)
	{
		super(t, l, b, r);
	}

	@Override
	public boolean isOpaque()
	{
		return true;
	}

	/**
	 * @see org.eclipse.draw2d.Border#paint(IFigure, Graphics, Insets)
	 */
	@Override
	public void paint(IFigure figure, Graphics g, Insets insets)
	{
		g.setLineStyle(Graphics.LINE_SOLID);
		g.setForegroundColor(ColorConstants.buttonLightest);
		Rectangle r = getPaintRectangle(figure, insets);
		r.resize(-1, -1);
		g.setLineWidth(2);
		g.drawLine(r.x, r.y + 1, r.right(), r.y + 1);
		g.drawLine(r.x + 1, r.y + 2, r.x + 1, r.bottom());
		g.setLineWidth(1);
		g.setForegroundColor(ColorConstants.buttonDarker);
		g.drawLine(r.right() - 1, r.y + 1, r.right() - 1, r.bottom());
		g.setForegroundColor(ColorConstants.buttonDarkest);
		g.drawLine(r.right(), r.y, r.right(), r.bottom());
	}
}
