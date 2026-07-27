package com.servoy.eclipse.model.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Login Dialog Skip in Test Mode - Integration")
class LoginSkipIntegrationTest {
	@Test
	@DisplayName("isTestRunning returns true, ensuring showLoginAndStart() short-circuits in test mode")
	void testModeGuardPreventsLoginDialog() {
		// The com.servoy.eclipse.ui.Activator.showLoginAndStart() method checks
		// ModelUtils.isTestRunning() as its first guard clause and returns immediately
		// when true, preventing the login dialog from opening.
		//
		// We cannot call showLoginAndStart() directly from this fragment because its
		// host is com.servoy.eclipse.model, which does not depend on
		// com.servoy.eclipse.ui.
		// Instead, we verify the guard condition holds in the PDE test environment.
		assertTrue(ModelUtils.isTestRunning(),
				"isTestRunning() must return true in PDE test environment so that " +
						"showLoginAndStart() short-circuits without opening a dialog");
	}
}
