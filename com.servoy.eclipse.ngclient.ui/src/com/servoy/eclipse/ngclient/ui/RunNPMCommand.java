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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.ngclient.ui.utils.NGClientConstants;

/**
 * @author jcompagner
 *
 */
public class RunNPMCommand extends WorkspaceJob
{

	/** this is the value returned by {@link #getExitCode()} in case the job was cancelled... */
	public static final int EXIT_CODE_CANCELLED = -2;

	private final File projectFolder;
	private final List<String> commandArguments;
	private final File nodePath;
	private final File npmPath;
	private Job nextJob;
	private final String familyJob;
	private Process process;
	private Thread workerThread;
	private boolean stillReadingOutput;
	private static boolean ngBuildRunning;
	private int exitCode = -1;

	private final ReentrantLock processLock = new ReentrantLock();

	public RunNPMCommand(String familyJob, File nodePath, File npmPath, File projectFolder, List<String> commands)
	{
		super("Executing NPM command: " + commandArgsToString(commands));
		this.familyJob = familyJob;
		this.commandArguments = commands;
		this.nodePath = nodePath;
		this.npmPath = npmPath;
		this.projectFolder = projectFolder;
	}

	public RunNPMCommand(File nodePath, File npmPath, File projectFolder, List<String> commands)
	{
		super("Executing NPM command: " + commandArgsToString(commands) + ". (for more info open 'NG Build Console' in 'Console' view)");
		this.commandArguments = commands;
		this.nodePath = nodePath;
		this.npmPath = npmPath;
		this.projectFolder = projectFolder;
		this.familyJob = "";
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
	{
		try
		{
			runCommand(monitor);
			if (nextJob != null) nextJob.schedule();
		}
		catch (Exception e)
		{
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	public void runCommand(IProgressMonitor monitor) throws IOException, InterruptedException
	{
		StringOutputStream console = Activator.getInstance().getConsole().outputStream();

		if (monitor.isCanceled())
		{
			writeConsole(console, "Cancel was requested; skipping command\n'" + commandArgsToString(commandArguments) + "'\n");
			exitCode = EXIT_CODE_CANCELLED;
			return;
		}

		// cancel button of 'monitor' should be able to stop long-running npm commands (and the job reading in a blocking manner from it's console output
		// can't handle it directly by checking cancellation if npm itself hangs);
		// as runCommands is not always called from this class running as an actual job but just a direct call from a different job, monitor might be that
		// of another job and we need to cancel on it as well so the overridden "canceling" method of this class is not enough
		workerThread = Thread.currentThread();
		final boolean[] cancelThreadDone = new boolean[] { false };
		Thread cancelThread = new Thread(() -> {
			while (!cancelThreadDone[0])
			{
				if (monitor.isCanceled()) // TODO add here also automatically canceling if console output of the running npm process didn't generate any output in a long time?! maybe followed by an automatic re-run? (it happened that npm just stalls)
				{
					cancelThreadDone[0] = true;
					canceling();
				}
				else try
				{
					Thread.sleep(300);
				}
				catch (InterruptedException e)
				{
				}
			}
		});

		try
		{
			ProcessBuilder builder = new ProcessBuilder();
			Map<String, String> environment = builder.environment();
			String pathkey = Platform.getOS().equals(Platform.OS_WIN32) ? "Path" : "PATH";
			String path = environment.get(pathkey);
			path = nodePath.getParent() + System.getProperty("path.separator") + path;
			environment.put(pathkey, path);
			environment.put("NODE_OPTIONS", "--max-old-space-size=4096");
			environment.put("NG_PERSISTENT_BUILD_CACHE", "1");
			builder.directory(projectFolder);
			builder.redirectErrorStream(true);
			if (commandArguments == NGClientConstants.NG_BUILD_COMMAND) // the command that runs the NG build
			{
				ngBuildRunning = true;
			}

			long time = System.currentTimeMillis();
			List<String> allCmdLineArgs = new ArrayList<>();
			allCmdLineArgs.add(nodePath.getCanonicalPath());
			allCmdLineArgs.add(npmPath.getCanonicalPath());
			allCmdLineArgs.addAll(commandArguments);
//			allCmdLineArgs.add("--scripts-prepend-node-path");
			writeConsole(console, "\n---- Running npm command:\n" + commandArgsToString(allCmdLineArgs));
			writeConsole(console, "In dir: " + projectFolder);
			builder.command(allCmdLineArgs);
			BufferedReader br;
			try
			{
				processLock.lock();
				process = builder.start();
				cancelThread.start();
				br = new BufferedReader(new InputStreamReader(process.getInputStream()));
			}
			finally
			{
				processLock.unlock();
			}
			stillReadingOutput = true;
			try
			{
				String str = null;
				while ((str = br.readLine()) != null)
				{
//						str = str.replaceAll(".*?m", "");
//						str = str.replaceAll("\b", "");
					writeConsole(console, str.trim());
					// The date, hash and time represents the last output line of the NG build process.
					// The NG build is finished when this conditions is met.
					if (str.trim().contains("Date:") && str.trim().contains("Hash:") && str.trim().contains("Time:"))
					{
						ngBuildRunning = false;
					}
				}
			}
			finally
			{
				stillReadingOutput = false;
				if (br != null) br.close();
			}
			try
			{
				processLock.lock();
				if (process != null)
				{
					process.waitFor(); // process can be set to null if canceling method was called meanwhile
					exitCode = process.exitValue();
					if (exitCode != 0) writeConsole(console, "EXIT_CODE was NOT zero but: " + exitCode);
				}
				writeConsole(console,
					"Finished running '" + commandArgsToString(commandArguments) + "' time: " + Math.round((System.currentTimeMillis() - time) / 1000) + "s\n");
			}
			catch (InterruptedException e)
			{
				if (monitor.isCanceled())
				{
					exitCode = EXIT_CODE_CANCELLED;
					writeConsole(console, "Process interrupted; operation was cancelled by the user!\n");
				}
				else throw e;
			}
		}
		finally
		{
			cancelThreadDone[0] = true;
			console.close();
		}
	}

	/**
	 * @return -1 if the command has not yet finished running; EXIT_CODE_CANCELLED if it was cancelled by the user; otherwise the EXIT_CODE of the command that has been run by this job.
	 */
	public int getExitCode()
	{
		return exitCode;
	}

	private void writeConsole(StringOutputStream console, String message)
	{
		try
		{
			console.write(message + "\n");
		}
		catch (IOException e2)
		{
		}
	}

	public void setNextJob(Job nextJob)
	{
		this.nextJob = nextJob;
	}

	@Override
	protected void canceling()
	{
		processLock.lock();
		try
		{
			if (process != null)
			{
				StringOutputStream console = Activator.getInstance().getConsole().outputStream();

				writeConsole(console, "Cancel requested by user... Trying to stop process...");
//			workerThread.interrupt(); // to get out of sync-reading console output in runCommands; actually don't know if that would work as the .read method of input stream only throws IOException; so I don't know if the actual native impl. of FileInputStream that is used here checks for thread interrupt status
				process.destroy();
				exitCode = EXIT_CODE_CANCELLED;

				try
				{
					int t = 10;
					while (t-- > 0 && isActuallyRunningProcess())
					{
						if (t == 8) writeConsole(console, "Waiting 10 sec for NPM to stop...");
						Thread.sleep(1000);
					}
				}
				catch (InterruptedException e)
				{
				}

				if (isActuallyRunningProcess())
				{
					writeConsole(console, "NPM did not stop nicely in 10 seconds... Trying to stop it forcibly...");
					process.destroyForcibly();
				}

				process = null;
				workerThread = null;
			}
		}
		finally
		{
			processLock.unlock();
		}
	}

	private boolean isActuallyRunningProcess()
	{
		// somehow npm can make it so that process.isAlive() is false, process.exitValue() is 1 after a
		// call to process.destroy(); but the inputStream of the process is still blocking and not closing for a few minutes...
		processLock.lock();
		try
		{
			return process.isAlive() || stillReadingOutput;
		}
		finally
		{
			processLock.unlock();
		}
	}

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

	public static String commandArgsToString(List<String> command)
	{
		return command.stream().reduce("", (a, b) -> a + (b.contains(" ") ? "\"" + b + "\"" : b) + (b.length() > 20 ? "\n" : " "));
	}

}
