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

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.SelectionRequest;
import org.eclipse.gef.tools.DragEditPartsTracker;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.commands.SelectModelsCommandWrapper;
import com.servoy.eclipse.designer.internal.MarqueeDragTracker;
import com.servoy.eclipse.designer.property.IFieldPositionerProvider;
import com.servoy.eclipse.designer.property.IPersistEditPart;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.TabPanel;

/**
 * Base class for editparts with persist model.
 *
 * @author rgansevles
 */

public abstract class BasePersistGraphicalEditPart extends BaseGraphicalEditPart implements IPersistEditPart, IFieldPositionerProvider
{
	protected final IApplication application;
	private final boolean inherited;
	private boolean selectable = true;

	public BasePersistGraphicalEditPart(IApplication application, IPersist persist, boolean inherited)
	{
		this.application = application;
		this.inherited = inherited;
		setModel(persist);
	}

	@Override
	public Command getCommand(Request request)
	{
		Command command = super.getCommand(request);
		if (command == null)
		{
			// delegate to parent element.
			// parent of a Tab or Portal field edit part is not the TabPanel/Portal edit part, but the Form edit part
			EditPart parentEditPart = getParent();
			if (parentEditPart.getModel() != getPersist().getParent() && !(parentEditPart.getModel() instanceof Form))
			{
				// the editpart hierarchy is not the same as persist hierarchy, find the editpart that has the model parent.

				// In case of a form, the parentEditPart is ok (inherited element on subform)

				// In case of a Tab/Portal element, find the TabPanel/Portal
				if (getPersist().getParent() instanceof TabPanel || getPersist().getParent() instanceof Portal)
				{
					for (EditPart ep : (List<EditPart>)parentEditPart.getChildren())
					{
						if (ep.getModel() instanceof ISupportChilds) // TabPanel or Portal
						{
							Iterator<IPersist> it = ((ISupportChilds)ep.getModel()).getAllObjects();
							while (it.hasNext())
							{
								if (it.next().equals(getModel()))
								{
									parentEditPart = ep;
									break;
								}
							}
						}
					}
				}

				// in case of element group, use the group's parent.
				else if (parentEditPart.getModel() instanceof FormElementGroup)
				{
					parentEditPart = parentEditPart.getParent();
				}

			}

			if (parentEditPart != null)
			{
				return parentEditPart.getCommand(request);
			}
		}
		return command;
	}

	@Override
	public boolean understandsRequest(Request req)
	{
		if (super.understandsRequest(req))
		{
			return true;
		}
		// delegate to parent element.
		// note: do not use getHost().getParent().getCommand(request) here, the editpart
		// parent of a Tab or Portal field edit part is not the TabPanel/Portal edit part, but the Form edit part
		EditPart parentEditPart = (EditPart)getViewer().getEditPartRegistry().get(getPersist().getParent());
		return parentEditPart != null && parentEditPart.understandsRequest(req);
	}

	public boolean isDesignerContextActive()
	{
		RootEditPart root = getRoot();
		return root instanceof BaseFormGraphicalRootEditPart && ((BaseFormGraphicalRootEditPart)root).getEditorPart().isDesignerContextActive();
	}

	/**
	 * Override getCommand to remove children from requests when their parent is also there.
	 * <p>
	 * This is needed to apply move parent to all its children regardless of whether these children were selected.
	 */

	@Override
	public DragTracker getDragTracker(Request request)
	{
		return createDragTracker(this, request);
	}

