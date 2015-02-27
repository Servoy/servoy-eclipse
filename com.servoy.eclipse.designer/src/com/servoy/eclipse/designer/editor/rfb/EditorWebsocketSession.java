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

import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IClientService;
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

	private static final WebComponentSpecification EDITOR_SERVICE_SPECIFICATION = new WebComponentSpecification(EDITOR_SERVICE, "", EDITOR_SERVICE, null, null,
		"", null);

	public EditorWebsocketSession(String uuid)
	{
		super(uuid);
	}

	@Override
	public boolean isValid()
	{
		// TODO: check if editor is still open?
		return true;
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

}
