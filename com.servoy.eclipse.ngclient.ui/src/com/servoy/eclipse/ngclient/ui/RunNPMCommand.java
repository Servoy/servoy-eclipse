/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.ngclient.ui.utils.NGClientConstants;

/**
 * @author jcompagner
 *
 */
public class RunNPMCommand extends WorkspaceJob
{

	private final File projectFolder;
	private final String[] commands;
	private final File nodePath;
	private final File npmPath;
	private Job nextJob;
	private final String familyJob;
	private Process process;
	private static boolean ngBuildRunning;

	/**
	 * RunNPMCommand constructor
	 *
	 * @param familyJob
	 * @param nodePath
	 * @param npmPath
	 * @param projectFolder
	 * @param commands
	 */
	public RunNPMCommand(String familyJob, File nodePath, File npmPath, File projectFolder, String... commands)
	{
		super("Execute NPM command: " + Arrays.toString(commands));
		this.familyJob = familyJob;
		this.commands = commands;
		this.nodePath = nodePath;
		this.npmPath = npmPath;
		this.projectFolder = projectFolder;
	}

	/**
	 * RunNPMCommand constructor
	 *
	 * @param nodePath
	 * @param npmPath
	 * @param projectFolder
	 * @param commands
	 */
	public RunNPMCommand(File nodePath, File npmPath, File projectFolder, String... commands)
	{
		super("Execute NPM command: " + Arrays.toString(commands));
		this.commands = commands;
		this.nodePath = nodePath;
		this.npmPath = npmPath;
		this.projectFolder = projectFolder;
		this.familyJob = "";
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
	{
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(projectFolder);
		try
		{
			builder.redirectErrorStream(true);
			for (String command : commands)
			{
				if (command.equals(NGClientConstants.NG_BUILD_COMMAND)) // the command that runs the NG build
				{
					ngBuildRunning = true;
				}
				List<String> lst = new ArrayList<>();
				lst.add(nodePath.getCanonicalPath());
				lst.add(npmPath.getCanonicalPath());
				lst.addAll(Arrays.asList(command.split(" ")));
				lst.add("--scripts-prepend-node-path");
				builder.command(lst);
				process = builder.start();
				try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream())))
				{
					String str = null;
					while ((str = br.readLine()) != null)
					{
						str = str.replaceAll(".*?m", "");
						str = str.replaceAll("\b", "");
						System.err.println(str.trim());
						// The date, hash and time represents the last output line of the NG build process.
						// The NG build is finished when this conditions is met.
						if (str.trim().contains("Date:") && str.trim().contains("Hash:") && str.trim().contains("Time:"))
						{
							ngBuildRunning = false;
						}
					}
				}
				process.waitFor();
			}
			if (nextJob != null) nextJob.schedule();
		}
		catch (Exception e)
		{
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	/**
	 * @param buildCommand
	 */
	public void setNextJob(Job nextJob)
	{
		this.nextJob = nextJob;
	}

	@Override
	protected void canceling()
	{
		if (process != null) process.destroy();
	}

	/**
	 * @param family the job family
	 */
	@Override
	public boolean belongsTo(Object family)
	{
		return this.familyJob.equals(family);
	}

	/**
	 * This method checks if the NG build is running or not.
	 * @return true if the NG build is running, otherwise false
	 */
	public static boolean isNGBuildRunning()
	{
		return ngBuildRunning;
	}
}
