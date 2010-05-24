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
import org.eclipse.gef.requests.GroupRequest;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.actions.SetPropertyRequest;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.ui.property.PersistPropertySource;

/**
 * Base action class to toggle change a boolean property of selected objects.
 */
public abstract class ToggleCheckboxAction extends DesignerSelectionAction
{
	private final String propertyId;
	private final String name;

	public ToggleCheckboxAction(IWorkbenchPart part, String propertyId, String name)
	{
		super(part, VisualFormEditor.REQ_SET_PROPERTY);
		this.propertyId = propertyId;
		this.name = name;
	}

	@Override
	protected void handleSelectionChanged()
	{
		firePropertyChange(new PropertyChangeEvent(this, IAction.CHECKED, null, Boolean.valueOf(isChecked())));
		super.handleSelectionChanged();
	}

	@Override
	public boolean isChecked()
	{
		for (Object sel : getSelectedObjects())
		{
			IPropertySource propertySource = (IPropertySource)Platform.getAdapterManager().getAdapter(sel, IPropertySource.class);
			if (propertySource instanceof PersistPropertySource)
			{
				if (((PersistPropertySource)propertySource).getPropertyDescriptor(propertyId) != null)
				{
					return Boolean.TRUE.equals(propertySource.getPropertyValue(propertyId));
				}
			}
		}

		return false;
	}

	@Override
	protected GroupRequest createRequest(List<EditPart> objects)
	{
		Boolean value = Boolean.valueOf(!isChecked());
		// set all editparts to the same value
		Map<EditPart, Object> values = new HashMap<EditPart, Object>(objects.size());
		for (EditPart editPart : objects)
		{
			values.put(editPart, value);
		}
		SetPropertyRequest setPropertyRequest = new SetPropertyRequest(requestType, propertyId, values, name);
		setPropertyRequest.setEditParts(objects);
		return setPropertyRequest;
	}

	@Override
	public void run()
	{
		super.run();
		firePropertyChange(new PropertyChangeEvent(this, IAction.CHECKED, null, Boolean.valueOf(isChecked())));
	}
}
