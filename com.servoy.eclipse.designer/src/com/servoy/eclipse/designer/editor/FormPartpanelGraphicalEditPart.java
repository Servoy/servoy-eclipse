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

import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageFigureController;
import com.servoy.eclipse.designer.internal.core.PartImageNotifier;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;

/**
 * Edit part for painting the part background, it cannot be selected.
 *
 * @since 6.1
 *
 * @author rgansevles
 *
 */
public class FormPartpanelGraphicalEditPart extends BaseGraphicalEditPart
{
	private final IApplication application;

	protected ImageFigureController imageFigureController;
	private PartImageNotifier partImageNotifier;

	public FormPartpanelGraphicalEditPart(IApplication application, PartpanelModel model)
	{
		this.application = application;
		setModel(model);
	}

	@Override
	protected void createEditPolicies()
	{
	}

	@Override
	protected IFigure createFigure()
	{
		ImageFigure fig = new ImageFigure();
		imageFigureController = new ImageFigureController();
		imageFigureController.setImageFigure(fig);
		return updateFigure(fig);
	}

	protected IFigure updateFigure(IFigure fig)
	{
		Form flattenedForm = application.getFlattenedSolution().getFlattenedForm(getModel().context);
		int start = flattenedForm.getPartStartYPos(getModel().part.getUUID().toString());
		fig.setBounds(new Rectangle(0, start, flattenedForm.getWidth(), getModel().part.getHeight() - start));

		return fig;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		updateFigure(getFigure());
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}

	@Override
	public boolean isSelectable()
	{
		return false;
	}

	@Override
	public PartpanelModel getModel()
	{
		return (PartpanelModel)super.getModel();
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
		if (partImageNotifier == null)
		{
			partImageNotifier = new PartImageNotifier(application, getModel().part, getModel().context);
		}
		return partImageNotifier;
	}

	/**
	 * Model container for part.
	 * Note: need wrapper to not conflict with form model in editpart registry.
	 *
	 * @author rgansevles
	 *
	 */
	public static class PartpanelModel
	{
		public final Part part;
		public final Form context;

		/**
		 * @param flattenedForm
		 */
		public PartpanelModel(Part part, Form context)
		{
			this.part = part;
			this.context = context;
		}
	}
}
