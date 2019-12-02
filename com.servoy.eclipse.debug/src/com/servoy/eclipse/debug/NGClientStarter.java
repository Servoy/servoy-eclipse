/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.debug;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.ngclient.ui.RunNPMCommand;
import com.servoy.eclipse.ngclient.ui.utils.NGClientConstants;

/**
 * @author fbandrei
 *
 * This interface is used for running a progress bar before starting the client.
 *
 */
public interface NGClientStarter
{
	/**
	 * Start a progress bar when the client is started before the build is started or during the build.
	 * The NPM install is also caught (it is executed before the build - if executed).
	 * 0 build jobs means that the build (in watch mode) was not started
	 *
	 * @param monitor
	 */
	default void runProgressBarAndNGClient(IProgressMonitor monitor)
	{
		Job[] buildJobs = Job.getJobManager().find(NGClientConstants.NPM_BUILD_JOB);
		if (buildJobs.length == 0 || RunNPMCommand.isNGBuildRunning())
		{
			Display.getDefault().asyncExec(() -> {
				ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
				try
				{
					dialog.run(true, false, new IRunnableWithProgress()
					{
						@Override
						public void run(IProgressMonitor monitorNpmBuild) throws InvocationTargetException, InterruptedException
						{
							monitorNpmBuild.beginTask(NGClientConstants.NPM_CONFIGURATION_TITLE_PROGRESS_BAR, IProgressMonitor.UNKNOWN);
							while (Job.getJobManager().find(NGClientConstants.NPM_BUILD_JOB).length == 0 || RunNPMCommand.isNGBuildRunning())
							{
								// verify the NG build job at every second
								// the progress bar is completed if the command takes more than the given workload
								Thread.sleep(1000);
								monitorNpmBuild.worked(1);
							}
							monitorNpmBuild.done();
							startNGClient(monitor);
						}
					});
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			});
		}
		else // Start the client directly, no progress bar needed.
		{
			startNGClient(monitor);
		}
	}

	/**
	 * This method starts the NG client (web or desktop).
	 *
	 * @param monitor
	 */
	void startNGClient(IProgressMonitor monitor);
}
