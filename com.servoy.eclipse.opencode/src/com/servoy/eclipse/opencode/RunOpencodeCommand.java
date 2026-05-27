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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.ui.RunNPMCommand;

/**
 * Long-running Eclipse Job that starts the opencode server via
 * {@code npm exec}.
 * <p>
 * Before launching, {@link #findFreePort(int)} scans upward from
 * {@link #DEFAULT_PORT} to find the first TCP port that is not already bound.
 * This handles the common case of a second Eclipse instance (or any other
 * process) already occupying the default port.
 * </p>
 * <p>
 * Delegates all node/npm path setup to
 * {@code com.servoy.eclipse.ngclient.ui.Activator}
 * via
 * {@link com.servoy.eclipse.ngclient.ui.Activator#createNPMCommand(File, List)}.
 * No direct knowledge of where node is extracted is needed here.
 * </p>
 * <p>
 * The job blocks until the opencode process exits. On unexpected exit it
 * reschedules itself up to {@link #MAX_RETRIES} times. A daemon watchdog thread
 * polls the server HTTP endpoint and calls {@link Activator#serverStarted(int)}
 * once it responds.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class RunOpencodeCommand extends Job {
	static final int DEFAULT_PORT = 4096;

	private static final int MAX_RETRIES = 3;
	private static final long RETRY_DELAY_MS = 5_000;

	private final File opencodeDir;
	private final int retryCount;

	public RunOpencodeCommand(File opencodeDir) {
		this(opencodeDir, 0);
	}

	private RunOpencodeCommand(File opencodeDir, int retryCount) {
		super("Running Servoy AI server");
		this.opencodeDir = opencodeDir;
		this.retryCount = retryCount;
		setUser(false);
		setSystem(true);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		com.servoy.eclipse.ngclient.ui.Activator ngActivator = com.servoy.eclipse.ngclient.ui.Activator.getInstance();
		if (ngActivator == null) {
			ServoyLog.logError("OpenCode: ngclient.ui Activator not available - cannot start server.", null);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Node.js Activator not available");
		}

		// Find the first free port starting at DEFAULT_PORT so we don't clash
		// with a running opencode instance from another Eclipse session.
		int port = findFreePort(DEFAULT_PORT);
		if (port != DEFAULT_PORT) {
			ServoyLog.logInfo("OpenCode: port " + DEFAULT_PORT + " in use, using port " + port + " instead.");
		}

		// Use npm exec - ngclient.ui resolves node/npm paths and waits for
		// extraction internally.
		RunNPMCommand serverCommand = ngActivator.createNPMCommand(opencodeDir,
				List.of("exec", "--", "opencode", "serve",
						"--port", String.valueOf(port),
						"--hostname", "127.0.0.1"));

		Activator activator = Activator.getInstance();
		if (activator == null) {
			ServoyLog.logError("OpenCode: Activator not available - cannot start server.", null);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Activator not available");
		}

		// Register both the outer job (this) and the inner command so stopServer()
		// can cancel this job's monitor (preventing spurious retry) AND kill the OS process.
		activator.setServerJob(this);
		activator.setServerCommand(serverCommand);

		ServoyLog.logInfo("OpenCode: launching server on port " + port + ".");
		startWatchdogThread(port);

		try {
			serverCommand.runCommand(monitor);
		} catch (IOException | InterruptedException e) {
			if (!monitor.isCanceled()) {
				ServoyLog.logError("OpenCode: server process I/O error", e);
			}
		}

		// Clear the job registration now that this run is done.
		Activator current = Activator.getInstance();
		if (current != null) {
			current.setServerJob(null);
		}

		int exitCode = serverCommand.getExitCode();
		if (exitCode != 0 && exitCode != RunNPMCommand.EXIT_CODE_CANCELLED && !monitor.isCanceled()) {
			if (retryCount < MAX_RETRIES) {
				ServoyLog.logInfo("OpenCode: unexpected exit (code " + exitCode + ") - scheduling retry " +
						(retryCount + 1) + "/" + MAX_RETRIES + " in 5 s.");
				new RunOpencodeCommand(opencodeDir, retryCount + 1).schedule(RETRY_DELAY_MS);
			} else {
				ServoyLog.logError("OpenCode: server exited unexpectedly (code " + exitCode + ") - no more retries.", null);
			}
		}

		return Status.OK_STATUS;
	}

	/**
	 * Polls {@code http://127.0.0.1:<port>/} every 500 ms (up to 120 s).
	 * Calls {@link Activator#serverStarted(int)} on the first successful HTTP
	 * response.
	 *
	 * @param port the port opencode was launched on
	 */
	private void startWatchdogThread(int port) {
		Thread watchdog = new Thread(() -> {
			for (int i = 0; i < 240; i++) {
				Activator activator = Activator.getInstance();
				if (activator == null || activator.isServerReady())
					return;
				try {
					Thread.sleep(500);
					URL url = new URL("http://127.0.0.1:" + port + "/");
					HttpURLConnection conn = (HttpURLConnection) url.openConnection();
					conn.setConnectTimeout(400);
					conn.setReadTimeout(400);
					if (conn.getResponseCode() > 0) {
						activator = Activator.getInstance();
						if (activator != null && !activator.isServerReady()) {
							activator.serverStarted(port);
						}
						return;
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				} catch (IOException ignored) {
					// server not yet ready - keep polling
				}
			}
			ServoyLog.logError("OpenCode: watchdog timed out - server did not respond on port " + port + " within 120 s.", null);
		}, "opencode-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	}

	/**
	 * Finds the first available TCP port starting at {@code startPort}, scanning
	 * upward up to 100 ports.
	 * <p>
	 * Uses a {@link ServerSocket} bind-test: if the bind succeeds the port is
	 * free; the socket is closed immediately so opencode can use it.
	 * </p>
	 *
	 * @param startPort the preferred port (normally {@link #DEFAULT_PORT})
	 * @return the first free port found, or {@code startPort} as a last resort
	 */
	static int findFreePort(int startPort) {
		for (int port = startPort; port < startPort + 100; port++) {
			try (ServerSocket ss = new ServerSocket(port)) {
				ss.setReuseAddress(true);
				return port;
			} catch (IOException e) {
				// port occupied, try next
			}
		}
		return startPort;
	}
}
