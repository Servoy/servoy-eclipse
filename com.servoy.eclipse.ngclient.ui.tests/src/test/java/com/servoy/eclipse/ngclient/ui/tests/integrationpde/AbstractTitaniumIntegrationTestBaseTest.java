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
package com.servoy.eclipse.ngclient.ui.tests.integrationpde;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.ngclient.ui.Activator;
import com.servoy.eclipse.ngclient.ui.tests.integrationpde.utils.AbstractTitaniumIntegrationTest;

/**
 * Unit tests for {@link AbstractTitaniumIntegrationTest}'s static lifecycle hooks
 * (SVY-21284). These tests call the public static methods directly — the same
 * methods that JUnit 5's {@code @BeforeAll} / {@code @AfterAll} annotations
 * invoke at suite time — so no OSGi runtime or workbench is required.
 */
public class AbstractTitaniumIntegrationTestBaseTest
{
	/** Original toggle state, captured before each test and restored after. */
	private boolean stateBeforeTest;

	@BeforeEach
	void captureState()
	{
		stateBeforeTest = Activator.isNodeExtractionAndTitaniumBuildDisabled();
	}

	@AfterEach
	void restoreState()
	{
		Activator.setNodeExtractionAndTitaniumBuildDisabled(stateBeforeTest);
	}

	@Nested
	class DisableHook
	{
		@Test
		@org.junit.jupiter.api.DisplayName("adjustTitaniumBuildJobEnablementForThisClass() sets Activator.isNodeExtractionAndTitaniumBuildDisabled() to true")
		void disableHookSetsDisabledTrue() throws Exception
		{
			Activator.setNodeExtractionAndTitaniumBuildDisabled(false); // start from known state
			AbstractTitaniumIntegrationTest.adjustTitaniumBuildJobEnablementForThisClass();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(),
				"After adjustTitaniumBuildJobEnablementForThisClass(), NodeFolderCreatorJob must report disabled=true");
		}

		@Test
		@org.junit.jupiter.api.DisplayName("adjustTitaniumBuildJobEnablementForThisClass() is idempotent when already disabled")
		void disableHookIsIdempotent() throws Exception
		{
			Activator.setNodeExtractionAndTitaniumBuildDisabled(true);
			AbstractTitaniumIntegrationTest.adjustTitaniumBuildJobEnablementForThisClass();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(),
				"Repeated disable calls must leave disabled=true");
		}

		@Test
		@org.junit.jupiter.api.DisplayName("adjustTitaniumBuildJobEnablementForThisClass() does not throw")
		void disableHookDoesNotThrow()
		{
			assertDoesNotThrow(AbstractTitaniumIntegrationTest::adjustTitaniumBuildJobEnablementForThisClass);
		}
	}

	@Nested
	class RestoreHook
	{
		@Test
		@org.junit.jupiter.api.DisplayName("restoreTitaniumBuildJobEnablementToDefault() sets Activator.isNodeExtractionAndTitaniumBuildDisabled() to true")
		void restoreHookSetsDisabledFalse()
		{
			Activator.setNodeExtractionAndTitaniumBuildDisabled(true); // start from known state
			AbstractTitaniumIntegrationTest.restoreTitaniumBuildJobEnablementToDefault();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(),
				"After restoreTitaniumBuildJobEnablementToDefault(), NodeFolderCreatorJob must report disabled=true");
		}

		@Test
		@org.junit.jupiter.api.DisplayName("restoreTitaniumBuildJobEnablementToDefault() is idempotent when already enabled")
		void restoreHookIsIdempotent()
		{
			Activator.setNodeExtractionAndTitaniumBuildDisabled(false);
			AbstractTitaniumIntegrationTest.restoreTitaniumBuildJobEnablementToDefault();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(),
				"Repeated restore calls must leave disabled=false");
		}

		@Test
		@org.junit.jupiter.api.DisplayName("restoreTitaniumBuildJobEnablementToDefault() does not throw")
		void restoreHookDoesNotThrow()
		{
			assertDoesNotThrow(AbstractTitaniumIntegrationTest::restoreTitaniumBuildJobEnablementToDefault);
		}
	}

	@Nested
	class LifecyclePair
	{
		@Test
		@org.junit.jupiter.api.DisplayName("enabled then restore leaves the toggle at true")
		void disableThenRestoreLeavesEnabled() throws Exception
		{
			Activator.setNodeExtractionAndTitaniumBuildDisabled(false);

			AbstractTitaniumIntegrationTest.adjustTitaniumBuildJobEnablementForThisClass();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(), "Must be disabled after setup hook");

			AbstractTitaniumIntegrationTest.restoreTitaniumBuildJobEnablementToDefault();
			assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(), "Must be re-enabled after teardown hook");
		}

		@Test
		@org.junit.jupiter.api.DisplayName("multiple disable/restore cycles are consistent")
		void multipleLifecycleCyclesAreConsistent() throws Exception
		{
			for (int i = 0; i < 5; i++)
			{
				Activator.setNodeExtractionAndTitaniumBuildDisabled(false);

				AbstractTitaniumIntegrationTest.adjustTitaniumBuildJobEnablementForThisClass();
				assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(), "Cycle " + i + ": must be disabled after setup");

				AbstractTitaniumIntegrationTest.restoreTitaniumBuildJobEnablementToDefault();
				assertTrue(Activator.isNodeExtractionAndTitaniumBuildDisabled(), "Cycle " + i + ": must be enabled after teardown");
			}
		}
	}

}
