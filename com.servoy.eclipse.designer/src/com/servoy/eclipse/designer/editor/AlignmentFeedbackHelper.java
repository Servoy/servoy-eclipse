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
import org.eclipse.gef.Request;

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
	}
}
