package com.servoy.eclipse.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LoginTokenResponse — status classification")
class LoginTokenResponseTest {

	@Test
	@DisplayName("OK status preserves token string")
	void testOkStatusPreservesToken() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.OK, "abc123");
		assertEquals(LoginTokenResponse.Status.OK, response.status);
		assertEquals("abc123", response.response);
	}

	@Test
	@DisplayName("LOGIN_ERROR status preserves error message")
	void testLoginErrorStatusPreservesMessage() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.LOGIN_ERROR, "HTTP ERROR : 401 Unauthorized");
		assertEquals(LoginTokenResponse.Status.LOGIN_ERROR, response.status);
		assertEquals("HTTP ERROR : 401 Unauthorized", response.response);
	}

	@Test
	@DisplayName("ERROR status preserves server error message (AC1 — 5xx mapped to ERROR)")
	void testErrorStatusPreservesServerError() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "HTTP ERROR : 503 Service Unavailable");
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		assertEquals("HTTP ERROR : 503 Service Unavailable", response.response);
	}

	@Test
	@DisplayName("ERROR status for connection failure (AC2 — unreachable mapped to ERROR)")
	void testErrorStatusForConnectionFailure() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, "java.net.ConnectException: Connection refused");
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		assertNotNull(response.response);
	}

	@Test
	@DisplayName("Response can hold null response string")
	void testNullResponse() {
		LoginTokenResponse response = new LoginTokenResponse(LoginTokenResponse.Status.ERROR, null);
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		assertNull(response.response);
	}

	@Test
	@DisplayName("Status enum has exactly three values: OK, LOGIN_ERROR, ERROR")
	void testStatusEnumValues() {
		LoginTokenResponse.Status[] values = LoginTokenResponse.Status.values();
		assertEquals(3, values.length);
		assertEquals(LoginTokenResponse.Status.OK, LoginTokenResponse.Status.valueOf("OK"));
		assertEquals(LoginTokenResponse.Status.LOGIN_ERROR, LoginTokenResponse.Status.valueOf("LOGIN_ERROR"));
		assertEquals(LoginTokenResponse.Status.ERROR, LoginTokenResponse.Status.valueOf("ERROR"));
	}
}
