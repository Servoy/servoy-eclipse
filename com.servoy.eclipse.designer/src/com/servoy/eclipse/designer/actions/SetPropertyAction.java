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

package com.servoy.eclipse.designer.actions;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.ui.actions.WorkbenchPartAction;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.ui.property.IRAGTEST;

/**
 * Action to set a property to a specific value.
 *
 * @author rgansevles
 *
 */
public class SetPropertyAction extends WorkbenchPartAction
{
	private final EditPart editPart;
	private final String propertyId;
	private final String name;
	private final Object newValue;

	public SetPropertyAction(IWorkbenchPart part, int style, EditPart editPart, String propertyId, String name, Object newValue)
	{
		super(part, style);
		this.editPart = editPart;
		this.propertyId = propertyId;
		this.name = name;
		this.newValue = newValue;
	}

	@Override
	public boolean isChecked()
	{
		IPropertySource propertySource = Platform.getAdapterManager().getAdapter(editPart, IPropertySource.class);
		if (propertySource instanceof IRAGTEST)
		{
			if (((IRAGTEST)propertySource).getPropertyDescriptor(propertyId) != null)
			{
				return Boolean.TRUE.equals(propertySource.getPropertyValue(propertyId));
			}
		}

		return false;
	}

	@Override
	protected boolean calculateEnabled()
	{
		return true;
	}

	protected Object getNewValue()
	{
		return newValue;
	}

	protected Request createRequest()
	{
		return new SetPropertyRequest(BaseVisualFormEditor.REQ_SET_PROPERTY, propertyId, getNewValue(), name);
	}

	@Override
	public void run()
	{
		execute(createCommand());
	}

	protected Command createCommand()
	{
		return editPart.getCommand(createRequest());
	}
}
