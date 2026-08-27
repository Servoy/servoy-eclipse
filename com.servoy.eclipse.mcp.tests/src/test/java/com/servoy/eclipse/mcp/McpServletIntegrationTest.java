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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * What the server does once a request reaches it: who it thinks the caller is, what it lets them
 * see, and whether it notices the solution changing underneath it.
 *
 * <p>These are the things a unit test cannot reach - they need a solution, a database and a client
 * pool - and the things the protocol layer cannot show, because they are about identity and data
 * rather than about JSON-RPC.</p>
 *
 * @author Servoy
 */
public class McpServletIntegrationTest
{
	private static final String SOLUTION = McpTestWorkspace.SOLUTION;

	@BeforeClass
	public static void prepareWorkspace() throws Exception
	{
		McpTestWorkspace.prepare();
	}

	// -----------------------------------------------------------------------
	// the catalogue
	// -----------------------------------------------------------------------

	@Test
	public void toolsListNeedsNoToken() throws Exception
	{
		McpCall listed = McpCall.toolsList(SOLUTION);

		assertEquals(200, listed.status());
		assertTrue("the catalogue is public - reading it commits nothing: " + listed.body(),
			listed.contains("myScope_whoAmI"));
	}

	@Test
	public void toolsListLeavesOutAnUnsupportedParameterType() throws Exception
	{
		McpCall listed = McpCall.toolsList(SOLUTION);

		assertFalse("a tool taking a Date cannot be described in JSON Schema, so it is not offered: " + listed.body(),
			listed.contains("unsupportedParameterType"));
	}

	@Test
	public void toolsListCarriesTheScopeInEveryName() throws Exception
	{
		McpCall listed = McpCall.toolsList(SOLUTION);

		assertTrue(listed.contains("salesTools_countRows"));
		assertTrue(listed.contains("myScope_add"));
	}

	@Test
	public void anUnknownSolutionIsNotFound() throws Exception
	{
		McpCall listed = McpCall.toolsList("no_such_solution");

		assertEquals("guessing a name must not reach anything", 404, listed.status());
	}

	// -----------------------------------------------------------------------
	// who the caller is - decided by the solution, not by us
	// -----------------------------------------------------------------------

	@Test
	public void aCallWithoutATokenIsRefused() throws Exception
	{
		McpCall called = McpCall.callTool(SOLUTION, "myScope_whoAmI", null, null);

		assertTrue("no token means no identity, and a tool always runs as someone: " + called.body(),
			called.contains("isError\":true") || called.contains("bearer"));
	}

	@Test
	public void anUnknownTokenIsRefusedByTheAuthenticator() throws Exception
	{
		McpCall called = McpCall.callTool(SOLUTION, "myScope_whoAmI", null, "not-a-real-token");

		assertTrue("the refusal is the solution's own, which is the whole point of the design: " + called.body(),
			called.contains("isError\":true"));
		assertFalse("and it must not be mistaken for a successful answer", called.contains("alice"));
	}

	@Test
	public void aTokenBecomesTheUserTheAuthenticatorNames() throws Exception
	{
		McpCall alice = McpCall.callTool(SOLUTION, "myScope_whoAmI", null, McpTestWorkspace.ALICE);

		assertTrue("the identity has to reach the client session, not just the request: " + alice.body(),
			alice.contains("alice"));
	}

	@Test
	public void twoTokensAreTwoDifferentUsers() throws Exception
	{
		McpCall alice = McpCall.callTool(SOLUTION, "myScope_whoAmI", null, McpTestWorkspace.ALICE);
		McpCall bob = McpCall.callTool(SOLUTION, "myScope_whoAmI", null, McpTestWorkspace.BOB);

		assertTrue(alice.contains("alice"));
		assertTrue(bob.contains("bob"));
		assertFalse("one user's client must never answer for another: " + bob.body(), bob.contains("alice"));
	}

	// -----------------------------------------------------------------------
	// the tenant - the part that would be a data leak if it were wrong
	// -----------------------------------------------------------------------

	@Test
	public void oneTenantDoesNotSeeAnother() throws Exception
	{
		McpCall alice = McpCall.callTool(SOLUTION, "salesTools_listMyItems", null, McpTestWorkspace.ALICE);
		McpCall bob = McpCall.callTool(SOLUTION, "salesTools_listMyItems", null, McpTestWorkspace.BOB);

		// the tool has no where clause: the filtering is the tenant's, applied by Servoy
		assertTrue("acme should see its own rows: " + alice.body(), alice.contains("Acme anvil"));
		assertFalse("and none of globex's: " + alice.body(), alice.contains("Globex"));

		assertTrue("globex should see its own rows: " + bob.body(), bob.contains("Globex gadget"));
		assertFalse("and none of acme's: " + bob.body(), bob.contains("Acme"));
	}

