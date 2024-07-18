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

import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.rulers.RulerProvider;

import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportAnchors;
import com.servoy.j2db.persistence.Part;

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
	public static final String PROPERTY_ALIGNMENT_ENABLED = "SnapToAlignment.isEnabled";

	/**
	 * Property set in the request extended data by the snap class, type will be  {@link ElementAlignmentItem[]}.
	 */
	public static final String ELEMENT_ALIGNMENT_REQUEST_DATA = "ElementAlignment.requestData";


	private final GraphicalEditPart container;

	private DesignerPreferences preferences;
	private int snapThreshold;
	private int indent;
	private int[] distances;
	private boolean setAnchor;

	public SnapToElementAlignment(GraphicalEditPart container)
	{
		this.container = container;
	}

	protected void readPreferences()
	{
		if (preferences == null)
		{
			preferences = new DesignerPreferences();
			snapThreshold = preferences.getAlignmentThreshold();
			indent = preferences.getAlignmentIndent();
			distances = preferences.getAlignmentDistances();
			setAnchor = preferences.getAnchor();
		}
	}

	protected void setSnapThreshold(int snapThreshold)
	{
		readPreferences();
		this.snapThreshold = snapThreshold;
	}

	protected int getSnapThreshold()
	{
		readPreferences();
		return snapThreshold;
	}

	protected int getIndent()
	{
		readPreferences();
		return indent;
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

	protected boolean getSetAnchor()
	{
		readPreferences();
		return setAnchor;
	}

	boolean isAligmenttEditPart(EditPart editPart)
	{
		if (!(editPart instanceof GraphicalEditPart))
		{
			return false;
		}
		Object model = editPart.getModel();
		if (model instanceof FormElementGroup && ((FormElementGroup)model).getParent() == container.getModel())
		{
			return true;
		}
		if (!(model instanceof Part) && model instanceof IPersist && ((IPersist)model).getParent() instanceof Form)
		{
			return true;
		}
		// do not align parts, tabs or portal fields
		return false;
	}

	protected ElementAlignmentItem[] getElementAlignmentForMoveOrResize(ChangeBoundsRequest request, int snapOrientation, boolean singleAlignmentPerDimension)
	{
		List< ? extends EditPart> editParts = request.getEditParts();
		if (editParts.size() == 0)
		{
			return null;
		}

		// calculate the rectangle around all selected elements
		PrecisionRectangle rect = null;
		for (EditPart ep : editParts)
		{
			if (isAligmenttEditPart(ep))
			{
				Rectangle bounds = ((GraphicalEditPart)ep).getFigure().getBounds();
				if (rect == null)
				{
					rect = new PrecisionRectangle(bounds);
				}
				else
				{
					rect.union(bounds);
				}
			}
		}

		if (rect == null)
		{
			// no applicable elements
			return null;
		}

		rect.translate(request.getMoveDelta());
		rect.resize(request.getSizeDelta());

		return getElementAlignment(rect, snapOrientation, request.getType().equals(RequestConstants.REQ_CLONE) ? null : editParts, singleAlignmentPerDimension);
	}

	protected ElementAlignmentItem[] getElementAlignmentForCreate(PrecisionRectangle baseRect, int snapOrientation, boolean singleAlignmentPerDimension)
	{
		Point loc = baseRect.getLocation().getCopy();
		container.getFigure().translateToRelative(loc);
		return getElementAlignment(new Rectangle(loc, baseRect.getSize()), snapOrientation, null, singleAlignmentPerDimension);
	}

	protected ElementAlignmentItem[] getElementAlignment(Rectangle rect, int snapOrientation, List< ? extends EditPart> skipEditparts,
		boolean singleAlignmentPerDimension)
	{
		if (snapOrientation == 0)
		{
			return null;
		}

		ElementAlignmentItem north = null;
		ElementAlignmentItem east = null;
		ElementAlignmentItem south = null;
		ElementAlignmentItem west = null;

		Form form = (Form)container.getModel();

		// Alignment: North to container
		if ((snapOrientation & (NORTH | VERTICAL)) != 0)
		{
			north = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, north, rect.y, 0, 10, form.getWidth() - 10, true, getSetAnchor());
		}

		// Alignment: West to container
		if ((snapOrientation & (WEST | HORIZONTAL)) != 0)
		{
			west = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, west, rect.x, 0, 10, form.getSize().height - 10, true, getSetAnchor());
		}

		// Alignment: East to container
		if ((snapOrientation & (EAST | HORIZONTAL)) != 0)
		{
			if (singleAlignmentPerDimension) east = west;

			east = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, east, rect.x + rect.width, form.getSize().width, 10,
				form.getSize().height - 10, true, getSetAnchor());

			if (singleAlignmentPerDimension) west = east;
		}

		// Snap against rulers
		EditPartViewer viewer = container.getViewer();

		RulerProvider rulerProvider = (RulerProvider)viewer.getProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER);
		if (rulerProvider != null)
		{
			for (int pos : rulerProvider.getGuidePositions())
			{
				// Alignment: West to guide
				if ((snapOrientation & (WEST | HORIZONTAL)) != 0)
				{
					west = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, west, rect.x, pos, 10, form.getSize().height - 10, true,
						getSetAnchor());
				}
				// Alignment: East to guide
				if ((snapOrientation & (EAST | HORIZONTAL)) != 0)
				{
					if (singleAlignmentPerDimension) east = west;

					east = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, east, rect.x + rect.width, pos, 10, form.getSize().height - 10,
						true, getSetAnchor());

					if (singleAlignmentPerDimension) west = east;
				}
			}
		}

		rulerProvider = (RulerProvider)viewer.getProperty(RulerProvider.PROPERTY_VERTICAL_RULER);
		if (rulerProvider != null)
		{
			for (int pos : rulerProvider.getGuidePositions())
			{
				// Alignment: North to guide
				if ((snapOrientation & (NORTH | VERTICAL)) != 0)
				{
					// distance to bottom-top
					north = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, north, rect.y, pos, Math.min(rect.x, 10),
						Math.max(rect.x + rect.width, 10 + form.getWidth() - 20), true, getSetAnchor());
				}

				// Alignment: South to guide
				if ((snapOrientation & (SOUTH | VERTICAL)) != 0)
				{
					if (singleAlignmentPerDimension) south = north;

					// distance to top-bottom
					south = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_SOUTH, south, rect.y + rect.height, pos, Math.min(rect.x, 10),
						Math.max(rect.x + rect.width, 10 + form.getWidth() - 20), true, getSetAnchor());

					if (singleAlignmentPerDimension) north = south;
				}
			}
		}

		for (EditPart child : (List<EditPart>)container.getChildren())
		{
			if ((!(child.getModel() instanceof Part) && !isAligmenttEditPart(child)) || (skipEditparts != null && skipEditparts.contains(child)))
			{
				continue;
			}

			Object elementModel = child.getModel();
			int anchors = 0;
			if (getSetAnchor() && elementModel instanceof ISupportAnchors)
			{
				anchors = ((ISupportAnchors)elementModel).getAnchors();
				if (anchors == 0) anchors = IAnchorConstants.DEFAULT;
				else if (anchors == -1) anchors = 0;
			}

			// align against bounds of child figure or part line
			Rectangle childBounds = (elementModel instanceof Part) ? new Rectangle(10, ((Part)elementModel).getHeight(), form.getWidth() - 20, 0)
				: ((GraphicalEditPart)child).getFigure().getBounds();

			// Alignment: North to element
			if ((snapOrientation & (NORTH | VERTICAL)) != 0)
			{
				// align on top
				north = getSideAlignmentItem(ElementAlignmentItem.ALIGN_TYPE_SIDE, ElementAlignmentItem.ALIGN_DIRECTION_NORTH, north, rect.y, childBounds.y,
					Math.min(rect.x, childBounds.x), Math.max(rect.x + rect.width, childBounds.x + childBounds.width), (anchors & IAnchorConstants.NORTH) != 0);
				// distance to bottom-top
				north = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_NORTH, north, rect.y, childBounds.y + childBounds.height,
					Math.min(rect.x, childBounds.x), Math.max(rect.x + rect.width, childBounds.x + childBounds.width), elementModel instanceof Part,
					getSetAnchor() && (elementModel instanceof Part || ((anchors & IAnchorConstants.NORTH) != 0 && (anchors & IAnchorConstants.SOUTH) == 0)));
			}


			// Alignment: South to element
			if ((snapOrientation & (SOUTH | VERTICAL)) != 0)
			{
				if (singleAlignmentPerDimension) south = north;

				// align on bottom
				south = getSideAlignmentItem(ElementAlignmentItem.ALIGN_TYPE_SIDE, ElementAlignmentItem.ALIGN_DIRECTION_SOUTH, south, rect.y + rect.height,
					childBounds.y + childBounds.height, Math.min(rect.x, childBounds.x), Math.max(rect.x + rect.width, childBounds.x + childBounds.width),
					(anchors & IAnchorConstants.SOUTH) != 0);
				// distance to top-bottom
				south = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_SOUTH, south, rect.y + rect.height, childBounds.y,
					Math.min(rect.x, childBounds.x), Math.max(rect.x + rect.width, childBounds.x + childBounds.width), elementModel instanceof Part,
					getSetAnchor() && (elementModel instanceof Part || ((anchors & IAnchorConstants.SOUTH) != 0 && (anchors & IAnchorConstants.NORTH) == 0)));

				if (singleAlignmentPerDimension) north = south;
			}

			// Alignment: West to element
			if ((snapOrientation & (WEST | HORIZONTAL)) != 0)
			{
				// indent to element, only on left side and below element
				if (rect.y > childBounds.y)
				{
					west = getSideAlignmentItem(ElementAlignmentItem.ALIGN_TYPE_INDENT, ElementAlignmentItem.ALIGN_DIRECTION_WEST, west, rect.x, childBounds.x +
						getIndent(), childBounds.y + childBounds.height, rect.y, (anchors & IAnchorConstants.WEST) != 0);
				}

				// align left
				west = getSideAlignmentItem(ElementAlignmentItem.ALIGN_TYPE_SIDE, ElementAlignmentItem.ALIGN_DIRECTION_WEST, west, rect.x, childBounds.x,
					Math.min(rect.y, childBounds.y), Math.max(rect.y + rect.height, childBounds.y + childBounds.height),
					(anchors & IAnchorConstants.WEST) != 0);
				// distance to right-left
				west = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_WEST, west, rect.x, childBounds.x + childBounds.width,
					Math.min(rect.y, childBounds.y), Math.max(rect.y + rect.height, childBounds.y + childBounds.height), false,
					(anchors & IAnchorConstants.WEST) != 0 && (anchors & IAnchorConstants.EAST) == 0);
			}

			// Alignment: East to element
			if ((snapOrientation & (EAST | HORIZONTAL)) != 0)
			{
				if (singleAlignmentPerDimension) east = west;

				// align on right
				east = getSideAlignmentItem(ElementAlignmentItem.ALIGN_TYPE_SIDE, ElementAlignmentItem.ALIGN_DIRECTION_EAST, east, rect.x + rect.width,
					childBounds.x + childBounds.width, Math.min(rect.y, childBounds.y), Math.max(rect.y + rect.height, childBounds.y + childBounds.height),
					(anchors & IAnchorConstants.EAST) != 0);
				// distance to left-right
				east = getDistanceAlignmentItem(ElementAlignmentItem.ALIGN_DIRECTION_EAST, east, rect.x + rect.width, childBounds.x,
					Math.min(rect.y, childBounds.y), Math.max(rect.y + rect.height, childBounds.y + childBounds.height), false,
					(anchors & IAnchorConstants.EAST) != 0 && (anchors & IAnchorConstants.WEST) == 0);

				if (singleAlignmentPerDimension) west = east;
			}
		}

		if (north == null && east == null && south == null && west == null)
		{
			return null;
		}
		List<ElementAlignmentItem> alignment = new ArrayList<ElementAlignmentItem>(4);
		if (north != null) alignment.add(north);
		if (!singleAlignmentPerDimension && east != null) alignment.add(east);
		if (!singleAlignmentPerDimension && south != null) alignment.add(south);
		if (west != null) alignment.add(west);
		return alignment.toArray(new ElementAlignmentItem[alignment.size()]);
	}

	protected ElementAlignmentItem getDistanceAlignmentItem(String alignDirection, ElementAlignmentItem item, int dragOffset, int offset, int start, int end,
		boolean addZeroDistance, boolean anchor)
	{
		int sign = (ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(alignDirection) || ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(alignDirection)) ? 1
			: -1;
		int largeOffset = offset + (sign * getLargeDistance());
		int mediumOffset = offset + (sign * getMediumDistance());
		int smallOffset = offset + (sign * getSmallDistance());

		int zeroDif = offset - dragOffset;
		int largeDif = largeOffset - dragOffset;
		int mediumDif = mediumOffset - dragOffset;
		int smallDif = smallOffset - dragOffset;

		int zeroDistance = Math.abs(zeroDif);
		int largeDistance = Math.abs(largeDif);
		int mediumDistance = Math.abs(mediumDif);
		int smallDistance = Math.abs(smallDif);

		int delta;
		String alignType;
		int alignOffset;
		if (largeDistance < mediumDistance)
		{
			delta = largeDif;
			alignOffset = largeOffset;
			alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_LARGE;
		}
		else if (smallDistance < mediumDistance)
		{
			if (addZeroDistance && zeroDistance < smallDistance)
			{
				delta = zeroDif;
				alignOffset = offset;
				alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_ZERO;
			}
			else
			{
				delta = smallDif;
				alignOffset = smallOffset;
				alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_SMALL;
			}
		}
		else
		{
			delta = mediumDif;
			alignOffset = mediumOffset;
			alignType = ElementAlignmentItem.ALIGN_TYPE_DISTANCE_MEDIUM;
		}

		if (Math.abs(delta) < (item == null ? getSnapThreshold() + 1 : Math.abs(item.delta)))
		{
			// closer match
			return new ElementAlignmentItem(alignDirection, alignType, alignOffset, delta, start, end, anchor);
		}

		if (item != null && delta == item.delta && alignType.equals(item.alignType))
		{
			// same match, extend start+end
			return new ElementAlignmentItem(alignDirection, alignType, alignOffset, delta, Math.min(item.start, start), Math.max(item.end, end), anchor ||
				item.anchor);
		}

		// no better match, keep existing
		return item;
	}

	protected ElementAlignmentItem getSideAlignmentItem(String alignType, String alignDirection, ElementAlignmentItem item, int dragOffset, int offset,
		int start, int end, boolean anchor)
	{
		int delta = offset - dragOffset;
		if (Math.abs(delta) < (item == null ? getSnapThreshold() + 1 : Math.abs(item.delta)))
		{
			// closer match
			return new ElementAlignmentItem(alignDirection, alignType, offset, delta, start, end, anchor);
		}

		if (item != null && offset == item.target && delta == item.delta && alignType.equals(item.alignType))
		{
			// same match, extend start+end
			return new ElementAlignmentItem(alignDirection, alignType, offset, delta, Math.min(item.start, start), Math.max(item.end, end), anchor ||
				item.anchor);
		}

		// no better match, keep existing
		return item;
	}

	@Override
	public int snapRectangle(Request request, int snapOrientation, PrecisionRectangle baseRect, PrecisionRectangle result)
	{
		boolean isResize = RequestConstants.REQ_RESIZE.equals(request.getType()) ||
			(request instanceof CreateElementRequest && ((CreateElementRequest)request).isResizable());
		// when it is a resizing request, create separate alignment for n/e/s/w

		ElementAlignmentItem[] elementAlignment = null;
		if (request instanceof ChangeBoundsRequest)
		{
			elementAlignment = getElementAlignmentForMoveOrResize((ChangeBoundsRequest)request, snapOrientation, !isResize);
		}
		else if (request instanceof CreateRequest)
		{
			elementAlignment = getElementAlignmentForCreate(baseRect, snapOrientation, !isResize);
		}

		if (elementAlignment == null)
		{
			return snapOrientation;
		}

		// store alignment info for feedback
		request.getExtendedData().put(ELEMENT_ALIGNMENT_REQUEST_DATA, elementAlignment);

		PrecisionRectangle correction = new PrecisionRectangle();
		container.getContentPane().translateToRelative(correction);

		int resultSnapOrientation = snapOrientation;
		for (ElementAlignmentItem item : elementAlignment)
		{
			String anchorProperty = null;

			// Snap north
			if (ElementAlignmentItem.ALIGN_DIRECTION_NORTH.equals(item.alignDirection) && (resultSnapOrientation & (NORTH | VERTICAL)) != 0)
			{
				correction.preciseY += item.delta;
				if (isResize)
				{
					correction.preciseHeight -= item.delta;
				}
				anchorProperty = AnchorPropertySource.TOP;
				resultSnapOrientation &= ~(NORTH | VERTICAL);
			}

			// Snap south
			else if (ElementAlignmentItem.ALIGN_DIRECTION_SOUTH.equals(item.alignDirection) && (resultSnapOrientation & (SOUTH | VERTICAL)) != 0)
			{
				if (isResize)
				{
					correction.preciseHeight += item.delta;
				}
				else
				{
					correction.preciseY += item.delta;
				}
				anchorProperty = AnchorPropertySource.BOTTOM;
				resultSnapOrientation &= ~(SOUTH | VERTICAL);
			}

			// Snap west
			if (ElementAlignmentItem.ALIGN_DIRECTION_WEST.equals(item.alignDirection) && (resultSnapOrientation & (WEST | HORIZONTAL)) != 0)
			{
				correction.preciseX += item.delta;
				if (isResize)
				{
					correction.preciseWidth -= item.delta;
				}
				anchorProperty = AnchorPropertySource.LEFT;
				resultSnapOrientation &= ~(WEST | HORIZONTAL);
			}

			// Snap east
			else if (ElementAlignmentItem.ALIGN_DIRECTION_EAST.equals(item.alignDirection) && (resultSnapOrientation & (EAST | HORIZONTAL)) != 0)
			{
				if (isResize)
				{
					correction.preciseWidth += item.delta;
				}
				else
				{
					correction.preciseX += item.delta;
				}
				anchorProperty = AnchorPropertySource.RIGHT;
				resultSnapOrientation &= ~(EAST | HORIZONTAL);
			}

			if (item.anchor && anchorProperty != null)
			{
				request.getExtendedData().put(SetValueCommand.REQUEST_PROPERTY_PREFIX + "anchors." + anchorProperty, Boolean.TRUE);
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
