/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.servoy.eclipse.cypress.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.servoy.eclipse.cypress.actions.CypressConsoleUtil;

/**
 * Integration tests for {@link CypressConsoleUtil}.
 * <p>
 * These tests require a running Eclipse workbench (ConsolePlugin and Display must be available).
 * Run as JUnit Plug-in Test via the AllDeveloperMcpIntegrationTests launch configuration.
 */
public class CypressConsoleUtilIntegrationTest
{
	private IConsoleManager consoleManager;

	@Before
	public void setUp()
	{
		assertNotNull("Display must be available for integration tests", Display.getDefault());
		assertNotNull("ConsolePlugin must be available", ConsolePlugin.getDefault());
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		assertNotNull("ConsoleManager must be available", consoleManager);
	}

	@After
	public void tearDown()
	{
		// Remove any "Cypress Form Tests" consoles created during tests
		for (IConsole c : consoleManager.getConsoles())
		{
			if ("Cypress Form Tests".equals(c.getName()))
			{
				consoleManager.removeConsoles(new IConsole[] { c });
			}
		}
	}

	// -----------------------------------------------------------------------
	// findOrCreateConsole tests
	// -----------------------------------------------------------------------

	@Test
	public void testFindOrCreateConsole_returnsNonNull()
	{
		MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
		assertNotNull("findOrCreateConsole should return a non-null console", console);
	}

	@Test
	public void testFindOrCreateConsole_returnsConsoleWithCorrectName()
	{
		MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
		assertEquals("Cypress Form Tests", console.getName());
	}

	@Test
	public void testFindOrCreateConsole_returnsMessageConsoleInstance()
	{
		MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
		assertTrue("Should be a MessageConsole instance", console instanceof MessageConsole);
	}

	@Test
	public void testFindOrCreateConsole_calledTwice_returnsSameConsole()
	{
		MessageConsole first = CypressConsoleUtil.findOrCreateConsole();
		MessageConsole second = CypressConsoleUtil.findOrCreateConsole();
		assertSame("Second call should return the same console instance", first, second);
	}

	@Test
	public void testFindOrCreateConsole_registersConsoleWithManager()
	{
		CypressConsoleUtil.findOrCreateConsole();

		boolean found = false;
		for (IConsole c : consoleManager.getConsoles())
		{
			if ("Cypress Form Tests".equals(c.getName()) && c instanceof MessageConsole)
			{
				found = true;
				break;
			}
		}
		assertTrue("Console should be registered with the console manager", found);
	}

	@Test
	public void testFindOrCreateConsole_findsExistingConsole()
	{
		// Pre-register a console
		MessageConsole preExisting = new MessageConsole("Cypress Form Tests", null);
		consoleManager.addConsoles(new IConsole[] { preExisting });

		MessageConsole result = CypressConsoleUtil.findOrCreateConsole();
		assertSame("Should find and return the pre-existing console", preExisting, result);
	}

	// -----------------------------------------------------------------------
	// showConsole tests
	// -----------------------------------------------------------------------

	@Test
	public void testShowConsole_doesNotThrow()
	{
		MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
		// showConsole uses asyncExec, so it schedules work on the UI thread.
		// This verifies it doesn't throw immediately.
		CypressConsoleUtil.showConsole(console);

		// Pump the display event loop to let asyncExec execute
		pumpEvents(500);
	}

	@Test
	public void testShowConsole_withNewConsole_doesNotThrow()
	{
		MessageConsole console = new MessageConsole("Cypress Form Tests", null);
		consoleManager.addConsoles(new IConsole[] { console });

		CypressConsoleUtil.showConsole(console);
		pumpEvents(500);
	}

	// -----------------------------------------------------------------------
	// isMatchingConsole integration (verified indirectly via findOrCreateConsole)
	// -----------------------------------------------------------------------

	@Test
	public void testFindOrCreateConsole_recognizesExistingMessageConsole()
	{
		// This indirectly tests isMatchingConsole's true path:
		// findOrCreateConsole iterates consoles and uses isMatchingConsole to find a match.
		MessageConsole preExisting = new MessageConsole("Cypress Form Tests", null);
		consoleManager.addConsoles(new IConsole[] { preExisting });

		MessageConsole result = CypressConsoleUtil.findOrCreateConsole();
		assertSame("findOrCreateConsole should recognize the pre-existing MessageConsole via isMatchingConsole",
			preExisting, result);
	}

	@Test
	public void testFindOrCreateConsole_doesNotMatchDifferentName()
	{
		// Only a console named "Cypress Form Tests" should match
		MessageConsole other = new MessageConsole("Some Other Console", null);
		consoleManager.addConsoles(new IConsole[] { other });

		MessageConsole result = CypressConsoleUtil.findOrCreateConsole();
		assertEquals("Cypress Form Tests", result.getName());
	}

	// -----------------------------------------------------------------------
	// Full cycle: findOrCreate -> show -> verify
	// -----------------------------------------------------------------------

	@Test
	public void testFullCycle_findOrCreate_show_verify()
	{
		MessageConsole console = CypressConsoleUtil.findOrCreateConsole();
		assertNotNull(console);
		assertEquals("Cypress Form Tests", console.getName());

		CypressConsoleUtil.showConsole(console);
		pumpEvents(500);

		// Verify console is still registered
		MessageConsole found = CypressConsoleUtil.findOrCreateConsole();
		assertSame("Should return same console after show", console, found);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private void pumpEvents(long ms)
	{
		try
		{
			Display display = Display.getDefault();
			long end = System.currentTimeMillis() + ms;
			if (display.getThread() == Thread.currentThread())
			{
				while (System.currentTimeMillis() < end)
					display.readAndDispatch();
			}
			else
			{
				Thread.sleep(ms);
			}
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
	}
}

