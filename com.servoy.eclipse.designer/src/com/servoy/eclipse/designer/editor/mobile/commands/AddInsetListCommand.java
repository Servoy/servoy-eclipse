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

package com.servoy.eclipse.designer.editor.mobile.commands;

import java.awt.Dimension;

import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

/**
 * Command to add a inset list to the form.
 * The inset list consists of a tabpanel with 3 elements
 * 
 * @author rgansevles
 *
 */
public class AddInsetListCommand extends BaseFormPlaceElementCommand
{
	public AddInsetListCommand(IApplication application, Form form, CreateRequest request)
	{
		super(application, form, null, request.getType(), null, null, request.getLocation().getSWTPoint(), null, form);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			Form form = (Form)parent;

			// create a portal
			Portal portal = ElementFactory.createPortal(form, null, false, false, false, false, location, "list");
			if (portal == null)
			{
				ServoyLog.logError("Could not create portal for inset list", null);
				return null;
			}

			portal.putCustomMobileProperty(IMobileProperties.LIST_COMPONENT.propertyName, Boolean.TRUE);
			// for debug in developer
			portal.setSize(new Dimension(((Form)parent).getWidth(), 300));
			portal.setAnchors(IAnchorConstants.ALL);

			// add items for properties
			MobileListModel model = AddFormListCommand.addlistItems(form, portal, location);

			// add header
			GraphicalComponent header = ElementFactory.createLabel(portal, null, location);
			// set labelfor for display in webclient
			model.button.setName("button" + UUID.randomUUID().toString().replace('-', '_').toLowerCase());
			header.setLabelFor(model.button.getName());
			header.setDisplaysTags(false);
			header.putCustomMobileProperty(IMobileProperties.LIST_ITEM_HEADER.propertyName, Boolean.TRUE);
			// for debug in developer
			header.setStyleClass("b"); // default for headers

			// models is portal
			return new IPersist[] { portal };
		}

		return null;
	}
}