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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.FigureUtilities;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.LayerConstants;
import org.eclipse.gef.Request;
import org.eclipse.gef.editpolicies.GraphicalEditPolicy;

import com.servoy.eclipse.designer.editor.commands.CreateDropRequest;

/**
 * Edit policy that shows feedback for dropping elements in the form editor.
 * 
 * @author rgansevles
 *
 */
final class DropFeedbackEditPolicy extends GraphicalEditPolicy
{
	private AlignmentFeedbackHelper alignmentFeedbackHelper;

	protected RectangleFigure dragNDropFeedbackFigure;

	@Override
	public EditPart getTargetEditPart(Request request)
	{
		if (understandsRequest(request))
		{
			return getHost();
		}
		return super.getTargetEditPart(request);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.gef.editpolicies.AbstractEditPolicy#understandsRequest(org.eclipse.gef.Request)
	 */
	@Override
	public boolean understandsRequest(Request request)
	{
		return request instanceof CreateDropRequest;
	}

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentFeedbackHelper()
	{
		if (alignmentFeedbackHelper == null)
		{
			alignmentFeedbackHelper = new AlignmentFeedbackHelper(getFeedbackLayer());
		}
		return alignmentFeedbackHelper;
	}

	@Override
	public void showTargetFeedback(Request request)
	{
		if (request instanceof CreateDropRequest)
		{
			showDragNDropFeedbackFigure((CreateDropRequest)request);
			getAlignmentFeedbackHelper().showElementAlignmentFeedback(request);
		}

		super.showTargetFeedback(request);
	}

	@Override
	public void eraseTargetFeedback(Request request)
	{
		getAlignmentFeedbackHelper().eraseElementAlignmentFeedback();
		removeDragNDropFeedbackFigure();
		super.eraseTargetFeedback(request);
	}

	/**
	 * @param request
	 */
	protected void showDragNDropFeedbackFigure(CreateDropRequest request)
	{
		if (dragNDropFeedbackFigure == null)
		{
			// Use a ghost rectangle for feedback
			dragNDropFeedbackFigure = new RectangleFigure();
			FigureUtilities.makeGhostShape(dragNDropFeedbackFigure);
			dragNDropFeedbackFigure.setLineStyle(Graphics.LINE_DOT);
			dragNDropFeedbackFigure.setForegroundColor(ColorConstants.white);
			dragNDropFeedbackFigure.setSize(request.getSize());

			getLayer(LayerConstants.FEEDBACK_LAYER).add(dragNDropFeedbackFigure);
		}
		else
		{
			dragNDropFeedbackFigure.setLocation(request.getLocation());
		}
	}

	protected void removeDragNDropFeedbackFigure()
	{
		if (dragNDropFeedbackFigure != null)
		{
			getLayer(LayerConstants.FEEDBACK_LAYER).remove(dragNDropFeedbackFigure);
			dragNDropFeedbackFigure = null;
		}
	}

}