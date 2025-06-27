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
 * WebsocketEndpoint for editor content (running design ngclient)
 *
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/rfb/angular/content/websocket/{clientnr}/{windowName}/{windownr}")
public class EditorContentEndpoint extends WebsocketEndpoint
{
	public EditorContentEndpoint()
	{
		super("designer");
	}

	@Override
	@OnOpen
	public void start(Session newSession, @PathParam("clientnr") String clientnr, @PathParam("windowName") final String windowName,
		@PathParam("windownr") final String windownr) throws Exception
	{
		super.start(newSession, clientnr, windowName, windownr);
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
		super.onError(t);
	}
}
