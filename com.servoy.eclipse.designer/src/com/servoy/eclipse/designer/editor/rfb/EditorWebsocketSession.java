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

import java.util.Locale;

import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebObjectSpecificationBuilder;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IClientService;
import org.sablo.websocket.IWindow;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.impl.ClientService;

/**
 * Handle communication with a remote rfb form editor.
 *
 * @author rgansevles
 *
 */
public class EditorWebsocketSession extends BaseWebsocketSession
{
	public static final String EDITOR_ENDPOINT = "editor";
	public static final String EDITOR_SERVICE = "$editorService";

	private static final WebObjectSpecification EDITOR_SERVICE_SPECIFICATION = new WebObjectSpecificationBuilder().withName(EDITOR_SERVICE).withPackageType(
		IPackageReader.WEB_SERVICE).build();

	public EditorWebsocketSession(WebsocketSessionKey sessionKey)
	{
		super(sessionKey);
	}

	// session will be destroyed when editor is closed
	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public boolean checkForWindowActivity()
	{
		return false;
	}

	@Override
	public Locale getLocale()
	{
		return Locale.getDefault();
	}

	@Override
	protected IClientService createClientService(String name)
	{
		if (EDITOR_SERVICE.equals(name))
		{
			return new ClientService(name, EDITOR_SERVICE_SPECIFICATION);
		}
		return super.createClientService(name);
	}

	@Override
	public boolean shouldTest()
	{
		return false;
	}

	@Override
	protected IWindow createWindow(int windowNr, String windowName)
	{
		return new EditorBaseWindow(this, windowNr, windowName);
	}

	@Override
	public String getLogInformation()
	{
		return "";
	}
}
