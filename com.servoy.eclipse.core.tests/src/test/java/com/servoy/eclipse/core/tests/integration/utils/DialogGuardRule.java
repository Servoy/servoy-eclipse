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
package com.servoy.eclipse.core.tests.integration.utils;

import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.servoy.eclipse.core.tests.integration.utils.TestDialogInterceptor.UnexpectedDialogFailure;

/**
 * JUnit 5 {@link org.junit.jupiter.api.extension.Extension} that guards integration tests against unexpected SWT dialogs.
 * <p>
 * Install on any test class (or on {@link TestUtilitiesClass} to cover all subclasses):
 * <pre>
 *   {@literal @}ExtendWith(DialogGuardRule.class)
 * </pre>
 * <p>
 * <b>Before each test:</b> activates the {@link TestDialogInterceptor} SWT display filter
 * and clears any leftover state from the previous test.
 * <p>
 * <b>After each test:</b> drains pending SWT {@code asyncExec} callbacks (so dialogs
 * opened in the last moments of the test are processed), then fails the test if any
 * unexpected dialog was detected. The failure message includes the dialog title,
 * the label text found inside the shell, and the UI-thread stack trace captured
 * at the moment the shell became visible — making it easy to identify exactly which
 * production-code callsite opened the dialog.
 * <p>
 * Tests that intentionally trigger a dialog must push a {@link DialogExpectation}
 * via {@link TestDialogInterceptor#expect(DialogExpectation)} <em>before</em> the call
 * that causes the dialog to appear.
 * 
 * NOTE: this class is copied/duplicated into other repos. with name DialogGuardRuleCopy; it is duplicated in order to avoid cross repo
 * dependencies on test projects which would require more configuration for Jenkins (one more feature, update site etc.).
 * Keep this original and all the copies in sync pls.
 */
public class DialogGuardRule implements BeforeEachCallback, AfterEachCallback
{
	@Override
	public void beforeEach(ExtensionContext context)
	{
		TestDialogInterceptor.beginTest();
	}

	@Override
	public void afterEach(ExtensionContext context)
	{
		// Drain any asyncExec callbacks that the filter may have posted during the
		// very last operation of the test.  We pump for up to 2 s or until the
		// queue is empty, whichever comes first.
		pumpDisplay(2000);

		List<UnexpectedDialogFailure> dialogFailures = TestDialogInterceptor.endTest();

		if (!dialogFailures.isEmpty())
		{
			StringBuilder msg = new StringBuilder();
			msg.append(dialogFailures.size())
				.append(" unexpected dialog(s) appeared during the test:\n\n");
			for (UnexpectedDialogFailure f : dialogFailures)
			{
				msg.append(f.fullInfoIncludingStackTrace).append("\n");
			}
			// Use AssertionError so JUnit marks the test as FAILED (not ERROR)
			throw new AssertionError(msg.toString());
		}
	}

	/**
	 * Pumps the SWT event loop for up to {@code maxMs} milliseconds, draining
	 * any pending {@code asyncExec} runnables posted by the display filter.
	 * Safe to call from any thread.
	 */
	private static void pumpDisplay(long maxMs)
	{
		Display display = Display.getDefault();
		if (display == null || display.isDisposed()) return;

		if (display.getThread() == Thread.currentThread())
		{
			long end = System.currentTimeMillis() + maxMs;
			while (System.currentTimeMillis() < end && display.readAndDispatch())
			{
				/* keep draining */
			}
		}
		else
		{
			// Block the test thread until a syncExec drains the pending runnables
			// on the UI thread.  A no-op syncExec flushes all earlier asyncExecs.
			display.syncExec(() -> { /* no-op — just flush */ });
		}
	}

}