	@Test
	public void theSameUserAsksTwiceAndStillSeesOnlyTheirTenant() throws Exception
	{
		McpCall bob = McpCall.callTool(SOLUTION, "salesTools_listMyItems", null, McpTestWorkspace.BOB);
		McpCall alice = McpCall.callTool(SOLUTION, "salesTools_listMyItems", null, McpTestWorkspace.ALICE);
		McpCall bobAgain = McpCall.callTool(SOLUTION, "salesTools_listMyItems", null, McpTestWorkspace.BOB);

		// a pooled client is reused, so this is where a leaked session would show
		assertEquals("a returned client must come back to the same user and the same tenant",
			bob.body(), bobAgain.body());
		assertFalse(alice.contains("Globex"));
	}

	// -----------------------------------------------------------------------
	// arguments and results
	// -----------------------------------------------------------------------

	@Test
	public void argumentsArriveInOrder() throws Exception
	{
		McpCall added = McpCall.callTool(SOLUTION, "myScope_add", "{\"first\":2,\"second\":40}", McpTestWorkspace.ALICE);

		assertTrue("the schema is by name, the call is positional - this is where that is bridged: " + added.body(),
			added.contains("42"));
	}

	@Test
	public void aWholeNumberComesBackWithoutADecimal() throws Exception
	{
		McpCall added = McpCall.callTool(SOLUTION, "myScope_add", "{\"first\":2,\"second\":40}", McpTestWorkspace.ALICE);

		assertFalse("the engine hands back a Double, the agent should not have to read 42.0: " + added.body(),
			added.contains("42.0"));
	}

	@Test
	public void aJsonArgumentArrivesAsAnObject() throws Exception
	{
		McpCall echoed = McpCall.callTool(SOLUTION, "myScope_echo",
			"{\"payload\":{\"k\":[1,2,3]}}", McpTestWorkspace.ALICE);

		assertTrue("an unconstrained parameter must not turn into a string on the way in: " + echoed.body(),
			echoed.contains("object"));
	}

	@Test
	public void anOmittedOptionalArgumentIsNotAnError() throws Exception
	{
		McpCall echoed = McpCall.callTool(SOLUTION, "myScope_echo",
			"{\"payload\":\"only the required one\"}", McpTestWorkspace.ALICE);

		assertFalse("leaving out an optional parameter is what optional means: " + echoed.body(),
			echoed.contains("isError\":true"));
	}

	@Test
	public void aToolReadsRealData() throws Exception
	{
		McpCall counted = McpCall.callTool(SOLUTION, "salesTools_countRows",
			"{\"tableName\":\"mcp_tenant_demo\"}", McpTestWorkspace.ALICE);

		// all five rows, not the three that belong to acme: a tenant filter applies to queries Servoy
		// builds, and countRows asks with a SQL string, which goes straight to the database. That is
		// the difference between this tool and listMyItems, which uses QBSelect and is filtered - and
		// it is worth a test of its own, because it is exactly the mistake someone writing a tool will
		// make.
		assertTrue("a tool that queries the database has to come back with a number: " + counted.body(),
			counted.contains("\"text\":\"5\""));
	}

	// -----------------------------------------------------------------------
	// the failure paths
	// -----------------------------------------------------------------------

	@Test
	public void anUnknownToolIsRefused() throws Exception
	{
		McpCall called = McpCall.callTool(SOLUTION, "no_such_tool", null, McpTestWorkspace.ALICE);

		assertTrue("an agent will try things that do not exist: " + called.body(),
			called.contains("-32602") || called.contains("isError\":true"));
	}

	@Test
	public void anUnsupportedMethodIsRefusedByTheProtocol() throws Exception
	{
		McpCall called = McpCall.post(SOLUTION, "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"resources/list\"}", null);

		assertTrue("we publish tools and nothing else: " + called.body(), called.contains("-32601"));
	}

	@Test
	public void aNotificationIsAcceptedWithoutAnAnswer() throws Exception
	{
		McpCall notified = McpCall.post(SOLUTION, "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", null);

		// a notification carries no id, so there is nothing to answer - 202 and an empty body is the
		// protocol's way of saying it was taken
		assertEquals(202, notified.status());
		assertEquals("", notified.body());
	}

	@Test
	public void aPathWithoutASolutionSaysSo() throws Exception
	{
		McpCall called = McpCall.post("", "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}", null);

		assertEquals(404, called.status());
	}
}
