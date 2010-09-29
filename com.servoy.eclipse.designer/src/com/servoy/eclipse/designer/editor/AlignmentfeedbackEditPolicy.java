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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.editpolicies.ResizableEditPolicy;
import org.eclipse.gef.handles.ResizeHandle;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.ResizeTracker;

/**
 * Edit policy for moving/resizing elements.
 * Alignment feedback is given.
 * 
 * @author rgansevles
 *
 */
final class AlignmentfeedbackEditPolicy extends ResizableEditPolicy
{
	private final Map<ElementAlignmentItem, IFigure> alignmentFeedbackFigures = new HashMap<ElementAlignmentItem, IFigure>();

	protected Handle createResizeHandle(GraphicalEditPart owner, final int direction)
	{
		return new ResizeHandle(owner, direction)
		{
			@Override
			protected DragTracker createDragTracker()
			{
				return new ResizeTracker(getOwner(), direction)
				{
					@Override
					protected void updateSourceRequest()
					{
						super.updateSourceRequest();
						BasePersistGraphicalEditPart.limitChangeBoundsRequest((ChangeBoundsRequest)getSourceRequest());
					}
				};
			}
		};
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.showChangeBoundsFeedback(request);
		showElementAlignmentFeedback(request);
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		eraseElementAlignmentFeedback();
	}

	protected void showElementAlignmentFeedback(ChangeBoundsRequest request)
	{
		ElementAlignmentItem[] feedbackItems = (ElementAlignmentItem[])request.getExtendedData().get(SnapToElementAlignment.ELEMENT_ALIGNMENT);

		// remove old feedbacks
		Iterator<Entry<ElementAlignmentItem, IFigure>> iterator = alignmentFeedbackFigures.entrySet().iterator();
		while (iterator.hasNext())
		{
			Entry<ElementAlignmentItem, IFigure> next = iterator.next();
			boolean remove = true;
			if (feedbackItems != null)
			{
				for (int i = 0; remove && i < feedbackItems.length; i++)
				{
					remove = !next.equals(feedbackItems[i]);
				}
			}
			if (remove)
			{
				iterator.remove();
				removeFeedback(next.getValue());
			}
		}

		if (feedbackItems == null)
		{
			return;
		}

		// create figures for new feedbackItems
		for (ElementAlignmentItem item : feedbackItems)
		{
			if (alignmentFeedbackFigures.get(item) == null)
			{
				IFigure figure = createAlignmentFeedbackFigure(item);
				if (figure != null)
				{
					alignmentFeedbackFigures.put(item, figure);
				}
			}
		}
	}

	protected void eraseElementAlignmentFeedback()
	{
		for (IFigure figure : alignmentFeedbackFigures.values())
		{
			removeFeedback(figure);
		}
		alignmentFeedbackFigures.clear();
	}

	protected IFigure createAlignmentFeedbackFigure(ElementAlignmentItem item)
	{
		IFigure figure = null;
		if (ElementAlignmentItem.ALIGN_TYPE_SIDE.equals(item.alignType))
		{
			figure = createSideAlignmentFeedbackFigure(item);
		}
		else if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE.equals(item.alignType) ||
			ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM.equals(item.alignType) || ElementAlignmentItem.ALIGN_TYPE_DISTANCE_SMALL.equals(item.alignType))
		{
			figure = createDistanceAlignmentFeedbackFigure(item);
		}

		if (figure != null)
		{
			addFeedback(figure);
		}
		return figure;
	}

	protected IFigure createSideAlignmentFeedbackFigure(ElementAlignmentItem item)
	{
		AlignmentFeedbackFigure line = new AlignmentFeedbackFigure();

		boolean horiozontal = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(item.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(item.alignDirection);
		line.addLine(horiozontal, item.target, item.start - 5, item.end + 5);
		line.setLineStyle(Graphics.LINE_SOLID);
		line.setForegroundColor(ColorConstants.red);
		return line;
	}

	protected IFigure createDistanceAlignmentFeedbackFigure(ElementAlignmentItem item)
	{
		AlignmentFeedbackFigure figure = new AlignmentFeedbackFigure();

		boolean horiozontal = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(item.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(item.alignDirection);
		int sign = ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(item.alignDirection) ||
			ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(item.alignDirection) ? -1 : 1;

		figure.addLine(horiozontal, item.target, item.start - 5, item.end + 5);
		if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE.equals(item.alignType) || ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM.equals(item.alignType))
		{
			figure.addLine(horiozontal, item.target + (sign * 5), item.start - 5, item.end + 5); // TODO: configurabe in preferences
			if (ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE.equals(item.alignType))
			{
				figure.addLine(horiozontal, item.target + (sign * 10), item.start - 5, item.end + 5);
			}
		}

		figure.setLineStyle(Graphics.LINE_SOLID);
		figure.setForegroundColor(ColorConstants.red); // TODO: configurable in preferences
		return figure;
	}
}