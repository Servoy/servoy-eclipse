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
package com.servoy.eclipse.cypress.integrationpde;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.servoy.eclipse.core.tests.integration.utils.DialogExpectation;
import com.servoy.eclipse.core.tests.integration.utils.DialogGuardRule;
import com.servoy.eclipse.core.tests.integration.utils.TestDialogInterceptor;
import com.servoy.eclipse.cypress.actions.RunCypressFormTestHandler;

/**
 * Integration tests for {@link RunCypressFormTestHandler} paths that need a real SWT {@link Display}.
 * <p>
 * Moved out of the plainjunit package: with no form selected {@code execute} opens a JFace error
 * dialog through {@code Display.getDefault()}, which kills the forked JVM when there is no display
 * (plain surefire run without Xvfb).
 */
@ExtendWith(DialogGuardRule.class)
@DisplayName("RunCypressFormTestHandler (PDE)")
public class RunCypressFormTestHandlerIntegrationTest {

	@Test
	@DisplayName("execute returns null and reports 'No active Servoy project' when there is no workbench context")
	public void executeReturnsNullWithoutContextAndShowsErrorDialog() {
		TestDialogInterceptor.expect(DialogExpectation.withTitleContaining("Cypress Form Test")
				.messageContains("No active Servoy project").clickButtonContains("OK"));
		RunCypressFormTestHandler handler = new RunCypressFormTestHandler();
		ExecutionEvent event = new ExecutionEvent(null, Collections.emptyMap(), null, null);

		AtomicInteger shownShells = new AtomicInteger();
		Listener countShells = (Event e) -> {
			if (e.widget instanceof Shell) {
				shownShells.incrementAndGet();
			}
		};
		Display display = Display.getDefault();
		runOnUiThread(display, () -> display.addFilter(SWT.Show, countShells));
		try {
			Object result = assertDoesNotThrow(() -> handler.execute(event));
			assertNull(result);

			// the dialog is opened with asyncExec; wait until it was shown and closed again
			flushAsyncExecs(display);
		} finally {
			runOnUiThread(display, () -> display.removeFilter(SWT.Show, countShells));
		}

		assertEquals(1, shownShells.get(), "Exactly one error dialog should have been shown");
	}

	private static void runOnUiThread(Display display, Runnable runnable) {
		if (display.getThread() == Thread.currentThread()) {
			runnable.run();
		} else {
			display.syncExec(runnable);
		}
	}

	/**
	 * Runs everything that was queued with asyncExec before this call. The modal dialog runs its own event loop inside
	 * the queued runnable and is answered by the {@link TestDialogInterceptor}, so this returns once it closed.
	 */
	private static void flushAsyncExecs(Display display) {
		if (display.getThread() == Thread.currentThread()) {
			while (display.readAndDispatch()) {
				// keep draining
			}
		} else {
			display.syncExec(() -> {
				// no-op, just waits for the earlier asyncExecs
			});
		}
	}
}
