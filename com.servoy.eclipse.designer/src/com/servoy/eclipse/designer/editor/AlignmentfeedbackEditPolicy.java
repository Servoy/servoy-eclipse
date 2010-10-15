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

import org.eclipse.draw2d.IFigure;
import org.eclipse.gef.DragTracker;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Handle;
import org.eclipse.gef.LayerConstants;
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

	/**
	 * the feedback figure for the selected element.
	 */
	protected IFigure selectedElementFeedbackFigure;

	private final FormGraphicalEditPart container;

	public AlignmentfeedbackEditPolicy(FormGraphicalEditPart container)
	{
		this.container = container;
	}

	@Override
	public GraphicalEditPart getHost()
	{
		return (GraphicalEditPart)super.getHost();
	}

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
		removeSelectedElementFeedbackFigure();
		showElementAlignmentFeedback(request);
	}

	@Override
	protected void eraseChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		super.eraseChangeBoundsFeedback(request);
		eraseElementAlignmentFeedback();
		addSelectedElementFeedbackFigure();
	}

	protected void showElementAlignmentFeedback(ChangeBoundsRequest request)
	{
		ElementAlignmentItem[] feedbackItems = (ElementAlignmentItem[])request.getExtendedData().get(SnapToElementAlignment.ELEMENT_ALIGNMENT_REQUEST_DATA);

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
				AlignmentFeedbackFigure figure = new AlignmentFeedbackFigure(item);
				alignmentFeedbackFigures.put(item, figure);
				addFeedback(figure);
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

	@Override
	protected void hideSelection()
	{
		super.hideSelection();
		removeSelectedElementFeedbackFigure();
	}

	@Override
	protected void addSelectionHandles()
	{
		super.addSelectionHandles();
		addSelectedElementFeedbackFigure();
	}

	protected void removeSelectedElementFeedbackFigure()
	{
		if (selectedElementFeedbackFigure != null)
		{
			IFigure layer = getLayer(LayerConstants.FEEDBACK_LAYER);
			layer.remove(selectedElementFeedbackFigure);
			selectedElementFeedbackFigure = null;
		}
	}

	/**
	 * Adds the alignment to the feedback layer.
	 */
	protected void addSelectedElementFeedbackFigure()
	{
		removeSelectedElementFeedbackFigure();
		IFigure layer = getLayer(LayerConstants.FEEDBACK_LAYER);
		selectedElementFeedbackFigure = new SelectedElementFeedbackFigure(container, getHost());
		layer.add(selectedElementFeedbackFigure);
	}
}