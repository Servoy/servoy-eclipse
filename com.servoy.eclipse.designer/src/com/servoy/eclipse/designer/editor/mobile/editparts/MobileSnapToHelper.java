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

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.SnapToHelper;
import org.eclipse.gef.requests.CreateRequest;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.RequestType;
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
		if (request instanceof CreateRequest && ((CreateRequest)request).getNewObjectType() instanceof RequestType)
		{
			RequestType requestType = (RequestType)((CreateRequest)request).getNewObjectType();


			((CreateRequest)request).getNewObjectType();
			MobileSnapData snapData = calculateSnapping(requestType, baseRect.x, baseRect.y);
			// store info for feedback and element creation
			request.getExtendedData().put(MOBILE_SNAP_DATA, snapData);

			if (snapData != null && snapData.bounds != null)
			{
				result.setBounds(snapData.bounds);
			}
		}
		return 0;
	}

	protected MobileSnapData calculateSnapping(RequestType requestType, int x, int y)
	{
		GraphicalEditPart target = findEditpartAt(x, y);
		if (target == null)
		{
			return null;
		}

		if (target.getModel() instanceof Part)
		{
			if (((Part)target.getModel()).getPartType() == Part.HEADER)
			{
				return calculateSnappingToHeader(requestType, target, x);
			}
			if (((Part)target.getModel()).getPartType() == Part.FOOTER)
			{
				return calculateSnappingToFooter(requestType, target, x, y);
			}
		}

		// Body

		return null;
	}

	protected MobileSnapData calculateSnappingToHeader(RequestType requestType, GraphicalEditPart target, int x)
	{
		// header, snap to leftbutton, text or rightbutton
		Rectangle headerBounds = target.getFigure().getBounds();
		if (x - headerBounds.x < headerBounds.width / 3 && requestType.type == RequestType.TYPE_BUTTON)
		{
			// left button
			return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderLeftButton, new Rectangle(headerBounds.x + 2, headerBounds.y + 2,
				headerBounds.width / 3 - 4, headerBounds.height - 4));
		}
		else if (x - headerBounds.x > headerBounds.width * 2 / 3 && requestType.type == RequestType.TYPE_BUTTON)
		{
			// right button
			return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderRightButton, new Rectangle(headerBounds.x + headerBounds.width * 2 / 3 + 2,
				headerBounds.y + 2, headerBounds.width / 3 - 4, headerBounds.height - 4));
		}
		else if (requestType.type == RequestType.TYPE_LABEL)
		{
			// text
			return new MobileSnapData(MobileSnapData.MobileSnapType.HeaderText, new Rectangle(headerBounds.x + headerBounds.width / 3 + 2, headerBounds.y + 2,
				headerBounds.width / 3 - 4, headerBounds.height - 4));
		}

		return null;
	}

	protected MobileSnapData calculateSnappingToFooter(RequestType requestType, GraphicalEditPart target, int x, int y)
	{
		// footer, snap to other elements
		Rectangle targetBounds = target.getFigure().getBounds();
		Rectangle.SINGLETON.x = targetBounds.x;
		Rectangle.SINGLETON.y = targetBounds.y;

		for (IFigure child : (List<IFigure>)target.getFigure().getChildren())
		{
			Rectangle bounds = child.getBounds();
			if (Rectangle.SINGLETON.y < bounds.y)
			{
				Rectangle.SINGLETON.y = bounds.y;
				Rectangle.SINGLETON.x = 0;
			}
			else if (Rectangle.SINGLETON.y == bounds.y && Rectangle.SINGLETON.x < bounds.x)
			{
				Rectangle.SINGLETON.x = bounds.x;
			}
		}

		return new MobileSnapData(MobileSnapData.MobileSnapType.FooterItem, new Rectangle(Rectangle.SINGLETON.x + 51, Rectangle.SINGLETON.y, 50, 40));
	}
}
