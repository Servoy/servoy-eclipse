/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.opencode;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.ngclient.ui.RunNPMCommand;

/**
 * Plugin activator for {@code com.servoy.eclipse.opencode}.
 * <p>
 * On startup, schedules {@link OpencodeFolderCreatorJob} (unless the {@code opencode.url} system property is set,
 * which means an external server is used). Holds a reference to the running {@link RunOpencodeCommand} job and the
 * inner {@link RunNPMCommand} so both can be cancelled cleanly on shutdown.
 * </p>
 * <p>
 * Server-ready coordination state is delegated to {@link OpencodeServerState} so that the latch/port logic can be
 * unit-tested without an OSGi runtime.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class Activator extends Plugin
{
	public static final String PLUGIN_ID = "com.servoy.eclipse.opencode";

	private static Activator instance;

	/** The outer Eclipse Job that owns the server lifecycle (used to cancel its monitor on shutdown). */
	private volatile Job serverJob;

	/** The inner RunNPMCommand that wraps the OS process (used to kill the process tree on shutdown). */
	private volatile RunNPMCommand serverCommand;

	/** Holds the CountDownLatch and port - extracted for testability. */
	private final OpencodeServerState serverState = new OpencodeServerState(RunOpencodeCommand.DEFAULT_PORT);

	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		instance = this;

		// Setup is deferred until the user has both logged in and has an active solution.
		// OpenCodeView.initUrl() calls ensureServerStarting() when all conditions are met.
	}

	@Override
	public void stop(BundleContext context) throws Exception
	{
		stopServer();
		instance = null;
		super.stop(context);
	}

	public static Activator getInstance()
	{
		return instance;
	}

	// --- server lifecycle ---

	private volatile boolean setupStarted = false;

	/**
	 * Schedules {@link OpencodeFolderCreatorJob} the first time it is called (idempotent).
	 * Must be called only after login is complete and an active solution is present.
	 */
	void ensureServerStarting()
	{
		String urlOverride = System.getProperty(OpencodePerspective.URL_PROPERTY);
		if (urlOverride != null) return; // external server, nothing to do
		if (setupStarted) return;
		setupStarted = true;
		log(IStatus.INFO, "OpenCode: prerequisites met Ã¢ scheduling setup job."); //$NON-NLS-1$
		new OpencodeFolderCreatorJob().schedule();
	}

	/**
	 * Called by {@link RunOpencodeCommand} once the server is ready to accept connections.
	 */
	void serverStarted(int port)
	{
		log(IStatus.INFO, "OpenCode server ready on port " + port + ".");
		serverState.serverStarted(port);
	}

	/**
	 * Blocks until the opencode server is ready or the timeout elapses.
	 *
	 * @return {@code true} if the server started within the timeout
	 */
	public boolean waitForServer(long timeoutMs) throws InterruptedException
	{
		return serverState.waitForServer(timeoutMs);
	}

	public int getServerPort()
	{
		return serverState.getServerPort();
	}

	public boolean isServerReady()
	{
		return serverState.isReady();
	}

	/**
	 * Registers the outer {@link RunOpencodeCommand} job. Called by {@link RunOpencodeCommand#run} before the
	 * server process is launched so that {@link #stopServer()} can cancel the job's own monitor, which in turn
	 * makes {@code monitor.isCanceled()} return {@code true} in the retry-guard check.
	 */
	void setServerJob(Job job)
	{
		this.serverJob = job;
	}

	void setServerCommand(RunNPMCommand cmd)
	{
		this.serverCommand = cmd;
	}

	/**
	 * Stops the running opencode server process (called from {@link #stop}).
	 * <p>
	 * The outer {@link RunOpencodeCommand} job is cancelled <em>first</em> so its {@code monitor.isCanceled()}
	 * returns {@code true}. This ensures the retry guard in {@code run()} sees the cancellation and does not
	 * schedule another attempt after the process is killed.
	 * </p>
	 * <p>
	 * Then {@link RunNPMCommand#cancel()} is called to set that job's cancel flag, followed by
	 * {@link #killProcessTree} to kill the OS process directly. We bypass {@code canceling()} in
	 * {@code RunNPMCommand} because during Eclipse shutdown the ngclient.ui activator is already null, causing
	 * {@code canceling()} to NPE before ever touching the process, which leaves the {@code readLine()} loop
	 * blocked.
	 * </p>
	 */
	public void stopServer()
	{
		// Cancel the outer job first so monitor.isCanceled() == true in RunOpencodeCommand.run()
		Job job = serverJob;
		if (job != null)
		{
			job.cancel();
			serverJob = null;
		}

		RunNPMCommand cmd = serverCommand;
		if (cmd != null)
		{
			cmd.cancel();
			killProcessTree(cmd);
			serverCommand = null;
		}
	}

	/**
	 * Kills the OS process (and its entire descendant tree) wrapped inside {@code cmd}.
	 * <p>
	 * Descendants are destroyed first (so their stdout handles are closed), then the root process. This ensures the
	 * {@code readLine()} loop in {@code RunNPMCommand.runCommand()} unblocks promptly on all platforms.
	 * </p>
	 */
	private void killProcessTree(RunNPMCommand cmd)
	{
		Process process = cmd.getProcess();
		if (process == null) return;

		process.descendants().forEach(ProcessHandle::destroyForcibly);
		process.destroyForcibly();
	}

	// --- logging helpers ---

	/**
	 * Writes directly to this plugin's log (i.e. {@code .metadata/.log}) using our own bundle ID. Safe to call
	 * during {@link #stop} because the platform log outlives individual plugin activators.
	 */
	private void log(int severity, String message)
	{
		ILog log = getLog();
		if (log != null)
		{
			log.log(new Status(severity, PLUGIN_ID, message));
		}
	}
}
