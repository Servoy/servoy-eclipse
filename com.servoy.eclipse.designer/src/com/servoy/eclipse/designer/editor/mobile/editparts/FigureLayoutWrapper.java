/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;

import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.eclipse.designer.editor.PersistImageFigure;
import com.servoy.j2db.debug.layout.ILayoutWrapper;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

/**
 * Layout wrapper for Figures.
 * 
 * @author rgansevles
 *
 */
public class FigureLayoutWrapper implements ILayoutWrapper
{
	private final IFigure figure;

	/**
	 * @param child
	 */
	public FigureLayoutWrapper(IFigure child)
	{
		this.figure = child;
	}

	@Override
	public void setBounds(int x, int y, int width, int height)
	{
		figure.setBounds(new Rectangle(x, y, width, height));
	}

	@Override
	public int getX()
	{
		return figure.getBounds().x;
	}

	@Override
	public int getY()
	{
		return figure.getBounds().y;
	}

	@Override
	public int getWidth()
	{
		return figure.getBounds().width;
	}

	@Override
	public int getHeight()
	{
		return figure.getBounds().height;
	}

	@Override
	public int getPreferredHeight()
	{
		return figure.getPreferredSize(-1, -1).height;
	}

	@Override
	public MobileFormSection getElementType()
	{
		if (figure instanceof MobilePartFigure)
		{
			if (((MobilePartFigure)figure).getPartType() == Part.HEADER)
			{
				return MobileFormSection.Header;
			}
			if (((MobilePartFigure)figure).getPartType() == Part.FOOTER)
			{
				return MobileFormSection.Footer;
			}
		}
		return MobileFormSection.ContentElement;
	}

	@Override
	public <T> T getMobileProperty(MobileProperty<T> property)
	{
		if (figure instanceof PersistImageFigure)
		{
			IPersist persist = ((PersistImageFigure)figure).getPersist();
			if (persist instanceof AbstractBase)
			{
				return (T)((AbstractBase)persist).getCustomMobileProperty(property.propertyName);
			}
		}

		return null;
	}

}
