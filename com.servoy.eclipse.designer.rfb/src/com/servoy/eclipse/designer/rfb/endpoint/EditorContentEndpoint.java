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

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.sablo.websocket.WebsocketEndpoint;

import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;

/**
 * WebsocketEndpoint for editor content (running design ngclient)
 * 
 * @author rgansevles
 *
 */

@ServerEndpoint(value = "/rfb/angular/content/websocket/{sessionid}/{windowid}/{solutionName}")
public class EditorContentEndpoint extends WebsocketEndpoint
{
	public EditorContentEndpoint()
	{
		super(WebsocketSessionFactory.DESIGN_ENDPOINT);
	}

	@Override
	@OnOpen
	public void start(Session newSession, @PathParam("sessionid")
	String sessionid, @PathParam("windowid")
	final String windowid, @PathParam("solutionName")
	final String solutionName) throws Exception
	{
		super.start(newSession, sessionid, windowid, solutionName);
	}

	@Override
	@OnMessage
	public void incoming(String msg, boolean lastPart)
	{
		super.incoming(msg, lastPart);
	}

	@Override
	@OnClose
	public void onClose()
	{
		super.onClose();
	}

	@OnError
	public void onError(Throwable t)
	{
		if (t instanceof IOException)
		{
			log.error("IOException happened", t.getMessage()); // TODO if it has no message but has a 'cause' it will not print anything useful
		}
		else
		{
			log.error("IOException happened", t);
		}
	}
}
