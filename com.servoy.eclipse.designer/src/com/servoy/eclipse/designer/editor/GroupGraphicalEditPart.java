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

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.LineBorder;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;

import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.util.Utils;

/**
 * Graphical edit part for element groups.
 *
 * @author rgansevles
 *
 */
public class GroupGraphicalEditPart extends BaseGroupGraphicalEditPart
{
	public GroupGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Form form, FormElementGroup group)
	{
		super(application, editorPart, form, group);
	}

	@Override
	protected List<ISupportFormElement> getModelChildren()
	{
		List<ISupportFormElement> returnList = Utils.asList(getGroup().getElements());
		Collections.sort(returnList, Form.FORM_INDEX_COMPARATOR);
		return returnList;
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new GroupEditPolicy());
	}

	@Override
	protected IFigure createFigure()
	{
		GroupFigure fig = new GroupFigure();
		fig.setBorder(new LineBorder());
		updateFigure(fig);
		return fig;
	}

	@Override
	protected void doRefresh()
	{
		updateFigure((GroupFigure)getFigure());
	}

	protected void updateFigure(Figure fig)
	{
		java.awt.Rectangle bounds = getGroup().getBounds();
		fig.setBounds(new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height));
	}

	/**
	 * Form part has no children
	 */
	@Override
	protected EditPart createChild(Object child)
	{
		EditPart editPart = FormGraphicalEditPart.createChild(getApplication(), getEditorPart(), getForm(), child);
		if (editPart instanceof BasePersistGraphicalEditPart)
		{
			((BasePersistGraphicalEditPart)editPart).setSelectable(false);
		}
		return editPart;
	}

	@Override
	protected void refreshVisuals()
	{
		super.refreshVisuals();
		updateFigure((GroupFigure)getFigure());
	}

	@Override
	public DragTracker getDragTracker(Request request)
	{
		return BasePersistGraphicalEditPart.createDragTracker(this, request);
	}

}
