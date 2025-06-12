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


import static java.util.Collections.emptyList;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.json.JSONObject;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.dataprocessing.TableFilter;
import com.servoy.j2db.dataprocessing.datasource.JSConnectionDefinition;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnName;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Procedure;
import com.servoy.j2db.persistence.QuerySet;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableChangeHandler;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.query.ISQLQuery;
import com.servoy.j2db.query.QueryColumn;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.util.SQLKeywords;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ITransactionConnection;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author emera
 */
public abstract class AbstractMemServer<T extends ITable> implements IServerInternal, IServer
{
	private final Map<String, T> tables = new HashMap<>();
	private final ServerConfig serverConfig;
	private final ServoyProject servoyProject;
	private final String datasource;
	private final String scheme;

	/**
	 * @param servoyProject
	 * @param solution
	 *
	 */
	public AbstractMemServer(ServoyProject servoyProject, String datasource, String scheme)
	{
		this.servoyProject = servoyProject;
		this.datasource = datasource;
		this.scheme = scheme;
		this.serverConfig = new ServerConfig.Builder()
			.setServerName("")
			.setUserName("")
			.setPassword("")
			.setServerUrl("")
			.setSkipSysTables(true)
			.setDriver("")
			.build();
		init();
	}

	public void init()
	{
		tables.clear();
		Iterator<IPersist> tableNodes = servoyProject.getSolution().getObjects(IRepository.TABLENODES);
		while (tableNodes.hasNext())
		{
			TableNode tableNode = (TableNode)tableNodes.next();
			if (tableNode.getDataSource().startsWith(datasource))
			{
				loadTable(tableNode);
			}
		}
	}

