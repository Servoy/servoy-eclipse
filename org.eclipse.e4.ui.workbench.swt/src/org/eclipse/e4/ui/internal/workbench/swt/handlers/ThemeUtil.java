/*******************************************************************************
 * Copyright (c) 2009, 2015 Siemens AG and others.
 *
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     Kai Tödter - initial implementation
 ******************************************************************************/

package org.eclipse.e4.ui.internal.workbench.swt.handlers;

import org.eclipse.e4.ui.services.IStylingEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class ThemeUtil {

	public static void applyDialogStyles(IStylingEngine engine, Control control) {
		if (engine != null) {
			Shell shell = control.getShell();
			if (shell.getBackgroundMode() == SWT.INHERIT_NONE) {
				shell.setBackgroundMode(SWT.INHERIT_DEFAULT);
			}

			engine.style(shell);
		}
	}
}
