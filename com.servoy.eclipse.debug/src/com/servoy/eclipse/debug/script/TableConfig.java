/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.debug.script;

import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Debug;

/**
 * This class is a small wrapper around a table object so it doesn't have to load it until it is requested.
 *
 * @author jcompagner
 *
 * @since 8.1
 *
 */
public class TableConfig
{

	private final String name;
	private final IServer server;

	/**
	 * @param name
	 * @param server
	 */
	public TableConfig(String name, IServer server)
	{
		this.name = name;
		this.server = server;
	}

	public ITable getTable()
	{
		try
		{
			return server.getTable(name);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return null;
	}

}
