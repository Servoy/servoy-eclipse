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

package com.servoy.eclipse.model.view;

import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * @author emera
 */
public class ViewFoundsetTable extends AbstractMemTable
{
	public ViewFoundsetTable(ViewFoundsetsServer viewServer, String name)
	{
		super(viewServer, name);
	}

	@Override
	public String getDataSource()
	{
		return DataSourceUtils.createViewDataSource(name);
	}

	@Override
	public String getDataSource(String tableName)
	{
		return DataSourceUtils.createViewDataSource(tableName);
	}

	public ViewFoundsetsServer getParent()
	{
		return (ViewFoundsetsServer)memServer;
	}


	@Override
	public boolean equals(Object obj)
	{
		if (obj instanceof ViewFoundsetTable)
		{
			ViewFoundsetTable table = (ViewFoundsetTable)obj;
			return memServer == table.memServer && name.equals(table.name);
		}
		return false;
	}
}
