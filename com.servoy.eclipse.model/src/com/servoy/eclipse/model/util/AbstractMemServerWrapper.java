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

package com.servoy.eclipse.model.util;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.inmemory.AbstractMemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author emera
 *
 */
public abstract class AbstractMemServerWrapper implements IDataSourceWrapper
{

	protected final String tablename;

	public AbstractMemServerWrapper(String tablename)
	{
		this.tablename = tablename;
	}

	public String getTableName()
	{
		return tablename;
	}

	public Collection<String> getTableNames()
	{
		Set<String> names = new TreeSet<>();
		try
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				names.addAll(getServer(servoyProject).getTableNames(false));
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return names;
	}

	protected abstract AbstractMemServer< ? > getServer(ServoyProject servoyProject);

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		AbstractMemServerWrapper other = (AbstractMemServerWrapper)obj;
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

	public abstract String getLabel();
}