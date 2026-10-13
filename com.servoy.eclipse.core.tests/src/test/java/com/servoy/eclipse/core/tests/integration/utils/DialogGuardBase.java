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

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Minimal base class for all PDE integration tests.
 * <p>
 * Installs {@link DialogGuardRule} as a JUnit 5 {@link ExtendWith} extension so that
 * any SWT dialog that appears unexpectedly during a test causes an immediate,
 * informative failure instead of hanging the test runner indefinitely.
 * <p>
 * {@link TestUtilitiesClass} extends this class, so every test that extends
 * {@code TestUtilitiesClass} (or its subclasses in dependent fragments) gets the
 * guard for free.
 * <p>
 * Tests that do not extend {@code TestUtilitiesClass} should extend this class
 * directly.
 * 
 * NOTE: this class is copied/duplicated into other repos. with name DialogGuardBaseCopy; it is duplicated in order to avoid cross repo
 * dependencies on test projects which would require more configuration for Jenkins (one more feature, update site etc.).
 * Keep this original and all the copies in sync pls.
 */
@ExtendWith(DialogGuardRule.class)
public class DialogGuardBase
{
}
