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

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Shape;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.GraphicalEditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.ForwardedRequest;
import org.eclipse.gef.requests.GroupRequest;

import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.DeleteListCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.ReorderContentElementsCommand;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Utils;

/**
 * Edit policy for adding/deleting components in mobile form editor.
 *
 * @author rgansevles
 *
 */
public class MobileFormXYLayoutEditPolicy extends XYLayoutEditPolicy
{
	@Override
	protected EditPolicy createChildEditPolicy(EditPart child)
	{
		return new MobileSelectionEditPolicy(!(child.getModel() instanceof Part)); // only drag contents elements
	}

	@Override
	protected Command getCreateCommand(CreateRequest request)
	{
		return null;
	}

	@Override
	protected Command getDeleteDependantCommand(Request request)
	{
		if (request instanceof ForwardedRequest)
		{
			EditPart childEditPart = ((ForwardedRequest)request).getSender();
			Object model = childEditPart.getModel();
			if (model instanceof IPersist)
			{
				CompoundCommand compoundCmd = new CompoundCommand();

				if (model instanceof Part) // parts have subeditpart in editor, but these are not subitems in persist model, so call delete on actual part elements
				{
					List< ? extends EditPart> partEditParts = childEditPart.getChildren();
					if (partEditParts.size() > 0)
					{
						GroupRequest deleteReq = new GroupRequest(RequestConstants.REQ_DELETE);
						deleteReq.setEditParts(partEditParts);

						for (EditPart sub : partEditParts)
						{
							Command cmd = sub.getCommand(deleteReq);
							if (cmd != null)
							{
								compoundCmd.add(cmd);
							}
						}
					}
				}

				compoundCmd.add(new FormElementDeleteCommand((IPersist)model));
				return compoundCmd;
			}

			if (model instanceof FormElementGroup)
			{
				// group
				return new FormElementDeleteCommand(Utils.asArray(((FormElementGroup)model).getElements(), IPersist.class));
			}

			if (model instanceof MobileListModel)
			{
				// list
				return new DeleteListCommand((MobileListModel)model);
			}
		}
		return super.getDeleteDependantCommand(request);
	}

	@Override
	protected Command createChangeConstraintCommand(ChangeBoundsRequest request, EditPart childEditPart, Object constraint)
	{
		if (childEditPart instanceof GraphicalEditPart && constraint instanceof Rectangle && childEditPart.getModel() instanceof ISupportBounds)
		{
			return new ReorderContentElementsCommand((GraphicalEditPart)childEditPart, (Rectangle)constraint);
		}
		return super.createChangeConstraintCommand(request, childEditPart, constraint);
	}

	@Override
	protected void showSizeOnDropFeedback(CreateRequest request)
	{
		super.showSizeOnDropFeedback(request);

		MobileSnapData snapData = (MobileSnapData)request.getExtendedData().get(MobileSnapToHelper.MOBILE_SNAP_DATA);
		if (snapData != null && snapData.snapType == MobileSnapType.ContentItem)
		{
			makeDropFeedbackLine((Shape)getSizeOnDropFeedback(request));
		}
	}

	public static void makeDropFeedbackLine(Shape feedback)
	{
		feedback.setBackgroundColor(ColorConstants.blue);
		feedback.setForegroundColor(ColorConstants.blue);
		feedback.setFillXOR(false);
		feedback.setOutlineXOR(false);

		Rectangle bounds = feedback.getBounds().getCopy();
		feedback.translateToAbsolute(bounds);

		feedback.setBounds(new Rectangle(0, bounds.y - feedback.getParent().getBounds().y, MobileFormLayout.MOBILE_FORM_WIDTH, 3));
	}
}
