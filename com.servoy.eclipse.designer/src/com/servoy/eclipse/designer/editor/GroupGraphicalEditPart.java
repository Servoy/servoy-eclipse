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


import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * Graphical edit part for element groups.
 * 
 * @author rob
 * 
 */
public class GroupGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	protected IApplication application;

	private final VisualFormEditor editorPart;
	private final Form form;

	public GroupGraphicalEditPart(IApplication application, VisualFormEditor editorPart, Form form, FormElementGroup group)
	{
		this.application = application;
		this.editorPart = editorPart;
		this.form = form;
		setModel(group);
	}

	@Override
	protected List<IFormElement> getModelChildren()
	{
		return Utils.asList(getGroup().getElements());
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
		updateFigure(fig);
		return fig;
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
		EditPart editPart = FormGraphicalEditPart.createChild(application, editorPart, form, child);
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
	public void activate()
	{
		// listen to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the elements
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		String groupId = getGroup().getGroupID();
		boolean found = false;
		Iterator<IPersist> iterator = changes.iterator();
		while (groupId != null && !found && iterator.hasNext())
		{
			IPersist persist = iterator.next();
			found = persist instanceof IFormElement && groupId.equals(((IFormElement)persist).getGroupID());
		}
		if (found)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					updateFigure((GroupFigure)getFigure());
				}
			});
		}
	}

	public FormElementGroup getGroup()
	{
		return (FormElementGroup)getModel();
	}

}
