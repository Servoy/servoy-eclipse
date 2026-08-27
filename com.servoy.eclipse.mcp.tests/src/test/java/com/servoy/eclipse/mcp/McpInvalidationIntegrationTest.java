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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Whether a tool added while the server is running is offered without a restart.
 *
 * <p>A solution is scanned once and its tools registered when its server is built, so this is the
 * test for everything that has to happen when that stops being true: the cached tool list, the
 * pooled clients that still have the old solution open, and the repository's own copy of the
 * solution - which was the part that made the first attempt look as though nothing had changed.</p>
 *
 * @author Servoy
 */
public class McpInvalidationIntegrationTest
{
	private static final String SOLUTION = McpTestWorkspace.SOLUTION;

	private static final String SCOPE_FILE = "addedWhileRunning.js";

	private static final String ADDED_TOOL = "addedWhileRunning_shout";

	/** A scope with one tool in it, written after the server has already answered once. */
	private static final String SCOPE_SOURCE = "/**\n" +
		" * Added while the server was running.\n" +
		" *\n" +
		" * @Tool\n" +
		" *\n" +
		" * @param {String} what something to shout\n" +
		" *\n" +
		" * @return {String}\n" +
		" *\n" +
		" * @properties={typeid:24,uuid:\"7C1E2A40-9B3D-4E51-8A62-D0F4C5E71A9B\"}\n" +
		" */\n" +
		"function shout(what) {\n" +
		"\treturn (what + '').toUpperCase() + '!';\n" +
		"}\n";

	/**
	 * How long to keep asking whether the change has been noticed.
	 *
	 * <p>Long, because the chain being waited on is not under this test's control - a resource change,
	 * a parse, a persist notification, a rescan - and every link of it is slower on a build agent than
	 * on the machine this was written on. The wait ends the moment the answer changes, so the ceiling
	 * costs nothing when things work.</p>
	 */
	private static final long NOTICED_TIMEOUT_MS = 180_000;

	@BeforeClass
	public static void prepareWorkspace() throws Exception
	{
		McpTestWorkspace.prepare();
	}

	@After
	public void removeTheAddedScope() throws Exception
	{
		IFile file = scopeFile();
		if (!file.exists()) return;

		file.delete(true, new NullProgressMonitor());

		// the next test starts by asserting the tool is absent, so leaving before that is true would
		// make it fail for the previous test's reason
		McpTestWorkspace.waitUntil(() -> !offersTheAddedTool(), NOTICED_TIMEOUT_MS,
			"'" + ADDED_TOOL + "' to disappear from the tool list");
	}

	@Test
	public void aToolAddedWhileRunningIsOffered() throws Exception
	{
		// the server answers once first, so its tool list is genuinely cached before the change
		assertFalse("the new scope must not be there yet", McpCall.toolsList(SOLUTION).contains(ADDED_TOOL));

		writeScopeFile();

		McpTestWorkspace.waitUntil(McpInvalidationIntegrationTest::offersTheAddedTool, NOTICED_TIMEOUT_MS,
			"'" + ADDED_TOOL + "' to appear in the tool list");

		assertTrue("saving a scope function has to reach the tool list, or a developer sees a stale server: " +
			McpCall.toolsList(SOLUTION).body(), offersTheAddedTool());
	}

	@Test
	public void aToolAddedWhileRunningCanBeCalled() throws Exception
	{
		writeScopeFile();

		McpTestWorkspace.waitUntil(McpInvalidationIntegrationTest::offersTheAddedTool, NOTICED_TIMEOUT_MS,
			"'" + ADDED_TOOL + "' to appear in the tool list");

		McpCall called = McpCall.callTool(SOLUTION, ADDED_TOOL, "{\"what\":\"it works\"}", McpTestWorkspace.ALICE);

		// being listed is not enough: the pooled client has to have the new solution open as well
		assertTrue("the new tool has to run, not merely appear: " + called.body(), called.contains("IT WORKS!"));
	}

	/**
	 * Whether the server currently offers the tool. Never throws: it is asked in a loop.
	 */
	static boolean offersTheAddedTool()
	{
		try
		{
			return McpCall.toolsList(SOLUTION).contains(ADDED_TOOL);
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private static void writeScopeFile() throws Exception
	{
		IFile file = scopeFile();
		ByteArrayInputStream source = new ByteArrayInputStream(SCOPE_SOURCE.getBytes(StandardCharsets.UTF_8));

		if (file.exists())
		{
			file.setContents(source, true, false, new NullProgressMonitor());
		}
		else
		{
			file.create(source, true, new NullProgressMonitor());
		}

		file.getProject().refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
	}

	private static IFile scopeFile()
	{
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(SOLUTION);
		return project.getFile(SCOPE_FILE);
	}
}
