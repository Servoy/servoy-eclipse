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

import java.util.Map;

import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;

import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IAnchorConstants;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

/**
 * Command to add a text input with label to the form
 * 
 * @author rgansevles
 *
 */
public class AddLabelCommand extends BaseFormPlaceElementCommand
{
	public AddLabelCommand(IApplication application, Form form, CreateRequest request)
	{
		this(application, form, request.getType(), request.getExtendedData(), request.getLocation() == null ? null : request.getLocation().getSWTPoint());
	}

	public AddLabelCommand(IApplication application, Form form, Object requestType, Map<Object, Object> data, Point location)
	{
		super(application, form, null, requestType, data, null, location, null, form);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			Form form = (Form)parent;

			// create a label and a text field in a group
			String groupID = UUID.randomUUID().toString();
			Point loc = location == null ? new Point(0, 0) : location;
			GraphicalComponent label = ElementFactory.createLabel(form, "Title", loc);
			label.setGroupID(groupID);
			label.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
			GraphicalComponent textLabel = ElementFactory.createLabel(form, "Text", new Point(loc.x, loc.y + 1)); // enforce order by y-pos
			textLabel.setGroupID(groupID);
			textLabel.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);

			return new Object[] { new FormElementGroup(groupID, ModelUtils.getEditingFlattenedSolution(parent), form) };
		}

		return null;

	}
}