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

package com.servoy.eclipse.designer.rfb.editor;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IMessageHandler;


/**
 * Endpoint for communication between form editor in java and browser.
 * 
 * @author rgansevles
 *
 */
@ServerEndpoint("/formeditor/{editorid}")
public class FormEditorEndpoint
{
	private Session session;

	private MessageHandler handler;

	@OnOpen
	public void onOpen(Session newSession, @PathParam("editorid")
	String editorid)
	{
		this.session = newSession;
		Debug.log("FormEditorEndpoint new session: editorid=" + editorid);
		register(editorid);
	}

	@OnError
	public void onError(Throwable t)
	{
		Debug.error(t);
	}

	@OnClose
	public void onClose()
	{
		deregister();
	}

	private final StringBuilder incommingPartialMessage = new StringBuilder();

	@OnMessage
	public void onMessage(String msg, boolean lastPart)
	{
		String message = msg;
		if (!lastPart)
		{
			incommingPartialMessage.append(message);
			return;
		}
		if (incommingPartialMessage.length() > 0)
		{
			incommingPartialMessage.append(message);
			message = incommingPartialMessage.toString();
			incommingPartialMessage.setLength(0);
		}

		if (message == null)
		{
			Debug.error("empty message!");
			return;
		}

		MessageDispatcher.INSTANCE.sendMessage(handler.getId(), message, handler);
	}

	private void register(String id)
	{
		if (id == null)
		{
			Debug.error("No id to register with");
			return;
		}
		MessageDispatcher.INSTANCE.register(id, handler = new MessageHandler(id));
	}

	private void deregister()
	{
		if (handler != null)
		{
			MessageDispatcher.INSTANCE.deregister(handler.getId(), handler);
			handler = null;
		}
	}

	public class MessageHandler implements IMessageHandler
	{
		private final String id;

		public MessageHandler(String id)
		{
			this.id = id;
		}

		public String getId()
		{
			return id;
		}

		@Override
		public void messageReceived(String message)
		{
			// message received, pass on to websocket client
			session.getAsyncRemote().sendText(message);
		}
	}
}