	/**
	 * @param tableNode
	 * @param property
	 */
	public void loadTable(TableNode tableNode)
	{
		Object property = tableNode.getProperty(IContentSpecConstants.PROPERTY_COLUMNS);
		if (property != null)
		{
			try
			{
				ITable table = createNewTable(null, tableNode.getDataSource().substring(scheme.length()));
				DatabaseUtils.deserializeInMemoryTable(ApplicationServerRegistry.get().getDeveloperRepository(), table, (ServoyJSONObject)property);
				table.setExistInDB(true);
				table.setInitialized(true);
				tableNode.setTable(table);
				IPersist editingTableNode = servoyProject.getEditingPersist(tableNode.getUUID());
				if (editingTableNode != null)
				{
					((TableNode)editingTableNode).setTable(table);
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public ServoyProject getServoyProject()
	{
		return servoyProject;
	}

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
	public T createNewTable(IValidateName validator, String tableName) throws RepositoryException
	{
		if (!tables.containsKey(tableName))
		{
			T table = createTable(tableName);
			tables.put(tableName, table);
		}
		return tables.get(tableName);
	}

	@Override
	public ITable createNewTable(IValidateName validator, String nm, boolean testSQL, boolean fireTableAdded) throws RepositoryException
	{
		return createNewTable(validator, nm, testSQL);
	}


	protected abstract T createTable(String tableName);


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createNewTable(com.servoy.j2db.persistence.IValidateName, java.lang.String, boolean)
	 */
	@Override
	public T createNewTable(IValidateName validator, String nm, boolean testSQL) throws RepositoryException
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
		return tables.containsKey(tableName);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#syncTableObjWithDB(com.servoy.j2db.persistence.ITable, boolean, boolean,
	 * com.servoy.j2db.persistence.Table)
	 */
	@Override
	public String[] syncTableObjWithDB(ITable table, boolean createMissingServoySequences, boolean createMissingDBSequences)
		throws RepositoryException, SQLException
	{
		DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
		for (Column c : table.getColumns())
		{
			if (c.getColumnInfo() == null && dataModelManager != null) dataModelManager.createNewColumnInfo(c, createMissingServoySequences); // Use supplied sequence info, don't assume anything!!
		}

		updateAllColumnInfo(table);
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#syncTableObjWithDB(com.servoy.j2db.persistence.Table, java.sql.Connection, boolean)
	 */
	@Override
	public String[] syncTableObjWithDB(Table table, Connection connection, boolean createMissingDBSequences) throws SQLException, RepositoryException
	{
		return syncTableObjWithDB(table, createMissingDBSequences, createMissingDBSequences);
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

	@Override
	public ServerConfig getConfig()
	{
		return serverConfig;
	}

	@Override
	public ServerSettings getSettings()
	{
		return ServerSettings.DEFAULT;
	}

	@Override
	public boolean dropTable(Table t) throws SQLException, RepositoryException
	{
		return false;
	}


	public String[] removeTable(ITable t) throws SQLException, RepositoryException
	{
		return removeTable(t, null);
	}

	@Override
	public String[] removeTable(Table t, Connection connection) throws SQLException, RepositoryException
	{
		return removeTable(t);
	}

	public String[] removeTable(ITable t, List<String> nodesToDelete) throws RepositoryException
	{
		if (t != null)
		{
			// first delete all tablenodes representing this table.
			IServoyModel sm = ServoyModelFinder.getServoyModel();
			FlattenedSolution fs = sm.getFlattenedSolution();
			Iterator<TableNode> tableNodes = fs.getTableNodes(t);
			while (tableNodes.hasNext())
			{
				TableNode tn = tableNodes.next();
				if (nodesToDelete != null)
				{
					// only delete the nodes that are given or the tablenode of the table itself.
					if (nodesToDelete.contains(tn.getRootObject().getName()) ||
						(tn.getColumns() != null && tn.getRootObject().equals(servoyProject.getSolution())))
					{
						deletePersist(tn);
					}
				}
				else if (tn.getColumns() == null || tn.getRootObject().equals(servoyProject.getSolution()))
				{
					// skip the one that has columns definition but is not of this project/solution
					deletePersist(tn);
				}
			}

			tables.remove(t.getName());
			fireTablesRemoved(new ITable[] { t }, true);
		}
		return null;
	}

	public String[] renameTable(ITable t, String newName) throws SQLException, RepositoryException
	{
		return renameTable(t, newName, null);
	}

	public String[] renameTable(ITable t, String newName, List<String> nodesToRename) throws RepositoryException, SQLException
	{
		if (t != null)
		{
			ITable newTable = createNewTable(null, t, newName);
			syncTableObjWithDB(newTable, false, true);

			// rename all tablenodes representing this table.
			IServoyModel sm = ServoyModelFinder.getServoyModel();
			FlattenedSolution fs = sm.getFlattenedSolution();
			Iterator<TableNode> tableNodes = fs.getTableNodes(t);
			while (tableNodes.hasNext())
			{
				TableNode tn = tableNodes.next();
				if (nodesToRename != null)
				{
					if (nodesToRename.contains(tn.getRootObject().getName()) ||
						(tn.getColumns() != null && tn.getRootObject().equals(servoyProject.getSolution())))
					{
						renameTableNode(t, newTable, newName, tn);
					}
				}
				else if (tn.getColumns() == null || tn.getRootObject().equals(servoyProject.getSolution()))
				{
					// skip the one that has columns definition but is not of this project/solution
					renameTableNode(t, newTable, newName, tn);
				}
			}
			initTableIfNecessary(newTable);
			tables.remove(t.getName());
			fireTablesRemoved(new ITable[] { t }, true);
		}
		return null;
	}

	protected void renameTableNode(ITable t, ITable newTable, String newName, TableNode tn)
	{
		try
		{
			IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(
				new Path(servoyProject.getSolution() + "/" + SolutionSerializer.DATASOURCES_DIR_NAME + "/" + datasource + "/"));
			IResource[] resources = folder.members();
			for (IResource res : resources)
			{
				if (res instanceof IFile && res.getName().equals(t.getName() + ".tbl") ||
					res instanceof IFile && res.getName().equals(t.getName() + "_calculations.js") ||
					res instanceof IFile && res.getName().equals(t.getName() + "_entity.js"))
				{
					IFile file = (IFile)res;
					InputStream is = file.getContents();
					String content = IOUtils.toString(is, "UTF-8");
					is.close();
					if (tn.getColumns() != null && file.getName().equals(t.getName() + ".tbl"))
					{
						ServoyJSONObject json = new ServoyJSONObject(content, false);
						json.getJSONObject("columns").put("name", newName);
						json.put("dataSource", newTable.getDataSource());
						content = json.toString(false);
					}

					IFile newFile = folder.getFile(file.getName().replace(t.getName(), newName));
					if (!newFile.exists())
					{
						newFile.create(new ByteArrayInputStream(content.getBytes()), IResource.NONE, new NullProgressMonitor());
					}
					else
					{
						ServoyLog.logInfo("Could not rename table node " + tn.getTableName() + ". File " + newFile.getName() + " already exists.");
					}
				}
			}
			deletePersist(tn);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not rename tablenode " + tn.getDataSource(), e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServer#getTenantColumns()
	 */
	@Override
	public List<ColumnName> getTenantColumns() throws RepositoryException, RemoteException
	{
		return emptyList();
	}

	private void deletePersist(TableNode persist)
	{
		IRootObject rootObject = persist.getRootObject();

		if (rootObject instanceof Solution)
		{
			EclipseRepository repository = (EclipseRepository)rootObject.getRepository();
			try
			{
				TableNode editingNode = (TableNode)servoyProject.getEditingPersist(persist.getUUID());
				repository.deleteObject(editingNode);
				servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true, false);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}


	protected void fireTablesRemoved(ITable removedTables[], boolean deleted)
	{
		TableChangeHandler.getInstance().fireTablesRemoved(this, removedTables, deleted);
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
		if (remove != null)
		{
			fireTablesRemoved(new ITable[] { remove }, true);
		}
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

	@Override
	public void refreshTable(String name) throws RepositoryException
	{
		T table = tables.remove(name);
		if (table != null)
		{
			Iterator<TableNode> tableNodes = servoyProject.getSolution().getTableNodes(table.getDataSource());
			if (tableNodes.hasNext())
			{
				loadTable(tableNodes.next());
			}
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#removeTableListener(com.servoy.j2db.persistence.ITableListener)
	 */
	@Override
	public void removeTableListener(ITableListener tableListener)
	{
		TableChangeHandler.getInstance().remove(this, tableListener);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#addTableListener(com.servoy.j2db.persistence.ITableListener)
	 */
	@Override
	public void addTableListener(ITableListener tableListener)
	{
		TableChangeHandler.getInstance().add(this, tableListener);
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
		return datasource;
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
		ArrayList<String> names = new ArrayList<>();
		for (T table : tables.values())
		{
			if (table.getExistInDB()) names.add(table.getName());
		}
		return names;
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
	public T getTable(String tableName) throws RepositoryException
	{
		T iTable = tables.get(tableName);
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


	@Override
	public Collection<Procedure> getProcedures() throws RepositoryException, RemoteException
	{
		return Collections.emptySet();
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
	public ITable createNewTable(IValidateName validator, ITable selectedTable, String tableName) throws RepositoryException
	{
		if (tables.containsKey(tableName))
		{
			throw new RepositoryException("A table with name " + tableName + " already exists");
		}
		if (SQLKeywords.checkIfKeyword(tableName, getDatabaseType()))
		{
			throw new RepositoryException("The name " + tableName + " is an reserved sql word");
		}
		T table = createTable(tableName);
		tables.put(tableName, table);

		DataModelManager dmm = DataModelManager.getColumnInfoManager(ApplicationServerRegistry.get().getServerManager());
		for (Column c : selectedTable.getColumns())
		{
			ColumnInfo columnInfo = c.getColumnInfo();
			Column newColumn = table.createNewColumn(validator, c.getSQLName(), c.getColumnType(), c.getAllowNull(),
				(c.getFlags() & IBaseColumn.PK_COLUMN) != 0);
			if (columnInfo != null)
			{
				dmm.createNewColumnInfo(newColumn, false);
				newColumn.getColumnInfo().copyFrom(columnInfo);
				newColumn.setColumnInfo(newColumn.getColumnInfo()); // update some members of the Column if they were changed in column info
			}
		}
		updateAllColumnInfo(table);
		return table;
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
	public QuerySet getSQLQuerySet(ISQLQuery sqlQuery, ArrayList<TableFilter> filters, int startRow, int rowsToRetrieve, boolean forceQualifyColumns,
		boolean disableUseArrayForIn)
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
 * @see com.servoy.j2db.persistence.IServerInternal#getIndexDropString(com.servoy.j2db.persistence.ITable, java.lang.String)
 */
	@Override
	public String getIndexDropString(ITable t, String indexName) throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#getIndexCreateString(com.servoy.j2db.persistence.ITable, java.lang.String,
	 * com.servoy.j2db.persistence.Column[], boolean)
	 */
	@Override
	public String getIndexCreateString(ITable t, String indexName, Column[] indexColumns, boolean unique) throws SQLException, RepositoryException
	{
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#createIndex(com.servoy.j2db.persistence.ITable, java.lang.String, com.servoy.j2db.persistence.Column[],
	 * boolean)
	 */
	@Override
	public String createIndex(ITable table, String indexName, Column[] indexColumns, boolean unique) throws RepositoryException
	{
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
	public void setTableMarkedAsHiddenInDeveloper(ITable table, boolean hiddenInDeveloper)
	{
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#setTableMarkedAsHiddenInDeveloper(java.lang.String, boolean, boolean)
	 */
	@Override
	public void setTableMarkedAsHiddenInDeveloper(ITable table, boolean hiddenInDeveloper, boolean fireTableHidden)
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
	 * @see com.servoy.j2db.persistence.IServerInternal#isTableInvalidInDeveloperBecauseNoPk(java.lang.String)
	 */
	@Override
	public boolean isTableInvalidInDeveloperBecauseNoPk(String tableName)
	{
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerInternal#isTableMarkedAsMetaData(java.lang.String)
	 */
	@Override
	public boolean isTableMarkedAsMetaData(String tableName)
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

	@Override
	public boolean createClientDatasource(JSConnectionDefinition definition)
	{
		return false;
	}

	@Override
	public void dropClientDatasource(String clientId)
	{
	}

	/**Checks if the given memTable is different from the stored property of the TableNode that stores this MemTable
	 * @param abstractMemTable
	 * @return
	 */
	public boolean isChanged(AbstractMemTable abstractMemTable)
	{
		Iterator<TableNode> tableNodes = this.servoyProject.getSolution().getTableNodes(abstractMemTable.getDataSource());
		if (tableNodes.hasNext())
		{
			TableNode next = tableNodes.next();
			JSONObject driveColumns = (JSONObject)next.getPropertiesMap().get(IContentSpecConstants.PROPERTY_COLUMNS);
			DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();
			String mem = dmm.serializeTable(abstractMemTable, false);
			ServoyJSONObject memoryVersion = new ServoyJSONObject(mem, false, false, true);
			return !memoryVersion.equals(driveColumns);
		}
		else return true;
	}

	@Override
	public String getDatabaseType()
	{
		return "inmem";
	}
}