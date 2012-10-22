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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.RequestConstants;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.ForwardedRequest;
import org.eclipse.gef.requests.GroupRequest;

import com.servoy.eclipse.designer.editor.BaseFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.mobile.MobileVisualFormEditor;
import com.servoy.eclipse.designer.editor.mobile.commands.DeleteListCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddButtonCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddHeaderTitleCommand;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
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
	private final IApplication application;

	public MobileFormXYLayoutEditPolicy(IApplication application)
	{
		this.application = application;
	}

	@Override
	protected EditPolicy createChildEditPolicy(EditPart child)
	{
		return null; // no resizing of elements
	}

	@Override
	protected Command getCreateCommand(CreateRequest request)
	{
		MobileSnapData snapData = (MobileSnapData)request.getExtendedData().get(MobileSnapToHelper.MOBILE_SNAP_DATA);

		MobileSnapType snapType = snapData == null ? null : snapData.snapType;

		Form form = ((BaseFormGraphicalEditPart)getHost()).getPersist();
		if (request.getNewObjectType() == MobileVisualFormEditor.REQ_PLACE_BUTTON)
		{
			return new MobileAddButtonCommand(application, form, request, snapType);
		}

		// TODO check drop target
		if (snapType == MobileSnapType.HeaderText && request.getNewObjectType() == MobileVisualFormEditor.REQ_PLACE_HEADER_TITLE)
		{
			return new MobileAddHeaderTitleCommand(form);
		}

//		if (snapData.snapType == MobileSnapType.FooterItem)
//		{
//			return new MobileAddFooterItemCommand(form, request.getNewObjectType());
//		}

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
					List<EditPart> partEditParts = childEditPart.getChildren();
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
			else if (model instanceof FormElementGroup)
			{
				// group
				CompoundCommand compoundCmd = new CompoundCommand();
				for (IFormElement elem : Utils.iterate(((FormElementGroup)model).getElements()))
				{
					compoundCmd.add(new FormElementDeleteCommand(elem));
				}
				return compoundCmd;
			}
			else if (model instanceof MobileListModel)
			{
				// list
				return new DeleteListCommand((MobileListModel)model);
			}
		}
		return super.getDeleteDependantCommand(request);
	}
}
