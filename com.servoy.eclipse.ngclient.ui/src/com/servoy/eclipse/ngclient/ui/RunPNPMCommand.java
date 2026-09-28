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
import java.nio.file.FileStore;
import java.nio.file.Files;
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
 * A pnpm-aware {@link IRunNPMCommand}, structurally based on {@link RunNPMCommand}. It runs the bundled
 * pnpm binary (a single executable) instead of <code>node npm-cli.js</code>, prepending the bundled Node
 * directory to <code>PATH</code> so <code>pnpm run build</code> finds the bundled Node. The npm verbs Servoy
 * uses are translated to their pnpm equivalents; <code>dedup</code> is a no-op because pnpm's global store +
 * symlinked node_modules already deduplicate.
 *
 * @author jcompagner
 *
 */
public class RunPNPMCommand extends WorkspaceJob implements IRunNPMCommand
{

	/** this is the value returned by {@link #getExitCode()} in case the job was cancelled... */
	public static final int EXIT_CODE_CANCELLED = -2;

	private final File projectFolder;
	private final List<String> commandArguments;
	private final File pnpmPath;
	private final File nodePath;
	private Job nextJob;
	private final String familyJob;
	private Process process;
	private Thread workerThread;
	private boolean stillReadingOutput;
	private static boolean ngBuildRunning;
	private int exitCode = -1;

	private final ReentrantLock processLock = new ReentrantLock();

	private Map<String, String> extraEnvironment = Map.of();

	private StringOutputStream customOutputStream;

	@Override
	public void setOutputStream(StringOutputStream outputStream)
	{
		this.customOutputStream = outputStream;
	}

	@Override
	public void setExtraEnvironment(Map<String, String> extra)
	{
		this.extraEnvironment = Map.copyOf(extra);
	}

	public RunPNPMCommand(String familyJob, File pnpmPath, File nodePath, File projectFolder, List<String> commands)
	{
		super("Executing PNPM command: " + RunNPMCommand.commandArgsToString(commands));
		this.familyJob = familyJob;
		this.commandArguments = commands;
		this.pnpmPath = pnpmPath;
		this.nodePath = nodePath;
		this.projectFolder = projectFolder;
	}

