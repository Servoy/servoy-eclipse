package com.servoy.eclipse.ngclient.startup.resourceprovider.plainjunit;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.servoy.eclipse.ngclient.startup.resourceprovider.DeveloperMediaResourcesServlet;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDebugNGClient;
import com.servoy.j2db.server.ngclient.INGApplication;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

import jakarta.servlet.http.HttpServletRequest;

class DeveloperMediaResourcesServletTest
{
	/** exposes the protected getClient and lets the test decide what the regular session lookup returns */
	private static class TestServlet extends DeveloperMediaResourcesServlet
	{
		private static final long serialVersionUID = 1L;

		private final INGClientWebsocketSession session;

		TestServlet(INGClientWebsocketSession session)
		{
			this.session = session;
		}

		@Override
		protected INGClientWebsocketSession getSession(HttpServletRequest request, int clientnr)
		{
			return session;
		}

		IApplication client(HttpServletRequest request, int clientnr)
		{
			return getClient(request, clientnr);
		}
	}

	private MockedStatic<ApplicationServerRegistry> registry;
	private IApplicationServerSingleton server;
	private final HttpServletRequest request = mock(HttpServletRequest.class);

	@BeforeEach
	void setUp()
	{
		registry = mockStatic(ApplicationServerRegistry.class);
		server = mock(IApplicationServerSingleton.class);
		registry.when(ApplicationServerRegistry::get).thenReturn(server);
	}

	@AfterEach
	void tearDown()
	{
		registry.close();
	}

	@Test
	@DisplayName("client of the regular session is returned and the debug client handler is not consulted")
	void sessionClientWins()
	{
		INGApplication sessionClient = mock(INGApplication.class);
		INGClientWebsocketSession session = mock(INGClientWebsocketSession.class);
		when(session.getClient()).thenReturn(sessionClient);

		IApplication result = new TestServlet(session).client(request, 3);

		assertSame(sessionClient, result);
		registry.verify(ApplicationServerRegistry::get, never());
	}

	@Test
	@DisplayName("without a session the debug NG client of the debug client handler is used")
	void fallsBackToDebugNGClient()
	{
		IDebugClientHandler handler = mock(IDebugClientHandler.class);
		IDebugNGClient debugClient = mock(IDebugNGClient.class);
		when(handler.getDebugNGClient()).thenReturn(debugClient);
		when(server.getDebugClientHandler()).thenReturn(handler);

		IApplication result = new TestServlet(null).client(request, 3);

		assertSame(debugClient, result);
	}

	@Test
	@DisplayName("without a session and without a debug client handler there is no client")
	void noHandlerNoClient()
	{
		when(server.getDebugClientHandler()).thenReturn(null);

		assertNull(new TestServlet(null).client(request, 1));
	}
}
