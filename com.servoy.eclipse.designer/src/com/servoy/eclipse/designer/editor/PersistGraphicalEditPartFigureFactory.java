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

import java.awt.Color;

import javax.swing.border.EmptyBorder;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IImageFigure.ImageChangedListener;
import org.eclipse.gef.GraphicalEditPart;

import com.servoy.eclipse.designer.internal.core.OutlineBorder;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * FigureFactory for persists in regular form editor.
 * 
 * @author rgansevles
 *
 */
public class PersistGraphicalEditPartFigureFactory implements IFigureFactory<PersistImageFigure>
{
	private final IApplication application;
	private final Form form;

	public PersistGraphicalEditPartFigureFactory(IApplication application, Form form)
	{
		this.application = application;
		this.form = form;
	}

	public PersistImageFigure createFigure(final GraphicalEditPart editPart)
	{
		PersistImageFigure fig = new PersistImageFigure(application, (IPersist)editPart.getModel(), form);

		fig.setBorder(new OutlineBorder(125, ((PersistGraphicalEditPart)editPart).isInherited() ? ColorConstants.red : ColorConstants.gray, null,
			Graphics.LINE_DOT));

		// show the border only when you cannot see the element from its background
		fig.addImageChangedListener(new ImageChangedListener()
		{
			public void imageChanged()
			{
				Color paintedBackground = null;
				if (editPart.getModel() instanceof AbstractBase)
				{
					paintedBackground = ((AbstractBase)editPart.getModel()).getRuntimeProperty(PersistPropertySource.LastPaintedBackgroundProperty);
				}

				boolean borderDisabled;
				if (paintedBackground == null)
				{
					// element is transparent
					borderDisabled = false;
				}
				else
				{
					// is there a border?
					Object border = ((PersistGraphicalEditPart)editPart).getPersistPropertyValue(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName());
					if (border != null && !(border instanceof EmptyBorder))
					{
						borderDisabled = true;
					}
					else
					{
						// compare colors form and painted
						Part part = form.getPartAt(editPart.getFigure().getBounds().y);
						if (part == null)
						{
							borderDisabled = false;
						}
						else
						{
							borderDisabled = !paintedBackground.equals(ComponentFactory.getPartBackground(application, part, form));
						}
					}
				}
				((OutlineBorder)editPart.getFigure().getBorder()).setBorderDisabled(borderDisabled);
			}
		});

		return fig;
	}
}
