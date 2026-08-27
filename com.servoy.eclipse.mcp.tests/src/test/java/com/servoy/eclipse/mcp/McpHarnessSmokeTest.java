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
package com.servoy.eclipse.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Proves the harness itself works, and nothing else.
 *
 * <p>It exists because "no tests ran" and "every test failed" look the same from outside, and the
 * difference decides where to look. This class touches no solution, no server and no workspace: if
 * it reports a result, the launch, the runner and the test bundle are sound and the fault is in the
 * tests; if it reports nothing, the fault is in the launch.</p>
 *
 * <p>Worth keeping once things work - it is the first thing to run when a build agent starts
 * reporting an empty suite.</p>
 *
 * @author Servoy
 */
public class McpHarnessSmokeTest
{
	@Test
	public void theHarnessRunsATest()
	{
		assertEquals(2, 1 + 1);
	}

	@Test
	public void theWorkspaceHelperLoads()
	{
		// loading this class pulls in the workspace and model APIs; if any of them is missing from the
		// launch, the failure happens here rather than somewhere less obvious
		assertEquals("mcp_sample", McpTestWorkspace.SOLUTION);
	}

	@Test
	public void theWorkspaceCanBePrepared() throws Exception
	{
		// what every integration test does first, on its own, so that a failure in it is reported as
		// a failure rather than as an empty test run
		McpTestWorkspace.prepare();
	}

	@Test
	public void theTestBundleCanSeeTheCodeUnderTest()
	{
		// the one dependency worth checking here: that servoy_mcp is actually wired to this bundle
		assertNotNull(com.servoy.mcp.McpRuntime.WEBSERVICE_NAME);
		assertEquals("mcp", com.servoy.mcp.McpRuntime.WEBSERVICE_NAME);
	}
}
