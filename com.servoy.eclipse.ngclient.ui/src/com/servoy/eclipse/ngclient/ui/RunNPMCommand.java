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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * @author jcompagner
 *
 */
public class RunNPMCommand extends WorkspaceJob
{

	private final File projectNodeFolder;
	private final String[] commands;
	private final File npmPath;
	private Job nextJob;

	/**
	 * @param name
	 */
	public RunNPMCommand(File npmPath, File projectNodeFolder, String... commands)
	{
		super("Execute NPM command: " + Arrays.toString(commands));
		this.commands = commands;
		this.npmPath = npmPath;
		this.projectNodeFolder = projectNodeFolder;
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
	{
		ProcessBuilder builder = new ProcessBuilder();
		builder.directory(projectNodeFolder);
		try
		{
			builder.redirectErrorStream(true);
			for (String command : commands)
			{
				List<String> lst = new ArrayList<>();
				lst.add(npmPath.toString());
				lst.addAll(Arrays.asList(command.split(" ")));
				builder.command(lst);
				Process process = builder.start();
				InputStream inputStream = process.getInputStream();
				byte[] bytes = new byte[512];
				int read = inputStream.read(bytes);
				while (read != -1)
				{
					System.err.println(new String(bytes, 0, read));
					read = inputStream.read(bytes);
				}
				inputStream.close();
				int exitValue = process.waitFor();
				System.err.println(exitValue);
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

}