	public static DragTracker createDragTracker(final GraphicalEditPart editPart, Request request)
	{
		if (request instanceof SelectionRequest && ((SelectionRequest)request).isShiftKeyPressed())
		{
			MarqueeDragTracker marqueeDragTracker = new MarqueeDragTracker();
			marqueeDragTracker.setStartEditpart(editPart);
			return marqueeDragTracker;
		}
		return new DragEditPartsTracker(editPart)
		{
			@Override
			protected void updateTargetRequest()
			{
				super.updateTargetRequest();
				limitChangeBoundsRequest((ChangeBoundsRequest)getTargetRequest());
			}

			@Override
			protected List createOperationSet()
			{
				List editParts = super.createOperationSet();
				List<Object> newEditParts = new ArrayList<Object>(editParts.size());
				for (Object editPart : editParts)
				{
					boolean foundParent = false;
					if (editPart instanceof IPersistEditPart)
					{
						IPersistEditPart persistEditpart = (IPersistEditPart)editPart;
						IPersist persist = persistEditpart.getPersist();
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

			@Override
			protected void performDirectEdit()
			{
				return; // Disabled direct edit via drag tracker, it activates direct edit on single click on selected element;
				// direct edit is handled in FormSelectionTool on double-click
			}

			@Override
			protected void performSelection()
			{
				super.performSelection();
				if (getCurrentInput().isShiftKeyDown() && getSourceEditPart() instanceof GraphicalEditPart)
				{
					// shift-select: select all edit parts inbetween the current selection and the source edit part
					EditPartViewer viewer = getCurrentViewer();
					List< ? extends EditPart> editParts = viewer.getContents().getChildren();
					// get the bounding box of all currently selected edit parts
					Rectangle selectedRectangle = null;
					for (EditPart editpart : editParts)
					{
						if (editpart != getSourceEditPart() && editpart.getSelected() != EditPart.SELECTED_NONE && editpart instanceof GraphicalEditPart)
						{
							if (selectedRectangle == null)
							{
								// make a copy, the union method modifies the rectangle
								selectedRectangle = ((GraphicalEditPart)editpart).getFigure().getBounds().getCopy();
							}
							else
							{
								selectedRectangle = selectedRectangle.union(((GraphicalEditPart)editpart).getFigure().getBounds());
							}
						}
					}
					if (selectedRectangle != null)
					{
						// select all edit parts that touch this box unioned with the current edit part
						selectedRectangle = selectedRectangle.union(((GraphicalEditPart)getSourceEditPart()).getFigure().getBounds());
						for (EditPart editpart : editParts)
						{
							if (editpart != getSourceEditPart() && editpart.getSelected() == EditPart.SELECTED_NONE && editpart instanceof GraphicalEditPart &&
								selectedRectangle.intersects(((GraphicalEditPart)editpart).getFigure().getBounds()))
							{
								viewer.appendSelection(editpart);
							}
						}
					}
				}
			}

			@Override
			protected Command getCommand()
			{
				Command command = super.getCommand();
				return command == null ? null : new SelectModelsCommandWrapper(editPart.getViewer(), editPart, command);
			}
		};
	}

	/**
	 * Limit a change bounds request.
	 *
	 * <p>
	 * Update the request when it updates the edit parts to move/resize beyond (0, 0)
	 *
	 * @param targetRequest
	 */
	public static void limitChangeBoundsRequest(ChangeBoundsRequest targetRequest)
	{
		Point moveDelta = targetRequest.getMoveDelta();
		Dimension sizeDelta = targetRequest.getSizeDelta();
		if ((moveDelta != null && (moveDelta.x != 0 || moveDelta.y != 0)))
		{
			int minX = Integer.MAX_VALUE;
			int minY = Integer.MAX_VALUE;
			for (Object editPart : targetRequest.getEditParts())
			{
				if (editPart instanceof GraphicalEditPart)
				{
					Rectangle bounds = ((GraphicalEditPart)editPart).getFigure().getBounds();
					if (bounds.x < minX) minX = bounds.x;
					if (bounds.y < minY) minY = bounds.y;
				}
			}

			// do not allow move beyond (0,0)
			if (moveDelta != null && (moveDelta.x != 0 || moveDelta.y != 0))
			{
				int xCorrection = (minX != Integer.MAX_VALUE && minX + moveDelta.x < 0) ? -minX - moveDelta.x : 0;
				int yCorrection = (minY != Integer.MAX_VALUE && minY + moveDelta.y < 0) ? -minY - moveDelta.y : 0;
				if (xCorrection != 0 || yCorrection != 0)
				{
					targetRequest.setMoveDelta(new Point(moveDelta.x + xCorrection, moveDelta.y + yCorrection));

					// do not allow resize beyond (0,0)
					if (sizeDelta != null && (sizeDelta.width != 0 || sizeDelta.height != 0))
					{
						targetRequest.setSizeDelta(new Dimension(sizeDelta.width - xCorrection, sizeDelta.height - yCorrection));
					}
				}
			}
		}
	}


	public IPersist getPersist()
	{
		return (IPersist)getModel();
	}

	public boolean isInherited()
	{
		return inherited;
	}

	public IFieldPositioner getFieldPositioner()
	{
		if (getParent() instanceof IFieldPositionerProvider)
		{
			return ((IFieldPositionerProvider)getParent()).getFieldPositioner();
		}
		return null;
	}


	public void setSelectable(boolean selectable)
	{
		this.selectable = selectable;
	}

	@Override
	public boolean isSelectable()
	{
		return selectable;
	}
}
