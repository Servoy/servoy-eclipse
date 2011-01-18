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
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.tools.ResizeTracker;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;

import com.servoy.j2db.persistence.Part;

/**
 * Tracker for moving parts in form editor.
 * 
 * @since 6.0
 * 
 * @author rgansevles
 *
 */
public class PartMoveTracker extends ResizeTracker
{
	private final GraphicalEditPart owner;
	private final Part part;
	private List<GraphicalEditPart> partsOperationSet;

	/**
	 * @param owner
	 * @param direction
	 */
	public PartMoveTracker(Part part, GraphicalEditPart owner, int direction)
	{
		super(owner, direction);
		this.part = part;
		this.owner = owner;
	}

	@Override
	protected Cursor getDefaultCursor()
	{
		return Cursors.SIZENS;
	}

	@Override
	protected Request createSourceRequest()
	{
		Request request = super.createSourceRequest();
		request.setType(REQ_MOVE);
		return request;
	}

	@Override
	protected List<GraphicalEditPart> getOperationSet()
	{
		if (partsOperationSet == null)
		{
			partsOperationSet = new ArrayList<GraphicalEditPart>();
			partsOperationSet.add((GraphicalEditPart)owner.getViewer().getEditPartRegistry().get(part));
		}
		return partsOperationSet;
	}

	@Override
	protected GraphicalEditPart getTargetEditPart()
	{
		return getOperationSet().get(0);
	}

	@Override
	protected void updateSourceRequest()
	{
		ChangeBoundsRequest request = (ChangeBoundsRequest)getSourceRequest();
		Dimension d = getDragMoveDelta();

		Point location = new Point(getLocation());
		Point moveDelta = new Point(0, 0);

		request.setConstrainedResize(false);

		request.setCenteredResize(getCurrentInput().isModKeyDown(SWT.MOD1));

		if ((getResizeDirection() & PositionConstants.NORTH) != 0)
		{
			moveDelta.y += d.height;
		}

		request.setMoveDelta(moveDelta);
		request.setSizeDelta(null);
		request.setLocation(location);
		request.setEditParts(getOperationSet());

		request.getExtendedData().clear();

		// TODO: snapping
	}
}