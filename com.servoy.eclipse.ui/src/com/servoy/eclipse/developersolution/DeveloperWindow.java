/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.developersolution;

import java.io.IOException;

import org.sablo.specification.property.IBrowserConverterContext;
import org.sablo.websocket.IToJSONWriter;
import org.sablo.websocket.utils.JSONUtils.IToJSONConverter;

import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClientWindow;

/**
 * @author jcompagner
 *
 * @since 2025.06
 *
 */
public class DeveloperWindow extends NGClientWindow
{
	/**
	 * @param session
	 * @param nr
	 * @param name
	 */
	public DeveloperWindow(INGClientWebsocketSession session, int nr, String name)
	{
		super(session, nr, name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.websocket.BaseWindow#sendMessageInternal(org.sablo.websocket.IToJSONWriter, org.sablo.websocket.utils.JSONUtils.IToJSONConverter,
	 * java.lang.Integer)
	 */
	@Override
	protected boolean sendMessageInternal(IToJSONWriter<IBrowserConverterContext> dataWriter, IToJSONConverter<IBrowserConverterContext> converter,
		Integer smsgidOptional) throws IOException
	{
		if (getEndpoint() == null)
		{
			return false; // if endpoint is null, then the developr ngclient doesn't have ui (yet)
		}
		return super.sendMessageInternal(dataWriter, converter, smsgidOptional);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.websocket.BaseWindow#sendOnlyThisMessageInternal(org.sablo.websocket.IToJSONWriter, org.sablo.websocket.utils.JSONUtils.IToJSONConverter)
	 */
	@Override
	protected String sendOnlyThisMessageInternal(IToJSONWriter<IBrowserConverterContext> dataWriter, IToJSONConverter<IBrowserConverterContext> converter)
		throws IOException
	{
		if (getEndpoint() == null)
		{
			return null; // if endpoint is null, then the developr ngclient doesn't have ui (yet)
		}
		return super.sendOnlyThisMessageInternal(dataWriter, converter);
	}

}
