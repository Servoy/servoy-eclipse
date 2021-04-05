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

import java.util.List;
import java.util.Map;

import org.sablo.websocket.BaseWindow;

/**
 * Editor's designer iframe code has to know when a BaseWindow was open due to a browser request.<br/><br/>
 *
 * It needs to know that client side it doesn't have the clientSideSpecs for components anymore... as in form designer iframe a part
 * of sablo is completely bypassed (only template json is sent so ClientSideTypeState cannot handler automatically the component client side specs.
 *
 * @author acostescu
 */
public class DesignerBaseWindow extends BaseWindow
{

	public DesignerBaseWindow(DesignerWebsocketSession designerWebsocketSession, int windowNr, String windowName)
	{
		super(designerWebsocketSession, windowNr, windowName);
	}

	@Override
	public DesignerWebsocketSession getSession()
	{
		return (DesignerWebsocketSession)super.getSession();
	}

	@Override
	public void onOpen(Map<String, List<String>> requestParams)
	{
		super.onOpen(requestParams);
		getSession().handleBrowserWindowRefresh(); // I wonder if we could simply just call this in DesignerWebsocketSession.createWindow(int, String) and remove this class completely
	}

}
