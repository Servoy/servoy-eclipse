/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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


import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.internal.core.BorderImageNotifier;
import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageFigureController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;

/**
 * Edit part for painting the form border.
 * This editpart is only for showing the border, it cannot be selected and has no commands.
 * 
 * @author rgansevles
 *
 */
public class FormBorderGraphicalEditPart extends AbstractGraphicalEditPart
{
	public static final int BORDER_MARGIN = 10;

	private final IApplication application;

	protected ImageFigureController imageFigureController;
	private BorderImageNotifier borderImageNotifier;

	public FormBorderGraphicalEditPart(IApplication application, BorderModel model)
	{
		this.application = application;
		setModel(model);
	}

	@Override
	protected IFigure createFigure()
	{
		ImageFigure fig = new ImageFigure();
		imageFigureController = new ImageFigureController();
		imageFigureController.setImageFigure(fig);
		return applyBounds(fig);
	}

	protected IFigure applyBounds(IFigure fig)
	{
		java.awt.Dimension size = getBorderModel().form.getSize();
		// add border size
		javax.swing.border.Border border = ElementFactory.getFormBorder(application, getBorderModel().form);
		Rectangle bounds;
		if (border == null)
		{
			bounds = new Rectangle(0, 0, size.width, size.height);
		}
		else
		{
			java.awt.Insets borderInsets = border.getBorderInsets(null);
			bounds = new Rectangle(-borderInsets.left, -borderInsets.top, size.width + borderInsets.left + borderInsets.right, size.height + borderInsets.top +
				borderInsets.bottom);
			// add some space for borders that print outside the insets (like TitleBorder)
			bounds.expand(Math.max(BORDER_MARGIN - borderInsets.left, 0), Math.max(BORDER_MARGIN - borderInsets.top, 0));
		}

		fig.setBounds(bounds);
		return fig;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		applyBounds(getFigure());
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}

	@Override
	public boolean isSelectable()
	{
		return false;
	}

	protected BorderModel getBorderModel()
	{
		return (BorderModel)getModel();
	}

	@Override
	protected void createEditPolicies()
	{
	}

	@Override
	public void activate()
	{
		super.activate();
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}

	@Override
	public void deactivate()
	{
		if (imageFigureController != null)
		{
			imageFigureController.deactivate();
		}
		super.deactivate();
	}

	protected IImageNotifier getFieldImageNotifier()
	{
		if (borderImageNotifier == null)
		{
			borderImageNotifier = new BorderImageNotifier(application, getBorderModel().form);
		}
		return borderImageNotifier;
	}

	/** 
	 * Model container for border.
	 * Note: need wrapper to not conflict with form model in editpart registry.
	 * 
	 * @author rgansevles
	 *
	 */
	public static class BorderModel
	{

		public final Form form;

		/**
		 * @param form
		 */
		public BorderModel(Form form)
		{
			this.form = form;
		}
	}

}
