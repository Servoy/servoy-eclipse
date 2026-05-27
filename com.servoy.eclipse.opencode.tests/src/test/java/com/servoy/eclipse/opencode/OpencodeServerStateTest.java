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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for {@link OpencodeServerState}.
 * <p>
 * No OSGi runtime is required — each test creates its own fresh
 * {@link OpencodeServerState} instance.
 * </p>
 *
 * @author jcompagner
 * @since 2026.06
 */
public class OpencodeServerStateTest
{
	@Test
	public void initiallyNotReady()
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		assertFalse("Server should not be ready before serverStarted() is called", state.isReady());
		assertEquals("Default port should be set from constructor", 4096, state.getServerPort());
	}

	@Test
	public void serverStarted_signalsReadyAndSetsPort()
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		state.serverStarted(4097);
		assertTrue("Server should be ready after serverStarted()", state.isReady());
		assertEquals("Port should be updated by serverStarted()", 4097, state.getServerPort());
	}

	@Test
	public void waitForServer_returnsTrueWhenAlreadyReady() throws Exception
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		state.serverStarted(4096);
		assertTrue("waitForServer should return true immediately when already ready",
			state.waitForServer(100));
	}

	@Test
	public void waitForServer_timesOutWhenNotStarted() throws Exception
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		assertFalse("waitForServer should return false when server never starts within timeout",
			state.waitForServer(80));
	}

	@Test
	public void waitForServer_returnsTrueWhenStartedConcurrently() throws Exception
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		Thread starter = new Thread(() -> {
			try
			{
				Thread.sleep(100);
				state.serverStarted(4096);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		});
		starter.setDaemon(true);
		starter.start();
		assertTrue("waitForServer should return true once a concurrent serverStarted() fires",
			state.waitForServer(2000));
	}

	@Test
	public void serverStarted_multipleCallsAreSafe()
	{
		OpencodeServerState state = new OpencodeServerState(4096);
		state.serverStarted(4096);
		state.serverStarted(4096); // second call — latch already at 0, no exception
		assertTrue("State should still be ready after redundant serverStarted() call", state.isReady());
	}
}
