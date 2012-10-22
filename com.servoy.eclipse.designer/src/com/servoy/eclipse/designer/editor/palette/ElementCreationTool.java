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

package com.servoy.eclipse.designer.editor.palette;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.AlignmentFeedbackHelper;
import com.servoy.eclipse.designer.editor.CreateElementRequest;

/**
 * Tool for creating elements from the palette in the form editor.
 * 
 * @author rgansevles
 *
 */
public class ElementCreationTool extends BaseElementCreationTool
{
	private AlignmentFeedbackHelper alignmentFeedbackHelper;
	private SnapToHelper snapToHelper;

	/**
	 * @return the alignmentFeedbackHelper
	 */
	public AlignmentFeedbackHelper getAlignmentFeedbackHelper()
	{
		if (alignmentFeedbackHelper == null)
		{
			IFigure feedbackLayer = getFeedbackLayer();
			if (feedbackLayer != null)
			{
				alignmentFeedbackHelper = new AlignmentFeedbackHelper(feedbackLayer);
			}
		}
		return alignmentFeedbackHelper;
	}

	@Override
	protected void showTargetFeedback()
	{
		super.showTargetFeedback();
		AlignmentFeedbackHelper helper = getAlignmentFeedbackHelper();
		if (helper != null)
		{
			helper.showElementAlignmentFeedback(getTargetRequest());
		}
	}

	@Override
	protected void eraseTargetFeedback()
	{
		super.eraseTargetFeedback();
		AlignmentFeedbackHelper helper = getAlignmentFeedbackHelper();
		if (helper != null)
		{
			helper.eraseElementAlignmentFeedback();
		}
	}

	/**
	 * @see org.eclipse.gef.Tool#deactivate()
	 */
	@Override
	public void deactivate()
	{
		super.deactivate();
		snapToHelper = null;
		alignmentFeedbackHelper = null;
	}

	/**
	 * Creates a {@link CreateRequest} and sets this tool's factory on the
	 * request.
	 * 
	 * @see org.eclipse.gef.tools.TargetingTool#createTargetRequest()
	 */
	@Override
	protected Request createTargetRequest()
	{
		CreationFactory fact = getFactory();
		CreateElementRequest request = new CreateElementRequest(fact);
		if (fact instanceof RequestTypeCreationFactory)
		{
			request.setSize(((RequestTypeCreationFactory)fact).getNewObjectSize());
		}
		return request;
	}

	protected SnapToHelper getSnapToHelper()
	{
		if (snapToHelper == null && getTargetEditPart() != null)
		{
			snapToHelper = (SnapToHelper)getTargetEditPart().getAdapter(SnapToHelper.class);
		}
		return snapToHelper;
	}

	@Override
	protected void updateTargetRequest()
	{
		CreateRequest req = getCreateRequest();
		req.getExtendedData().clear();

		if (isInState(STATE_DRAG_IN_PROGRESS))
		{
			// User is drawing selected palette item
			Point loq = getStartLocation();
			Rectangle bounds = new Rectangle(loq, loq);
			bounds.union(loq.getTranslated(getDragMoveDelta()));
			req.setSize(bounds.getSize());
			req.setLocation(bounds.getLocation());
			if (!getCurrentInput().isAltKeyDown() && getSnapToHelper() != null)
			{
				PrecisionRectangle baseRect = new PrecisionRectangle(bounds);
				PrecisionRectangle result = baseRect.getPreciseCopy();
				getSnapToHelper().snapRectangle(req, PositionConstants.NSEW, baseRect, result);
				req.setLocation(result.getLocation());
				req.setSize(result.getSize());
			}
		}
		else if (req.getSize() != null)
		{
			// User is moving over form
			Rectangle bounds = new Rectangle(getLocation(), req.getSize());
			PrecisionRectangle baseRect = new PrecisionRectangle(bounds);
			PrecisionRectangle result = baseRect.getPreciseCopy();
			if (!getCurrentInput().isAltKeyDown() && getSnapToHelper() != null)
			{
				getSnapToHelper().snapRectangle(req, PositionConstants.NSEW, baseRect, result);
			}
			req.setLocation(result.getLocation());
			req.setSize(result.getSize());
		}

		if (getCreateRequest() instanceof CreateElementRequest)
		{
			((CreateElementRequest)getCreateRequest()).setResizable(isInState(STATE_DRAG_IN_PROGRESS));
		}
	}
}
