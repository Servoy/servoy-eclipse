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
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.designer.property.PropertyDirectEditManager;
import com.servoy.eclipse.designer.property.PropertyDirectEditManager.PropertyCellEditorLocator;
import com.servoy.eclipse.designer.property.PropertyDirectEditPolicy;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.IRAGTEST;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StaticContentSpecLoader.TypedProperty;

/**
 * Graphical editpart for a IPersist.
 *
 * @author rgansevles
 */
public class PersistGraphicalEditPart extends BasePersistGraphicalEditPart
{
	private DirectEditManager directEditManager;
	private IRAGTEST persistProperties;

	private final Form form;
	private final IFigureFactory< ? extends PersistImageFigure> figureFactory;

	public PersistGraphicalEditPart(IApplication application, IPersist model, Form form, boolean inherited,
		IFigureFactory< ? extends PersistImageFigure> figureFactory)
	{
		super(application, model, inherited);
		this.form = form;
		this.figureFactory = figureFactory;
		setModel(model);
	}

	@Override
	public void deactivate()
	{
		if (figure != null)
		{
			getFigure().deactivate();
		}
		super.deactivate();
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(PasteToSupportChildsEditPolicy.PASTE_ROLE, new PasteToSupportChildsEditPolicy(application, getFieldPositioner()));
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new PersistEditPolicy(application, getFieldPositioner()));
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new PropertyDirectEditPolicy(getPersist(), form));
	}

	protected IRAGTEST getPersistProperties()
	{
		if (persistProperties == null)
		{
			persistProperties = PersistPropertySource.createPersistPropertySource(getPersist(), form, false);
		}
		return persistProperties;
	}

	protected Object getPersistPropertyValue(String id)
	{
		Object value = getPersistProperties().getPropertyValue(id);
		if (value instanceof ComplexProperty< ? >)
		{
			value = ((ComplexProperty< ? >)value).getValue();
		}
		return value;
	}

	@Override
	public PersistImageFigure getFigure()
	{
		return (PersistImageFigure)super.getFigure();
	}

	@Override
	protected PersistImageFigure createFigure()
	{
		return figureFactory.createFigure(this);
	}

	protected void applyBounds(Object model, IFigure fig)
	{
		if (model instanceof ISupportBounds)
		{
			int x = 0;
			int y = 0;
			int width = 60;
			int height = 20;
			java.awt.Point loc = CSSPositionUtils.getLocation((ISupportBounds)model);
			if (loc != null)
			{
				x = loc.x > 0 ? loc.x : x;
				y = loc.y > 0 ? loc.y : y;
			}
			java.awt.Dimension size = CSSPositionUtils.getSize((ISupportBounds)model);
			if (size != null)
			{
				width = size.width > 0 ? size.width : width;
				height = size.height > 0 ? size.height : height;
			}
			fig.setBounds(new Rectangle(x, y, width, height));
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
			TypedProperty<String> prop;
			if ((getPersist()).getTypeID() == IRepository.FIELDS)
			{
				prop = StaticContentSpecLoader.PROPERTY_DATAPROVIDERID;
			}
			else
			{
				prop = StaticContentSpecLoader.PROPERTY_TEXT;
			}
			if (getPersistProperties().getPropertyDescriptor(prop.getPropertyName()) != null)
			{
				directEditManager = new PropertyDirectEditManager(this, new PropertyCellEditorLocator(this), prop.getPropertyName());
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

		Display.getCurrent().asyncExec(new Runnable()
		{
			public void run()
			{
				getFigure().refresh();
			}
		});
	}
}
