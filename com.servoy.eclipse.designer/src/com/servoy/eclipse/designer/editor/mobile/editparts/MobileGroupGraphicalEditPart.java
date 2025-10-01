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
package com.servoy.eclipse.designer.editor.mobile.editparts;


import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;

import com.servoy.eclipse.designer.editor.BaseGroupGraphicalEditPart;
import com.servoy.eclipse.designer.editor.BasePersistGraphicalEditPart;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.ComponentDeleteEditPolicy;
import com.servoy.eclipse.designer.editor.GroupFigure;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.ISupportFormElement;

/**
 * Graphical edit part for element groups in mobile editor.
 *
 * @author rgansevles
 *
 */
public class MobileGroupGraphicalEditPart extends BaseGroupGraphicalEditPart
{
	public MobileGroupGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Form form, FormElementGroup group)
	{
		super(application, editorPart, form, group);
	}

	@Override
	protected List<ISupportFormElement> getModelChildren()
	{
		return MobileFormLayout.getGroupElements(getGroup());
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentDeleteEditPolicy());
	}

	@Override
	protected IFigure createFigure()
	{
		IFigure fig = new GroupFigure();
		fig.setLayoutManager(new MobileGroupLayoutManager());
		fig.addFigureListener(new SetBoundsToSupportBoundsFigureListener(getForm(), getGroup(), false));
		return fig;
	}

	@Override
	protected void doRefresh()
	{
		refreshChildren();
	}

	@Override
	protected EditPart createChild(Object child)
	{
		EditPart editPart = MobileFormGraphicalEditPart.createChild(getApplication(), getEditorPart(), getForm(), child);
		((BasePersistGraphicalEditPart)editPart).setSelectable(false);
		return editPart;
	}
}
