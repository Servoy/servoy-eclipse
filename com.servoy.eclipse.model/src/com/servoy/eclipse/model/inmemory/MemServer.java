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

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITransactionConnection;

/**
 * @author gganea
 *
 */
public class MemServer implements IServerInternal, IServer
{
	private final Map<String, ITable> tables = new HashMap<String, ITable>();
	private volatile ISequenceProvider sequenceManager;
	private final ServerConfig serverConfig;
	private final List<ITableListener> tableListeners = new ArrayList<ITableListener>();

	/**
	 *
	 */
	public MemServer()
	{
		this.serverConfig = new ServerConfig(DataSourceUtils.INMEM_DATASOURCE, "", "", "", null, "", "", null, true, true, "");
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#flushTables(java.util.List)
	 */
	@Override
	public void flushTables(List<ITable> tabelList) throws RepositoryException
	{

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createNewTable(com.servoy.j2db.persistence.IValidateName, java.lang.String)
	 */
	@Override
	public ITable createNewTable(IValidateName validator, String tableName) throws RepositoryException
	{
		if (!tables.containsKey(tableName))
		{
			ITable table = new MemTable(tableName);
			tables.put(tableName, table);
			table.setExistInDB(true);
		}
		return tables.get(tableName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createNewTable(com.servoy.j2db.persistence.IValidateName, java.lang.String, boolean)
	 */
	@Override
	public ITable createNewTable(IValidateName validator, String nm, boolean testSQL) throws RepositoryException
	{
		return createNewTable(validator, nm);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createNewTable(com.servoy.j2db.persistence.IValidateName, com.servoy.j2db.persistence.Table)
	 */
	@Override
	public ITable createNewTable(IValidateName validator, ITable otherServerTable) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#reloadTables()
	 */
	@Override
	public void reloadTables() throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#hasTable(java.lang.String)
	 */
	@Override
	public boolean hasTable(String tableName) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return false;
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#syncTableObjWithDB(com.servoy.j2db.persistence.ITable, boolean, boolean,
	 * com.servoy.j2db.persistence.Table)
	 */
	@Override
	public String[] syncTableObjWithDB(ITable table, boolean createMissingServoySequences, boolean createMissingDBSequences, Table templateTable)
		throws RepositoryException, SQLException
	{
		DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
		Iterator<Column> it = table.getColumns().iterator();
		while (it.hasNext())
		{
			Column c = it.next();
			c.setExistInDB(true);
			if (c.getColumnInfo() == null && dataModelManager != null) dataModelManager.createNewColumnInfo(c, createMissingServoySequences); // Use supplied sequence info, don't assume anything!!
		}

		updateAllColumnInfo(table);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#syncWithExternalTable(java.lang.String, com.servoy.j2db.persistence.Table)
	 */
	@Override
	public void syncWithExternalTable(String tableName, Table externalTable) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#syncColumnSequencesWithDB(com.servoy.j2db.persistence.Table)
	 */
	@Override
	public void syncColumnSequencesWithDB(ITable t) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getServerURL()
	 */
	@Override
	public String getServerURL()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getConfig()
	 */
	@Override
	public ServerConfig getConfig()
	{
		return serverConfig;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#removeTable(com.servoy.j2db.persistence.ITable)
	 */
	@Override
	public String[] removeTable(ITable t) throws SQLException, RepositoryException
	{
		if (t != null)
		{
			tables.remove(t.getName());
			fireTablesRemoved(new ITable[] { t }, true);
		}
		return null;
	}


	protected void fireTablesRemoved(ITable removedTables[], boolean deleted)
	{
		if (removedTables.length == 0) return;

		for (int i = 0; i < tableListeners.size(); i++)
		{
			(tableListeners.get(i)).tablesRemoved(this, removedTables, deleted);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#removeTable(java.lang.String)
	 */
	@Override
	public void removeTable(String tableName) throws SQLException, RepositoryException
	{
		ITable remove = tables.remove(tableName);
		fireTablesRemoved(new ITable[] { remove }, true);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#testConnection(int)
	 */
	@Override
	public void testConnection(int i) throws Exception
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#flagValid()
	 */
	@Override
	public void flagValid()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#flagInvalid()
	 */
	@Override
	public void flagInvalid()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getState()
	 */
	@Override
	public int getState()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#fireStateChanged(int, int)
	 */
	@Override
	public void fireStateChanged(int oldState, int state)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#checkIfTableExistsInDatabase(java.sql.Connection, java.lang.String)
	 */
	@Override
	public boolean checkIfTableExistsInDatabase(Connection connection, String tableName)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#updateColumnInfo(com.servoy.j2db.query.QueryColumn)
	 */
	@Override
	public boolean updateColumnInfo(QueryColumn queryColumn) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#updateAllColumnInfo(com.servoy.j2db.persistence.Table)
	 */
	@Override
	public void updateAllColumnInfo(ITable table) throws RepositoryException
	{
		String tname = table.getName();
//		boolean createColumnInfo = true;
//		if (tname.toUpperCase().startsWith(IServer.SERVOY_UPPERCASE_PREFIX) &&
//			!(SERVOY_GROUPS.equals(tname) || SERVOY_GROUP_ELEMENTS.equals(tname) || SERVOY_USER_GROUPS.equals(tname) || SERVOY_USERS.equals(tname)))
//		{
//			createColumnInfo = false;
//		}

		//if (createColumnInfo && !tname.toUpperCase().startsWith(TEMP_UPPERCASE_PREFIX))
		{
			DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
			if (dataModelManager != null)
			{
				//always save all columninfo
				dataModelManager.updateAllColumnInfo(table);
			}
			table.updateDataproviderIDsIfNeeded();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#refreshTable(java.lang.String)
	 */
	@Override
	public void refreshTable(String name) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#removeTableListener(com.servoy.j2db.persistence.ITableListener)
	 */
	@Override
	public void removeTableListener(ITableListener tableListener)
	{
		this.tableListeners.remove(tableListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#addTableListener(com.servoy.j2db.persistence.ITableListener)
	 */
	@Override
	public void addTableListener(ITableListener tableListener)
	{
		this.tableListeners.add(tableListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getNextSequence(java.lang.String, java.lang.String)
	 */
	@Override
	public Object getNextSequence(String tableName, String columnName) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#supportsSequenceType(int, com.servoy.j2db.persistence.Column)
	 */
	@Override
	public boolean supportsSequenceType(int i, Column column) throws Exception
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#toHTML()
	 */
	@Override
	public String toHTML()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getServerManager()
	 */
	@Override
	public IServerManagerInternal getServerManager()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getName()
	 */
	@Override
	public String getName()
	{
		return DataSourceUtils.INMEM_DATASOURCE;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getViewNames(boolean)
	 */
	@Override
	public List<String> getViewNames(boolean hideTempViews) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getTableNames(boolean)
	 */
	@Override
	public List<String> getTableNames(boolean hideTempTables) throws RepositoryException
	{
		return Arrays.asList(tables.keySet().toArray(new String[tables.size()]));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getTableAndViewNames(boolean)
	 */
	@Override
	public List<String> getTableAndViewNames(boolean hideTemporary) throws RepositoryException
	{
		return Arrays.asList(tables.keySet().toArray(new String[tables.size()]));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getTable(java.lang.String)
	 */
	@Override
	public ITable getTable(String tableName) throws RepositoryException
	{

		ITable iTable = tables.get(tableName);
		if (iTable != null)
		{
			initTableIfNecessary(iTable);
		}
		return iTable;
	}

	/**
	 * @param iTable
	 */
	private void initTableIfNecessary(ITable iTable)
	{
		try
		{
			if (!iTable.isInitialized())
			{
				DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
				dataModelManager.loadAllColumnInfo(iTable);
				iTable.setInitialized(true);
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}

	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createRepositoryTables()
	 */
	@Override
	public IRepository createRepositoryTables() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getRepository()
	 */
	@Override
	public IRepository getRepository() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getRepositoryTable(java.lang.String)
	 */
	@Override
	public Table getRepositoryTable(String name) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createNewTable(com.servoy.j2db.persistence.IValidateName, com.servoy.j2db.persistence.Table,
	 * java.lang.String)
	 */
	@Override
	public Table createNewTable(IValidateName nameValidator, ITable selectedTable, String tableName) throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#isTableListLoaded()
	 */
	@Override
	public boolean isTableListLoaded()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#isTableLoaded(java.lang.String)
	 */
	@Override
	public boolean isTableLoaded(String tableName)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#reloadTableColumnInfo(com.servoy.j2db.persistence.Table)
	 */
	@Override
	public void reloadTableColumnInfo(ITable t) throws RepositoryException
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#reloadServerInfo()
	 */
	@Override
	public void reloadServerInfo()
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#isValid()
	 */
	@Override
	public boolean isValid()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getConnection()
	 */
	@Override
	public ITransactionConnection getConnection() throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getUnmanagedConnection()
	 */
	@Override
	public ITransactionConnection getUnmanagedConnection() throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getSQLQuerySet(com.servoy.j2db.query.ISQLQuery, java.util.ArrayList, int, int, boolean)
	 */
	@Override
	public QuerySet getSQLQuerySet(ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve, boolean forceQualifyColumns)
		throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getMissingDBSequences(com.servoy.j2db.persistence.Table)
	 */
	@Override
	public String[] getMissingDBSequences(ITable table) throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createMissingDBSequences(com.servoy.j2db.persistence.ITable)
	 */
	@Override
	public String[] createMissingDBSequences(ITable table) throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getDialectClassName()
	 */
	@Override
	public String getDialectClassName()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getDataSource()
	 */
	@Override
	public DataSource getDataSource() throws Exception
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getIndexDropString(java.sql.Connection, com.servoy.j2db.persistence.Table, java.lang.String)
	 */
	@Override
	public String getIndexDropString(Connection connection, Table t, String indexName) throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getIndexCreateString(java.sql.Connection, com.servoy.j2db.persistence.Table, java.lang.String,
	 * com.servoy.j2db.persistence.Column[], boolean)
	 */
	@Override
	public String getIndexCreateString(Connection connection, Table t, String indexName, Column[] indexColumns, boolean unique) throws SQLException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getRawConnection()
	 */
	@Override
	public Connection getRawConnection() throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#setTableMarkedAsHiddenInDeveloper(java.lang.String, boolean)
	 */
	@Override
	public void setTableMarkedAsHiddenInDeveloper(String tableName, boolean hiddenInDeveloper)
	{
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#isTableMarkedAsHiddenInDeveloper(java.lang.String)
	 */
	@Override
	public boolean isTableMarkedAsHiddenInDeveloper(String tableName)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getTableAndViewNames(boolean, boolean)
	 */
	@Override
	public List<String> getTableAndViewNames(boolean hideTempTables, boolean hideHiddenInDeveloper) throws RepositoryException
	{
		return Arrays.asList(tables.keySet().toArray(new String[tables.size()]));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getLogTable()
	 */
	@Override
	public Table getLogTable() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createLogTable()
	 */
	@Override
	public Table createLogTable() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getClientStatsTable()
	 */
	@Override
	public Table getClientStatsTable() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createClientStatsTable()
	 */
	@Override
	public Table createClientStatsTable() throws RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getTableBySqlname(java.lang.String)
	 */
	@Override
	public ITable getTableBySqlname(String tableSQLName) throws RepositoryException, RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getInitializedTables()
	 */
	@Override
	public Map<String, ITable> getInitializedTables() throws RepositoryException, RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getTableType(java.lang.String)
	 */
	@Override
	public int getTableType(String tableName) throws RepositoryException, RemoteException
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getDatabaseProductName()
	 */
	@Override
	public String getDatabaseProductName() throws RepositoryException, RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getQuotedIdentifier(java.lang.String, java.lang.String)
	 */
	@Override
	public String getQuotedIdentifier(String tableSqlName, String columnSqlName) throws RepositoryException, RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getDataModelClonesFrom()
	 */
	@Override
	public String[] getDataModelClonesFrom() throws RemoteException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getSequenceProvider()
	 */
	@Override
	public ISequenceProvider getSequenceProvider()
	{
		return sequenceManager;
	}

	/**
	 * @param sequenceManager the sequenceManager to set
	 */
	public void setSequenceProvider(ISequenceProvider sequenceManager)
	{
		this.sequenceManager = sequenceManager;
	}

}
