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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Holds the server-ready coordination state for the embedded opencode server.
 * <p>
 * Extracted from {@link Activator} so that the latch/port logic can be
 * unit-tested
 * independently of the OSGi plugin lifecycle.
 * </p>
 * <p>
 * A new instance is created each time the {@link Activator} starts, giving each
 * Eclipse session a fresh latch. Tests create their own instances directly.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
class OpencodeServerState {
	private final CountDownLatch serverReady = new CountDownLatch(1);
	private volatile int serverPort;

	OpencodeServerState(int defaultPort) {
		this.serverPort = defaultPort;
	}

	/**
	 * Called by {@link RunOpencodeCommand} (or its watchdog) once the server is
	 * accepting HTTP connections. Idempotent â?? subsequent calls are no-ops for
	 * the
	 * latch but will update {@code serverPort} if called again (guarded externally
	 * by
	 * {@link #isReady()} checks).
	 */
	void serverStarted(int port) {
		this.serverPort = port;
		serverReady.countDown();
	}

	/**
	 * Blocks the calling thread until the server is ready or the timeout elapses.
	 *
	 * @param timeoutMs maximum wait in milliseconds
	 * @return {@code true} if the server became ready within the timeout
	 */
	boolean waitForServer(long timeoutMs) throws InterruptedException {
		return serverReady.await(timeoutMs, TimeUnit.MILLISECONDS);
	}

	/** @return the port the server is (or will be) listening on */
	int getServerPort() {
		return serverPort;
	}

	/** @return {@code true} once {@link #serverStarted(int)} has been called */
	boolean isReady() {
		return serverReady.getCount() == 0;
	}
}
