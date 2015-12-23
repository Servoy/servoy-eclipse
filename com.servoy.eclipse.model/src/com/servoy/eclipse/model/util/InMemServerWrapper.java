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

package com.servoy.eclipse.model.util;

import java.util.ArrayList;
import java.util.List;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.DataSourceUtils;

/**
 * @author jcompagner
 * @since 8.1
 */
public class InMemServerWrapper implements IDataSourceWrapper
{
	private final String tablename;

	public InMemServerWrapper()
	{
		this.tablename = null;
	}

	public InMemServerWrapper(String tablename)
	{
		this.tablename = tablename;
	}

	@Override
	public String getDataSource()
	{
		return DataSourceUtils.createInmemDataSource(tablename);
	}

	public String getTableName()
	{
		return tablename;
	}

	public String getServerName()
	{
		return DataSourceUtils.INMEM_DATASOURCE;
	}

	public List<String> getTableNames()
	{
		List<String> names = new ArrayList<>();
		try
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				names.addAll(servoyProject.getMemServer().getTableNames(false));
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return names;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		InMemServerWrapper other = (InMemServerWrapper)obj;
		if (tablename == null)
		{
			if (other.tablename != null) return false;
		}
		else if (!tablename.equals(other.tablename)) return false;
		return true;
	}

	@Override
	public int hashCode()
	{
		return tablename == null ? 1 : tablename.hashCode();
	}
}
