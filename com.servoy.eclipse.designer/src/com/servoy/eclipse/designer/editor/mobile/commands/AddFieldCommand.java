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
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Pair;

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
		this(application, form, request.getType(), request.getExtendedData(), request.getLocation().getSWTPoint());
	}

	public AddFieldCommand(IApplication application, Form form, Object requestType, Map<Object, Object> objectProperties, Point defaultLocation)
	{
		super(application, form, null, requestType, objectProperties, null, defaultLocation, null, form);
	}

	@Override
	protected Object[] placeElements(Point location) throws RepositoryException
	{
		if (parent instanceof Form)
		{
			Pair<Field, GraphicalComponent> pair = ElementFactory.createMobileFieldWithTitle((Form)parent, location);
			if (objectProperties != null && objectProperties.size() > 0) setProperiesOnModel(pair.getLeft(), objectProperties);
			return new Object[] { new FormElementGroup(pair.getLeft().getGroupID(), ModelUtils.getEditingFlattenedSolution(parent), (Form)parent) };
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