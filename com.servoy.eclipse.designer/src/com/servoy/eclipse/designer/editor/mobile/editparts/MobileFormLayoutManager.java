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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.XYLayout;
import org.eclipse.draw2d.geometry.Dimension;

import com.servoy.j2db.debug.layout.ILayoutWrapper;
import com.servoy.j2db.debug.layout.MobileFormLayout;

/** 
 * Layout for form elements in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormLayoutManager extends XYLayout
{
	@Override
	public void layout(IFigure container)
	{
		List<ILayoutWrapper> elements = new ArrayList<ILayoutWrapper>();
		// children are based on model order as created in editPart.getModelChildren()
		for (IFigure child : (List<IFigure>)container.getChildren())
		{
			elements.add(new FigureLayoutWrapper(child));
		}
		MobileFormLayout.layoutForm(elements);
	}

	@Override
	protected Dimension calculatePreferredSize(IFigure container, int wHint, int hHint)
	{
		return new Dimension(wHint, hHint);
	}
}
