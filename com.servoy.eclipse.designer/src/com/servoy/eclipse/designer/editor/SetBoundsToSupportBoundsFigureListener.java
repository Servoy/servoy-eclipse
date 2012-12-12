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

package com.servoy.eclipse.designer.editor;

import java.awt.Dimension;
import java.awt.Point;

import org.eclipse.draw2d.FigureListener;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.eclipse.designer.property.FormElementGroupPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.ISupportBounds;

/**
 * Temporary class used by mobile form editor to save location and size of edit part figure in underlying persist.
 * Added to be able to debug mobile forms in developer
 * 
 * @author rgansevles
 *
 */
public class SetBoundsToSupportBoundsFigureListener implements FigureListener
{
	private final Form form;
	private final ISupportBounds element;
	private final boolean relativeToParent;

	public SetBoundsToSupportBoundsFigureListener(Form context, ISupportBounds element, boolean relativeToParent)
	{
		this.form = context;
		this.element = element;
		this.relativeToParent = relativeToParent;
	}

	public void figureMoved(IFigure figure)
	{
		Rectangle bounds = figure.getBounds();

		if (relativeToParent)
		{
			bounds = Rectangle.SINGLETON.setBounds(bounds); // copy
			Rectangle parentBounds = figure.getParent().getBounds();
			bounds.translate(-parentBounds.x, -parentBounds.y);
		}

		Point loc = element.getLocation();
		if (loc == null || loc.x != bounds.x || loc.y != bounds.y)
		{
			Point newLocation = new Point(bounds.x, bounds.y);
			if (element instanceof FormElementGroup)
			{
				FormElementGroupPropertySource formElementGroupPropertySource = new FormElementGroupPropertySource((FormElementGroup)element, form);
				formElementGroupPropertySource.setLocation(newLocation);
			}
			else
			{
				element.setLocation(newLocation);
			}
		}
		Dimension dim = element.getSize();
		if (dim == null || dim.width != bounds.width || dim.height != bounds.height)
		{
			Dimension newSize = new Dimension(bounds.width, bounds.height);
			if (element instanceof FormElementGroup)
			{
				FormElementGroupPropertySource formElementGroupPropertySource = new FormElementGroupPropertySource((FormElementGroup)element, null);
				formElementGroupPropertySource.setSize(newSize);
			}
			else
			{
				element.setSize(newSize);
			}
		}
	}
}
