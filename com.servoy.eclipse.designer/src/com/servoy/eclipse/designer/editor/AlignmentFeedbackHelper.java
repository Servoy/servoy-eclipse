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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;

import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.ISupportBounds;

/**
 * Helper for adding/removing of alignment feedback figures from the feedback layer.
 * 
 * @author rgansevles
 *
 */
public class AlignmentFeedbackHelper
{
	private final Map<ElementAlignmentItem, IFigure> alignmentFeedbackFigures = new HashMap<ElementAlignmentItem, IFigure>();

	private final IFigure feedbackLayer;

	/**
	 * @param host
	 */
	public AlignmentFeedbackHelper(IFigure feedbackLayer)
	{
		this.feedbackLayer = feedbackLayer;
	}

	public void showElementAlignmentFeedback(Request request)
	{
		showElementAlignmentFeedback((ElementAlignmentItem[])request.getExtendedData().get(SnapToElementAlignment.ELEMENT_ALIGNMENT_REQUEST_DATA));
		if (request instanceof ChangeBoundsRequest)
		{
			EditorUtil.setStatuslineMessage(getChangeBoundsFeedbackMessage((ChangeBoundsRequest)request));
		}
		else if (request instanceof CreateRequest)
		{
			EditorUtil.setStatuslineMessage(getCreateFeedbackMessage((CreateRequest)request));
		}
	}

	public void showElementAlignmentFeedback(ElementAlignmentItem[] feedbackItems)
	{
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
				feedbackLayer.remove(next.getValue());
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
				AlignmentFeedbackFigure figure = new AlignmentFeedbackFigure(item, null);
				alignmentFeedbackFigures.put(item, figure);
				feedbackLayer.add(figure);
			}
		}
	}

	public void eraseElementAlignmentFeedback()
	{
		for (IFigure figure : alignmentFeedbackFigures.values())
		{
			feedbackLayer.remove(figure);
		}
		alignmentFeedbackFigures.clear();
		EditorUtil.setStatuslineMessage(null);
	}

	protected String getChangeBoundsFeedbackMessage(ChangeBoundsRequest request)
	{
		boolean move = request.getMoveDelta().x != 0 || request.getMoveDelta().y != 0;
		boolean resize = request.getSizeDelta().width != 0 || request.getSizeDelta().height != 0;
		StringBuilder sb = new StringBuilder(resize ? "Resize to" : "Move to");

		for (EditPart editPart : (List<EditPart>)request.getEditParts())
		{
			if (sb.length() > 50) break; // don't make the string too long

			if (editPart.getModel() instanceof ISupportBounds)
			{
				java.awt.Point location = ((ISupportBounds)editPart.getModel()).getLocation();
				sb.append(" (");
				if (move)
				{
					sb.append(location.x + request.getMoveDelta().x).append(',').append(location.y + request.getMoveDelta().y);
				}
				if (resize)
				{
					java.awt.Dimension size = ((ISupportBounds)editPart.getModel()).getSize();
					if (move) sb.append(' ');
					sb.append(size.width + request.getSizeDelta().width).append('x').append(size.height + request.getSizeDelta().height);
				}
				sb.append(')');
			}
		}
		return sb.toString();
	}

	protected String getCreateFeedbackMessage(CreateRequest request)
	{
		Point location = request.getLocation();
		if (location == null)
		{
			return "";
		}
		location = location.getCopy();
		feedbackLayer.translateToRelative(location);
		StringBuilder sb = new StringBuilder().append('(').append(location.x).append(',').append(location.y);
		Dimension size = request.getSize();
		if (size != null) sb.append(' ').append(size.width).append('x').append(size.height);
		return sb.append(')').toString();
	}
}
