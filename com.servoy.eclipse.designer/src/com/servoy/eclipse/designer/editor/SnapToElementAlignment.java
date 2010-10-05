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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.ChangeBoundsRequest;

import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.IAnchorConstants;
import com.servoy.j2db.util.Settings;

/**
 * Snap elements according to alignment with other elements or container borders.
 * 
 * @author rgansevles
 *
 */
public class SnapToElementAlignment extends SnapToHelper
{
	/**
	 * A viewer property indicating whether the snap function is enabled. The
	 * value must  be a Boolean.
	 */
	public static final String PROPERTY_ALIGNMENT_ENABLED = "SnapToAlignment.isEnabled"; //$NON-NLS-1$

	/**
	 * Property set in the request extended data by the snap class, type will be  {@link ElementAlignmentItem[]}.
	 */
	public static final String ELEMENT_ALIGNMENT_REQUEST_DATA = "ElementAlignment.requestData"; //$NON-NLS-1$


	private final GraphicalEditPart container;

	private DesignerPreferences preferences;
	private int snapThreshhold;
	private int[] distances;

	public SnapToElementAlignment(GraphicalEditPart container)
	{
		this.container = container;
	}

	protected void readPreferences()
	{
		if (preferences == null)
		{
			preferences = new DesignerPreferences(Settings.getInstance());
			snapThreshhold = preferences.getAlignmentThreshold();
			distances = preferences.getAlignmentDistances();
		}
	}

	protected int getSnapThreshold()
	{
		readPreferences();
		return snapThreshhold;
	}

	protected int getSmallDistance()
	{
		readPreferences();
		return distances[0];
	}

	protected int getMediumDistance()
	{
		readPreferences();
		return distances[1];
	}

	protected int getLargeDistance()
	{
		readPreferences();
		return distances[2];
	}

