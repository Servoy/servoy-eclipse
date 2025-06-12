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

package com.servoy.eclipse.model.inmemory;


import java.util.Collections;
import java.util.List;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.dataprocessing.IndexInfo;
import com.servoy.j2db.persistence.AbstractTable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnChangeHandler;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author emera
 */
public abstract class AbstractMemTable extends AbstractTable
{
	protected final String name;
	protected final AbstractMemServer memServer;
	private boolean existInDB;

	public AbstractMemTable(AbstractMemServer abstractMemServer, String name)
	{
		this.memServer = abstractMemServer;
		this.name = name;
	}

	//TODO
	//TODO
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

	protected String getDataSource(String tableName)
	{
		return DataSourceUtils.createInmemDataSource(tableName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getName()
	 */
	@Override
	public String getName()
	{
		return name;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getCatalog()
	 */
	@Override
	public String getCatalog()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getSchema()
	 */
	@Override
	public String getSchema()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getTableType()
	 */
	@Override
	public int getTableType()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getColumnType(java.lang.String)
	 */
	@Override
	public int getColumnType(String name)
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getSQLName()
	 */
	@Override
	public String getSQLName()
	{
		return name;
	}

	@Override
	public String getServerName()
	{
		return memServer.getName();
	}

	@Override
	public boolean getExistInDB()
	{
		return existInDB;
	}

	@Override
	public void setExistInDB(boolean b)
	{
		existInDB = b;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#acquireWriteLock()
	 */
	@Override
	public void acquireWriteLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeColumn(com.servoy.j2db.persistence.Column)
	 */
	@Override
	public void removeColumn(Column column)
	{
		columns.remove(column.getName());
		fireIColumnRemoved(column);
	}

	private void fireIColumnRemoved(Column c)
	{
		ColumnChangeHandler.getInstance().fireItemRemoved(this, c);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isMarkedAsHiddenInDeveloper()
	 */
	@Override
	public boolean isMarkedAsHiddenInDeveloper()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isMarkedAsMetaData()
	 */
	@Override
	public boolean isMarkedAsMetaData()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setHiddenInDeveloperBecauseNoPk(boolean)
	 */
	@Override
	public void setTableInvalidInDeveloperBecauseNoPk(boolean b)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#isHiddenInDeveloperBecauseNoPk()
	 */
	@Override
	public boolean isTableInvalidInDeveloperBecauseNoPk()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setMarkedAsMetaData(boolean)
	 */
	@Override
	public void setMarkedAsMetaData(boolean selection)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#setMarkedAsHiddenInDeveloperInternal(boolean)
	 */
	@Override
	public void setMarkedAsHiddenInDeveloperInternal(boolean selection)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#releaseWriteLock()
	 */
	@Override
	public void releaseWriteLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#updateDataproviderIDsIfNeeded()
	 */
	@Override
	public void updateDataproviderIDsIfNeeded()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#getPKColumnTypeRowIdentCount()
	 */
	@Override
	public int getPKColumnTypeRowIdentCount()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#acquireReadLock()
	 */
	@Override
	public void acquireReadLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#releaseReadLock()
	 */
	@Override
	public void releaseReadLock()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#removeColumn(java.lang.String)
	 */
	@Override
	public void removeColumn(String columnName)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.ITable#columnDataProviderIDChanged(java.lang.String)
	 */
	@Override
	public void columnDataProviderIDChanged(String oldDataProviderID)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.AbstractTable#validateNewColumn(com.servoy.j2db.persistence.IValidateName, java.lang.String)
	 */
	@Override
	protected void validateNewColumn(IValidateName validator, String colname) throws RepositoryException
	{
		if (columns.containsKey(colname))
		{
			throw new RepositoryException("A column on table " + getName() + "/server " + getServerName() + " with name " + colname + " already exists");
		}
	}

	/**
	 * @param contents
	 */
	public void setColumns(String contents)
	{
		try
		{
			TableNode tableNode = memServer.getServoyProject().getEditingSolution().getOrCreateTableNode(getDataSource(getName()));
			tableNode.setColumns(new ServoyJSONObject(contents, false));
			tableNode.setTable(this);
			setExistInDB(true);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

	}

	@Override
	public List<IndexInfo> getIndexes()
	{
		return Collections.emptyList();
	}


	/**
	 * @return
	 */
	public boolean isChanged()
	{
		return memServer.isChanged(this);
	}

	@Override
	public int hashCode()
	{
		return name.hashCode();
	}

	public ServoyProject getServoyProject()
	{
		return memServer.getServoyProject();
	}
}
