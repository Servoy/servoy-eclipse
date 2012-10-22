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

import java.util.List;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

/**
 * Layout title above input
 * 
 * @author rgansevles
 *
 */
public class MobileGroupLayoutManager extends AbstractLayout
{
	public void layout(IFigure container)
	{
		Rectangle containerBounds = container.getBounds();
		// children are based on model order as created in editPart.getModelChildren()

		Rectangle.SINGLETON.height = (containerBounds.height - 4) / 2;


		int i = 0;
		List<IFigure> children = container.getChildren();
		for (IFigure child : children)
		{
			if (i == 0)
			{
				// first child is Title
				Rectangle.SINGLETON.x = containerBounds.x + 2;
				Rectangle.SINGLETON.y = containerBounds.y + 2;
				Rectangle.SINGLETON.width = containerBounds.width - 4;
			}
			else
			{
				// distribute other children over line 2
				Rectangle.SINGLETON.width = (containerBounds.width - 4) / (children.size() - 1);
				Rectangle.SINGLETON.x = containerBounds.x + 2 + ((i - 1) * Rectangle.SINGLETON.width);
				Rectangle.SINGLETON.y = containerBounds.y + 4 + Rectangle.SINGLETON.height;
			}

			child.setBounds(Rectangle.SINGLETON);
			i++;
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		// Title above input
		return new Dimension(wHint, 80);
	}
}
