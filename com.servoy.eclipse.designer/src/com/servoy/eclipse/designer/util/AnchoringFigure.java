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

import org.eclipse.draw2d.Border;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Figure that shows the anchoring state of an ISupportAnchors model.
 * 
 * @author rgansevles
 *
 */
public class AnchoringFigure extends Figure
{
	private final ISupportAnchors anchored;

	protected static final Border TOOL_TIP_BORDER = new MarginBorder(0, 2, 0, 2);

	public AnchoringFigure(ISupportAnchors anchored)
	{
		this.anchored = anchored;
		init();
	}

	/**
	 * Initializes the figure.
	 */
	protected void init()
	{
		setPreferredSize(new Dimension(10, 10));
		Label tooltip = new Label("Anchoring");
		tooltip.setBorder(TOOL_TIP_BORDER);
		setToolTip(tooltip);
	}

	@Override
	public void paintFigure(Graphics g)
	{
		Rectangle r = getBounds();
		g.setBackgroundColor(ColorConstants.gray);
		g.fillRectangle(r.x, r.y, r.width, r.height);

		g.setBackgroundColor(ColorConstants.white);
		g.fillRectangle(r.x + 1, r.y + 1, r.width - 2, r.height - 2);

		int anchors = anchored.getAnchors();
		if (anchors == 0)
		{
			anchors = IAnchorConstants.DEFAULT;
		}
		else if (anchors == -1)
		{
			anchors = 0;
		}

		Rectangle a = new Rectangle();
		a.x = r.x + 1;
		a.y = r.y + 1;
		a.width = r.width - 2;
		a.height = r.height - 2;

		int offSet = 2;
		if ((anchors & IAnchorConstants.NORTH) == 0) //&& (anchors & IAnchorConstants.SOUTH) != 0)
		{
			a.y += offSet;
			a.height -= offSet;
		}
		if ((anchors & IAnchorConstants.WEST) == 0) // && (anchors & IAnchorConstants.EAST) != 0)
		{
			a.x += offSet;
			a.width -= offSet;
		}
		if ((anchors & IAnchorConstants.SOUTH) == 0)
		{
			a.height -= offSet;
		}
		if ((anchors & IAnchorConstants.EAST) == 0)
		{
			a.width -= offSet;
		}

		g.setBackgroundColor(ColorConstants.gray);
		g.fillRectangle(a);
	}
}
