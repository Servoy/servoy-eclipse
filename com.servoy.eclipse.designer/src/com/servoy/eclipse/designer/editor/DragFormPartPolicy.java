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

import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.swt.graphics.RGB;

import com.servoy.eclipse.designer.editor.commands.MovePartCommand;
import com.servoy.eclipse.ui.resource.ColorResource;
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
	public static final RGB PART_HANDLE_RGB = new RGB(0x41, 0x8F, 0xD4);

	@Override
	protected Polyline createDragSourceFeedbackFigure()
	{
		Polyline polyline;
		addFeedback(polyline = createMovePartFeedbackFigure());
		return polyline;
	}

	public static Polyline createMovePartFeedbackFigure()
	{
		// Use a line for feedback
		Polyline polyline = new Polyline();
		polyline.setForegroundColor(ColorResource.INSTANCE.getColor(PART_HANDLE_RGB)); // TODO: add preference 
		polyline.setLineStyle(Graphics.LINE_DOT);

		return polyline;
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		Polyline feedback = (Polyline)getDragSourceFeedbackFigure();
		PrecisionRectangle rect = new PrecisionRectangle(getInitialFeedbackBounds());

		getHostFigure().translateToAbsolute(rect);

		rect.setY(request.getLocation().y);
		feedback.translateToRelative(rect);

		feedback.removeAllPoints();
		feedback.addPoint(new Point(0, rect.y));
		feedback.addPoint(new Point(rect.x, rect.y));
	}

	@Override
	public Command getCommand(Request request)
	{
		if (request instanceof ChangeBoundsRequest)
		{
			CompoundCommand compoundCommand = new CompoundCommand();
			compoundCommand.add(new MovePartCommand((Part)getHost().getModel(), (IPersist)getHost().getParent().getModel(), request.getType(),
				getHostFigure().getBounds().y + ((ChangeBoundsRequest)request).getMoveDelta().y));
			if (RequestConstants.REQ_CLONE.equals(request.getType()) || ((ChangeBoundsRequest)request).isCenteredResize())
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
			boolean resize = false;
			boolean move = model instanceof Part && model != part && ((Part)model).getHeight() >= part.getHeight(); // part below current part, needs to move as well

			if (!move && model instanceof ISupportBounds && ((ISupportBounds)model).getLocation() != null)
			{
				int y = ((ISupportBounds)model).getLocation().y;
				move = y > part.getHeight(); // element on next part

				if (!move && (prevPart == null || y > prevPart.getHeight()))
				{
					// element on this part, only move/resize when anchored SOUTH
					move = model instanceof ISupportAnchors && (((ISupportAnchors)model).getAnchors() & IAnchorConstants.SOUTH) != 0;
					resize = move && (((ISupportAnchors)model).getAnchors() & IAnchorConstants.NORTH) != 0;
				}
			}
			if (resize)
			{
				command.add(sibling.getCommand(resizeRequest));
			}
			else if (move)
			{
				command.add(sibling.getCommand(moveRequest));
			}
		}
	}

	@Override
	public boolean understandsRequest(Request req)
	{
		return req.getType().equals(REQ_MOVE);
	}
}
