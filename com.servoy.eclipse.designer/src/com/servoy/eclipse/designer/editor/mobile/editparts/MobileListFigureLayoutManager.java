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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.draw2d.AbstractLayout;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListElementEditpart.MobileListElementType;

/**
 * Layout items in (inset) list.
 * 
 * @author rgansevles
 *
 */
public class MobileListFigureLayoutManager extends AbstractLayout
{
	Map<IFigure, MobileListElementType> constraints = new HashMap<IFigure, MobileListElementType>();

	public void layout(IFigure container)
	{
		Rectangle containerBounds = container.getBounds();

		for (IFigure child : (List<IFigure>)container.getChildren())
		{

			MobileListElementType constraint = getConstraint(child);
			if (constraint != null)
			{
				int y = containerBounds.y + 43;
				int height = 30;
				int x;
				int width;
				switch (constraint)
				{
					case Header :
						y = containerBounds.y;
						x = containerBounds.x;
						width = containerBounds.width;
						break;

					case Image :
						x = containerBounds.x + 2;
						width = 30;
						break;

					case Icon :
						width = 30;
						x = containerBounds.x + containerBounds.width - 2 - width;
						break;

					case Button :
						x = containerBounds.x + 34;
						width = containerBounds.width - 200;
						break;

					case Subtext :
						y = containerBounds.y + 75;
						x = containerBounds.x + 34;
						width = 200;
						height = 20;
						break;

					case CountBubble :
						x = containerBounds.x + containerBounds.width - 80;
						width = 30;
						height = (containerBounds.height - 30) / 2;
						break;

					default :
						continue;
				}

				child.setBounds(new Rectangle(x, y, width, height));
			}
		}
	}

	@Override
	public MobileListElementType getConstraint(IFigure child)
	{
		return constraints.get(child);
	}

	@Override
	public void setConstraint(IFigure figure, Object newConstraint)
	{
		super.setConstraint(figure, newConstraint);
		if (newConstraint instanceof MobileListElementType)
		{
			constraints.put(figure, (MobileListElementType)newConstraint);
		}
	}

	@Override
	public void remove(IFigure figure)
	{
		super.remove(figure);
		constraints.remove(figure);
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		return new Dimension(wHint, 100);
	}
}
