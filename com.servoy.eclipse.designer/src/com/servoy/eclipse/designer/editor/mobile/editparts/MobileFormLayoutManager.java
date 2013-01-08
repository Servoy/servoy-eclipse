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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.j2db.persistence.Part;

/** 
 * Layout for form elements in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormLayoutManager extends XYLayout
{
	public static final int MOBILE_FORM_WIDTH = 350; // fixed width, future: make configurable

	private static final int MIN_FORM_HEIGHT = 250;

	public static final MobileFormLayoutManager INSTANCE = new MobileFormLayoutManager();

	private MobileFormLayoutManager()
	{
	}

	@Override
	public void layout(IFigure container)
	{
		// children are based on model order as created in editPart.getModelChildren()
		int y = 0;
		int height = 0;

		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			int x;
			if (child instanceof MobilePartFigure)
			{
				x = 0;
			}
			else
			{
				x = 10;
				y++;
			}
			int width = MOBILE_FORM_WIDTH - (2 * x);
			y += height;

			Dimension childSize = child.getPreferredSize(width, -1);
			height = childSize.height == 0 ? 55 : childSize.height;

			if (y + height < MIN_FORM_HEIGHT && child instanceof MobilePartFigure && ((MobilePartFigure)child).getPartType() == Part.FOOTER)
			{
				y = MIN_FORM_HEIGHT - height;
			}

			child.setBounds(new Rectangle(x, y, width, height));
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		return new Dimension(wHint, hHint);
	}
}
