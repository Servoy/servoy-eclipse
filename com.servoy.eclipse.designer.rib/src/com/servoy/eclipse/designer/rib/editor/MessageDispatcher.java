/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.designer.rib.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IMessageHandler;

/**
 * Broadcast messages to all handlers registered to the same subject
 * 
 * @author rgansevles
 *
 */
public class MessageDispatcher
{
	public static MessageDispatcher INSTANCE = new MessageDispatcher();

	private MessageDispatcher()
	{
	}

	private final Map<String, List<IMessageHandler>> handlers = new HashMap<String, List<IMessageHandler>>();

	public void register(String id, IMessageHandler handler)
	{
		synchronized (handlers)
		{
			List<IMessageHandler> registered = handlers.get(id);
			if (registered == null)
			{
				handlers.put(id, registered = new ArrayList<IMessageHandler>(2));
			}
			if (!registered.contains(handler))
			{
				registered.add(handler);
			}
		}
	}

	public void deregister(String id, IMessageHandler handler)
	{
		synchronized (handlers)
		{
			List<IMessageHandler> registered = handlers.get(id);
			if (registered != null)
			{
				registered.remove(handler);
				if (registered.size() == 0)
				{
					handlers.remove(id);
				}
			}
		}
	}

	public void sendMessage(String id, String message, IMessageHandler sender)
	{
		List<IMessageHandler> toSend = new ArrayList<IMessageHandler>(1);
		synchronized (handlers)
		{
			List<IMessageHandler> registered = handlers.get(id);
			if (registered != null)
			{
				for (IMessageHandler h : registered)
				{
					if (!h.equals(sender))
					{
						toSend.add(h);
					}
				}
			}
		}
		for (IMessageHandler h : toSend)
		{
			try
			{
				h.messageReceived(message);
			}
			catch (Exception e)
			{
				Debug.error("Error sending message '" + message + "' for id '" + id + "' to handler " + h, e);
			}
		}
	}
}
