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

import org.eclipse.draw2d.FocusBorder;
import org.eclipse.draw2d.PositionConstants;
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

import com.servoy.eclipse.designer.editor.commands.MovePartCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * Edit policy for dragging the part in form designer.
 *
 * @author rgansevles
 *
 */
public class DragFormPartPolicy extends ResizableEditPolicy
{
	public static final String PROPERTY_ALLOW_FORM_RESIZE = "Allow form resize";

	private Part previousPart = null;

	@Override
	protected DragFormBoundsFigure createDragSourceFeedbackFigure()
	{
		DragFormBoundsFigure movePartFeedbackFigure;
		movePartFeedbackFigure = new DragFormBoundsFigure();
		movePartFeedbackFigure.addPartHandle(((Part)getHost().getModel()).getEditorName());
		addFeedback(movePartFeedbackFigure);
		return movePartFeedbackFigure;
	}

	@Override
	protected void showSelection()
	{
		// do not show selection handles on form part
		getHostFigure().setBorder(new FocusBorder());
	}

	@Override
	protected void hideSelection()
	{
		getHostFigure().setBorder(null);
	}

	protected Part getPreviousPart()
	{
		if (previousPart == null)
		{
			previousPart = DesignerUtil.getPreviousPart((Part)getHost().getModel());
		}
		return previousPart;
	}


	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		boolean allowFormResize = Boolean.TRUE.equals(request.getExtendedData().get(PROPERTY_ALLOW_FORM_RESIZE));
		DragFormBoundsFigure feedback = (DragFormBoundsFigure)getDragSourceFeedbackFigure();
		PrecisionRectangle rect = new PrecisionRectangle(getInitialFeedbackBounds());

		getHostFigure().translateToAbsolute(rect);

		rect.setY(rect.y + request.getMoveDelta().y);
		if (allowFormResize)
		{
			rect.setX(rect.x + request.getMoveDelta().x);
		}
		feedback.translateToRelative(rect);

		feedback.setHorizontalLine(rect.y, rect.x);
		int formWidth = -1;
		if (allowFormResize && request.getMoveDelta().x != 0)
		{
			Object parentModel = getHost().getParent().getModel();
			if (parentModel instanceof Form)
			{
				feedback.setVerticalLine(formWidth = ((Form)parentModel).getWidth() + request.getMoveDelta().x, rect.y);
			}
		}

		// feedback on status line
		StringBuilder message = new StringBuilder("Part height ");
		int newHeight = ((Part)getHost().getModel()).getHeight() + request.getMoveDelta().y;
		message.append(newHeight);
		Part prev = getPreviousPart();
		if (prev != null && prev.getHeight() > 0)
		{
			message.append(" (").append(newHeight - prev.getHeight()).append(')');
		}
		if (formWidth != -1)
		{
			message.append(", Form width " + formWidth);
		}
		EditorUtil.setStatuslineMessage(message.toString());
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		EditorUtil.setStatuslineMessage(null);
		previousPart = null;
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
			if (Boolean.TRUE.equals(request.getExtendedData().get(PROPERTY_ALLOW_FORM_RESIZE)))
			{
				// add form resize command
				compoundCommand.add(new ResizeFormCommand((FormGraphicalEditPart)(getHost().getParent()), PositionConstants.SOUTH_EAST,
					((ChangeBoundsRequest)request).getMoveDelta().x, RequestConstants.REQ_CLONE.equals(request.getType()) ||
						((ChangeBoundsRequest)request).isCenteredResize()));
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

		List< ? extends EditPart> siblings = getHost().getParent().getChildren();
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
