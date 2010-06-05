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

import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.ImageFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;

import com.servoy.eclipse.designer.internal.core.IImageNotifier;
import com.servoy.eclipse.designer.internal.core.ImageFigureController;
import com.servoy.eclipse.designer.internal.core.OutlineBorder;
import com.servoy.eclipse.designer.internal.core.PersistImageNotifier;
import com.servoy.eclipse.designer.property.PropertyDirectEditManager;
import com.servoy.eclipse.designer.property.PropertyDirectEditPolicy;
import com.servoy.eclipse.designer.property.PropertyDirectEditManager.PropertyCellEditorLocator;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;

/**
 * Graphical editpart for a IPersist.
 */
public class PersistGraphicalEditPart extends BasePersistGraphicalEditPart
{
	protected ImageFigureController imageFigureController;
	private DirectEditManager directEditManager;
	private PersistPropertySource persistProperties;
	private PersistImageNotifier persistImageNotifier;
	private final Form form;

	public PersistGraphicalEditPart(IApplication application, IPersist model, Form form, boolean readOnly)
	{
		super(application, model, readOnly);
		this.form = form;
		setModel(model);
	}

	@Override
	protected List getModelChildren()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	public void activate()
	{
		super.activate();
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}

	protected IImageNotifier getFieldImageNotifier()
	{
		if (persistImageNotifier == null)
		{
			persistImageNotifier = new PersistImageNotifier(application, getPersist(), form);
		}
		return persistImageNotifier;
	}

	@Override
	public void deactivate()
	{
		if (imageFigureController != null)
		{
			imageFigureController.deactivate();
		}
//		errorNotifier.dispose();
		super.deactivate();
	}

	@Override
	protected void createEditPolicies()
	{
		if (!isReadOnly())
		{
			installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(getFieldPositioner()));
			installEditPolicy(EditPolicy.COMPONENT_ROLE, new PersistEditPolicy(getFieldPositioner()));
			installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new PropertyDirectEditPolicy(getPersistProperties()));
		}
	}

	protected PersistPropertySource getPersistProperties()
	{
		if (persistProperties == null)
		{
			persistProperties = new PersistPropertySource(getPersist(), form, false);
		}
		return persistProperties;
	}

	@Override
	protected IFigure createFigure()
	{
		IPersist model = getPersist();
		ImageFigure fig = new ImageFigure()
		{
			@Override
			public void paint(Graphics graphics)
			{
				super.paint(graphics);
				String drawName = PersistPropertySource.getActualComponentName(getPersist());
				if (drawName != null)
				{
					drawName(this, drawName, FontResource.getDefaultFont(SWT.NORMAL, -1), graphics);
				}
			}
		};
		fig.setBorder(new OutlineBorder(125, isReadOnly() ? ColorConstants.red : ColorConstants.gray, null, Graphics.LINE_DOT));
		imageFigureController = new ImageFigureController();
		imageFigureController.setImageFigure(fig);
		applyBounds(model, fig);

		return fig;
	}

	protected void applyBounds(Object model, IFigure fig)
	{
		if (model instanceof ISupportBounds)
		{
			int x = 0;
			int y = 0;
			int width = 60;
			int height = 20;
			java.awt.Point loc = ((ISupportBounds)model).getLocation();
			if (loc != null)
			{
				x = loc.x > 0 ? loc.x : x;
				y = loc.y > 0 ? loc.y : y;
			}
			((ISupportBounds)model).setLocation(new java.awt.Point(x, y));
			java.awt.Dimension size = ((ISupportBounds)model).getSize();
			if (size != null)
			{
				width = size.width > 0 ? size.width : width;
				height = size.height > 0 ? size.height : height;
			}
			fig.setBounds(new Rectangle(x, y, width, height));
		}
	}

	/**
	 * @param drawName
	 * @param graphics
	 */
	private static void drawName(IFigure figure, String name, Font font, Graphics graphics)
	{
		Font saveFont = graphics.getFont();
		try
		{
			graphics.setFont(font);
			Point bottomLeft = figure.getBounds().getBottomLeft();
			Dimension dim = FigureUtilities.getStringExtents(name, font);
			graphics.drawString(name, bottomLeft.x + 2, bottomLeft.y - dim.height);
		}
		finally
		{
			graphics.setFont(saveFont);
		}
	}

	@Override
	public void performRequest(Request request)
	{
		if (request.getType() == RequestConstants.REQ_DIRECT_EDIT) performDirectEdit();
		else super.performRequest(request);
	}

	protected void performDirectEdit()
	{
		if (directEditManager == null)
		{
			String prop = null;
			if ((getPersist()).getTypeID() == IRepository.FIELDS)
			{
				prop = "dataProviderID";
			}
			else
			{
				prop = "text";
			}
			if (getPersistProperties().getPropertyDescriptor(prop) != null)
			{
				directEditManager = new PropertyDirectEditManager(this, new PropertyCellEditorLocator(this), prop);
			}
		}
		if (directEditManager != null)
		{
			directEditManager.show();
		}
	}

	/**
	 * Persist element has no children
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		return null;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		applyBounds(getModel(), getFigure());
		imageFigureController.setImageNotifier(getFieldImageNotifier());
	}
}
