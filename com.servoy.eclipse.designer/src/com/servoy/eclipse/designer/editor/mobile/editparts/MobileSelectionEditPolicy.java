/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.designer.editor.mobile.editparts;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.RectangleFigure;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.Handle;
import org.eclipse.gef.editpolicies.NonResizableEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;

/**
 * SelectionEditPolicy for use in mobile form editor.
 *
 * @author rgansevles
 *
 */
public class MobileSelectionEditPolicy extends NonResizableEditPolicy
{
	public MobileSelectionEditPolicy(boolean isDragAllowed)
	{
		setDragAllowed(isDragAllowed);
	}

	@Override
	protected List< ? extends Handle> createSelectionHandles()
	{
		// just 1 line (the move handles), no drag handles
		List<Handle> list = new ArrayList<>();
		createMoveHandle(list);
		return list;
	}

	@Override
	protected void showChangeBoundsFeedback(ChangeBoundsRequest request)
	{
		MobileSnapData snapData = (MobileSnapData)request.getExtendedData().get(MobileSnapToHelper.MOBILE_SNAP_DATA);
		if (snapData != null && snapData.snapType == MobileSnapType.ContentItem)
		{
			Shape feedback = (Shape)getDragSourceFeedbackFigure();

			Rectangle rect = new Rectangle(getInitialFeedbackBounds().getCopy());
			rect.translate(request.getMoveDelta());
			feedback.translateToRelative(rect);
			feedback.setBounds(rect);
			MobileFormXYLayoutEditPolicy.makeDropFeedbackLine(feedback);
		}
	}

	@Override
	protected IFigure createDragSourceFeedbackFigure()
	{
		RectangleFigure r = new RectangleFigure();
		r.setBounds(getInitialFeedbackBounds());
		addFeedback(r);
		return r;
	}
}
