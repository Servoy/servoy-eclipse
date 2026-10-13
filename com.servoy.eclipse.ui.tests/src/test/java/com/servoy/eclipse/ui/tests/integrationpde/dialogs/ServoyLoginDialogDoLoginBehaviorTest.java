package com.servoy.eclipse.ui.tests.integrationpde.dialogs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog.LoginTokenResponse;
import com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.StubSecurePreferences;
import com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.TestableServoyLoginDialog;

@DisplayName("ServoyLoginDialog — doLogin response handling logic")
class ServoyLoginDialogDoLoginBehaviorTest extends TestUtilitiesClass {

	private TestableServoyLoginDialog dialog;

	protected ServoyLoginDialogDoLoginBehaviorTest() {
		super(null, null);
	}

	@BeforeEach
	void setUp() throws Exception {
		dialog = new TestableServoyLoginDialog();
		com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.setCloudReachable(true);
	}

	@AfterEach
	void cleanup() throws Exception {
		com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.setCloudReachable(true);
		com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.shutdownRetryExecutor();
	}

	@Test
	@DisplayName("AC8: LOGIN_ERROR does NOT change cloudReachable (stays true)")
	void testLoginErrorDoesNotChangeCloudReachable() {
		com.servoy.eclipse.ui.tests.plainjunit.dialogs.ServoyLoginDialogDoLoginBehaviorTest.setCloudReachable(true);
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, "401 Unauthorized");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, token -> { }, new StubSecurePreferences());
		assertTrue(ServoyLoginDialog.isCloudReachable());
	}


	@Test
	@DisplayName("AC8: LOGIN_ERROR triggers doLogin re-call (re-prompt for credentials)")
	void testLoginErrorTriggersDoLoginReCall() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, "401 Unauthorized");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, token -> { }, new StubSecurePreferences());
		// Drain the SWT event queue so the asyncExec'd doLogin() call executes
		
		pumpEventsUntil(5000, () -> {
			assertTrue(dialog.doLoginRecalled.get(), "doLogin MUST be re-called on LOGIN_ERROR (re-prompt)");
		});
	}

}
