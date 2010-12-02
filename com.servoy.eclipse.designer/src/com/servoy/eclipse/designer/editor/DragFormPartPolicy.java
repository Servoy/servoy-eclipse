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

import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.internal.ui.rulers.GuideEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.servoy.eclipse.designer.editor.commands.MovePartCommand;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IAnchorConstants;

/**
 * Edit policy for dragging the part in form designer.
 * 
 * @author rgansevles
 * 
 */
public class DragFormPartPolicy extends ResizableEditPolicy
{
	private IFigure dragFigure;
	private boolean dragInProgress = false;

	protected IFigure createDragFigure()
	{
		return new GuideEditPart.GuideLineFigure();
	}

	@Override
	public void deactivate()
	{
		if (dragInProgress)
		{
			removeFeedback();
		}
		super.deactivate();
	}

	@Override
	public void eraseSourceFeedback(Request request)
	{
		removeFeedback();
		dragInProgress = false;
	}

	@Override
	public Command getCommand(Request request)
	{
		if (request instanceof ChangeBoundsRequest)
		{
			CompoundCommand compoundCommand = new CompoundCommand();
			compoundCommand.add(new MovePartCommand((Part)getHost().getModel(), (IPersist)getHost().getParent().getModel(), request.getType(),
				getHostFigure().getBounds().y + ((ChangeBoundsRequest)request).getMoveDelta().y));
			if (RequestConstants.REQ_CLONE.equals(request.getType()))
			{
				// control held while dragging
				addCommandsForElementsMovingWithPart(compoundCommand, ((ChangeBoundsRequest)request).getMoveDelta().y);
			}

			return compoundCommand.unwrap();
		}
		return super.getCommand(request);
	}

	public void addCommandsForElementsMovingWithPart(CompoundCommand command, int moveDelta)
	{
		Part part = (Part)getHost().getModel();
		Part prevPart;
		try
		{
			prevPart = part.getPreviousPart();
		}
		catch (RepositoryException e)
		{
			// when will this happen?
			Debug.error(e);
			return;
		}
		ChangeBoundsRequest moveRequest = new ChangeBoundsRequest(RequestConstants.REQ_MOVE);
		moveRequest.setMoveDelta(new Point(0, moveDelta));
		ChangeBoundsRequest resizeRequest = new ChangeBoundsRequest(RequestConstants.REQ_RESIZE);
		resizeRequest.setSizeDelta(new Dimension(0, moveDelta));

		List<EditPart> siblings = getHost().getParent().getChildren();
		for (EditPart sibling : siblings)
		{
			Object model = sibling.getModel();
			boolean move = false;
			boolean resize = false;
			if (model instanceof Part && model != part && ((Part)model).getHeight() >= part.getHeight())
			{
				// part below current part, needs to move as well
				move = true;
			}
			else if (model instanceof ISupportBounds && ((ISupportBounds)model).getLocation() != null)
			{
				ISupportBounds supportBounds = (ISupportBounds)model;
				if (supportBounds.getLocation().y > part.getHeight())
				{
					// element on next part
					move = true;
				}
				else if (prevPart == null || supportBounds.getLocation().y > prevPart.getHeight())
				{
					// element on this part, only move when anchored SOUTH
					if (model instanceof ISupportAnchors && (((ISupportAnchors)model).getAnchors() & IAnchorConstants.SOUTH) != 0)
					{
						if ((((ISupportAnchors)model).getAnchors() & IAnchorConstants.NORTH) != 0)
						{
							resize = true;
						}
						else
						{
							move = true;
						}
					}
				}
			}
			if (move)
			{
				command.add(sibling.getCommand(moveRequest));
			}
			else if (resize)
			{
				command.add(sibling.getCommand(resizeRequest));
			}
		}
	}

	protected IFigure getDragFigure()
	{
		if (dragFigure == null)
		{
			dragFigure = createDragFigure();
		}
		return dragFigure;
	}

	protected void removeFeedback()
	{
		removeFeedback(getDragFigure());
	}

	protected void updateDragFigureLocation(ChangeBoundsRequest request)
	{
		Rectangle hostBounds = getHostFigure().getBounds();
		getDragFigure().setBounds(new Rectangle(0, hostBounds.y + request.getMoveDelta().y, hostBounds.x, hostBounds.y + request.getMoveDelta().y));
	}

	@Override
	public void showSourceFeedback(Request request)
	{
		if (!dragInProgress)
		{
			dragInProgress = true;
			addFeedback(getDragFigure());
		}
		updateDragFigureLocation((ChangeBoundsRequest)request);
	}

	@Override
	public boolean understandsRequest(Request req)
	{
		return req.getType().equals(REQ_MOVE);
	}
}
