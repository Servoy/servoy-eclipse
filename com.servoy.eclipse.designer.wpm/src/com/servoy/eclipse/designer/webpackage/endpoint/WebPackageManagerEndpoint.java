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


import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.servoy.j2db.util.Debug;

/**
 * WebsocketEndpoint for the Web Package Manager.
 *
 * @author gganea
 *
 */

@ServerEndpoint("/wpm/angular2/websocket")
public class WebPackageManagerEndpoint
{

	private final WebPackagesServiceHandler webPackagesServiceHandler = new WebPackagesServiceHandler();
	private Session session;

	public WebPackageManagerEndpoint()
	{
	}

	@OnOpen
	public void start(Session newSession) throws Exception
	{
		this.session = newSession;
	}

	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		String handleMessage = this.webPackagesServiceHandler.handleMessage(msg);
		if (handleMessage != null && session != null && session.isOpen())
		{
			Future<Void> sendObject = session.getAsyncRemote().sendText(handleMessage);
			try
			{
				sendObject.get();
			}
			catch (InterruptedException | ExecutionException e)
			{
				Debug.log(e);
			}
		}
	}

	@OnClose
	public void onClose(CloseReason closeReason)
	{
		//TODO
	}

	@OnError
	public void onError(Throwable t)
	{
		Debug.error(t);
	}
}
