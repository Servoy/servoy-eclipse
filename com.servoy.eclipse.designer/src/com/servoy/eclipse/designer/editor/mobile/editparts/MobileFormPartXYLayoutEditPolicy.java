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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.XYLayoutEditPolicy;
import org.eclipse.gef.requests.CreateRequest;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddButtonCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddHeaderTitleCommand;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Edit policy for adding/deleting components header/footer in mobile form editor.
 * 
 * @author rgansevles
 *
 */
public class MobileFormPartXYLayoutEditPolicy extends XYLayoutEditPolicy
{
	private final IApplication application;

	public MobileFormPartXYLayoutEditPolicy(IApplication application)
	{
		this.application = application;
	}

	@Override
	protected EditPolicy createChildEditPolicy(EditPart child)
	{
		return new MobileSelectionEditPolicy(false); // only drag contents elements
	}

	@Override
	protected Command getCreateCommand(CreateRequest request)
	{
		MobileSnapData snapData = (MobileSnapData)request.getExtendedData().get(MobileSnapToHelper.MOBILE_SNAP_DATA);
		MobileSnapType snapType = snapData == null ? null : snapData.snapType;

		Form form = ((MobilePartGraphicalEditPart)getHost()).getEditorPart().getForm();
		if (request.getNewObjectType() == VisualFormEditor.REQ_PLACE_BUTTON)
		{
			for (IPersist element : form.getAllObjectsAsList())
			{
				if (snapType == MobileSnapType.HeaderLeftButton && element instanceof AbstractBase &&
					Boolean.TRUE.equals(((AbstractBase)element).getCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName)))
				{
					// already a left button
					return null;
				}
				if (snapType == MobileSnapType.HeaderRightButton && element instanceof AbstractBase &&
					Boolean.TRUE.equals(((AbstractBase)element).getCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName)))
				{
					// already a right button
					return null;
				}
			}

			return new MobileAddButtonCommand(application, form, request, snapType);
		}

		if (snapType == MobileSnapType.HeaderText && request.getNewObjectType() == VisualFormEditor.REQ_PLACE_HEADER_TITLE)
		{
			for (IPersist element : form.getAllObjectsAsList())
			{
				if (element instanceof AbstractBase &&
					Boolean.TRUE.equals(((AbstractBase)element).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName)))
				{
					// already a title text
					return null;
				}
			}
			return new MobileAddHeaderTitleCommand(application, form, request);
		}

//		if (snapData.snapType == MobileSnapType.FooterItem)
//		{
//			return new MobileAddFooterItemCommand(form, request.getNewObjectType());
//		}

		return null;
	}
}
