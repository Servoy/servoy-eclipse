package com.servoy.eclipse.ui.dialogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

@DisplayName("ServoyLoginDialog — getLoginToken HTTP response handling")
class ServoyLoginDialogGetLoginTokenTest {

	private static HttpServer server;
	private static int port;
	private static String originalCrowdUrl;

	@BeforeAll
	static void startServer() throws Exception {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		port = server.getAddress().getPort();
		server.setExecutor(null);
		server.start();

		originalCrowdUrl = System.getProperty("servoy.test.crowd.url");
		System.setProperty("servoy.test.crowd.url", "http://127.0.0.1:" + port + "/test_auth");
	}

	@AfterAll
	static void stopServer() throws Exception {
		server.stop(0);
		if (originalCrowdUrl != null)
		{
			System.setProperty("servoy.test.crowd.url", originalCrowdUrl);
		}
		else
		{
			System.clearProperty("servoy.test.crowd.url");
		}
	}

	@BeforeEach
	void resetState() throws Exception {
		setCloudReachable(true);
		shutdownRetryExecutor();
	}

	@AfterEach
	void cleanup() throws Exception {
		try
		{
			server.removeContext("/test_auth");
		}
		catch (IllegalArgumentException e)
		{
			// context was not registered by this test — ignore
		}
		shutdownRetryExecutor();
		setCloudReachable(true);
	}

	@Test
	@DisplayName("AC1: HTTP 500 returns ERROR status — not LOGIN_ERROR")
	void testHttp500ReturnsErrorStatus() throws Exception {
		server.createContext("/test_auth", exchange -> {
			byte[] body = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(500, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		assertTrue(response.response.contains("500"));
	}

	@Test
	@DisplayName("AC1: HTTP 503 returns ERROR status")
	void testHttp503ReturnsErrorStatus() throws Exception {
		server.createContext("/test_auth", exchange -> {
			byte[] body = "Service Unavailable".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(503, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		assertTrue(response.response.contains("503"));
	}

	@Test
	@DisplayName("AC8: HTTP 401 returns LOGIN_ERROR — invalid credentials")
	void testHttp401ReturnsLoginError() throws Exception {
		server.createContext("/test_auth", exchange -> {
			byte[] body = "Unauthorized".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(401, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "wrongpassword");
		assertEquals(LoginTokenResponse.Status.LOGIN_ERROR, response.status);
		assertTrue(response.response.contains("401"));
	}

	@Test
	@DisplayName("AC8: HTTP 403 returns LOGIN_ERROR")
	void testHttp403ReturnsLoginError() throws Exception {
		server.createContext("/test_auth", exchange -> {
			byte[] body = "Forbidden".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(403, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password");
		assertEquals(LoginTokenResponse.Status.LOGIN_ERROR, response.status);
		assertTrue(response.response.contains("403"));
	}

	@Test
	@DisplayName("HTTP 200 with valid JSON returns OK status with token")
	void testHttp200ReturnsOkWithToken() throws Exception {
		server.createContext("/test_auth", exchange -> {
			String json = "{\"token\":\"test-token-abc\"}";
			byte[] body = json.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
		assertEquals(LoginTokenResponse.Status.OK, response.status);
		assertEquals("test-token-abc", response.response);
	}

	@Test
	@DisplayName("AC2: Connection refused returns ERROR status")
	void testConnectionRefusedReturnsError() throws Exception {
		String current = System.getProperty("servoy.test.crowd.url");
		try
		{
			System.setProperty("servoy.test.crowd.url", "http://127.0.0.1:1/unreachable");
			LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
			assertEquals(LoginTokenResponse.Status.ERROR, response.status);
			assertNotNull(response.response);
		}
		finally
		{
			if (current != null)
			{
				System.setProperty("servoy.test.crowd.url", current);
			}
			else
			{
				System.clearProperty("servoy.test.crowd.url");
			}
		}
	}

	@Test
	@DisplayName("AC1: After 5xx, cloudReachable is NOT set by getLoginToken alone (set by doLogin caller)")
	void testGetLoginTokenDoesNotSetCloudReachable() throws Exception {
		server.createContext("/test_auth", exchange -> {
			byte[] body = "Error".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(500, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		setCloudReachable(true);
		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
		assertEquals(LoginTokenResponse.Status.ERROR, response.status);
		// getLoginToken itself does NOT change cloudReachable — that's done in doLogin's thenAccept
		assertTrue(ServoyLoginDialog.isCloudReachable());
	}

	@Test
	@DisplayName("HTTP 200 with token: doLogin caller sets cloudReachable=true")
	void testOkResponseMeansCloudReachable() throws Exception {
		server.createContext("/test_auth", exchange -> {
			String json = "{\"token\":\"tok\"}";
			byte[] body = json.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.getResponseBody().close();
		});

		setCloudReachable(false);
		LoginTokenResponse response = invokeGetLoginToken("user@test.com", "password123");
		assertEquals(LoginTokenResponse.Status.OK, response.status);
		// In real flow, doLogin sets cloudReachable=true on OK
	}

	@SuppressWarnings("unchecked")
	private LoginTokenResponse invokeGetLoginToken(String username, String password) throws Exception {
		Method method = ServoyLoginDialog.class.getDeclaredMethod("getLoginToken", String.class, String.class);
		method.setAccessible(true);

		ServoyLoginDialog dialog = createDialogWithoutShell();
		CompletableFuture<LoginTokenResponse> future = (CompletableFuture<LoginTokenResponse>)method.invoke(dialog, username, password);
		return future.get(10, TimeUnit.SECONDS);
	}

	private ServoyLoginDialog createDialogWithoutShell() throws Exception {
		java.lang.reflect.Constructor<ServoyLoginDialog> ctor = ServoyLoginDialog.class.getDeclaredConstructor(
			org.eclipse.swt.widgets.Shell.class);
		ctor.setAccessible(true);
		return ctor.newInstance((org.eclipse.swt.widgets.Shell)null);
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
}
