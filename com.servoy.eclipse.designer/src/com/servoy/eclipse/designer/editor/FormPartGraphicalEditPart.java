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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.DirectEditManager;
import org.eclipse.gef.tools.DragEditPartsTracker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.internal.MarqueeDragTracker;
import com.servoy.eclipse.designer.property.IFieldPositionerProvider;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.eclipse.designer.property.PropertyDirectEditManager;
import com.servoy.eclipse.designer.property.PropertyDirectEditManager.PropertyCellEditorLocator;
import com.servoy.eclipse.designer.property.PropertyDirectEditPolicy;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.StaticContentSpecLoader;

/**
 * Edit part for parts in form designer.
 *
 * @author rgansevles
 */

public class FormPartGraphicalEditPart extends BaseGraphicalEditPart implements IPersistEditPart, IFieldPositionerProvider, IPersistChangeListener
{
	protected IApplication application;
	private DirectEditManager directEditManager;

	private final boolean inherited;
	private final BaseVisualFormEditor editorPart;

	public FormPartGraphicalEditPart(IApplication application, BaseVisualFormEditor editorPart, Part part, boolean inherited)
	{
		this.application = application;
		this.editorPart = editorPart;
		this.inherited = inherited;
		setModel(part);
	}

	@Override
	protected List getModelChildren()
	{
		return Collections.EMPTY_LIST;
	}

