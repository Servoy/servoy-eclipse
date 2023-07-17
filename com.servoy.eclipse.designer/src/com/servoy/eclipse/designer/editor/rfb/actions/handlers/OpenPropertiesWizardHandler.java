/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.commands.ConfigureCustomTypeCommand;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.util.Debug;

/**
 * @author emera
 */
public class OpenPropertiesWizardHandler implements IServerService
{
	private final ISelectionProvider selectionProvider;

	public OpenPropertiesWizardHandler(ISelectionProvider selectionProvider)
	{
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, JSONObject args)
	{
		PersistContext[] selection = null;
		if (selectionProvider != null)
		{
			selection = (PersistContext[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new PersistContext[0]);
		}
		if (selection.length >= 1 && args.has("name"))
		{
			String propertyName = args.getString("name");
			Command command = (PlatformUI.getWorkbench().getService(ICommandService.class)).getCommand(ConfigureCustomTypeCommand.COMMAND_ID);
			ExecutionEvent executionEvent = null;
			try
			{
				Map<String, String> parameters = new HashMap<>();
				parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.config.type", propertyName);
				executionEvent = new ExecutionEvent(command, parameters, new Event(), null);
				command.executeWithChecks(executionEvent);
			}
			catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e)
			{
				Debug.log(e);
			}
		}
		return null;
	}
}
