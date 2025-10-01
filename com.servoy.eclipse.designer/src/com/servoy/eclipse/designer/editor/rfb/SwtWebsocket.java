/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.designer.editor.rfb;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.eclipse.designer.rfb.endpoint.EditorContentEndpoint;
import com.servoy.eclipse.designer.rfb.endpoint.EditorEndpoint;
import com.servoy.j2db.server.ngclient.NGRuntimeWindowManager;
import com.servoy.j2db.util.Debug;

import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Extension;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.MessageHandler.Partial;
import jakarta.websocket.MessageHandler.Whole;
import jakarta.websocket.RemoteEndpoint.Async;
import jakarta.websocket.RemoteEndpoint.Basic;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Fake WebSocket implementation that mimics WebSocket via SWT Browser functions.
 *
 * @author rgansevles
 *
 */
public class SwtWebsocket
{
	private WebsocketEndpoint websocketEndpoint;

	public SwtWebsocket(Browser browser, String uriString, int id, int editorClientnr, int contentClientnr) throws Exception
	{
		Session newSession = new SwtWebSocketSession(browser, uriString, id);
		if (!createAndStartEditorEndpoint(uriString, newSession, editorClientnr) &&
			!createAndStartEditorContentEndpoint(uriString, newSession, contentClientnr))
		{
			throw new IllegalArgumentException("Could not create websocket endpoint for uri '" + uriString + "'");
		}

		browser.addDisposeListener(e -> close());
	}

	private boolean createAndStartEditorEndpoint(String uriString, Session newSession, int editorClientnr) throws Exception
	{
		// expecting ws://localhost:8080/rfb/angular/websocket/{clientnr}
		String[] args = getEndpointArgs(EditorEndpoint.class, uriString);

		if (args == null || args.length != 1)
		{
			return false;
		}

		websocketEndpoint = new EditorEndpoint();
		((EditorEndpoint)websocketEndpoint).start(newSession, String.valueOf(editorClientnr));
		return true;
	}

	private boolean createAndStartEditorContentEndpoint(String uriString, Session newSession, int contentClientnr) throws Exception
	{
		// expecting ws://localhost:8080/rfb/angular/content/websocket/{clientnr}/{windowName}/{windownr}?solution=tst&id=%23editor&f=orders&s=tst&replacewebsocket=true
		String[] args = getEndpointArgs(EditorContentEndpoint.class, uriString);

		if (args == null || args.length != 3)
		{
			return false;
		}

		websocketEndpoint = new EditorContentEndpoint();
		websocketEndpoint.start(newSession, String.valueOf(contentClientnr), args[1], args[2].split("\\?")[0]);
		websocketEndpoint.getWindow().getSession().registerServerService(NGRuntimeWindowManager.WINDOW_SERVICE, new IServerService()
		{
			@Override
			public Object executeMethod(String methodName, JSONObject arguments) throws Exception
			{
				// editor content endpoint session doesn't have a window service so we ignore requests comming from client for that...
				// before adding this, one would get messages in the log file when opening/resizing form editors (because client was trying to send resize call to server side window service) like:
				// INFO [main] org.sablo.websocket.WebsocketEndpoint - Unknown service called from the client: $windowService [ ]

				if ("windowClosing".equals(methodName))
				{
					return Boolean.TRUE;
				} // else the others don't return anything anyway

				return null;
			}
		});
		return true;
	}

	private static String[] getEndpointArgs(Class< ? > cls, String uriString)
	{
		ServerEndpoint annotation = cls.getAnnotation(ServerEndpoint.class);
		if (annotation == null)
		{
			return null;
		}
		String endpointPath = annotation.value();
		//  strip everything off before first argument /xy/y/{args0}/{args1}

		String endpointPrefix = endpointPath.substring(0, endpointPath.indexOf('{'));
		if (uriString.indexOf(endpointPrefix) < 0)
		{
			return null;
		}

		return uriString.substring(uriString.indexOf(endpointPrefix) + endpointPrefix.length()).split("/");
	}

	private void send(String string)
	{
		websocketEndpoint.incoming(string, true);
	}

	private void close()
	{
		websocketEndpoint.onClose(new CloseReason(CloseCodes.NORMAL_CLOSURE, "swt websocket close"));
	}

	public static void installFakeWebSocket(Browser browser, int editorClientnr, int contentClientnr)
	{
		// install fake WebSocket in case browser does not support it
		new BrowserFunction(browser, "SwtWebsocketBrowserFunction", true, new String[0])
		{
			HashMap<String, SwtWebsocket> swtWebsockets = new HashMap<String, SwtWebsocket>();

			@Override
			public Object function(Object[] arguments)
			{
				try
				{
					if (Debug.tracing())
					{
						Debug.trace("SwtWebsocket message: " + Arrays.toString(arguments));
					}

					if (arguments.length >= 1)
					{
						SwtWebsocket swtWebsocket = swtWebsockets.get(arguments[2].toString());
						if ("open".equals(arguments[0]))
						{
							if (swtWebsocket != null)
							{
								swtWebsocket.close();
							}

							swtWebsocket = new SwtWebsocket(browser, ((String)arguments[1]), ((Number)arguments[2]).intValue(), editorClientnr,
								contentClientnr);
							swtWebsockets.put(arguments[2].toString(), swtWebsocket);
						}

						else if ("send".equals(arguments[0]))
						{
							if (swtWebsocket != null)
							{
								swtWebsocket.send(((String)arguments[1]));
							}
						}
					}
				}
				catch (Exception e)
				{
					Debug.error(e);
				}
				return null;
			}
		};
	}

