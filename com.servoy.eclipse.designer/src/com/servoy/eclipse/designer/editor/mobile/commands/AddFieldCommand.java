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

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.designer.editor.commands.BaseFormPlaceElementCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Field;
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
public class AddFieldCommand extends BaseFormPlaceElementCommand
{
	public AddFieldCommand(IApplication application, Form form, CreateRequest request)
	{
		super(application, form, null, request.getType(), request.getExtendedData(), null, request.getLocation().getSWTPoint(), null, form);
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
			label.setDisplaysTags(false);
			label.setGroupID(groupID);
			label.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
			label.putCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName, Boolean.TRUE);
			Field field = ElementFactory.createField(form, null, new Point(loc.x, loc.y + 1)); // enforce order by y-pos
			field.setGroupID(groupID);
			field.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
			if (objectProperties != null && objectProperties.size() > 0) setProperiesOnModel(field, objectProperties);

			return new Object[] { new FormElementGroup(groupID, ModelUtils.getEditingFlattenedSolution(parent), form) };
		}

		return null;
	}

	@Override
	protected void setPropertiesOnModels()
	{
		// should only be done on field
	}

	public static void setProperiesOnModel(Object model, Map<Object, Object> objectProperties)
	{
		Command setPropertiesCommand = SetValueCommand.createSetPropertiesCommand(
			(IPropertySource)Platform.getAdapterManager().getAdapter(model, IPropertySource.class), objectProperties);
		if (setPropertiesCommand != null)
		{
			setPropertiesCommand.execute();
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, model, true);
		}

	}


}