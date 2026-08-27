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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Everything that needs a running developer.
 *
 * <p>Run with <code>mvn verify -Pintegration</code>, which starts one, builds the workspace from
 * <code>testresources</code> and stops it again. The unit tests live elsewhere, in
 * <code>servoy_mcp_tests</code>, and need none of this.</p>
 *
 * <p>The HTTP test comes first on purpose: if the bundle did not resolve or the endpoint was never
 * registered, everything after it would fail for the same reason, and it is better to read that
 * once at the top than fifteen times.</p>
 *
 * @author Servoy
 */
@RunWith(Suite.class)
@SuiteClasses({ McpHttpIntegrationTest.class, McpServletIntegrationTest.class, McpInvalidationIntegrationTest.class })
public class AllMcpIntegrationTests
{
}
