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

package com.servoy.eclipse.designer.webpackage.endpoint;


import java.io.IOException;

import com.servoy.j2db.util.Debug;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * WebsocketEndpoint for the Web Package Manager.
 *
 * @author gganea
 *
 */

@ServerEndpoint("/wpm/angular2/websocket")
public class WebPackageManagerEndpoint
{

	private final WebPackagesServiceHandler webPackagesServiceHandler = new WebPackagesServiceHandler(this);
	private Session session;

	public WebPackageManagerEndpoint()
	{
	}

	@OnOpen
	public void start(Session newSession) throws Exception
	{
		this.session = newSession;
	}

	private final StringBuilder incomingPartialMessage = new StringBuilder();

	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		String message = msg;
		if (!lastPart)
		{
			incomingPartialMessage.append(message);
			return;
		}
		if (incomingPartialMessage.length() > 0)
		{
			incomingPartialMessage.append(message);
			message = incomingPartialMessage.toString();
			incomingPartialMessage.setLength(0);
		}
		String handleMessage = this.webPackagesServiceHandler.handleMessage(message);
		send(handleMessage);
	}

	/**
	 * @param message
	 */
	public synchronized void send(String message)
	{
		if (message != null && session != null && session.isOpen())
		{
			try
			{
				session.getBasicRemote().sendText(message);
			}
			catch (IOException e)
			{
				Debug.log(e);
			}
		}
	}

	@OnClose
	public void onClose(CloseReason closeReason)
	{
		webPackagesServiceHandler.dispose();
		session = null;
	}

	@OnError
	public void onError(Throwable t)
	{
		Debug.error(t);
	}
}
