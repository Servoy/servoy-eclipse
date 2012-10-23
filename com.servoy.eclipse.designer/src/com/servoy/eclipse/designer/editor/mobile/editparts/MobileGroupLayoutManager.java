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

		Rectangle.SINGLETON.x = containerBounds.x + 2;
		Rectangle.SINGLETON.y = containerBounds.y + 2;
		Rectangle.SINGLETON.width = containerBounds.width - 4;

		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			Dimension childPrefSize = child.getPreferredSize();
			Rectangle.SINGLETON.height = (childPrefSize.height > 0 ? childPrefSize.height : 38);

			child.setBounds(Rectangle.SINGLETON);

			Rectangle.SINGLETON.y += Rectangle.SINGLETON.height + 2;
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		int height = 0;
		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			Dimension childPrefSize = child.getPreferredSize();
			height += (childPrefSize.height > 0 ? childPrefSize.height : 38) + 2;
		}

		return new Dimension(wHint, height);
	}
}
