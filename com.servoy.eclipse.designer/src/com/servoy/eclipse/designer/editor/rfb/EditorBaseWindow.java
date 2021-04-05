/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

import org.sablo.websocket.BaseWindow;
import org.sablo.websocket.ClientSideSpecState;
import org.sablo.websocket.ClientSideWindowState;

/**
 * Window used in form designer editor - in the root part of the designer (with palette etc.) - so NOT in the design client iframe.
 *
 * @author acostescu
 */
public class EditorBaseWindow extends BaseWindow
{

	public EditorBaseWindow(EditorWebsocketSession editorWebsocketSession, int windowNr, String windowName)
	{
		super(editorWebsocketSession, windowNr, windowName);
	}

	@Override
	protected ClientSideWindowState createClientSideWindowState()
	{
		return new ClientSideWindowState(this, new ClientSideSpecState(this)
		{
			@Override
			public void sendAllServiceClientSideSpecs()
			{
				// we are in main window of form editor that currently doesn't even load type code nor does it use client-side types/push to server values for existing services
				// so we do not send anything here to avoid error messages in the editor
			}
			// note - component specs will not be sent anyway as there is no form/container shown in that main window; so no need to block those
		});
	}

}
