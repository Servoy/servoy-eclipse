/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.debug.handlers;

import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.UIElement;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * @author emera
 */
public class StartClientHandler extends StartDebugHandler implements IElementUpdater
{


	static final String START_WEB_CLIENT = "com.servoy.eclipse.ui.StartWebClient";
	static final String START_SMART_CLIENT = "com.servoy.eclipse.ui.StartSmartClient";
	static final String START_NG_CLIENT = "com.servoy.eclipse.ui.StartNGClient";
	static final String START_MOBILE_CLIENT = "com.servoy.eclipse.ui.StartMobileClient";
	final static IDialogSettings fDialogSettings = Activator.getDefault().getDialogSettings();

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();

		Job job = new Job("Starting client")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				StartClientHandler.this.run(monitor);
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	private String getStartTitle(String commandId)
	{
		switch (commandId)
		{
			case START_SMART_CLIENT :
				return "Smart client start";
			case START_WEB_CLIENT :
				return "Web client start";
			case START_MOBILE_CLIENT :
				return "Mobile client start";
			default :
				return "NG client start";
		}
	}

	protected void run(IProgressMonitor monitor)
	{
		String commandId = getCommandId();
		if (commandId != null)
		{
			monitor.beginTask(getStartTitle(commandId), 2);
			monitor.worked(1);
			try
			{
				ICommandService cmdService = PlatformUI.getWorkbench().getService(ICommandService.class);
				Command command = cmdService.getCommand(commandId);
				final Event trigger = new Event();
				IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
				ExecutionEvent executionEvent = handlerService.createExecutionEvent(command, trigger);
				command.executeWithChecks(executionEvent);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			finally
			{
				monitor.done();
			}
		}

	}

	private String getCommandId()
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject != null && activeProject.getSolution() != null)
		{
			final Solution solution = activeProject.getSolution();
			switch (solution.getSolutionType())
			{
				case SolutionMetaData.SMART_CLIENT_ONLY :
					return START_SMART_CLIENT;
				case SolutionMetaData.WEB_CLIENT_ONLY :
					return START_WEB_CLIENT;
				case SolutionMetaData.NG_CLIENT_ONLY :
					return START_NG_CLIENT;
				case SolutionMetaData.MOBILE :
					return START_MOBILE_CLIENT;
				default :
					return fDialogSettings.get("lastDebugCommandId") != null ? fDialogSettings.get("lastDebugCommandId") : START_NG_CLIENT;//default NG
			}
		}
		return null;
	}

	public static void setLastCommand(String commandId)
	{
		fDialogSettings.put("lastDebugCommandId", commandId);
		ICommandService cmdService = PlatformUI.getWorkbench().getService(ICommandService.class);
		cmdService.refreshElements("com.servoy.eclipse.ui.StartClient", null);
	}

	@Override
	public void updateElement(UIElement element, Map parameters)
	{
		element.setTooltip(getStartTitle(getCommandId()));
	}
}
