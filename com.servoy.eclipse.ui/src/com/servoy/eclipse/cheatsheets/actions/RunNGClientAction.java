/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.cheatsheets.actions;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;

import com.servoy.j2db.util.Debug;

/**
 * @author gboros
 *
 */
public class RunNGClientAction extends Action
{
	@Override
	public void run()
	{
		Command command = (PlatformUI.getWorkbench().getService(ICommandService.class)).getCommand("com.servoy.eclipse.ui.StartNGClient");
		final Event trigger = new Event();
		ExecutionEvent executionEvent = (PlatformUI.getWorkbench().getService(IHandlerService.class)).createExecutionEvent(command, trigger);
		try
		{
			command.executeWithChecks(executionEvent);
		}
		catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e)
		{
			Debug.log(e);
		}
	}
}
