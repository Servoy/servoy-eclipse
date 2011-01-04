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
package com.servoy.eclipse.designer.editor.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;

/**
 * Base action class to toggle change a boolean property of selected objects.
 * 
 * @author rgansevles
 */
public abstract class ToggleCheckboxActionDelegateHandler extends DesignerSelectionActionDelegateHandler
{
	private final String propertyId;
	private final String name;

	public ToggleCheckboxActionDelegateHandler(String propertyId, String name)
	{
		super(VisualFormEditor.REQ_SET_PROPERTY);
		this.propertyId = propertyId;
		this.name = name;
	}

	@Override
	protected Boolean calculateChecked()
	{
		for (Object sel : getSelection().toArray())
		{
			IPropertySource propertySource = (IPropertySource)Platform.getAdapterManager().getAdapter(sel, IPropertySource.class);
			if (propertySource instanceof PersistPropertySource)
			{
				if (((PersistPropertySource)propertySource).getPropertyDescriptor(propertyId) != null)
				{
					return (Boolean)propertySource.getPropertyValue(propertyId);
				}
			}
		}

		return Boolean.FALSE;
	}

	@Override
	protected Map<EditPart, Request> createRequests(List<EditPart> objects)
	{
		Boolean value = Boolean.valueOf(!calculateChecked().booleanValue());
		// set all editparts to the same value
		Map<EditPart, Request> requests = null;
		for (EditPart editPart : objects)
		{
			if (requests == null)
			{
				requests = new HashMap<EditPart, Request>(objects.size());
			}

			requests.put(editPart, new SetPropertyRequest(requestType, propertyId, value, name));
		}
		return requests;
	}
}
