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

import com.servoy.eclipse.designer.editor.PersistImageFigure;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

/** 
 * Layout for header or footer elements in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormPartLayoutManager extends AbstractLayout
{
	private final int partType;

	public MobileFormPartLayoutManager(int partType)
	{
		this.partType = partType;
	}

	public void layout(IFigure container)
	{
		if (partType == Part.HEADER)
		{
			layoutHeader(container);
		}
		else
		{
			layoutFooter(container);
		}
	}

	/*
	 * left button, text and right button
	 */
	public void layoutHeader(IFigure container)
	{
		Rectangle containerBounds = container.getBounds();
		// children are based on model order as created in editPart.getModelChildren()
		Rectangle.SINGLETON.y = containerBounds.y + 6;
		Rectangle.SINGLETON.height = Math.min(35, containerBounds.height - 10);

		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			Rectangle.SINGLETON.width = Math.min(50, containerBounds.width / 5);
			if (child instanceof PersistImageFigure)
			{
				IPersist persist = ((PersistImageFigure)child).getPersist();
				if (persist instanceof AbstractBase)
				{
					if (((AbstractBase)persist).getCustomMobileProperty("headerLeftButton") != null)
					{
						Rectangle.SINGLETON.x = containerBounds.x + 20;
					}
					else if (((AbstractBase)persist).getCustomMobileProperty("headerText") != null)
					{
						Rectangle.SINGLETON.width = Math.min(80, 2 * containerBounds.width / 5);
						Rectangle.SINGLETON.x = containerBounds.x + (containerBounds.width - Rectangle.SINGLETON.width) / 2;
					}
					else if (((AbstractBase)persist).getCustomMobileProperty("headerRightButton") != null)
					{
						Rectangle.SINGLETON.x = containerBounds.x + containerBounds.width - Rectangle.SINGLETON.width - 20;
					}
					else continue;
				}

				child.setBounds(Rectangle.SINGLETON);
			}
		}
	}

	public void layoutFooter(IFigure container)
	{
		Rectangle containerBounds = container.getBounds();
		// children are based on model order as created in editPart.getModelChildren()
		Rectangle.SINGLETON.x = containerBounds.x + 2;
		Rectangle.SINGLETON.y = containerBounds.y + 2;
		Rectangle.SINGLETON.width = 50;
		Rectangle.SINGLETON.height = 40;

		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			child.setBounds(Rectangle.SINGLETON);

			Rectangle.SINGLETON.x += Rectangle.SINGLETON.width + 2;
			if (Rectangle.SINGLETON.x + Rectangle.SINGLETON.width > containerBounds.width)
			{
				// next line
				Rectangle.SINGLETON.x = containerBounds.x + 2;
				Rectangle.SINGLETON.y += Rectangle.SINGLETON.height + 2;
			}
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		int height;
		if (partType == Part.HEADER)
		{
			height = 50; // always 1 row
		}
		else
		{
			height = 50; // TODO: layout multiple rows if there are many footer items
		}
		return new Dimension(wHint, height);
	}
}
