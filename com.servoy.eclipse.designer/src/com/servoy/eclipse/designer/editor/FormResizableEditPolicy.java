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

import org.eclipse.draw2d.Polyline;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.servoy.eclipse.designer.editor.FormBorderGraphicalEditPart.BorderModel;
import com.servoy.eclipse.ui.util.EditorUtil;

/**
 * Edit policy for resizing form via boundary.
 * 
 * @since 6.0
 * 
 * @author rgansevles
 *
 */
public class FormResizableEditPolicy extends ResizableEditPolicy
{

	public FormResizableEditPolicy(FormBorderGraphicalEditPart host)
	{
		setHost(host);
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		Polyline feedback = (Polyline)getDragSourceFeedbackFigure();

		PrecisionRectangle rect = new PrecisionRectangle(getInitialFeedbackBounds());

		feedback.removeAllPoints();
		feedback.addPoint(new Point(rect.width + request.getSizeDelta().width, rect.y));
		feedback.addPoint(new Point(rect.width + request.getSizeDelta().width, rect.y + rect.height));

		// feedback on status line
		EditorUtil.setStatuslineMessage("Form width " + (((BorderModel)getHost().getModel()).flattenedForm.getWidth() + request.getSizeDelta().width));
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		EditorUtil.setStatuslineMessage(null);
	}

	@Override
	protected Polyline createDragSourceFeedbackFigure()
	{
		Polyline polyline;
		addFeedback(polyline = DragFormPartPolicy.createMovePartFeedbackFigure());
		return polyline;
	}

	@Override
	protected Command getResizeCommand(ChangeBoundsRequest request)
	{
		return new ResizeFormCommand((FormGraphicalEditPart)(getHost().getParent()), request.getResizeDirection(), request.getSizeDelta(),
			request.isCenteredResize());
	}
}
