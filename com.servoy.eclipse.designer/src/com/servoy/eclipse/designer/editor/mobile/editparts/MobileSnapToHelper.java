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

import java.util.List;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.CreateRequest;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.Part;

/**
 * SnapToHelper for snapping to positions in mobile form editor.
 * Resulting snap data is stored in request.extendedData using key MOBILE_SNAP_DATA.
 * 
 * @author rgansevles
 *
 */
public class MobileSnapToHelper extends SnapToHelper
{
	/**
	 * Property set in the request extended data by the snap class, type will be  {@link MobileSnapData}.
	 */
	public static final String MOBILE_SNAP_DATA = "MobileSnapToHelper.requestData"; //$NON-NLS-1$

	private final GraphicalEditPart container;

	public MobileSnapToHelper(GraphicalEditPart container)
	{
		this.container = container;
	}

	private GraphicalEditPart findEditpartAt(int x, int y)
	{
		for (EditPart editPart : (List<EditPart>)container.getChildren())
		{
			if (editPart instanceof GraphicalEditPart && ((GraphicalEditPart)editPart).getFigure().getBounds().contains(x, y))
			{
				return (GraphicalEditPart)editPart;
			}
		}
		return null;
	}

	@Override
	public int snapRectangle(Request request, int snapOrientation, PrecisionRectangle baseRect, PrecisionRectangle result)
	{
		container.getFigure().translateToRelative(baseRect);

		MobileSnapData snapData = calculateSnapping(request, baseRect.x, baseRect.y, baseRect.height);
		// store info for feedback and element creation
		request.getExtendedData().put(MOBILE_SNAP_DATA, snapData);

		if (snapData != null)
		{
			result.setPreciseX(result.preciseX() + snapData.xdelta);
			result.setPreciseY(result.preciseY() + snapData.ydelta);
		}

		return 0;
	}

	protected MobileSnapData calculateSnapping(Request request, int x, int y, int height)
	{
		GraphicalEditPart target = findEditpartAt(MobileFormLayout.MOBILE_FORM_WIDTH / 2, y);
		if (target == null)
		{
			return null;
		}

		if (request instanceof CreateRequest && ((CreateRequest)request).getNewObjectType() instanceof RequestType)
		{
			RequestType requestType = (RequestType)((CreateRequest)request).getNewObjectType();
			if (target.getModel() instanceof Part)
			{
				if (((Part)target.getModel()).getPartType() == Part.HEADER || ((Part)target.getModel()).getPartType() == Part.TITLE_HEADER)
				{
					return calculateSnappingToHeader(requestType, target, ((CreateRequest)request).getSize(), x, y);
				}
				if (((Part)target.getModel()).getPartType() == Part.FOOTER || ((Part)target.getModel()).getPartType() == Part.TITLE_FOOTER)
				{
					return calculateSnappingToFooter(target, x, y);
				}
			}
		}

		GraphicalEditPart snapTarget = findEditpartAt(MobileFormLayout.MOBILE_FORM_WIDTH / 2, y + height);
		if (snapTarget == null)
		{
			snapTarget = target;
		}
		// Content
		return new MobileSnapData(MobileSnapData.MobileSnapType.ContentItem, 0, snapTarget.getFigure().getBounds().y +
			snapTarget.getFigure().getBounds().height - y + ((GraphicalEditPart)snapTarget.getParent()).getFigure().getBounds().y);
	}

	protected MobileSnapData calculateSnappingToHeader(RequestType requestType, GraphicalEditPart target, Dimension size, int x, int y)
	{
		// header, snap to leftbutton, text or rightbutton
		Rectangle headerBounds = target.getFigure().getBounds();
		if (requestType.type == RequestType.TYPE_BUTTON)
		{
			if (Math.abs(headerBounds.x - x) < headerBounds.width / 3)
			{
				// left button
				return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderLeftButton, 20 + headerBounds.x - x, 10 - y);
			}
			// right button
			return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderRightButton, headerBounds.x + headerBounds.width - size.width - 20 - x, 10 - y);
		}

		if (requestType.type == RequestType.TYPE_LABEL)
		{
			// text
			return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderText, (headerBounds.width - size.width) / 2 + headerBounds.x - x, 10 - y);
		}

		return null;
	}

	protected MobileSnapData calculateSnappingToFooter(GraphicalEditPart target, int x, int y)
	{
//		// footer, snap to other elements
//		Rectangle targetBounds = target.getFigure().getBounds();
//		Rectangle.SINGLETON.x = targetBounds.x;
//		Rectangle.SINGLETON.y = targetBounds.y;
//
//		for (IFigure child : (List<IFigure>)target.getFigure().getChildren())
//		{
//			Rectangle bounds = child.getBounds();
//			if (Rectangle.SINGLETON.y < bounds.y)
//			{
//				Rectangle.SINGLETON.y = bounds.y;
//				Rectangle.SINGLETON.x = 0;
//			}
//			else if (Rectangle.SINGLETON.y == bounds.y && Rectangle.SINGLETON.x < bounds.x)
//			{
//				Rectangle.SINGLETON.x = bounds.x;
//			}
//		}

		// TODO: snap between existing buttons
		return new MobileSnapData(MobileSnapData.MobileSnapType.FooterItem, 0, 0);
	}
}