	protected ElementAlignmentItem[] getElementAlignment(GraphicalEditPart container, ChangeBoundsRequest request)
	{
		List<EditPart> editParts = request.getEditParts();
		if (editParts.size() != 1 || !(editParts.get(0) instanceof GraphicalEditPart))
		{
			return null;
		}

		GraphicalEditPart editPart = (GraphicalEditPart)editParts.get(0);
		IFigure hostFigure = editPart.getFigure();
		PrecisionRectangle rect = new PrecisionRectangle(hostFigure.getBounds());
		rect.translate(request.getMoveDelta());
		rect.resize(request.getSizeDelta());

		ElementAlignmentItem vertical = null;
		ElementAlignmentItem horizontal = null;

		Object model = editPart.getModel();
		Form form = (Form)((model instanceof IPersist) ? ((IPersist)model).getAncestor(IRepository.FORMS) : null);
		if (form != null)
		{
			// Alignment: North to container
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.NORTH) != 0))
			{
				vertical = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, vertical, rect.y, 0, 10, form.getWidth() - 10, true);
			}

			// Alignment: West to container
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.WEST) != 0))
			{
				horizontal = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, horizontal, rect.x, 0, 10, form.getWidth() - 10, true);
			}

			// Alignment: East to container
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.EAST) != 0))
			{
				horizontal = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, horizontal, rect.x + rect.width, form.getWidth(), 10,
					form.getSize().height - 10, true);
			}
		}

		List<EditPart> children = container.getChildren();
		for (EditPart child : children)
		{
			if (!(child instanceof GraphicalEditPart) || editParts.contains(child))
			{
				continue;
			}

			Object elementModel = child.getModel();
			int anchors = 0;
			if ((elementModel instanceof ISupportAnchors))
			{
				anchors = ((ISupportAnchors)elementModel).getAnchors();
				if (anchors == 0) anchors = IAnchorConstants.DEFAULT;
				else if (anchors == -1) anchors = 0;
			}

			Rectangle childBounds = ((GraphicalEditPart)child).getFigure().getBounds();

			// Alignment: North to element
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.NORTH) != 0))
			{
				// align on top
				vertical = getSideAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, vertical, rect.y, childBounds.y, childBounds.x, childBounds.x +
					childBounds.width, (anchors & IAnchorConstants.NORTH) != 0);
				// distance to bottom-top
				vertical = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, vertical, rect.y, childBounds.y + childBounds.height,
					childBounds.x, childBounds.x + childBounds.width, elementModel instanceof Part ||
						((anchors & IAnchorConstants.NORTH) != 0 && (anchors & IAnchorConstants.SOUTH) == 0));
			}

			// Alignment: South to element
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.SOUTH) != 0))
			{
				// align on bottom
				vertical = getSideAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_SOUTH, vertical, rect.y + rect.height, childBounds.y + childBounds.height,
					childBounds.x, childBounds.x + childBounds.width, (anchors & IAnchorConstants.SOUTH) != 0);
				// distance to top-bottom
				vertical = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_SOUTH, vertical, rect.y + rect.height, childBounds.y, childBounds.x,
					childBounds.x + childBounds.width, elementModel instanceof Part ||
						((anchors & IAnchorConstants.SOUTH) != 0 && (anchors & IAnchorConstants.NORTH) == 0));
			}

			// Alignment: West to element
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.WEST) != 0))
			{
				// align left
				horizontal = getSideAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, horizontal, rect.x, childBounds.x, childBounds.y, childBounds.y +
					childBounds.height, (anchors & IAnchorConstants.WEST) != 0);
				// distance to right-left
				horizontal = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, horizontal, rect.x, childBounds.x + childBounds.width,
					childBounds.y, childBounds.y + childBounds.height, (anchors & IAnchorConstants.WEST) != 0 && (anchors & IAnchorConstants.EAST) == 0);
			}

			// Alignment: East to element
			if (RequestConstants.REQ_MOVE.equals(request.getType()) ||
				(RequestConstants.REQ_RESIZE.equals(request.getType()) && (request.getResizeDirection() & PositionConstants.EAST) != 0))
			{
				// align on left
				horizontal = getSideAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, horizontal, rect.x + rect.width,
					childBounds.x + childBounds.width, childBounds.y, childBounds.y + childBounds.height, (anchors & IAnchorConstants.EAST) != 0);
				// distance to left-right
				horizontal = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, horizontal, rect.x + rect.width, childBounds.x, childBounds.y,
					childBounds.y + childBounds.height, (anchors & IAnchorConstants.EAST) != 0 && (anchors & IAnchorConstants.WEST) == 0);
			}
		}

		if (horizontal == null && vertical == null)
		{
			return null;
		}
		if (horizontal == null)
		{
			return new ElementAlignmentItem[] { vertical };
		}
		if (vertical == null)
		{
			return new ElementAlignmentItem[] { horizontal };
		}
		return new ElementAlignmentItem[] { horizontal, vertical };
	}

	protected ElementAlignmentItem getDistanceAlignmentItem(String alignDirection, ElementAlignmentItem item, int dragOffset, int offset, int start, int end,
		boolean anchor)
	{
		int sign = (ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(alignDirection) || ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(alignDirection)) ? 1
			: -1;
		int largeOffset = offset + (sign * getLargeDistance());
		int mediumOffset = offset + (sign * getMediumDistance());
		int smallOffset = offset + (sign * getSmallDistance());


		int largeDif = Math.abs(largeOffset - dragOffset);
		int mediumDif = Math.abs(mediumOffset - dragOffset);
		int smallDif = Math.abs(smallOffset - dragOffset);

		int dif;
		String alignType;
		int alignOffset;
		if (largeDif < mediumDif)
		{
			dif = largeDif;
			alignOffset = largeOffset;
			alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE;
		}
		else if (smallDif < mediumDif)
		{
			dif = smallDif;
			alignOffset = smallOffset;
			alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_SMALL;
		}
		else
		{
			dif = mediumDif;
			alignOffset = mediumOffset;
			alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM;
		}

		if (dif < (item == null ? getSnapThreshold() : item.distance))
		{
			// closer match
			return new ElementAlignmentItem(alignDirection, alignType, alignOffset, dif, start, end, anchor);
		}

		if (item != null && dif == item.distance && alignType.equals(item.alignType))
		{
			// same match, extend start+end
			return new ElementAlignmentItem(alignDirection, alignType, alignOffset, dif, Math.min(item.start, start), Math.max(item.end, end), anchor ||
				item.anchor);
		}

		// no better match, keep existing
		return item;
	}

	protected ElementAlignmentItem getSideAlignmentItem(String alignDirection, ElementAlignmentItem item, int dragOffset, int offset, int start, int end,
		boolean anchor)
	{
		int dif = Math.abs(offset - dragOffset);
		if (dif < (item == null ? getSnapThreshold() : item.distance))
		{
			// closer match
			return new ElementAlignmentItem(alignDirection, ElementAlignmentItem.ALIGN_TYPE_SIDE, offset, dif, start, end, anchor);
		}

		if (item != null && dif == item.distance && ElementAlignmentItem.ALIGN_TYPE_SIDE.equals(item.alignType))
		{
			// same match, extend start+end
			return new ElementAlignmentItem(alignDirection, ElementAlignmentItem.ALIGN_TYPE_SIDE, offset, dif, Math.min(item.start, start), Math.max(item.end,
				end), anchor || item.anchor);
		}

		// no better match, keep existing
		return item;
	}


	@Override
	public int snapRectangle(Request request, int snapOrientation, PrecisionRectangle baseRect, PrecisionRectangle result)
	{
		ElementAlignmentItem[] elementAlignment = getElementAlignment(container, (ChangeBoundsRequest)request);
		if (elementAlignment == null)
		{
			return snapOrientation;
		}

		// store alignment info for feedback
		request.getExtendedData().put(ELEMENT_ALIGNMENT_REQUEST_DATA, elementAlignment);

		PrecisionRectangle baseRectCopy = baseRect.getPreciseCopy();
		container.getContentPane().translateToRelative(baseRectCopy);

		PrecisionRectangle correction = new PrecisionRectangle();
		container.getContentPane().translateToRelative(correction);

		int resultSnapOrientation = snapOrientation;
		for (ElementAlignmentItem item : elementAlignment)
		{
			String anchorProperty = null;

			// Snap north
			if (ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(item.alignDirection) && (resultSnapOrientation & (NORTH | VERTICAL)) != 0)
			{
				correction.preciseY += item.target - baseRectCopy.preciseY;
				if (RequestConstants.REQ_RESIZE.equals(request.getType()))
				{
					correction.preciseHeight += baseRectCopy.preciseY - item.target;
				}
				anchorProperty = AnchorPropertySource.TOP;
				resultSnapOrientation &= ~(NORTH | VERTICAL);
			}

			// Snap south
			else if (ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(item.alignDirection) && (resultSnapOrientation & (SOUTH | VERTICAL)) != 0)
			{
				if (RequestConstants.REQ_RESIZE.equals(request.getType()))
				{
					correction.preciseHeight += item.target - (baseRectCopy.y + baseRectCopy.height);
				}
				else
				{
					correction.preciseY += item.target - (baseRectCopy.preciseY + baseRectCopy.preciseHeight);
				}
				anchorProperty = AnchorPropertySource.BOTTOM;
				resultSnapOrientation &= ~(NORTH | VERTICAL);
			}

			// Snap west
			if (ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(item.alignDirection) && (resultSnapOrientation & (WEST | HORIZONTAL)) != 0)
			{
				correction.preciseX += item.target - baseRectCopy.preciseX;
				if (RequestConstants.REQ_RESIZE.equals(request.getType()))
				{
					correction.preciseWidth += baseRectCopy.preciseX - item.target;
				}
				anchorProperty = AnchorPropertySource.LEFT;
				resultSnapOrientation &= ~(WEST | HORIZONTAL);
			}

			// Snap east
			else if (ElementAlignmentItem.ALIGN_DIRECTION_EAST.equals(item.alignDirection) && (resultSnapOrientation & (EAST | HORIZONTAL)) != 0)
			{
				if (RequestConstants.REQ_RESIZE.equals(request.getType()))
				{
					correction.preciseWidth += item.target - (baseRectCopy.x + baseRectCopy.width);
				}
				else
				{
					correction.preciseX += item.target - (baseRectCopy.preciseX + baseRectCopy.preciseWidth);
				}
				anchorProperty = AnchorPropertySource.RIGHT;
				resultSnapOrientation &= ~(EAST | HORIZONTAL);
			}

			if (item.anchor && anchorProperty != null)
			{
				request.getExtendedData().put(FormXYLayoutPolicy.REQUEST_PROPERTY_PREFIX + "anchors." + anchorProperty, Boolean.TRUE);
			}
		}

		correction.updateInts();
		makeAbsolute(container.getContentPane(), correction);
		result.preciseX += correction.preciseX;
		result.preciseY += correction.preciseY;
		result.preciseWidth += correction.preciseWidth;
		result.preciseHeight += correction.preciseHeight;
		result.updateInts();

		return resultSnapOrientation;
	}
}
