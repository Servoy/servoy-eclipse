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

import org.eclipse.draw2d.AbstractBackground;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.handles.HandleBounds;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.designer.Activator;
import com.servoy.eclipse.ui.resource.ColorResource;

/**
 * Figure for (Inset) lists.
 * 
 * @author rgansevles
 * 
 */
public class MobileListFigure extends ImageFigure implements HandleBounds
{
	public static final Image ARROW_IMAGE = Activator.loadImageDescriptorFromBundle("list_arrow.png").createImage();

	public MobileListFigure(boolean isInset)
	{
		super(ARROW_IMAGE, PositionConstants.EAST);
		setOpaque(false); // border handles all
		setBackgroundColor(ColorResource.INSTANCE.getColor(new RGB(245, 245, 245))); // TODO use scheme
		setBorder(new MobileListBackgroundBorder(new Insets(0, 0, 0, 10), isInset));
	}


	@Override
	public Dimension getPreferredSize(int wHint, int hHint)
	{
		if (prefSize != null) return prefSize;
		if (getLayoutManager() != null)
		{
			Dimension d = getLayoutManager().getPreferredSize(this, wHint, hHint);
			if (d != null) return d;
		}
		return super.getPreferredSize(wHint, hHint);
	}

	public Rectangle getHandleBounds()
	{
		// just show the line as rectangle
		return getBounds();
	}

	public static class MobileListBackgroundBorder extends AbstractBackground
	{
		private final Insets insets;
		private final boolean isInset;

		/**
		 * @param isInset
		 */
		public MobileListBackgroundBorder(Insets insets, boolean isInset)
		{
			this.insets = insets;
			this.isInset = isInset;
		}

		@Override
		public Insets getInsets(IFigure figure)
		{
			return insets;
		}

		@Override
		public void paintBackground(IFigure figure, Graphics graphics, Insets is)
		{
			int arc = isInset ? 20 : 0;
			graphics.fillRoundRectangle(figure.getBounds(), arc, arc);
		}
	}

}
