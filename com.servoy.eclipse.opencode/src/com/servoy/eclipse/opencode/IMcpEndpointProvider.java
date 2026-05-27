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

import java.util.List;

/**
 * Extension point interface for
 * {@code com.servoy.eclipse.opencode.mcpEndpoint}.
 * <p>
 * Implementors return the list of MCP endpoint URLs served by the local Tomcat
 * MCP bridge, plus the auth token required to call those endpoints. The
 * framework
 * rewrites each URL to use {@code {env:MCP_PORT}} in place of the actual port
 * and
 * merges the entries into {@code opencode.json}.
 * </p>
 *
 * <h4>URL contract</h4>
 * Each URL must be of the form {@code http://localhost:<port>/<path>}, e.g.
 * {@code http://localhost:8085/mcp/eclipse-ide}. The MCP server name written
 * into
 * {@code opencode.json} is derived from the last path segment of the URL
 * (e.g. {@code eclipse-ide}).
 *
 * <h4>Port contract</h4>
 * All URLs returned by all registered providers must use the same port (the
 * Tomcat MCP bridge port). The framework extracts it from the first URL it
 * encounters and sets it as {@code MCP_PORT}.
 *
 * @author jcompagner
 * @since 2026.06
 */
public interface IMcpEndpointProvider {

	/**
	 * Returns the fully-qualified MCP endpoint URLs exposed by this provider.
	 * An empty list means this provider has no active endpoints right now.
	 *
	 * @return list of URLs, e.g. {@code ["http://localhost:8085/mcp/eclipse-ide"]}
	 */
	List<String> getUrls();

	/**
	 * Returns the Basic auth token used in the {@code Authorization} header for
	 * these endpoints, or {@code null} if no authentication is needed.
	 *
	 * @return the raw token value (placed after "Basic " in the header), or
	 *         {@code null}
	 */
	String getAuthToken();
}
