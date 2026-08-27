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
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * The one test that goes over the wire.
 *
 * <p>Everything else drives the servlet directly, which is faster and steadier. This one exists for
 * what a direct call cannot show, and all of it is wiring rather than logic:</p>
 *
 * <ul>
 * <li>the <code>servoy_mcp</code> bundle resolved in OSGi at all - it carries the SDK on its own
 * class path, so a missing jar shows up here and nowhere else;</li>
 * <li><code>registerWebService</code> ran during <code>ApplicationServer.startServices()</code>;</li>
 * <li><code>WebServicesServlet</code> routes <code>/servoy-service/mcp/&lt;solution&gt;</code> to
 * us, with the path still carrying the alias;</li>
 * <li>the <code>Accept</code> header the SDK insists on is what a real client has to send.</li>
 * </ul>
 *
 * @author Servoy
 */
public class McpHttpIntegrationTest
{
	private static final Duration TIMEOUT = Duration.ofSeconds(120);

	@BeforeClass
	public static void prepareWorkspace() throws Exception
	{
		McpTestWorkspace.prepare();
	}

	@Test
	public void theEndpointIsServedAndAnswersToolsList() throws Exception
	{
		String url = "http://localhost:" + McpTestWorkspace.serverPort() +
			"/servoy-service/mcp/" + McpTestWorkspace.SOLUTION;

		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(TIMEOUT)
			.header("Content-Type", "application/json")
			// SDK 2.0.1 refuses anything else; 1.1.2 did not, so this is worth pinning down
			.header("Accept", "application/json, text/event-stream")
			.POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
			.build();

		HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
			.send(request, HttpResponse.BodyHandlers.ofString());

		assertEquals("the endpoint should be served: " + response.body(), 200, response.statusCode());
		assertTrue("and it should be our server answering: " + response.body(), response.body().contains("myScope_whoAmI"));
	}

	@Test
	public void theAcceptHeaderIsRequired() throws Exception
	{
		String url = "http://localhost:" + McpTestWorkspace.serverPort() +
			"/servoy-service/mcp/" + McpTestWorkspace.SOLUTION;

		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
			.timeout(TIMEOUT)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}"))
			.build();

		HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()
			.send(request, HttpResponse.BodyHandlers.ofString());

		// not our rule but the SDK's, and worth having written down: it is the first thing that
		// goes wrong when someone writes a client by hand
		assertTrue("a request without the Accept header should be refused, not silently served: " + response.body(),
			response.body().contains("Accept") || response.statusCode() != 200);
	}
}
