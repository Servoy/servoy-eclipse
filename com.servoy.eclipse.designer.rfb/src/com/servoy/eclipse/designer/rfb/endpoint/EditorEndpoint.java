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

package com.servoy.eclipse.designer.rfb.endpoint;


import java.io.IOException;

import org.sablo.websocket.WebsocketEndpoint;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebsocketEndpoint for the RFB Editor.
 *
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/rfb/{angularversion}/websocket/{clientnr}")
public class EditorEndpoint extends WebsocketEndpoint
{
	public static final String EDITOR_ENDPOINT = "editor";

	public EditorEndpoint()
	{
		super(EDITOR_ENDPOINT);
	}

	@OnOpen
	public void start(Session newSession, @PathParam("clientnr") String clientnr) throws Exception
	{
		super.start(newSession, clientnr, "null", "null");
	}

	@Override
	protected HttpSession getHttpSession(Session session)
	{
		return EditorHttpSession.getInstance();
	}

	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
	}

	@Override
	@OnClose
	public void onClose(CloseReason closeReason)
	{
		super.onClose(closeReason);
	}

	@Override
	@OnError
	public void onError(Throwable t)
	{
		if (t instanceof IOException)
		{
			String msg = t.getMessage();
			// Ignore  "broken pipe" errors, those are fired when websocket is closed by the client (navigate to a different url),
			// then the server gets the close event, but also tries to send a message back to the already closed client, to confirm the close!
			// According to Tomcat this is because: "RFC 6455, section 5.5.1 Close is a two-stage process and closing the TCP connection is the server's responsibility."
			if (msg != null && msg.toLowerCase().indexOf("broken pipe") != -1)
			{
				return;
			}
			log.error("IOException happened", t.getMessage()); // TODO if it has no message but has a 'cause' it will not print anything useful
		}
		else
		{
			log.error("IOException happened", t);
		}
	}
}
