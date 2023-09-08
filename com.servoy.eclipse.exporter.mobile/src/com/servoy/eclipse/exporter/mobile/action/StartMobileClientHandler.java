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
package com.servoy.eclipse.exporter.mobile.action;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartClientHandler;
import com.servoy.eclipse.exporter.mobile.launch.IMobileLaunchConstants;
import com.servoy.eclipse.exporter.mobile.launch.MobileLaunchUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 *
 */
public class StartMobileClientHandler extends StartClientHandler implements IHandler
{
	final static IDialogSettings fDialogSettings = Activator.getDefault().getDialogSettings();

	public StartMobileClientHandler()
	{
		super();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		//make sure the plugins are loaded
		DebugPlugin.getDefault();
		DebugUIPlugin.getDefault();

		final String launchConfig = event.getParameter("com.servoy.eclipse.mobile.launch.config") != null
			? event.getParameter("com.servoy.eclipse.mobile.launch.config") : fDialogSettings.get("com.servoy.eclipse.mobile.launch.config");
		Job job = new Job("Starting mobile client")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();

				if (StartMobileClientContribution.ORGANIZE.equals(launchConfig))
				{
					final String groupId = DebugUITools.getLaunchGroup(
						StartMobileClientContribution.getLaunchConfigsForProject(activeProject, true, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID)[0],
						"run").getIdentifier();
					try
					{
						Display.getDefault().syncExec(new Runnable()
						{
							public void run()
							{
								DebugUITools.openLaunchConfigurationDialogOnGroup(UIUtils.getActiveShell(), new StructuredSelection(), groupId);
							}
						});
					}
					catch (Exception ex)
					{
						/*
						 * in linux it throws a null pointer exception : java.lang.NullPointerException at
						 * org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationsDialog.close(LaunchConfigurationsDialog.java:350)
						 */
						ServoyLog.logError(ex);
					}
				}
				else
				{
					String solutionName = activeProject.getSolution().getName();
					String config = launchConfig == null
						? StartMobileClientContribution.getDefaultConfigName(solutionName, IMobileLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID, true)
						: launchConfig;
					DebugUITools.launch(StartMobileClientContribution.getLaunchConfigByName(solutionName, config), ILaunchManager.RUN_MODE);
					fDialogSettings.put("com.servoy.eclipse.mobile.launch.config", config);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public static String getDefaultApplicationURL(boolean testURL)
	{
		String solutionName = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution().getName();
		return MobileLaunchUtils.getDefaultApplicationURL(MobileLaunchUtils.getWarFileName(solutionName, testURL),
			ApplicationServerRegistry.get().getWebServerPort());
	}
}
