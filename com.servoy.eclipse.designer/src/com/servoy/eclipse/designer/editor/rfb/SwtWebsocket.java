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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.EncodeException;
import javax.websocket.Extension;
import javax.websocket.MessageHandler;
import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.commons.lang.StringEscapeUtils;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.widgets.Display;
import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.util.Debug;

/**
 * Fake WebSocket implementation that mimics WebSocket via SWT Browser functions.
 * 
 * @author rgansevles
 *
 */
public class SwtWebsocket
{
	private final WebsocketEndpoint websocketEndpoint;
	private String endpointType;

	public SwtWebsocket(Browser browser, String uriString) throws Exception
	{
		// @ServerEndpoint(value = "/websocket/{endpointType}/{sessionid}/{windowid}/{argument}")

		String[] split = uriString.split("/");
		if (split.length < 5 || !"websocket".equals(split[split.length - 5]))
		{
			throw new IllegalArgumentException(uriString);
		}

		websocketEndpoint = new WebsocketEndpoint();
		websocketEndpoint.start(new SwtWebSocketSession(browser), //
			endpointType = split[split.length - 4], //
			/* sessionid = */split[split.length - 3], //
			/* windowid = */split[split.length - 2], //
			/* argument = */split[split.length - 1] //
		);
	}

	private void send(String string)
	{
		websocketEndpoint.incoming(string, true);
	}

	private void close()
	{
		websocketEndpoint.onClose(endpointType);
	}

	public static void installFakeWebSocket(final Browser browser)
	{
		// install fake WebSocket in case browser does not support it
		new BrowserFunction(browser, "SwtWebsocketBrowserFunction")
		{
			SwtWebsocket swtWebsocket;

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
						if ("open".equals(arguments[0]))
						{
							if (swtWebsocket != null)
							{
								swtWebsocket.close();
							}

							swtWebsocket = new SwtWebsocket(browser, ((String)arguments[1]));
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

		private SwtWebSocketSession(Browser browser)
		{
			basicRemote = new SwtWebSocketBasic(browser);
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
	}

	private static class SwtWebSocketBasic implements Basic
	{
		private final Browser browser;

		private SwtWebSocketBasic(Browser browser)
		{
			this.browser = browser;
		}

		@Override
		public void sendText(final String text) throws IOException
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					if (!browser.isDisposed())
					{
						browser.execute("SwtWebsocketClient('receive', '" + StringEscapeUtils.escapeJavaScript(text) + "')");
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