	public RunPNPMCommand(File pnpmPath, File nodePath, File projectFolder, List<String> commands)
	{
		super("Executing PNPM command: " + RunNPMCommand.commandArgsToString(commands) + ". (for more info open 'NG Build Console' in 'Console' view)");
		this.commandArguments = commands;
		this.pnpmPath = pnpmPath;
		this.nodePath = nodePath;
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
		StringOutputStream console = customOutputStream != null ? customOutputStream : Activator.getInstance().getConsole().outputStream();

		if (monitor.isCanceled())
		{
			writeConsole(console, "Cancel was requested; skipping command\n'" + RunNPMCommand.commandArgsToString(commandArguments) + "'\n");
			exitCode = EXIT_CODE_CANCELLED;
			return;
		}

		List<String> pnpmArguments = translateToPnpmArguments(commandArguments);
		if (pnpmArguments == null)
		{
			// dedup is a no-op in pnpm mode: pnpm's global store + symlinked node_modules already deduplicate.
			writeConsole(console, "\n---- Skipping '" + RunNPMCommand.commandArgsToString(commandArguments) + "' in pnpm mode (no-op).");
			exitCode = 0;
			return;
		}

		// cancel button of 'monitor' should be able to stop long-running pnpm commands (and the job reading in a blocking manner from it's console output
		// can't handle it directly by checking cancellation if pnpm itself hangs);
		// as runCommands is not always called from this class running as an actual job but just a direct call from a different job, monitor might be that
		// of another job and we need to cancel on it as well so the overridden "canceling" method of this class is not enough
		workerThread = Thread.currentThread();
		final boolean[] cancelThreadDone = new boolean[] { false };
		Thread cancelThread = new Thread(() -> {
			while (!cancelThreadDone[0])
			{
				if (monitor.isCanceled())
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
			// prepend the bundled Node directory (and the pnpm directory if it is separate) to PATH so 'pnpm run build' finds the bundled Node
			String prependPaths = nodePath.getParent();
			if (pnpmPath.getParent() != null && !pnpmPath.getParent().equals(nodePath.getParent()))
			{
				prependPaths = pnpmPath.getParent() + System.getProperty("path.separator") + prependPaths;
			}
			path = prependPaths + System.getProperty("path.separator") + path;
			environment.put(pathkey, path);
			environment.put("NODE_OPTIONS", "--max-old-space-size=4096");
			environment.put("NG_PERSISTENT_BUILD_CACHE", "1");
			if (!extraEnvironment.isEmpty())
			{
				environment.putAll(extraEnvironment);
			}
			builder.directory(projectFolder);
			builder.redirectErrorStream(true);
			if (commandArguments == NGClientConstants.NG_BUILD_COMMAND) // the command that runs the NG build
			{
				ngBuildRunning = true;
			}

			long time = System.currentTimeMillis();
			List<String> allCmdLineArgs = new ArrayList<>();
			allCmdLineArgs.add(pnpmPath.getCanonicalPath());
			allCmdLineArgs.addAll(pnpmArguments);
			// SVY-21456 §3.7.2: hardlinks cannot cross volumes. pnpm's default store lives under the user home
			// (on Windows always C:). When the target folder is on a different drive/volume, pnpm would silently
			// copy every file instead of hardlinking. Point the store at a generic per-drive store on the target's
			// own volume so hardlinks work and are shared across all workspaces on that drive. When target and
			// default store are on the same volume (the common case, e.g. a C: workspace), this returns null and
			// pnpm keeps its default store.
			String storeDirArg = computeStoreDirArgIfCrossVolume(projectFolder);
			if (storeDirArg != null)
			{
				allCmdLineArgs.add(storeDirArg);
			}
			writeConsole(console, "\n---- Running pnpm command:\n" + RunNPMCommand.commandArgsToString(allCmdLineArgs));
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
					"Finished running '" + RunNPMCommand.commandArgsToString(commandArguments) + "' time: " +
						Math.round((System.currentTimeMillis() - time) / 1000) + "s\n");
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
	 * Computes a <code>--config.store-dir=&lt;path&gt;</code> argument when the target folder is on a different
	 * drive/volume than pnpm's default content-addressable store, otherwise <code>null</code> (SVY-21456 §3.7.2).
	 * <p>
	 * pnpm hardlinks package files from its global store into <code>node_modules</code>. Hardlinks cannot cross
	 * volume boundaries: on Windows the default store is under the user home (always the <code>C:</code> drive), so
	 * a workspace/target on another drive (e.g. <code>D:\servoy_workspaces\workspace1</code>) would make pnpm copy
	 * every file instead of hardlinking - slower and no dedup. To avoid that we point the store at a generic,
	 * per-drive location on the target's own volume (<code>&lt;drive&gt;\.pnpm-store</code> on Windows,
	 * <code>&lt;mount&gt;/.pnpm-store</code> elsewhere) that is shared by every workspace on that drive so dedup
	 * still works across them. When the target and the default store already share a volume (the common case, e.g.
	 * a <code>C:</code> workspace) this returns <code>null</code> and pnpm keeps its default store.
	 * <p>
	 * This is primarily a Windows concern (separate drive letters = separate volumes); on macOS/Linux everything is
	 * usually on one volume, so the volume comparison simply matches and no override is emitted.
	 */
	private static String computeStoreDirArgIfCrossVolume(File targetFolder)
	{
		try
		{
			File defaultStore = getDefaultPnpmStoreDir();
			if (targetFolder == null || defaultStore == null) return null;

			// Decide whether an override is needed by comparing the actual FILESYSTEM VOLUMES, not drive letters.
			// A 'subst' drive (or a symlink) is an alias to a directory on another volume; its drive letter differs
			// but Files.getFileStore() sees through the alias to the real volume, so hardlinks between them DO work
			// and no override is needed. Only a genuinely different volume needs a per-drive store.
			FileStore targetStore = getFileStoreOfNearestExisting(targetFolder);
			FileStore storeStore = getFileStoreOfNearestExisting(defaultStore);
			if (targetStore != null && targetStore.equals(storeStore))
			{
				return null; // same volume (or subst alias to it): pnpm's default store hardlinks fine
			}

			// Genuinely different volumes: use a generic per-drive store on the target's own volume, shared by all
			// workspaces on that drive so dedup still works across them.
			File targetRoot = getVolumeRoot(targetFolder);
			if (targetRoot == null) return null;
			File perDriveStore = new File(targetRoot, ".pnpm-store");
			return "--config.store-dir=" + perDriveStore.getAbsolutePath();
		}
		catch (RuntimeException e)
		{
			// never let store-dir detection break a build; fall back to pnpm's default store
			return null;
		}
	}

	/**
	 * @return the {@link FileStore} (filesystem volume) that {@code file} - or its nearest existing ancestor - lives
	 *         on, or {@code null} if it cannot be determined. Walking up to an existing ancestor is needed because
	 *         the target folder may not exist yet at the time of the first command.
	 */
	private static FileStore getFileStoreOfNearestExisting(File file)
	{
		File probe = file.getAbsoluteFile();
		while (probe != null && !probe.exists())
		{
			probe = probe.getParentFile();
		}
		if (probe == null) return null;
		try
		{
			return Files.getFileStore(probe.toPath());
		}
		catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * @return the filesystem root that {@code file} lives on (the drive root on Windows, e.g. {@code D:\}; the
	 *         top-level mount root elsewhere), or {@code null} if it cannot be determined.
	 */
	private static File getVolumeRoot(File file)
	{
		if (file == null) return null;
		File abs = file.getAbsoluteFile();
		if (Platform.getOS().equals(Platform.OS_WIN32))
		{
			// on Windows the volume is identified by the drive letter root (e.g. C:\, D:\)
			java.nio.file.Path root = abs.toPath().getRoot();
			return root != null ? root.toFile() : null;
		}
		// on macOS/Linux, walk up to the highest existing ancestor (closest to the mount root)
		File root = abs;
		while (root.getParentFile() != null)
		{
			root = root.getParentFile();
		}
		return root;
	}

	/**
	 * @return pnpm's default global store directory (<code>~/AppData/Local/pnpm/store</code> on Windows,
	 *         <code>~/.local/share/pnpm/store</code> / <code>~/Library/pnpm/store</code> elsewhere), used only to
	 *         determine which volume the default store is on. Returns <code>null</code> if the user home is unknown.
	 */
	private static File getDefaultPnpmStoreDir()
	{
		String userHome = System.getProperty("user.home");
		if (userHome == null) return null;
		File home = new File(userHome);
		if (Platform.getOS().equals(Platform.OS_WIN32))
		{
			return new File(home, "AppData/Local/pnpm/store");
		}
		// exact sub-path differs per OS, but for a volume check any path under the user home is sufficient
		return home;
	}

	/**
	 * Translates the npm verbs Servoy uses to their pnpm equivalents.
	 *
	 * @return the translated pnpm arguments, or <code>null</code> if the command is a no-op in pnpm mode (dedup).
	 */
	private static List<String> translateToPnpmArguments(List<String> commands)
	{
		if (commands == null || commands.isEmpty()) return new ArrayList<>();

		String verb = commands.get(0);
		List<String> rest = commands.subList(1, commands.size());
		List<String> result = new ArrayList<>();
		switch (verb)
		{
			case "install" :
				// 'install' with no package args installs all deps; 'install <pkg...> <path>' adds packages.
				// pnpm splits these: 'pnpm install' vs 'pnpm add <pkg...>'.
				if (rest.isEmpty())
				{
					result.add("install");
				}
				else
				{
					result.add("add");
					result.addAll(rest);
				}
				addUnblockDepBuildsFlag(result);
				break;
			case "ci" :
				result.add("install");
				result.add("--frozen-lockfile");
				addUnblockDepBuildsFlag(result);
				break;
			case "update" :
				result.add("update");
				result.addAll(rest);
				addUnblockDepBuildsFlag(result);
				break;
			case "uninstall" :
				result.add("remove");
				result.addAll(rest);
				addUnblockDepBuildsFlag(result);
				break;
			case "run" :
			case "run-script" :
				result.add("run");
				result.addAll(rest);
				break;
			case "dedup" :
				// no-op in pnpm mode
				return null;
			default :
				result.add(verb);
				result.addAll(rest);
				break;
		}
		return result;
	}

	/**
	 * QUICK UNBLOCK (SVY-21456): pnpm 11+ turns the "Ignored build scripts" warning into a hard exit-1 failure
	 * (ERR_PNPM_IGNORED_BUILDS) via strictDepBuilds when a dependency ships an install/postinstall build script that
	 * is not on pnpm's onlyBuiltDependencies allow-list. npm never failed on this. Until proper per-solution
	 * approval of dependency build scripts is designed (Developer UI prompt + persisted in the solution so the
	 * headless WAR export can build unattended - tracked as a separate case), disable the strict check on the
	 * package-mutating verbs so ignored build scripts stay a warning, matching npm's previous behaviour.
	 */
	private static void addUnblockDepBuildsFlag(List<String> result)
	{
		result.add("--config.strictDepBuilds=false");
	}

	/**
	 * @return -1 if the command has not yet finished running; EXIT_CODE_CANCELLED if it was cancelled by the user;
	 *         otherwise the EXIT_CODE of the command that has been run by this job.
	 */
	public int getExitCode()
	{
		return exitCode;
	}

	/**
	 * Returns the underlying OS {@link Process}, or {@code null} if the process has not started yet or has already
	 * been cleaned up by {@link #canceling()}.
	 * <p>
	 * Callers may use this to kill the process tree directly (e.g. during Eclipse shutdown) without going through
	 * the normal {@link #canceling()} path.
	 * </p>
	 */
	public Process getProcess()
	{
		return process;
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
				StringOutputStream console = customOutputStream != null ? customOutputStream : Activator.getInstance().getConsole().outputStream();

				writeConsole(console, "Cancel requested by user... Trying to stop process...");
				process.destroy();
				exitCode = EXIT_CODE_CANCELLED;

				try
				{
					int t = 10;
					while (t-- > 0 && isActuallyRunningProcess())
					{
						if (t == 8) writeConsole(console, "Waiting 10 sec for PNPM to stop...");
						Thread.sleep(1000);
					}
				}
				catch (InterruptedException e)
				{
				}

				if (isActuallyRunningProcess())
				{
					writeConsole(console, "PNPM did not stop nicely in 10 seconds... Trying to stop it forcibly...");
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
		// somehow pnpm can make it so that process.isAlive() is false, process.exitValue() is 1 after a
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

}
