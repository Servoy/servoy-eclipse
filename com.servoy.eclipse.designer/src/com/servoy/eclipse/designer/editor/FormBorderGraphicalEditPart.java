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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Handle;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.gef.editparts.LayerManager;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.internal.core.FormImageNotifier;
import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageFigureController;
import com.servoy.eclipse.designer.util.BoundsImageFigure;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.JavaVersion;
import com.servoy.j2db.util.Utils;

/**
 * Edit part for painting the form border.
 * This editpart is only for showing the border, it cannot be selected.
 * 
 * It is also used for resizing the form.
 * 
 * @since 6.0
 * 
 * @author rgansevles
 *
 */
public class FormBorderGraphicalEditPart extends AbstractGraphicalEditPart
{
	public static final int BORDER_MARGIN = 10;

	private final IApplication application;

	protected ImageFigureController imageFigureController;
	private FormImageNotifier borderImageNotifier;

	private List<Handle> handles;

	public FormBorderGraphicalEditPart(IApplication application, BorderModel model)
	{
		this.application = application;
		setModel(model);
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.LAYOUT_ROLE, new FormResizableEditPolicy(this));
	}

	protected void createHandles()
	{
		removeHandles();
		EditPartViewer viewer = getViewer();
		if (viewer != null)
		{
			LayerManager layermanager = (LayerManager)viewer.getEditPartRegistry().get(LayerManager.ID);
			if (layermanager != null)
			{
				handles = new ArrayList<Handle>();
				handles.add(new FormResizeHandle(this, PositionConstants.EAST)); // resize form via right side of form

				Iterator<Part> parts = getModel().flattenedForm.getParts();
				while (parts.hasNext())
				{
					Part next = parts.next();
					PartMoveHandle handle = new PartMoveHandle(next, this, PositionConstants.NORTH);
					handles.add(handle);
					if (!parts.hasNext())
					{
						// resize form via handle in right-bottom corner
						handles.add(new ResizeFormAndMovePartHandle(next, this, PositionConstants.SOUTH_EAST));
					}
					else
					{
						handle.setOpaque(true); // paint all but the last one
					}
				}

				IFigure layer = layermanager.getLayer(LayerConstants.HANDLE_LAYER);
				for (int i = 0; i < handles.size(); i++)
				{
					layer.add((IFigure)handles.get(i));
				}
			}
		}
	}

	protected void removeHandles()
	{
		if (handles != null)
		{
			LayerManager layermanager = (LayerManager)getViewer().getEditPartRegistry().get(LayerManager.ID);
			IFigure layer = layermanager.getLayer(LayerConstants.HANDLE_LAYER);
			for (Handle handle : handles)
			{
				layer.remove((IFigure)handle);
			}

			handles = null;
		}
	}

	@Override
	public BoundsImageFigure getFigure()
	{
		return (BoundsImageFigure)super.getFigure();
	}

	@Override
	protected IFigure createFigure()
	{
		BoundsImageFigure fig = new BoundsImageFigure();
		imageFigureController = new ImageFigureController();
		imageFigureController.setImageFigure(fig);
		return updateFigure(fig);
	}

	protected IFigure updateFigure(BoundsImageFigure fig)
	{
		java.awt.Dimension size = getModel().flattenedForm.getSize();
		// add border size
		Insets insets = IFigure.NO_INSETS;
		final javax.swing.border.Border border = ElementFactory.getFormBorder(application, getModel().flattenedForm);
		if (border != null)
		{
			java.awt.Insets borderInsets;
			//TODO: find better way to handle this for MAC running with java 1.7
			if (Utils.isAppleMacOS() && JavaVersion.CURRENT_JAVA_VERSION.major >= 7)
			{
				final java.awt.Insets[] fInset = { null };
				try
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(new Runnable()
					{
						public void run()
						{
							fInset[0] = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
						}
					});
				}
				catch (Exception e)
				{
					ServoyLog.logError("Error getting border insets for border  " + border, e);
				}
				borderInsets = fInset[0];
			}
			else
			{
				borderInsets = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
			}

			insets = new Insets(borderInsets.top, borderInsets.left, borderInsets.bottom, borderInsets.right);
		}

		fig.setBorder(new MarginBorder(insets));
		fig.setBounds(new Rectangle(0, 0, size.width, size.height).expand(insets));
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
	public BorderModel getModel()
	{
		return (BorderModel)super.getModel();
	}

	@Override
	public void activate()
	{
		super.activate();
		imageFigureController.setImageNotifier(getFieldImageNotifier());
		createHandles();
		// scroll form so that border is shown, have to asyncExec because layout is not done yet.
		Display.getCurrent().asyncExec(new Runnable()
		{
			public void run()
			{
				if (getParent() != null) ((ModifiedScrollingGraphicalViewer)getViewer()).scrollTo(-BORDER_MARGIN, -BORDER_MARGIN);
			}
		});
	}

	@Override
	public void deactivate()
	{
		if (imageFigureController != null)
		{
			imageFigureController.deactivate();
		}
		removeHandles();
		super.deactivate();
	}

	protected IImageNotifier getFieldImageNotifier()
	{
		if (borderImageNotifier == null)
		{
			borderImageNotifier = new FormImageNotifier(application, getModel().flattenedForm);
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
		public final Form flattenedForm;

		/**
		 * @param flattenedForm
		 */
		public BorderModel(Form flattenedForm)
		{
			this.flattenedForm = flattenedForm;
		}
	}
}
