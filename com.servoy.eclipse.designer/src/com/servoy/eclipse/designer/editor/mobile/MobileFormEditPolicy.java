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
package com.servoy.eclipse.designer.editor.mobile;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editpolicies.ComponentEditPolicy;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;

import com.servoy.eclipse.designer.editor.AddPartsCommand;
import com.servoy.eclipse.designer.editor.CreateElementRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.mobile.commands.AddBeanCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddFieldCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddFormListCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddInsetListCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddLabelCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddButtonCommand;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;
import com.servoy.eclipse.designer.editor.palette.RequestTypeCreationFactory;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * This edit policy enables edit actions on a Form.
 */
public class MobileFormEditPolicy extends ComponentEditPolicy
{
	private final IApplication application;

	/**
	 * @param application
	 */
	public MobileFormEditPolicy(IApplication application)
	{
		this.application = application;
	}

	@Override
	public Command getCommand(final Request request)
	{
		Command command = null;

		if (request instanceof CreateRequest)
		{
			if (request instanceof CreateElementRequest)
			{
				CreationFactory factory = ((CreateElementRequest)request).getFactory();
				if (factory instanceof RequestTypeCreationFactory)
				{
					request.getExtendedData().putAll(((RequestTypeCreationFactory)factory).getExtendedData());
				}
			}

			Form form = (Form)getHost().getModel();

			Object createType = ((CreateRequest)request).getNewObjectType();

			if ((createType == VisualFormEditor.REQ_PLACE_HEADER && !form.hasPart(Part.HEADER) && !form.hasPart(Part.TITLE_HEADER)) ||
				(createType == VisualFormEditor.REQ_PLACE_FOOTER && !form.hasPart(Part.FOOTER) && !form.hasPart(Part.TITLE_FOOTER)))
			{
				command = new AddPartsCommand(form, new int[] { createType == VisualFormEditor.REQ_PLACE_HEADER ? Part.TITLE_HEADER : Part.TITLE_FOOTER })
				{
					@Override
					protected Part createPart(int partTypeId) throws RepositoryException
					{
						Part part = super.createPart(partTypeId);
						part.setStyleClass("b"); // default theme
						return part;
					}
				};
			}
			else if (createType == VisualFormEditor.REQ_PLACE_FIELD)
			{
				command = new AddFieldCommand(application, form, (CreateRequest)request);
			}
			else if (createType == VisualFormEditor.REQ_PLACE_LABEL)
			{
				command = new AddLabelCommand(application, form, (CreateRequest)request);
			}
			else if (createType == VisualFormEditor.REQ_PLACE_INSET_LIST)
			{
				command = new AddInsetListCommand(application, form, (CreateRequest)request);
			}
			else if (createType == VisualFormEditor.REQ_PLACE_FORM_LIST)
			{
				command = new AddFormListCommand(application, form, request.getType(), ((CreateRequest)request).getLocation().getSWTPoint());
			}
			else if (createType == VisualFormEditor.REQ_PLACE_BUTTON)
			{
				command = new MobileAddButtonCommand(application, form, (CreateRequest)request, MobileSnapType.ContentItem);
			}
			else if (createType == VisualFormEditor.REQ_PLACE_BEAN)
			{
				command = new AddBeanCommand(application, form, (CreateRequest)request);
			}
		}

		if (command == null)
		{
			return super.getCommand(request);
		}
		return command;
	}
}
