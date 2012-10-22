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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.util.Collection;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.ComponentDeleteEditPolicy;
import com.servoy.eclipse.designer.editor.SetBoundsToPartFigureListener;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;

/**
 * Edit part for header or footer part in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public abstract class MobilePartGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	protected IApplication application;

	private final BaseVisualFormEditor editorPart;

	public MobilePartGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Part model)
	{
		this.application = application;
		this.editorPart = editorPart;
		setModel(model);
	}

	/**
	 * @return the editorPart
	 */
	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	@Override
	public Part getModel()
	{
		return (Part)super.getModel();
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentDeleteEditPolicy());
		installEditPolicy(EditPolicy.SELECTION_FEEDBACK_ROLE, new MobileSelectionEditPolicy());
	}

	@Override
	protected IFigure createFigure()
	{
		IFigure fig = new MobilePartFigure();
		fig.addFigureListener(new SetBoundsToPartFigureListener(getModel()));
		fig.setLayoutManager(new MobileFormPartLayoutManager(getModel().getPartType()));
		return fig;
	}

	@Override
	protected EditPart createChild(Object child)
	{
		return MobileFormGraphicalEditPart.createChild(application, editorPart, editorPart.getForm(), child);
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
		for (IPersist persist : changes)
		{
			if (getEditorPart().getForm().equals(persist.getAncestor(IRepository.FORMS)))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						refresh();
					}
				});
				return;
			}
		}
	}

//	@Override
//	public DragTracker getDragTracker(Request request)
//	{
//		return BasePersistGraphicalEditPart.createDragTracker(this, request);
//	}


}
