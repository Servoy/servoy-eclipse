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
package com.servoy.eclipse.designer.internal.core;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;

/**
 * Border for painting elements in form designer.
 * 
 * @author rgansevles
 */

public class OutlineBorder extends AbstractBorder
{

	protected Color foreground = ColorConstants.black, background;
	private int alpha = 255;

	private boolean borderDisabled; // Is the border disabled? If it is then it won't draw.
	private boolean overrideAndDisable; // A temporary override of the border disabled state. If true, then override and disable, if false no override. 

	private static final Insets insets = new Insets(0, 0, 0, 0);
	protected int lineStyle = SWT.LINE_SOLID;

	public OutlineBorder()
	{
	}

	public OutlineBorder(Color foreground, Color background)
	{
		this.foreground = foreground;
		this.background = background;
	}

	public OutlineBorder(int alpha, Color foreground, Color background)
	{
		this(foreground, background);
		setAlpha(alpha);
	}

	public OutlineBorder(Color foreground, Color background, int lineStyle)
	{
		this(foreground, background);
		this.lineStyle = lineStyle;
	}

	public OutlineBorder(int alpha, Color foreground, Color background, int lineStyle)
	{
		this(alpha, foreground, background);
		this.lineStyle = lineStyle;
	}

	public void paint(IFigure aFigure, Graphics g, Insets ins)
	{
		if (overrideAndDisable || borderDisabled) return; // Disabled, don't pain.
		Rectangle r = getPaintRectangle(aFigure, ins);
		r.resize(-1, -1); // Make room for the outline.
		try
		{
			g.setAlpha(getAlpha());
		}
		catch (SWTException e)
		{
			// Occurs if alpha's not available. No check on Graphics that tests for this yet.
		}
		g.setForegroundColor(foreground);
		if (lineStyle != SWT.LINE_SOLID)
		{
			// Non-solid lines need a background color to be set. If we have one use it, else compute it.
			if (background != null) g.setBackgroundColor(background);
			else
			{
				// If no background is set then make the background black
				// and set it to XOR true.  This means the line will dash over
				// the background.  The foreground will also XOR
				// so it only works well if the foreground is Black or Gray.  Colors
				// don't work well because they only paint true on black
				// areas
				g.setBackgroundColor(ColorConstants.black);
				g.setXORMode(true);
			}
		}
		g.setLineStyle(lineStyle);
		g.drawRectangle(r);
	}

	public void setColors(Color foreground, Color background)
	{
		this.foreground = foreground;
		this.background = background;
	}

	public void setLineStyle(int aStyle)
	{
		lineStyle = aStyle;
	}

	public Insets getInsets(IFigure aFigure)
	{
		return insets;
	}

	@Override
	public boolean isOpaque()
	{
		return true;
	}

	/**
	 * Set the border to be disable so it doesn't paint.
	 * 
	 * @param borderDisabled
	 *            The borderDisabled to set.
	 * @since 1.0.0
	 */
	public void setBorderDisabled(boolean borderDisabled)
	{
		this.borderDisabled = borderDisabled;
	}

	/**
	 * Return whether the borderDisable flag is on.
	 * 
	 * @return Returns the borderDisabled.
	 */
	public boolean isBorderDisabled()
	{
		return borderDisabled;
	}

	/**
	 * Override the current border disable setting and force a disable if <code>true</code> or restore current border disable state if <code>false</code>
	 * 
	 * @param overrideAndDisable
	 *            <code>true</code> to force disable, <code>false</code> to revert to current disable state.
	 */
	public void setOverrideAndDisable(boolean overrideAndDisable)
	{
		this.overrideAndDisable = overrideAndDisable;
	}

	/**
	 * Answers if currently override the disable state and it is temporarily disabled.
	 * 
	 * @return Returns the overrideAndDisable.
	 */
	public boolean isOverrideAndDisable()
	{
		return overrideAndDisable;
	}

	/**
	 * @param alpha
	 *            The alpha to set.
	 */
	public void setAlpha(int alpha)
	{
		this.alpha = alpha;
	}

	/**
	 * @return Returns the alpha.
	 */
	public int getAlpha()
	{
		return alpha;
	}
}