	private static class SwtWebSocketSession implements Session
	{
		private final Basic basicRemote;
		private final String uriString;

		private SwtWebSocketSession(Browser browser, String uriString, int id)
		{
			basicRemote = new SwtWebSocketBasic(browser, id);
			this.uriString = uriString;
		}

		@Override
		public Basic getBasicRemote()
		{
			return basicRemote;
		}

		@Override
		public void addMessageHandler(MessageHandler arg0) throws IllegalStateException
		{
		}

		@Override
		public void close() throws IOException
		{
		}

		@Override
		public void close(CloseReason arg0) throws IOException
		{
		}

		@Override
		public Async getAsyncRemote()
		{
			return null;
		}

		@Override
		public WebSocketContainer getContainer()
		{
			return null;
		}

		@Override
		public String getId()
		{
			return null;
		}

		@Override
		public int getMaxBinaryMessageBufferSize()
		{
			return 0;
		}

		@Override
		public long getMaxIdleTimeout()
		{
			return 0;
		}

		@Override
		public int getMaxTextMessageBufferSize()
		{
			return 0;
		}

		@Override
		public Set<MessageHandler> getMessageHandlers()
		{
			return null;
		}

		@Override
		public List<Extension> getNegotiatedExtensions()
		{
			return null;
		}

		@Override
		public String getNegotiatedSubprotocol()
		{
			return null;
		}

		@Override
		public Set<Session> getOpenSessions()
		{
			return null;
		}

		@Override
		public Map<String, String> getPathParameters()
		{
			return null;
		}

		@Override
		public String getProtocolVersion()
		{
			return null;
		}

		@Override
		public String getQueryString()
		{
			return null;
		}

		@Override
		public Map<String, List<String>> getRequestParameterMap()
		{
			if (uriString.contains("?"))
			{
				Map<String, List<String>> map = new HashMap<String, List<String>>();
				String[] params = uriString.split("\\?")[1].split("&");
				for (String param : params)
				{
					String[] pair = null;
					if ((pair = param.split("=")).length > 1)
					{
						map.put(pair[0], Arrays.asList(new String[] { pair[1] }));
					}
					else
					{
						map.put(param, new ArrayList<String>());
					}
				}
				return map;
			}
			return null;
		}

		@Override
		public URI getRequestURI()
		{
			return null;
		}

		@Override
		public Principal getUserPrincipal()
		{
			return null;
		}

		@Override
		public Map<String, Object> getUserProperties()
		{
			return null;
		}

		@Override
		public boolean isOpen()
		{
			return true;
		}

		@Override
		public boolean isSecure()
		{
			return false;
		}

		@Override
		public void removeMessageHandler(MessageHandler arg0)
		{
		}

		@Override
		public void setMaxBinaryMessageBufferSize(int arg0)
		{
		}

		@Override
		public void setMaxIdleTimeout(long arg0)
		{
		}

		@Override
		public void setMaxTextMessageBufferSize(int arg0)
		{
		}

		@Override
		public <T> void addMessageHandler(Class<T> arg0, Partial<T> arg1) throws IllegalStateException
		{
		}

		@Override
		public <T> void addMessageHandler(Class<T> arg0, Whole<T> arg1) throws IllegalStateException
		{
		}
	}

	private static class SwtWebSocketBasic implements Basic
	{
		private final Browser browser;
		private final int id;

		private SwtWebSocketBasic(Browser browser, int id)
		{
			this.browser = browser;
			this.id = id;
		}

		@Override
		public void sendText(String text) throws IOException
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (!browser.isDisposed())
					{
						browser.execute("SwtWebsocketClient('receive', '" + StringEscapeUtils.escapeEcmaScript(text) + "',null, " + id + ")");
					}
				}
			});
		}

		@Override
		public void sendText(String text, boolean isLast) throws IOException
		{
			sendText(text);
		}

		@Override
		public void flushBatch() throws IOException
		{
		}

		@Override
		public boolean getBatchingAllowed()
		{
			return false;
		}

		@Override
		public void sendPing(ByteBuffer arg0) throws IOException, IllegalArgumentException
		{

		}

		@Override
		public void sendPong(ByteBuffer arg0) throws IOException, IllegalArgumentException
		{
		}

		@Override
		public void setBatchingAllowed(boolean arg0) throws IOException
		{
		}

		@Override
		public OutputStream getSendStream() throws IOException
		{
			return null;
		}

		@Override
		public Writer getSendWriter() throws IOException
		{
			return null;
		}

		@Override
		public void sendBinary(ByteBuffer arg0) throws IOException
		{
		}

		@Override
		public void sendBinary(ByteBuffer arg0, boolean arg1) throws IOException
		{
		}

		@Override
		public void sendObject(Object arg0) throws IOException, EncodeException
		{
		}
	}

}
