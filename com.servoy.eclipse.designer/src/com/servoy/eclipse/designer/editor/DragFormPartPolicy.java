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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.internal.ui.rulers.GuideEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.servoy.eclipse.designer.editor.commands.MovePartCommand;
import com.servoy.j2db.persistence.Part;

/**
 * Edit policy for dragging the part in form designer.
 * 
 * @author rob
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
			return new MovePartCommand((Part)getHost().getModel(), (ChangeBoundsRequest)request, getHostFigure().getBounds().y +
				((ChangeBoundsRequest)request).getMoveDelta().y);
		}
		return super.getCommand(request);
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
