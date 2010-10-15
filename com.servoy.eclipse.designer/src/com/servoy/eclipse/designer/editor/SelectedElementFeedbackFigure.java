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

import java.util.List;

import org.eclipse.draw2d.AncestorListener;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.requests.ChangeBoundsRequest;

/**
 * Feedback figure for a selected edit part.
 * 
 * @author rgansevles
 *
 */
public class SelectedElementFeedbackFigure extends Figure implements AncestorListener
{
	private final GraphicalEditPart container;
	private final GraphicalEditPart editPart;

	boolean addedFeedbackChildren = false;

	public SelectedElementFeedbackFigure(GraphicalEditPart container, GraphicalEditPart editPart)
	{
		this.container = container;
		this.editPart = editPart;
		bounds = null;
	}

	@Override
	public List<IFigure> getChildren()
	{
		if (!addedFeedbackChildren)
		{
			addFeedbackChildren();
			addedFeedbackChildren = true;
		}
		return super.getChildren();
	}

	@Override
	public void addNotify()
	{
		super.addNotify();
		// Listen to the owner figure so the handle moves when the
		// figure moves.
		editPart.getFigure().addAncestorListener(this);
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		editPart.getFigure().removeAncestorListener(this);
	}

	protected void addFeedbackChildren()
	{
		// create a fake request: move(0.0)
		ChangeBoundsRequest changeBoundsRequest = new ChangeBoundsRequest(RequestConstants.REQ_MOVE);
		changeBoundsRequest.setEditParts(editPart);
		ElementAlignmentItem[] elementAlignment = new SnapToElementAlignment(container).getElementAlignment(changeBoundsRequest);
		if (elementAlignment != null)
		{
			for (ElementAlignmentItem item : elementAlignment)
			{
				add(new AlignmentFeedbackFigure(item));
			}
		}
	}

	@Override
	public Rectangle getBounds()
	{
		if (bounds == null)
		{
			for (IFigure child : getChildren())
			{
				if (bounds == null)
				{
					bounds = child.getBounds().getCopy();
				}
				else
				{
					bounds.union(child.getBounds());
				}
			}
			if (bounds == null)
			{
				bounds = new Rectangle();
			}
		}
		return bounds;
	}


	public void ancestorAdded(IFigure ancestor)
	{
	}

	public void ancestorMoved(IFigure ancestor)
	{
		// recreate the feedback child figures
		removeAll();
		bounds = null;
		addFeedbackChildren();
	}

	public void ancestorRemoved(IFigure ancestor)
	{
	}

}
