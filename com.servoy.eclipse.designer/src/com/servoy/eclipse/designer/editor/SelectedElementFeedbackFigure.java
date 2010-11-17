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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.AncestorListener;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;

import com.servoy.j2db.persistence.Part;


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

	private final PropertyChangeListener viewerPropertyListener = new PropertyChangeListener()
	{
		public void propertyChange(PropertyChangeEvent evt)
		{
			String property = evt.getPropertyName();
			if (property.equals(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_ENABLED) ||
				property.equals(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE))
			{
				refresh();
			}
		}
	};

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
		editPart.getViewer().addPropertyChangeListener(viewerPropertyListener);
	}

	@Override
	public void removeNotify()
	{
		super.removeNotify();
		editPart.getFigure().removeAncestorListener(this);
		editPart.getViewer().removePropertyChangeListener(viewerPropertyListener);
	}

	protected void addFeedbackChildren()
	{
		addElementAlignmentFeedback();
		addSameSizeFeedback();
	}

	protected void addElementAlignmentFeedback()
	{
		if (!Boolean.TRUE.equals(editPart.getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE)))
		{
			return;
		}

		List<EditPart> editParts = new ArrayList<EditPart>(1);
		editParts.add(editPart);
		SnapToElementAlignment snapToElementAlignment = new SnapToElementAlignment(container);
		snapToElementAlignment.setSnapThreshold(0);
		Rectangle figureBounds = editPart.getFigure().getBounds();
		ElementAlignmentItem[] elementAlignment = snapToElementAlignment.getElementAlignment(figureBounds, PositionConstants.NSEW, editParts, false);
		if (elementAlignment != null)
		{
			for (ElementAlignmentItem item : elementAlignment)
			{
				add(new AlignmentFeedbackFigure(item, figureBounds));
			}
		}
	}

	protected void addSameSizeFeedback()
	{
		if (!Boolean.TRUE.equals(editPart.getViewer().getProperty(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_ENABLED)))
		{
			return;
		}

		Rectangle myBounds = editPart.getFigure().getBounds();
		boolean addedSameWidth = false;
		boolean addedSameHeight = false;

		List<EditPart> children = container.getChildren();
		for (EditPart child : children)
		{
			if (child.getModel() instanceof Part || child == editPart || !(child instanceof GraphicalEditPart))
			{
				continue;
			}

			IFigure childFigure = ((GraphicalEditPart)child).getFigure();
			Rectangle childBounds = childFigure.getBounds();
			if (myBounds.width >= 5 && myBounds.width == childBounds.width)
			{
				add(new SameSizeFeedbackFigure(SameSizeFeedbackFigure.SAME_WIDTH, childFigure));
				addedSameWidth = true;
			}
			if (myBounds.height >= 5 && myBounds.height == childBounds.height)
			{
				add(new SameSizeFeedbackFigure(SameSizeFeedbackFigure.SAME_HEIGHT, childFigure));
				addedSameHeight = true;
			}
		}

		if (addedSameWidth)
		{
			add(new SameSizeFeedbackFigure(SameSizeFeedbackFigure.SAME_WIDTH, editPart.getFigure()));
		}
		if (addedSameHeight)
		{
			add(new SameSizeFeedbackFigure(SameSizeFeedbackFigure.SAME_HEIGHT, editPart.getFigure()));
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

	protected void refresh()
	{
		// recreate the feedback child figures
		removeAll();
		bounds = null;
		addFeedbackChildren();
	}


	public void ancestorAdded(IFigure ancestor)
	{
	}

	public void ancestorMoved(IFigure ancestor)
	{
		refresh();
	}

	public void ancestorRemoved(IFigure ancestor)
	{
	}

}
