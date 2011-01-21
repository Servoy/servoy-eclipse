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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.Cursors;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.ResizeTracker;
import org.eclipse.swt.SWT;

/**
 * Drag tracker for resizing forms.
 * 
 * @since 6.0
 * 
 * @author rgansevles
 *
 */
public class FormResizeTracker extends ResizeTracker
{
	private final GraphicalEditPart owner;
	private List<EditPart> resizeOperationSet;

	/**
	 * @param owner
	 * @param direction
	 */
	public FormResizeTracker(GraphicalEditPart owner, int direction)
	{
		super(owner, direction);
		this.owner = owner;
		setDefaultCursor((PositionConstants.NORTH_SOUTH & direction) != 0 ? Cursors.SIZENS : Cursors.SIZEWE);
	}


	@Override
	protected List<EditPart> getOperationSet()
	{
		if (resizeOperationSet == null)
		{
			resizeOperationSet = new ArrayList<EditPart>(1);
			resizeOperationSet.add(owner);
		}
		return resizeOperationSet;
	}

	@Override
	protected GraphicalEditPart getTargetEditPart()
	{
		return owner;
	}

	@Override
	protected void updateSourceRequest()
	{
		ChangeBoundsRequest request = (ChangeBoundsRequest)getSourceRequest();
		Dimension d = getDragMoveDelta();

		Point location = new Point(getLocation());
		Point moveDelta = new Point(0, 0);
		Dimension resizeDelta = new Dimension(0, 0);

		request.setConstrainedResize(false);

		request.setCenteredResize(getCurrentInput().isModKeyDown(SWT.MOD1));

		if ((getResizeDirection() & PositionConstants.NORTH) != 0)
		{
			moveDelta.y += d.height;
			resizeDelta.height -= d.height;
		}
		if ((getResizeDirection() & PositionConstants.SOUTH) != 0)
		{
			resizeDelta.height += d.height;
		}
		if ((getResizeDirection() & PositionConstants.WEST) != 0)
		{
			moveDelta.x += d.width;
			resizeDelta.width -= d.width;
		}
		if ((getResizeDirection() & PositionConstants.EAST) != 0)
		{
			resizeDelta.width += d.width;
		}

		request.setMoveDelta(moveDelta);
		request.setSizeDelta(resizeDelta);
		request.setLocation(location);
		request.setEditParts(getOperationSet());

		request.getExtendedData().clear();

		// TODO: snapping
	}
}