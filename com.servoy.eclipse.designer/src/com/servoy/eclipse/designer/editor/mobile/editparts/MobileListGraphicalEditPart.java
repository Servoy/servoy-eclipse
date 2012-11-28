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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.editparts.AbstractGraphicalEditPart;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.ComponentDeleteEditPolicy;
import com.servoy.eclipse.designer.editor.SetBoundsToSupportBoundsFigureListener;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListElementEditpart.MobileListElementType;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Pair;

/**
 * Edit part for Lists in mobile form editor.
 * Model is MobileListModel.
 * 
 * @author rgansevles
 *
 */
public class MobileListGraphicalEditPart extends AbstractGraphicalEditPart implements IPersistChangeListener
{
	private final IApplication application;
	private final BaseVisualFormEditor editorPart;

	public MobileListGraphicalEditPart(IApplication application, MobileListModel model, BaseVisualFormEditor editorPart)
	{
		this.application = application;
		this.editorPart = editorPart;
		setModel(model);
	}

	@Override
	public MobileListModel getModel()
	{
		return (MobileListModel)super.getModel();
	}

	@Override
	protected List<Pair<BaseComponent, MobileListElementType>> getModelChildren()
	{
		MobileListModel model = getModel();
		List<Pair<BaseComponent, MobileListElementType>> modelChildren = new ArrayList<Pair<BaseComponent, MobileListElementType>>(4);
		if (model.header != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.header, MobileListElementType.Header));
		}
		if (model.image != null && model.image.getDataProviderID() != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.image, MobileListElementType.Image));
		}
		else if (model.button != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.button, MobileListElementType.Icon));
		}
		if (model.button != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.button, MobileListElementType.Button));
		}
		if (model.subtext != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.subtext, MobileListElementType.Subtext));
		}
		if (model.countBubble != null && model.countBubble.getDataProviderID() != null)
		{
			modelChildren.add(new Pair<BaseComponent, MobileListElementType>(model.countBubble, MobileListElementType.CountBubble));
		}
		return modelChildren;

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
		IFigure fig = new MobileListFigure(getModel().tabPanel != null); // when tabPanel is filled this is an inset list
		fig.setLayoutManager(new MobileListFigureLayoutManager());
		fig.addFigureListener(new SetBoundsToSupportBoundsFigureListener(getModel(), false));
		return fig;
	}

	@Override
	protected void addChildVisual(EditPart childEditPart, int index)
	{
		// make sure the type is set as constraint for the layout manager (see MobileListFigureLayoutManager)
		IFigure child = ((GraphicalEditPart)childEditPart).getFigure();
		MobileListElementType constraint = null;
		if (childEditPart instanceof MobileListElementEditpart)
		{
			constraint = ((MobileListElementEditpart)childEditPart).getType();
		}
		getContentPane().add(child, constraint, index);
	}


	@Override
	protected EditPart createChild(Object child)
	{
		return new MobileListElementEditpart(application, editorPart, (Pair<BaseComponent, MobileListElementType>)child);
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

	public void persistChanges(Collection<IPersist> changes)
	{
		MobileListModel m = getModel();
		for (IPersist persist : changes)
		{
			if (persist == m.button || persist == m.containedForm || persist == m.countBubble || persist == m.form || persist == m.header ||
				persist == m.image || persist == m.subtext || persist == m.tab || persist == m.tabPanel)
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

}