	@Override
	protected void createEditPolicies()
	{
		installEditPolicy(EditPolicy.COMPONENT_ROLE, new ComponentDeleteEditPolicy());
		installEditPolicy(EditPolicy.DIRECT_EDIT_ROLE, new PropertyDirectEditPolicy(getPersist(), editorPart.getForm()));
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
			directEditManager = new PropertyDirectEditManager(this, new PropertyCellEditorLocator(this),
				StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName());
		}
		directEditManager.show();
	}


	@Override
	protected IFigure createFigure()
	{
		Part part = getPersist();
		PartFigure fig = new PartFigure();
		fig.setCursor(Cursors.SIZENS);
		fig.setFont(FontResource.getDefaultFont(SWT.NONE, 0));
		if (inherited)
		{
			fig.setForegroundColor(ColorConstants.red);
		}

		updateFigure(part, fig);
		return fig;
	}

	protected void updateFigure(Part part, PartFigure fig)
	{
		if (editorPart.getForm() == null) return;
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm());

		fig.setText(part.getEditorName());
		Dimension dim = fig.getMinimumSize(0, 0);
		fig.setBounds(new Rectangle(flattenedForm.getWidth() + 3, part.getHeight(), dim.width, dim.height));
	}

	/**
	 * Form part has no children
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
		updateFigure(getPersist(), (PartFigure)getFigure());
	}

	@Override
	public void activate()
	{
		// listen to changes to the form
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addPersistChangeListener(false, this);

		super.activate();
	}

	@Override
	public void deactivate()
	{
		// stop listening to changes to the form
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.removePersistChangeListener(false, this);

		super.deactivate();
	}

	// If the form changed width we have to refresh
	public void persistChanges(Collection<IPersist> changes)
	{
		if (editorPart.getForm() != null)
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (getParent() != null) refresh();
				}
			});
		}
	}

	public Part getPersist()
	{
		return (Part)getModel();
	}

	public IFieldPositioner getFieldPositioner()
	{
		if (getParent() instanceof IFieldPositionerProvider)
		{
			return ((IFieldPositionerProvider)getParent()).getFieldPositioner();
		}
		return null;
	}


	@Override
	public DragTracker getDragTracker(Request request)
	{
		if (request instanceof SelectionRequest && ((SelectionRequest)request).isShiftKeyPressed())
		{
			MarqueeDragTracker marqueeDragTracker = new MarqueeDragTracker();
			marqueeDragTracker.setStartEditpart(this);
			return marqueeDragTracker;
		}

		return new DragEditPartsTracker(this)
		{
			private boolean allowFormResize;

			@Override
			protected void updateTargetRequest()
			{
				super.updateTargetRequest();
				ChangeBoundsRequest targetRequest = (ChangeBoundsRequest)getTargetRequest();
				targetRequest.setMoveDelta(limitPartMove(targetRequest.getMoveDelta()));
				targetRequest.getExtendedData().put(DragFormPartPolicy.PROPERTY_ALLOW_FORM_RESIZE, Boolean.valueOf(allowFormResize));
			}

			@Override
			protected List< ? extends EditPart> createOperationSet()
			{
				return filterMovableEditParts(super.createOperationSet());
			}

			@Override
			protected void performDirectEdit()
			{
				return;
				// Disabled direct edit via drag tracker, it activates direct edit on single click on selected	  element;
				// direct edit is handled in FormSelectionTool on double-click
			}

			@Override
			protected void applyProperty(Object key, Object value)
			{
				if (DragFormPartPolicy.PROPERTY_ALLOW_FORM_RESIZE.equals(key))
				{
					allowFormResize = Boolean.TRUE.equals(value);
					return;
				}
				super.applyProperty(key, value);
			}

			@Override
			protected Command getCommand()
			{
				// handle move parts with control key
				if (isMove() && isCloneActive())
				{

					CompoundCommand command = new CompoundCommand();
					command.setDebugLabel("Drag Object Tracker");

					Request request = getTargetRequest();

					request.setType(REQ_CLONE);

					for (EditPart element : getOperationSet())
					{
						command.add(element.getCommand(request));
					}
					if (command.canExecute())
					{
						return command;
					}
				}

				return super.getCommand();
			}
		};
	}

	public static List< ? extends EditPart> filterMovableEditParts(List< ? extends EditPart> editParts)
	{
		List<EditPart> newEditParts = new ArrayList<>(editParts.size());
		for (EditPart editPart : editParts)
		{
			boolean foundParent = false;
			if (editPart instanceof FormPartGraphicalEditPart)
			{
				FormPartGraphicalEditPart partEditpart = (FormPartGraphicalEditPart)editPart;
				IPersist persist = partEditpart.getPersist();
				for (Object editPart2 : editParts)
				{
					if (editPart2 instanceof IPersistEditPart && ((IPersistEditPart)editPart2).getPersist() == persist.getParent())
					{
						foundParent = true;
					}
				}
			}
			if (!foundParent) newEditParts.add(editPart);
		}
		return newEditParts;
	}

	/**
	 * Limit a change bounds request.
	 *
	 * Update the request when it updates the edit parts to move/resize beyond (0, 0)
	 *
	 */
	public Point limitPartMove(Point moveDelta)
	{
		if ((moveDelta == null || (moveDelta.x == 0 && moveDelta.y == 0)))
		{
			return moveDelta;
		}

		EditPart parentPart = getParent();
		List<Part> neighboursNode = new ArrayList<Part>();
		for (Object child : parentPart.getChildren())
		{
			if (child instanceof FormPartGraphicalEditPart && child != this)
			{
				neighboursNode.add(((FormPartGraphicalEditPart)child).getPersist());
			}
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int yCorrection = 0;
		int xCorrection = 0;

		Rectangle bounds = getFigure().getBounds();
		if (bounds.x < minX) minX = bounds.x;
		if (bounds.y < minY) minY = bounds.y;

		xCorrection = (minX != Integer.MAX_VALUE && minX + moveDelta.x < 0) ? -minX - moveDelta.x : 0;
		yCorrection = (minY != Integer.MAX_VALUE && minY + moveDelta.y < 0) ? -minY - moveDelta.y : 0;
		if (!checkMovement(getPersist(), neighboursNode, moveDelta.y))
		{
			return new Point(0, 0);
		}

		return new Point(moveDelta.x + xCorrection, moveDelta.y + yCorrection);
	}

	private static boolean checkMovement(Part currentPart, List<Part> neighboursNode, int deltaY)
	{
		int part_type = currentPart.getPartType();
		if (Part.BODY == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}

				else if (neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER ||
					neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}

			}
		}
		else if (Part.TITLE_HEADER == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY || neighbourType == Part.LEADING_SUBSUMMARY ||
					neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY ||
					neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}
		}
		else if (Part.HEADER == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.TITLE_HEADER)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.LEADING_GRAND_SUMMARY || neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY ||
					neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER ||
					neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}
		}
		else if (Part.LEADING_GRAND_SUMMARY == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.LEADING_SUBSUMMARY == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY || neighbourType == Part.TRAILING_GRAND_SUMMARY ||
					neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TRAILING_SUBSUMMARY == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TRAILING_GRAND_SUMMARY == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY)
				{
					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.FOOTER || neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.FOOTER == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY)
				{

					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
				else if (neighbourType == Part.TITLE_FOOTER)
				{
					if (nPart.getHeight() < currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}
			}

		}

		else if (Part.TITLE_FOOTER == part_type)
		{
			for (Part nPart : neighboursNode)
			{
				int neighbourType = nPart.getPartType();
				if (neighbourType == Part.HEADER || neighbourType == Part.TITLE_HEADER || neighbourType == Part.LEADING_GRAND_SUMMARY ||
					neighbourType == Part.LEADING_SUBSUMMARY || neighbourType == Part.BODY || neighbourType == Part.TRAILING_SUBSUMMARY ||
					neighbourType == Part.TRAILING_GRAND_SUMMARY || neighbourType == Part.FOOTER)
				{

					if (nPart.getHeight() > currentPart.getHeight() + deltaY)
					{
						return false;
					}
				}

			}
		}

		return true;
	}

	public boolean isInherited()
	{
		return inherited;
	}
}
