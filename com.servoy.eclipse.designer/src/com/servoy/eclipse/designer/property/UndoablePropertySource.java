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
package com.servoy.eclipse.designer.property;

import org.eclipse.gef.commands.Command;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;

/**
 * set properties via the editor's command stack
 * 
 * @author rgansevles
 * 
 */
public class UndoablePropertySource implements IPropertySource
{
	private final IPropertySource propertySource;
	private final BaseVisualFormEditor editorPart;

	public UndoablePropertySource(IPropertySource propertySource, BaseVisualFormEditor editorPart)
	{
		this.propertySource = propertySource;
		this.editorPart = editorPart;
	}

	public Object getEditableValue()
	{
		return propertySource.getEditableValue();
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		return propertySource.getPropertyDescriptors();
	}

	public Object getPropertyValue(Object id)
	{
		return propertySource.getPropertyValue(id);
	}

	public boolean isPropertySet(Object id)
	{
		return propertySource.isPropertySet(id);
	}

	protected IPropertyDescriptor getPropertyDescriptor(Object id)
	{
		for (IPropertyDescriptor pd : propertySource.getPropertyDescriptors())
		{
			if (pd.getId().equals(id))
			{
				return pd;
			}
		}
		return null;
	}

	public void setPropertyValue(Object id, Object value)
	{
		IPropertyDescriptor pd = getPropertyDescriptor(id);
		executeCommand(SetValueCommand.createSetvalueCommand(pd == null ? "" : pd.getDisplayName(), propertySource, id, value));
	}

	public void resetPropertyValue(Object id)
	{
		if (propertySource.isPropertySet(id))
		{
			ResetValueCommand restoreCmd = new ResetValueCommand();
			restoreCmd.setTarget(propertySource);
			restoreCmd.setPropertyId(id);
			executeCommand(restoreCmd);
		}
	}

	protected void executeCommand(Command command)
	{
		if (editorPart.getCommandStackEventListener().isRunningCommand())
		{
			// we are already running on the command stack, execute the command directly to prevent double-ctl-z for undo
			if (command != null && command.canExecute())
			{
				command.execute();
			}
		}
		else
		{
			editorPart.getCommandStack().execute(command);
		}
	}

	@Override
	public String toString()
	{
		return propertySource.toString();
	}
}
