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

package com.servoy.eclipse.debug.handlers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;

import com.servoy.base.util.ITagResolver;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ZipUtils;
import com.servoy.eclipse.debug.Activator;
import com.servoy.eclipse.debug.actions.IDebuggerStartListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

/**
 * @author costinchiulan
 *
 */
public class StartNGDesktopClientHandler extends StartDebugHandler implements IRunnableWithProgress, IDebuggerStartListener
{
	public static ITagResolver noReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			return "%%" + name + "%%";
		}
	};

	public static ITagResolver simpleReplacementResolver = new ITagResolver()
	{
		public String getStringValue(String name)
		{
			if ("prefix".equals(name)) return "forms.customer.";
			else if ("elementName".equals(name)) return ".elements.customer_id";
			else return "%%" + name + "%%";
		}
	};

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{

		Job job = new Job("NGDesktop client launch")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartNGDesktopClientHandler.this.run(monitor);
				}
				catch (InvocationTargetException e)
				{
					ServoyLog.logError(e);
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
		return null;
	}

	public String getStartTitle()
	{
		return "NGDesktop client launch";
	}


	private void writeElectronJsonFile(Solution solution, File stateLocation, String fileExtension, IProgressMonitor monitor) throws IOException
	{

		String solutionUrl = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + solution.getName() + "/index.html";

		String osxContent = File.separator + "Contents" + File.separator;

		String fileUrl = osxContent + (Utils.isAppleMacOS() ? "Resources" : "resources") + File.separator + "app" + File.separator + "config" + File.separator +
			"servoy.json";

		File f = new File(stateLocation.getAbsolutePath() + (Utils.isAppleMacOS() ? "/ServoyNGDesktop.app" : "") + fileUrl);

		StringBuffer jsonFile = new StringBuffer();
		String line = null;
		BufferedReader bufferedReader = new BufferedReader(new FileReader(f));
		while ((line = bufferedReader.readLine()) != null)
		{
			jsonFile.append(line);
		}

		JSONObject configFile = new JSONObject(jsonFile.toString());

		JSONObject options = (JSONObject)configFile.get("options");

		options.put("url", solutionUrl);
		configFile.put("options", options);

		bufferedReader.close();

		FileWriter file = new FileWriter(f);
		BufferedWriter out = new BufferedWriter(file);
		out.write(configFile.toString());
		out.close();
		try
		{
			String cmd = Utils.isAppleMacOS() ? "/usr/bin/open" : Utils.isWindowsOS() ? "cmd /c start" : "./";

			String[] command = new String[] { cmd, stateLocation.getAbsolutePath() + File.separator + "ServoyNGDesktop" + fileExtension };

			monitor.beginTask("Open NGDesktop", 3);
			Runtime.getRuntime().exec(command);
			monitor.worked(2);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot find servoy NGDesktop executable", e);
		}

	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		StartClientHandler.setLastCommand(StartClientHandler.START_NG_DESKTOP_CLIENT);
		monitor.beginTask(getStartTitle(), 5);
		monitor.worked(1);

		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject != null && activeProject.getSolution() != null)
		{
			final Solution solution = activeProject.getSolution();

			if (solution.getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(Display.getDefault().getActiveShell(), "Solution type problem", "Cant open this solution type in this client");
					}
				});
				return;
			}
			monitor.worked(2);

			if (testAndStartDebugger())
			{
				String ngDesktopAppName = "ServoyNgDesktop";
				String extension = Utils.isAppleMacOS() ? ".app" : (Utils.isWindowsOS() ? ".exe" : "");

				File stateLocation = Activator.getDefault().getStateLocation().append(File.separator + "NGDesktop").toFile();

				File executable = new File((stateLocation.getAbsolutePath() + File.separator + ngDesktopAppName + extension));

				if (executable.exists())
				{
					try
					{
						writeElectronJsonFile(solution, stateLocation, extension, monitor);
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							ProgressMonitorDialog dialog = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
							try
							{
								dialog.run(true, false, new DownloadElectron());
								if (executable.exists()) writeElectronJsonFile(solution, stateLocation, extension, monitor);
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}

						}
					});
				}

			}
		}
		monitor.done();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.debug.actions.StartDebugAction#getDebuggerAboutToStartListener()
	 */
	@Override
	protected IDebuggerStartListener getDebuggerAboutToStartListener()
	{
		return this;
	}

	public void aboutToStartDebugClient()
	{

	}

}


class DownloadElectron implements IRunnableWithProgress
{


	/**
	 * @param appServer
	 * @param name
	 */
	public DownloadElectron()
	{
	}


	@Override
	public void run(IProgressMonitor monitor)
	{
		try
		{
			monitor.beginTask("Start downloading electron", 3);

			File f = new File(Activator.getDefault().getStateLocation().toOSString() + File.separator + "NGDesktop");
			f.mkdirs();

			URL fileUrl = new URL("http://download.servoy.com/ngdesktop/servoyngdesktop-2019.06-" +
				(Utils.isAppleMacOS() ? "mac" : (Utils.isWindowsOS() ? "win" : "linux")) + ".tar.gz");

			ZipUtils.extractTarGZ(fileUrl, f);
			monitor.worked(2);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Cannot find Electron in download center", e);
		}
	}


}

