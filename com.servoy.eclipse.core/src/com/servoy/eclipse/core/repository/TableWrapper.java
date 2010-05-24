/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.core.repository;

/**
 * Wrapper to hold table and server name, used for l;azy loading of tables.
 * 
 * @author rob
 * 
 */
public class TableWrapper
{
	private final String serverName;
	private final String tableName;

	public TableWrapper(String serverName, String tableName)
	{
		this.serverName = serverName;
		this.tableName = tableName;
	}

	public String getServerName()
	{
		return serverName;
	}

	public String getTableName()
	{
		return tableName;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((serverName == null) ? 0 : serverName.hashCode());
		result = prime * result + ((tableName == null) ? 0 : tableName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		final TableWrapper other = (TableWrapper)obj;
		if (serverName == null)
		{
			if (other.serverName != null) return false;
		}
		else if (!serverName.equals(other.serverName)) return false;
		if (tableName == null)
		{
			if (other.tableName != null) return false;
		}
		else if (!tableName.equals(other.tableName)) return false;
		return true;
	}

}
