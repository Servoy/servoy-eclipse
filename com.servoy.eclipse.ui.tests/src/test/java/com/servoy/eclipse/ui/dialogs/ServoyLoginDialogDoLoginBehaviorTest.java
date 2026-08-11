package com.servoy.eclipse.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServoyLoginDialog — doLogin response handling logic")
class ServoyLoginDialogDoLoginBehaviorTest {

	private TestableServoyLoginDialog dialog;

	@BeforeEach
	void setUp() throws Exception {
		dialog = new TestableServoyLoginDialog();
		setCloudReachable(true);
	}

	@AfterEach
	void cleanup() throws Exception {
		setCloudReachable(true);
		shutdownRetryExecutor();
	}

	@Test
	@DisplayName("AC1: ERROR status during first login sets cloudReachable=false")
	void testErrorDuringFirstLoginSetsCloudUnreachable() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, token -> { }, new StubSecurePreferences());
		assertFalse(ServoyLoginDialog.isCloudReachable());
	}

	@Test
	@DisplayName("AC5: ERROR status during background refresh (not first login) sets cloudReachable=false")
	void testErrorDuringBackgroundRefreshSetsCloudUnreachable() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, token -> { }, new StubSecurePreferences());
		assertFalse(ServoyLoginDialog.isCloudReachable());
	}

	@Test
	@DisplayName("AC1/AC4: ERROR during first login passes null token to onLogin consumer")
	void testErrorDuringFirstLoginPassesNullToken() {
		AtomicReference<String> receivedToken = new AtomicReference<>("NOT_CALLED");
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, receivedToken::set, new StubSecurePreferences());
		assertNull(receivedToken.get());
	}

	@Test
	@DisplayName("AC5: ERROR during background refresh passes null token to onLogin consumer")
	void testErrorDuringBackgroundRefreshPassesNullToken() {
		AtomicReference<String> receivedToken = new AtomicReference<>("NOT_CALLED");
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, receivedToken::set, new StubSecurePreferences());
		assertNull(receivedToken.get());
	}

	@Test
	@DisplayName("OK status sets cloudReachable=true and passes token to consumer")
	void testOkStatusSetsCloudReachableAndPassesToken() {
		setCloudReachable(false);
		AtomicReference<String> receivedToken = new AtomicReference<>(null);
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.OK, "my-token");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, receivedToken::set, new StubSecurePreferences());
		assertTrue(ServoyLoginDialog.isCloudReachable());
		assertEquals("my-token", receivedToken.get());
	}

	@Test
	@DisplayName("AC8: LOGIN_ERROR does NOT change cloudReachable (stays true)")
	void testLoginErrorDoesNotChangeCloudReachable() {
		setCloudReachable(true);
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, "401 Unauthorized");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, token -> { }, new StubSecurePreferences());
		assertTrue(ServoyLoginDialog.isCloudReachable());
	}

	@Test
	@DisplayName("AC7: After ERROR then OK, cloudReachable is restored to true")
	void testCloudRestoredAfterErrorThenOk() {
		LoginTokenResponse errorResp = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(errorResp, "user@test.com", "pass", false, token -> { }, new StubSecurePreferences());
		assertFalse(ServoyLoginDialog.isCloudReachable());

		AtomicReference<String> receivedToken = new AtomicReference<>(null);
		LoginTokenResponse okResp = new LoginTokenResponse(LoginTokenResponse.Status.OK, "restored-token");
		dialog.handleLoginTokenResponse(okResp, "user@test.com", "pass", false, receivedToken::set, new StubSecurePreferences());
		assertTrue(ServoyLoginDialog.isCloudReachable());
		assertEquals("restored-token", receivedToken.get());
	}

	@Test
	@DisplayName("AC3: ERROR during first login triggers showCloudUnavailableDialog")
	void testErrorDuringFirstLoginTriggersInfoDialog() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, token -> { }, new StubSecurePreferences());
		assertTrue(dialog.cloudUnavailableDialogShown.get());
	}

	@Test
	@DisplayName("AC5: ERROR during background refresh does NOT trigger info dialog")
	void testErrorDuringBackgroundRefreshNoDialog() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", false, token -> { }, new StubSecurePreferences());
		assertFalse(dialog.cloudUnavailableDialogShown.get());
	}

	@Test
	@DisplayName("AC4: ERROR during first login persists credentials to secure storage")
	void testErrorDuringFirstLoginPersistsCredentials() {
		StubSecurePreferences node = new StubSecurePreferences();
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "testuser@example.com", "secret123", true, token -> { }, node);
		assertEquals("testuser@example.com", node.storedUsername);
		assertEquals("secret123", node.storedPassword);
	}

	@Test
	@DisplayName("AC4: OK status persists credentials and token to secure storage")
	void testOkStatusPersistsCredentialsAndToken() {
		StubSecurePreferences node = new StubSecurePreferences();
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.OK, "the-token");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass123", false, token -> { }, node);
		assertEquals("user@test.com", node.storedUsername);
		assertEquals("pass123", node.storedPassword);
		assertEquals("the-token", node.storedToken);
	}

	@Test
	@DisplayName("AC1: ERROR during first login does NOT re-open login dialog (no infinite loop)")
	void testErrorDuringFirstLoginDoesNotReopenDialog() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "server down");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, token -> { }, new StubSecurePreferences());
		assertFalse(dialog.doLoginRecalled.get(), "doLogin must NOT be re-called on ERROR status (old infinite loop bug)");
	}

	@Test
	@DisplayName("AC8: LOGIN_ERROR triggers doLogin re-call (re-prompt for credentials)")
	void testLoginErrorTriggersDoLoginReCall() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, "401 Unauthorized");
		dialog.handleLoginTokenResponse(response, "user@test.com", "pass", true, token -> { }, new StubSecurePreferences());
		// Drain the SWT event queue so the asyncExec'd doLogin() call executes
		while (org.eclipse.swt.widgets.Display.getDefault().readAndDispatch())
		{
			// keep processing
		}
		assertTrue(dialog.doLoginRecalled.get(), "doLogin MUST be re-called on LOGIN_ERROR (re-prompt)");
	}

	private static void setCloudReachable(boolean value) {
		try
		{
			Field field = ServoyLoginDialog.class.getDeclaredField("cloudReachable");
			field.setAccessible(true);
			field.set(null, value);
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static void shutdownRetryExecutor() {
		try
		{
			Field field = ServoyLoginDialog.class.getDeclaredField("retryExecutor");
			field.setAccessible(true);
			ScheduledExecutorService exec = (ScheduledExecutorService)field.get(null);
			if (exec != null)
			{
				exec.shutdownNow();
				field.set(null, null);
			}
		}
		catch (Exception e)
		{
			// ignore
		}
	}

	private static class TestableServoyLoginDialog extends ServoyLoginDialog {
		final AtomicBoolean cloudUnavailableDialogShown = new AtomicBoolean(false);
		final AtomicBoolean doLoginRecalled = new AtomicBoolean(false);

		TestableServoyLoginDialog() {
			super(null);
		}

		@Override
		protected void showCloudUnavailableDialog()
		{
			cloudUnavailableDialogShown.set(true);
		}

		@Override
		public void doLogin(java.util.function.Consumer<String> onLogin)
		{
			doLoginRecalled.set(true);
		}
	}

	private static class StubSecurePreferences implements ISecurePreferences {
		String storedUsername;
		String storedPassword;
		String storedToken;

		@Override
		public void put(String key, String value, boolean encrypt) throws StorageException
		{
			if (ServoyLoginDialog.SERVOY_LOGIN_USERNAME.equals(key)) storedUsername = value;
			else if (ServoyLoginDialog.SERVOY_LOGIN_PASSWORD.equals(key)) storedPassword = value;
			else if (ServoyLoginDialog.SERVOY_LOGIN_TOKEN.equals(key)) storedToken = value;
		}

		@Override public String get(String key, String def) throws StorageException { return def; }
		@Override public void remove(String key) { }
		@Override public void clear() { }
		@Override public boolean getBoolean(String key, boolean def) { return def; }
		@Override public void putBoolean(String key, boolean value, boolean encrypt) throws StorageException { }
		@Override public int getInt(String key, int def) { return def; }
		@Override public void putInt(String key, int value, boolean encrypt) throws StorageException { }
		@Override public float getFloat(String key, float def) { return def; }
		@Override public void putFloat(String key, float value, boolean encrypt) throws StorageException { }
		@Override public long getLong(String key, long def) { return def; }
		@Override public void putLong(String key, long value, boolean encrypt) throws StorageException { }
		@Override public double getDouble(String key, double def) { return def; }
		@Override public void putDouble(String key, double value, boolean encrypt) throws StorageException { }
		@Override public byte[] getByteArray(String key, byte[] def) { return def; }
		@Override public void putByteArray(String key, byte[] value, boolean encrypt) throws StorageException { }
		@Override public boolean isEncrypted(String key) throws StorageException { return false; }
		@Override public String[] keys() { return new String[0]; }
		@Override public String[] childrenNames() { return new String[0]; }
		@Override public ISecurePreferences parent() { return null; }
		@Override public ISecurePreferences node(String pathName) { return this; }
		@Override public boolean nodeExists(String pathName) { return false; }
		@Override public void removeNode() { }
		@Override public String name() { return "stub"; }
		@Override public String absolutePath() { return "/stub"; }
		@Override public void flush() throws java.io.IOException { }
	}
}
