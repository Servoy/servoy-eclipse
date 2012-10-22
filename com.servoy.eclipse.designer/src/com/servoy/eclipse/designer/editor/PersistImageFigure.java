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

import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageFigureController;
import com.servoy.eclipse.designer.internal.core.PersistImageNotifier;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Figure for perist elements in form editor.
 * 
 * @author rgansevles
 *
 */
public class PersistImageFigure extends ImageFigure
{
	private final IApplication application;
	private final IPersist persist;
	private final Form form;

	private final ImageFigureController imageFigureController;
	private IImageNotifier persistImageNotifier;


	public PersistImageFigure(IApplication application, IPersist persist, Form form)
	{
		this.application = application;
		this.persist = persist;
		this.form = form;
		imageFigureController = new ImageFigureController();
		imageFigureController.setImageFigure(this);
	}

	public IPersist getPersist()
	{
		return persist;
	}

	protected IImageNotifier getFieldImageNotifier()
	{
		if (persistImageNotifier == null)
		{
			persistImageNotifier = createImageNotifier();
		}
		return persistImageNotifier;
	}

	protected IImageNotifier createImageNotifier()
	{
		return new PersistImageNotifier(application, persist, form, this);
	}

	@Override
	public void paint(Graphics graphics)
	{
		super.paint(graphics);
		String drawName = PersistPropertySource.getActualComponentName(getPersist());
		if (drawName != null)
		{
			Font nameFont = FontResource.getDefaultFont(SWT.NORMAL, -1);
			Font saveFont = graphics.getFont();
			try
			{
				graphics.setFont(nameFont);
				Point bottomLeft = getBounds().getBottomLeft();
				Dimension dim = FigureUtilities.getStringExtents(drawName, nameFont);
				graphics.drawString(drawName, bottomLeft.x + 2, bottomLeft.y - dim.height);
			}
			finally
			{
				graphics.setFont(saveFont);
			}
		}
	}

	public void refresh()
	{
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}

	public void deactivate()
	{
		if (imageFigureController != null)
		{
			imageFigureController.deactivate();
		}
	}
}