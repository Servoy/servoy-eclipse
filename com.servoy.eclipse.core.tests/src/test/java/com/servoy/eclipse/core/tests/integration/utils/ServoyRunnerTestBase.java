/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.
*/
package com.servoy.eclipse.core.tests.integration.utils;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.widgets.Display;

/**
 * Shared base class for JSUnit runner integration tests (Layer 3 and Layer 4).
 * <p>
 * Provides common constants and utility methods:
 * <ul>
 * <li>App-server wait / skip logic via {@link #waitForAppServer()}.</li>
 * <li>SWT event-pump wrapper via
 * {@link #runOnBackgroundThread(ThrowingSupplier)}.</li>
 * <li>Workspace file helper via {@link #writeProjectFile}.</li>
 * <li>Markdown-table count extractors via {@link #extractCount}.</li>
 * </ul>
 * 
 * NOTE: this class is copied/duplicated into other repos. with name ServoyRunnerTestBaseCopy; it is duplicated in order to avoid cross repo
 * dependencies on test projects which would require more configuration for Jenkins (one more feature, update site etc.).
 * Keep this original and all the copies in sync pls.
 */
public abstract class ServoyRunnerTestBase extends TestUtilitiesClass {

	/** Timeout in seconds for each JSUnit SmartClient run. */
	protected static final int TIMEOUT_SECONDS = 30;

	public ServoyRunnerTestBase(String testSolutionName, String servoyResourcesProjectName) {
		super(testSolutionName, servoyResourcesProjectName);
	}

	// -----------------------------------------------------------------------
	// SWT event-pump wrapper
	// -----------------------------------------------------------------------

	/** Functional interface for a callable that throws Exception. */
	@FunctionalInterface
	protected interface ThrowingSupplier {
		String get() throws Exception;
	}

	/**
	 * Runs the supplied callable on a dedicated background thread and waits for it
	 * to finish while pumping the SWT event queue.
	 * <p>
	 * PDE JUnit tests execute <em>on the SWT UI thread</em>. Calling
	 * {@code t.join()} would block {@code readAndDispatch()}, deadlocking every
	 * {@code Display.syncExec()} call made by {@code JSUnitRunnerService}. This
	 * method pumps SWT events in a loop so those runnables can execute.
	 */
	protected String runOnBackgroundThread(ThrowingSupplier supplier) throws Exception {
		String[] result = new String[1];
		Exception[] error = new Exception[1];

		Thread t = new Thread(() -> {
			try {
				result[0] = supplier.get();
			} catch (Exception e) {
				error[0] = e;
			}
		}, "jsunit-test-runner");
		t.start();

		long deadline = System.currentTimeMillis() + (TIMEOUT_SECONDS + 30) * 1000L;

		Display display = Display.getDefault();
		boolean onUIThread = (display != null && !display.isDisposed()
				&& Thread.currentThread() == display.getThread());

		if (onUIThread) {
			while (t.isAlive() && System.currentTimeMillis() < deadline) {
				if (display.readAndDispatch()) {
					continue;
				}
				Thread.sleep(10);
			}
		} else {
			long remaining = deadline - System.currentTimeMillis();
			if (remaining > 0) {
				t.join(remaining);
			}
		}

		if (t.isAlive()) {
			System.err.println("[ServoyRunnerTestBase] WARNING: background thread still alive after "
					+ (TIMEOUT_SECONDS + 30) + "s deadline. Thread state: " + t.getState());
		}

		if (error[0] != null)
			throw error[0];
		assertNotNull(result[0], "Background thread did not complete within timeout");
		return result[0];
	}

	// -----------------------------------------------------------------------
	// Markdown-table count extractors
	// -----------------------------------------------------------------------

	/**
	 * Pattern that matches {@code | **N**} cells in the summary row produced by
	 * {@code JSUnitRunnerService.formatResults()}.
	 */
	private static final Pattern COUNT_PATTERN = Pattern.compile("\\| \\*\\*(\\d+)\\*\\*");

	/**
	 * Extracts the numeric value from the {@code columnIndex}-th count cell in the
	 * {@code formatResults()} markdown summary row.
	 * <p>
	 * Column indices: 0 = Passed, 1 = Failed, 2 = Errors, 3 = Ignored.
	 *
	 * @return the extracted count, or {@code -1} if parsing fails
	 */
	protected int extractCount(String result, int columnIndex) {
		Matcher m = COUNT_PATTERN.matcher(result);
		for (int i = 0; i <= columnIndex; i++) {
			if (!m.find())
				return -1;
			if (i == columnIndex) {
				try {
					return Integer.parseInt(m.group(1));
				} catch (NumberFormatException e) {
					return -1;
				}
			}
		}
		return -1;
	}

	/** Extracts the passed-test count from the {@code formatResults()} output. */
	protected int extractPassedCount(String result) {
		return extractCount(result, 0);
	}

	/** Extracts the failed-test count from the {@code formatResults()} output. */
	protected int extractFailedCount(String result) {
		return extractCount(result, 1);
	}

	/** Extracts the error count from the {@code formatResults()} output. */
	protected int extractErrorCount(String result) {
		return extractCount(result, 2);
	}
}
