/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.model.inmemory;

import com.servoy.j2db.util.DataSourceUtils;

/**
 * @author gganea
 *
 */
public class MemTable extends AbstractMemTable
{

	public MemTable(MemServer memServer, String name)
	{
		super(memServer, name);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getDataSource()
	 */
	@Override
	public String getDataSource()
	{
		return DataSourceUtils.createInmemDataSource(name);
	}

	@Override
	protected String getDataSource(String tableName)
	{
		return DataSourceUtils.createInmemDataSource(tableName);
	}

	public MemServer getParent()
	{
		return (MemServer)memServer;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof MemTable)
		{
			MemTable table = (MemTable)obj;
			return memServer == table.memServer && name.equals(table.name);
		}
		return false;
	}
}
