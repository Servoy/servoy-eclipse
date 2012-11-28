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

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.persistence.Form;

/** 
 * Layout for form elements in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormLayoutManager extends AbstractLayout
{
	private final Form form;

	public MobileFormLayoutManager(Form form)
	{
		this.form = form;
	}

	public void layout(IFigure container)
	{
		// children are based on model order as created in editPart.getModelChildren()
		int formWidth = ModelUtils.getEditingFlattenedSolution(form).getFlattenedForm(form).getWidth();
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
			int width = formWidth - (2 * x);
			y += height;

			Dimension childSize = child.getPreferredSize(width, -1);
			height = childSize.height == 0 ? 55 : childSize.height;

			child.setBounds(new Rectangle(x, y, width, height));
		}
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		return new Dimension(wHint, hHint);
	}
}
