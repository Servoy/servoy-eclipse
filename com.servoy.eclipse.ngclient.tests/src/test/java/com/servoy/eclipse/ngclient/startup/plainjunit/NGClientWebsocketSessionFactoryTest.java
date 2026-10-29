package com.servoy.eclipse.ngclient.startup.plainjunit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.sablo.websocket.IWebsocketSessionFactory;

import com.servoy.eclipse.ngclient.startup.NGClientWebsocketSessionFactory;
import com.servoy.eclipse.ngclient.startup.NGClientWebsocketSessionFactory.ClientKind;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;

class NGClientWebsocketSessionFactoryTest
{
	private static Map<String, List<String>> params(String... names)
	{
		Map<String, List<String>> result = new HashMap<>();
		for (String name : names)
		{
			result.put(name, List.of("x"));
		}
		return result;
	}

	private static ClientKind select(Map<String, List<String>> params, boolean developerClient, boolean debugHandler)
	{
		return NGClientWebsocketSessionFactory.selectClientKind(params, developerClient, debugHandler);
	}

	@Nested
	class SelectClientKind
	{
		@Test
		@DisplayName("formpreview wins over nodebug and svy_developer, with or without developer client/debug handler")
		void formPreviewWins()
		{
			assertAll(
				() -> assertEquals(ClientKind.FORM_PREVIEW, select(params("formpreview"), false, false)),
				() -> assertEquals(ClientKind.FORM_PREVIEW, select(params("formpreview", "nodebug"), true, true)),
				() -> assertEquals(ClientKind.FORM_PREVIEW, select(params("formpreview", "svy_developer"), true, true)),
				() -> assertEquals(ClientKind.FORM_PREVIEW, select(params("formpreview", "nodebug", "svy_developer"), true, true)));
		}

		@Test
		@DisplayName("nodebug selects NO_DEBUG and wins over svy_developer")
		void noDebug()
		{
			assertAll(
				() -> assertEquals(ClientKind.NO_DEBUG, select(params("nodebug"), false, false)),
				() -> assertEquals(ClientKind.NO_DEBUG, select(params("nodebug"), true, true)),
				() -> assertEquals(ClientKind.NO_DEBUG, select(params("nodebug", "svy_developer"), true, true)));
		}

		@Test
		@DisplayName("svy_developer with a developer client selects DEVELOPER, regardless of debug handler")
		void developerWithClient()
		{
			assertAll(
				() -> assertEquals(ClientKind.DEVELOPER, select(params("svy_developer"), true, false)),
				() -> assertEquals(ClientKind.DEVELOPER, select(params("svy_developer"), true, true)));
		}

		@Test
		@DisplayName("svy_developer with null developer client falls back to PLAIN (headless-safe), even with a debug handler")
		void developerWithoutClient()
		{
			assertAll(
				() -> assertEquals(ClientKind.PLAIN, select(params("svy_developer"), false, false)),
				() -> assertEquals(ClientKind.PLAIN, select(params("svy_developer"), false, true)));
		}

		@ParameterizedTest(name = "developerClient={0}")
		@CsvSource({ "true", "false" })
		@DisplayName("no special parameter with a debug handler selects DEBUG")
		void debugWithHandler(boolean hasDeveloperClient)
		{
			assertEquals(ClientKind.DEBUG, select(params(), hasDeveloperClient, true), "no request parameters + debug handler must use the debug client path");
			assertEquals(ClientKind.DEBUG, select(params("unrelated"), hasDeveloperClient, true), "unrelated parameters must not change the choice");
		}

		@ParameterizedTest(name = "developerClient={0}")
		@CsvSource({ "true", "false" })
		@DisplayName("no special parameter without a debug handler (headless) selects PLAIN")
		void plainWithoutHandler(boolean hasDeveloperClient)
		{
			assertEquals(ClientKind.PLAIN, select(params(), hasDeveloperClient, false));
			assertEquals(ClientKind.PLAIN, select(params("unrelated"), hasDeveloperClient, false));
		}
	}

	@Nested
	class ShouldReuseFormPreviewClient
	{
		@Test
		@DisplayName("reuse only when the client exists, is alive and has the same session key")
		void reuseWhenAllHold()
		{
			assertTrue(NGClientWebsocketSessionFactory.shouldReuseFormPreviewClient(true, false, true));
		}

		@Test
		@DisplayName("no existing client (after shutdownExisting, getInstance()==null) creates a new client")
		void noExisting()
		{
			assertFalse(NGClientWebsocketSessionFactory.shouldReuseFormPreviewClient(false, false, true));
			assertFalse(NGClientWebsocketSessionFactory.shouldReuseFormPreviewClient(false, true, false));
		}

		@Test
		@DisplayName("shut down existing client is not reused")
		void shutDown()
		{
			assertFalse(NGClientWebsocketSessionFactory.shouldReuseFormPreviewClient(true, true, true));
		}

		@Test
		@DisplayName("existing client of a different session key is not reused")
		void differentSession()
		{
			assertFalse(NGClientWebsocketSessionFactory.shouldReuseFormPreviewClient(true, false, false));
		}
	}

	@Nested
	class FactoryType
	{
		@Test
		@DisplayName("factory is a named, non-anonymous IWebsocketSessionFactory detectable via instanceof")
		void namedType()
		{
			Object factory = new NGClientWebsocketSessionFactory(() -> null, () -> null);
			assertAll(
				() -> assertInstanceOf(IWebsocketSessionFactory.class, factory),
				() -> assertInstanceOf(NGClientWebsocketSessionFactory.class, factory),
				() -> assertFalse(factory.getClass().isAnonymousClass(), "factory must not be anonymous (SVY-21173 regression guard)"),
				() -> assertEquals("NGClientWebsocketSessionFactory", factory.getClass().getSimpleName()));
		}

		@Test
		@DisplayName("forHeadless() returns a new named factory instance each time")
		void forHeadless()
		{
			NGClientWebsocketSessionFactory first = NGClientWebsocketSessionFactory.forHeadless();
			NGClientWebsocketSessionFactory second = NGClientWebsocketSessionFactory.forHeadless();
			assertAll(
				() -> assertNotNull(first),
				() -> assertFalse(first.getClass().isAnonymousClass()),
				() -> assertNotSame(first, second, "each call must produce a new instance"));
		}

		@Test
		@DisplayName("session class is named (not anonymous) and extends NGClientWebsocketSession")
		void sessionClassNamed() throws ClassNotFoundException
		{
			Class< ? > session = Class.forName(NGClientWebsocketSessionFactory.class.getName() + "$FactorySession");
			assertAll(
				() -> assertFalse(session.isAnonymousClass()),
				() -> assertEquals("FactorySession", session.getSimpleName()),
				() -> assertEquals(NGClientWebsocketSession.class, session.getSuperclass()));
		}
	}
}
