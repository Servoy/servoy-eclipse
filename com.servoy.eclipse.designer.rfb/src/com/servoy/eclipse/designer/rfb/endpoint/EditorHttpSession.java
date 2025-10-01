/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import static java.util.UUID.randomUUID;

import java.util.Collections;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;


/**
 * A global 'session' for the editor.
 *
 * @author rgansevles
 *
 */
public class EditorHttpSession implements HttpSession
{
	private static final AtomicReference<EditorHttpSession> instance = new AtomicReference<>();

	private final String id = randomUUID().toString();

	private final ConcurrentMap<String, Object> attributes = new ConcurrentHashMap<>();

	private EditorHttpSession()
	{
	}

	public static EditorHttpSession getInstance()
	{
		return instance.updateAndGet((httpSession) -> httpSession == null ? new EditorHttpSession() : httpSession);
	}

	private static void clearInstance()
	{
		instance.set(null);
	}

	@Override
	public long getCreationTime()
	{
		return 0;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public long getLastAccessedTime()
	{
		return 0;
	}

	@Override
	public ServletContext getServletContext()
	{
		return null;
	}

	@Override
	public void setMaxInactiveInterval(int interval)
	{
	}

	@Override
	public int getMaxInactiveInterval()
	{
		return 0;
	}

	@Override
	public Object getAttribute(String name)
	{
		return attributes.get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames()
	{
		return Collections.enumeration(attributes.keySet());
	}

	@Override
	public void setAttribute(String name, Object value)
	{
		attributes.put(name, value);
	}

	@Override
	public void removeAttribute(String name)
	{
		attributes.remove(name);
	}

	@Override
	public void invalidate()
	{
		clearInstance();
	}

	@Override
	public boolean isNew()
	{
		return false;
	}

}
