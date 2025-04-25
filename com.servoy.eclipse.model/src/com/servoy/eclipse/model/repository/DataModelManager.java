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
package com.servoy.eclipse.model.repository;

import static com.servoy.j2db.util.DatabaseUtils.deserializeServerInfo;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.json.JSONArray;
import org.json.JSONException;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.preferences.DbiPreferences;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.UpdateMarkersJob;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInfoManager;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableMetaInfo;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.XMLUtils;
import com.servoy.j2db.util.keyword.Ident;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * This class manages the column information from Eclipse Servoy Resources projects.
 * It reads dbi files into column information, writes dbi files from column information and checks for dbi errors and inconsistencies between dbi files and actual database structure.
 * @author acostescu
 */
public class DataModelManager implements IServerInfoManager
{
	public static final String SECURITY_FILE_EXTENSION = "sec";
	public static final String SECURITY_FILE_EXTENSION_WITH_DOT = '.' + SECURITY_FILE_EXTENSION;
	public static final String SECURITY_DIRECTORY = "security";
	public static final String SECURITY_FILENAME = "security" + SECURITY_FILE_EXTENSION_WITH_DOT;

	public static final String COLUMN_INFO_FILE_EXTENSION = "dbi";
	public static final String COLUMN_INFO_FILE_EXTENSION_WITH_DOT = '.' + COLUMN_INFO_FILE_EXTENSION;

	public static final String TABLE_DATA_FILE_EXTENSION_WITH_DOT = ".data";
	public static final String TEMP_UPPERCASE_PREFIX = "TEMP_"; // tables that are not considered as being 'real'

	private final IProject resourceProject;
	private final IServerManagerInternal sm;

	// this flag is useless when making write operations inside workspace jobs that bundle & delay the resource changed events, but can be used for other cases anyway;
	private IFile writingMarkerFreeDBIFile;

	private UpdateMarkersJob updateMarkersJob;
	private final TableDifferencesHolder differences = new TableDifferencesHolder();
	private final HashMap<String, Long> missingDbiFileMarkerIds = new HashMap<String, Long>();

	private boolean writeDBIFiles = true;
	private final IServerListener serverListener;
	private final ITableListener tableListener;
	private boolean doNotShowProblemMarkers;

	public DataModelManager(IProject resourceProject, IServerManagerInternal sm)
	{
		this.resourceProject = resourceProject;
		this.sm = sm;

		tableListener = new ITableListener.TableListener()
		{
			public void tablesRemoved(IServerInternal server, ITable tables[], boolean deleted)
			{
//				for (ITable iTable : tables)
//				{
//					IFile dbiFile = getDBIFile(server.getName(), iTable.getName());
//					try
//					{
//						dbiFile.delete(false, new NullProgressMonitor());
//					}
//					catch (CoreException e)
//					{
//						Debug.error(e);
//					}
//				}
			}

			@Override
			public void serverStateChanged(IServerInternal server, int oldState, int newState)
			{
				if ((oldState & ITableListener.ENABLED) == 0 && (newState & ITableListener.ENABLED) != 0 && (newState & ITableListener.VALID) != 0)
				{
					// this means that a server that was disabled became enabled and valid - so reload column info to regenerate problem markers if needed
					reloadAllColumnInfo(server);
				}
				else if ((newState & ITableListener.ENABLED) == 0 && (oldState & ITableListener.ENABLED) != 0 && (oldState & ITableListener.VALID) != 0)
				{
					// an enabled, valid server was disabled, so remove any markers it might have
					removeErrorMarkers(server.getName());
				}
			}

			@Override
			public void tableInitialized(Table t)
			{
				for (Column column : t.getColumns())
				{
					if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
						!column.getColumnInfo().hasFlag(IBaseColumn.UUID_COLUMN))
					{
						try
						{
							IFile dbiFile = getDBIFile(t.getServerName(), t.getName());
							if (dbiFile.exists())
							{

								String customSeverity = ServoyBuilder.getSeverity(ServoyBuilder.COLUMN_UUID_FLAG_NOT_SET.getLeft(),
									ServoyBuilder.COLUMN_UUID_FLAG_NOT_SET.getRight().name(),
									ServoyModelFinder.getServoyModel().getActiveProject().getProject());
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ColumnUUIDFlagNotSet.fill(t.getName(), column.getName());
									IMarker marker = dbiFile.createMarker(mk.getType());
									marker.setAttribute(IMarker.MESSAGE, mk.getText());
									marker.setAttribute(IMarker.SEVERITY,
										ServoyBuilder.getTranslatedSeverity(customSeverity, ServoyBuilder.COLUMN_UUID_FLAG_NOT_SET.getRight()));
									marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
									marker.setAttribute(IMarker.LOCATION, "JSON file");
								}
							}
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
			}

		};
		// add listeners to initial server list
		String[] array = sm.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal server = (IServerInternal)sm.getServer(server_name, false, false);
			server.addTableListener(tableListener);
		}

		serverListener = new IServerListener()
		{
			public void serverAdded(IServerInternal s)
			{
				s.addTableListener(tableListener);
				if (s.getConfig().isEnabled() && s.isValid()) reloadAllColumnInfo(s);
			}

			public void serverRemoved(IServerInternal s)
			{
				// remove error markers for server s
				s.removeTableListener(tableListener);
				removeErrorMarkers(s.getName());
			}
		};
		sm.addServerListener(serverListener);
	}

	public void loadAllColumnInfo(final ITable t) throws RepositoryException
	{
		if (t == null || !t.getExistInDB()) return;
		removeErrorMarker(t.getServerName(), t.getName());
		for (Column element : t.getColumns())
		{
			element.removeColumnInfo();
		}
		IServerInternal s = (IServerInternal)sm.getServer(t.getServerName());
		String serverName = t.getServerName();
		if (s != null)
		{
			String parentServerName = s.getConfig().getDataModelCloneFrom();
			if (parentServerName != null)
			{
				serverName = parentServerName;
			}
		}
		IFile file = getDBIFile(serverName, t.getName());
		if (file.exists())
		{
			InputStream is = null;
			try
			{
				is = file.getContents(true);
				String json_table = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				if (s != null && s.getConfig().isEnabled() && s.isValid() && json_table != null)
				{
					deserializeTable(s, t, json_table);
				}
			}
			catch (JSONException e)
			{
				// maybe the .dbi file content is corrupt... add an error marker
				addTableDeserializeErrorMarker(t.getServerName(), t.getName(), e.getMessage());
				throw new RepositoryException(e);
			}
			catch (CoreException e)
			{
				// maybe the .dbi file content is corrupt... add an error marker
				addTableDeserializeErrorMarker(t.getServerName(), t.getName(), e.getMessage());
				throw new RepositoryException(e);
			}
			catch (RepositoryException e)
			{
				// maybe the .dbi file content is corrupt... add an error marker
				addTableDeserializeErrorMarker(t.getServerName(), t.getName(), e.getMessage());
				throw e;
			}
			finally
			{
				if (is != null)
				{
					Utils.closeInputStream(is);
					is = null;
				}
			}
		}
		else
		{
			boolean clonedServerWithoutTableDbiInDeveloper = false;
			if (ApplicationServerRegistry.get().isDeveloperStartup())
			{
				// checking if the server is a clone
				if (s != null && s.getConfig() != null && s.getConfig().getDataModelCloneFrom() != null && s.getConfig().getDataModelCloneFrom().length() != 0)
					clonedServerWithoutTableDbiInDeveloper = true;
			}
			if (!clonedServerWithoutTableDbiInDeveloper)
			{
				addMissingDBIMarker(t.getServerName(), t.getName(), false);
			}
			for (Column c : t.getColumns())
			{
				createNewColumnInfo(c, t.getPKColumnTypeRowIdentCount() == 1);//was missing - create automatic sequences if missing
			}
		}
	}

	public TableMetaInfo getTableMetainfo(IServerInternal server, String tableName)
	{
		if (server == null || tableName == null || !server.getConfig().isEnabled() || !server.isValid()) return TableMetaInfo.DEFAULT; // this should never happen
		IFile file = getDBIFile(server.getConfig().getDataModelCloneFrom() != null ? server.getConfig().getDataModelCloneFrom() : server.getName(), tableName);
		if (file.exists())
		{
			InputStream is = null;
			String errMsg = null;
			try
			{
				is = file.getContents(true);
				String json_table = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				if (json_table != null)
				{
					TableDef tableInfo = DatabaseUtils.deserializeTableInfo(json_table);
					if (tableName.equals(tableInfo.name))
					{
						return new TableMetaInfo(tableInfo.hiddenInDeveloper, Boolean.TRUE.equals(tableInfo.isMetaData));
					}
				}
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
				errMsg = e.getMessage();
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
				errMsg = e.getMessage();
			}
			finally
			{
				// maybe the .dbi file content is corrupt... add an error marker
				if (errMsg != null)
				{
					removeErrorMarker(server.getName(), tableName);
					addTableDeserializeErrorMarker(server.getName(), tableName, errMsg);
				}
				if (is != null)
				{
					Utils.closeInputStream(is);
					is = null;
				}
			}
		}

		return TableMetaInfo.DEFAULT; // file does not exist, is empty or something unexpected happened
	}

	// delete file
	public void removeAllColumnInfo(ITable t) throws RepositoryException
	{
		if (t == null) return;
		IFile file = getDBIFile(t.getServerName(), t.getName());
		if (file.exists())
		{
			writingMarkerFreeDBIFile = file;
			try
			{
				file.delete(true, null);
			}
			catch (CoreException e)
			{
				throw new RepositoryException(e);
			}
			finally
			{
				writingMarkerFreeDBIFile = null;
			}
		}
		else
		{
			// there may be a marker on the resources project saying that the .dbi file for the table that has
			// just been deleted is missing; remove it
			removeErrorMarker(t.getServerName(), t.getName());
		}

		// also remove the data file if it exists
		IFile dataFile = getMetaDataFile(t.getDataSource());
		if (dataFile != null && dataFile.exists())
		{
			try
			{
				dataFile.delete(true, null);
			}
			catch (CoreException e)
			{
				throw new RepositoryException(e);
			}
		}
	}

	// delete item
	public void removeColumnInfo(Column c) throws RepositoryException
	{
//		Table t = c.getTable();
		c.removeColumnInfo();
		// should only write the dbi once, not for every column; make sure updateAllColumnInfo is called at the end
		//updateAllColumnInfo(t);
	}

	public static void reloadAllColumnInfo(IServerInternal server)
	{
		server.reloadServerInfo();
		try
		{
			for (String tableName : server.getTableAndViewNames(true))
			{
				if (server.isTableLoaded(tableName))
				{
					server.reloadTableColumnInfo(server.getTable(tableName));
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void updateAllColumnInfo(final ITable t) throws RepositoryException
	{
		updateAllColumnInfoImpl(t, true);
	}

	private void updateAllColumnInfoImpl(final ITable t, boolean checkForMarkers) throws RepositoryException
	{
		if (t == null || !writeDBIFiles) return;

		// do not write dbi files of clones, just load content from parent server
		IServerInternal server = (IServerInternal)sm.getServer(t.getServerName());
		if (server != null && server.getConfig().getDataModelCloneFrom() != null) return;

		// this optimize would normally be OK - but would block writes when startAsTeamProvider is true... so leave it out for now (until we stop using 2 column info providers)
		// if there are no changes in column info, no use saving them... just to produce (time stamp) outgoing changes in team providers
//		boolean changed = false;
//		for (Column c : t.getColumns())
//		{
//			ColumnInfo ci = c.getColumnInfo();
//			if (ci == null || ci.isChanged()) // null would be strange here - but safer to check
//			{
//				changed = true;
//				break;
//			}
//		}
//		if (!changed) return;

		try
		{
			String out = null;
			try
			{
				// create file contents as string
				t.acquireReadLock();
				out = serializeTable(t, !(t instanceof AbstractMemTable));
			}
			finally
			{
				t.releaseReadLock();
			}
			if (t instanceof AbstractMemTable)
			{
				AbstractMemTable mem = (AbstractMemTable)t;
				mem.setColumns(out);
			}
			else try
			{
				InputStream source = new ByteArrayInputStream(out.getBytes("UTF8"));
				IFile file = getDBIFile(t.getServerName(), t.getName());
				writingMarkerFreeDBIFile = file;
				if (file.exists())
				{
					int differenceType = differences.getDifferenceTypeForTable(t);
					if (checkForMarkers && !doNotShowProblemMarkers) // if doNotShowProblemMarkers == true then (at least when this flag was added) a .servoy import is in progress
					{
						if (differenceType == TableDifference.COLUMN_CONFLICT || differenceType == TableDifference.COLUMN_MISSING_FROM_DB ||
							differenceType == TableDifference.DESERIALIZE_PROBLEM)
						{
							// if the user chose 'Yes' for example, write the contents
							if (!ModelUtils.getUnexpectedSituationHandler().allowUnexpectedDBIWrite(t))
							{
								// disallowed
								return;
							}
						}
					}

					if (differenceType != -1)
					{
						writingMarkerFreeDBIFile = null;
					}

					file.setContents(source, true, false, null);
				}
				else
				{
					writingMarkerFreeDBIFile = null; // do not inhibit reload of dbi file - because we could have error markers saying that the file does not exist - and they need to be cleared
					ResourcesUtils.createFileAndParentContainers(file, source, true);
				}
			}
			catch (UnsupportedEncodingException e)
			{
				throw new RepositoryException(e);
			}
			catch (CoreException e)
			{
				throw new RepositoryException(e);
			}
			finally
			{
				writingMarkerFreeDBIFile = null;
			}
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
	}

	@Override
	public void updateServerSettings(String serverName, ServerSettings serverSettings)
	{
		if (serverName == null || !writeDBIFiles)
		{
			return;
		}

		IFile dbiFile = getServerDBIFile(serverName);
		if (serverSettings == null)
		{
			if (dbiFile.exists())
			{
				try
				{
					dbiFile.delete(false, null);
				}
				catch (CoreException e)
				{
					Debug.error(e);
				}
			}
		}
		else
		{
			String json = DatabaseUtils.serializeServerSettings(serverSettings);
			try
			{
				ResourcesUtils.createOrWriteFileUTF8(dbiFile, json, true);
			}
			catch (CoreException e)
			{
				Debug.error(e);
			}
		}
	}

	@Override
	public ServerSettings loadServerSettings(String serverName) throws RepositoryException
	{
		IFile dbiFile = getServerDBIFile(serverName);
		if (dbiFile.exists())
		{
			try (InputStream is = dbiFile.getContents(true))
			{
				String json = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				return deserializeServerInfo(serverName, json).serverSettings;
			}
			catch (Exception e)
			{
				addServerDeserializeErrorMarker(serverName, e.getMessage());
				throw new RepositoryException(e);
			}
		}

		return ServerSettings.DEFAULT;
	}


	public void testTableAndCreateDBIFile(ITable table)
	{
		if (table == null) return;
		IFile dbiFile = getDBIFile(table.getDataSource());
		if (dbiFile != null && !dbiFile.exists())
		{
			try
			{
				updateAllColumnInfo(table);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public void updateHiddenInDeveloperState(Table t) throws RepositoryException
	{
		if (t == null || !t.getExistInDB()) return;
		InputStream is = null;
		try
		{
			IFile file = getDBIFile(t.getServerName(), t.getName());
			if (file.exists())
			{
				is = file.getContents(true);
				String json_table = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				IServerInternal s = (IServerInternal)sm.getServer(t.getServerName());
				if (s != null && s.getConfig().isEnabled() && s.isValid() && json_table != null)
				{
					String tObj = null;
					try
					{
						t.acquireReadLock();
						TableDef tableInfo = DatabaseUtils.deserializeTableInfo(json_table);
						tableInfo.hiddenInDeveloper = t.isMarkedAsHiddenInDeveloper();
						tObj = serializeTableInfo(tableInfo);
					}
					finally
					{
						t.releaseReadLock();
					}
					InputStream source = new ByteArrayInputStream(tObj.getBytes("UTF8"));
					file.setContents(source, true, false, null);
				}
			}
			else
			{
				if (t.isMarkedAsHiddenInDeveloper())
				{
					String tObj = null;
					try
					{
						t.acquireReadLock();
						tObj = serializeTable(t);
					}
					finally
					{
						t.releaseReadLock();
					}

					InputStream source = new ByteArrayInputStream(tObj.getBytes("UTF8"));
					ResourcesUtils.createFileAndParentContainers(file, source, true);
				}
			}
		}
		catch (JSONException e)
		{
			// maybe the .dbi file content is corrupt... add an error marker
			addTableDeserializeErrorMarker(t.getServerName(), t.getName(), e.getMessage());
			throw new RepositoryException(e);
		}
		catch (CoreException e)
		{
			// maybe the .dbi file content is corrupt... add an error marker
			addTableDeserializeErrorMarker(t.getServerName(), t.getName(), e.getMessage());
			throw new RepositoryException(e);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new RepositoryException(e);
		}
		finally
		{
			if (is != null)
			{
				Utils.closeInputStream(is);
				is = null;
			}
		}
	}

	/**
	 * Will save the dbi file later or now, depending on the parameters.
	 *
	 * @param t the table.
	 * @param writeBackLater specifies whether the column info should be updater now or later.
	 * @throws RepositoryException
	 */
	public void updateAllColumnInfo(final ITable t, boolean writeBackLater) throws RepositoryException
	{
		updateAllColumnInfo(t, writeBackLater, true);
	}

	/**
	 * Will save the dbi file later or now, depending on the parameters.
	 *
	 * @param t the table.
	 * @param writeBackLater specifies whether the column info should be updater now or later.
	 * @throws RepositoryException
	 */
	public void updateAllColumnInfo(final ITable t, boolean writeBackLater, final boolean checkForMarkers) throws RepositoryException
	{
		if (writeBackLater)
		{
			// write them async, because this can be called from a resource change event (that locks the resource tree for writing)
			Job job = new Job("Updating table column information")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					try
					{
						updateAllColumnInfoImpl(t, checkForMarkers);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
					return Status.OK_STATUS;
				}
			};
			job.setRule(resourceProject);
			job.setSystem(true);
			job.schedule();
		}
		else
		{
			updateAllColumnInfoImpl(t, checkForMarkers);
		}
	}

	/**
	 * Returns true if the specified file in the process of being written/deleted, useful for sequential resource change event; false otherwise.
	 * @param file the file to be checked.
	 * @return true if the specified file in the process of being written/deleted, useful for sequential resource change event; false otherwise.
	 */
	public boolean isWritingMarkerFreeDBIFile(IFile file)
	{
		return file.equals(writingMarkerFreeDBIFile);
	}

	private void deserializeTable(IServerInternal s, ITable t, String json_table) throws RepositoryException, JSONException
	{
		int existingColumnInfo = 0;
		TableDef tableInfo = DatabaseUtils.deserializeTableInfo(json_table);
		if (!t.getName().equals(tableInfo.name))
		{
			throw new RepositoryException("Table name does not match dbi file name for " + t.getName());
		}

		List<IColumn> changedColumns = null;

		if (tableInfo.columnInfoDefSet.size() > 0)
		{
			changedColumns = new ArrayList<IColumn>(tableInfo.columnInfoDefSet.size());
			for (ColumnInfoDef cid : tableInfo.columnInfoDefSet)
			{
				String cname = cid.name;
				Column c = t.getColumn(cname);

				if (c != null)
				{
					existingColumnInfo++;
					DatabaseUtils.updateColumnInfo(ApplicationServerRegistry.get().getDeveloperRepository().getNewElementID(null), c, cid);
					changedColumns.add(c);
				}
				addDifferenceMarkersIfNecessary(c, cid, t, cname);
			}
		}

		for (Column c : t.getColumns())
		{
			if (c.getColumnInfo() == null)
			{
				boolean colExists = false; //searching for keyword column name prefixed with '_' character
				for (int j = 0; j < tableInfo.columnInfoDefSet.size() && !colExists; j++)
				{
					ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(j);
					if (c.getName().equals(Ident.RESERVED_NAME_PREFIX + cid.name))
					{
						addDifferenceMarker(new TableDifference(t, c.getName(), TableDifference.COLUMN_MISSING_FROM_DBI_FILE, null, null, true));
						colExists = true;
					}
				}
				if (!colExists)
				{
					addDifferenceMarker(new TableDifference(t, c.getName(), TableDifference.COLUMN_MISSING_FROM_DBI_FILE, null, null));
				}
				// only create servoy sequences when this was a new table and there is only 1 pk column
				createNewColumnInfo(c, existingColumnInfo == 0 && t.getPKColumnTypeRowIdentCount() == 1);//was missing - create automatic sequences if missing
			}
		}

		if (t.getRowIdentColumnsCount() == 0)
		{
			t.setTableInvalidInDeveloperBecauseNoPk(true);
			//s.setTableMarkedAsHiddenInDeveloper(t.getName(), true);
		}
		else s.setTableMarkedAsHiddenInDeveloper(t, tableInfo.hiddenInDeveloper);

		t.setMarkedAsMetaData(Boolean.TRUE.equals(tableInfo.isMetaData));

		// let table editors and so on now that a columns are loaded
		if (changedColumns != null)
		{
			t.fireIColumnsChanged(changedColumns);
		}
	}

	private void addDifferenceMarkersIfNecessary(Column c, ColumnInfoDef cid, ITable t, String columnName)
	{
		if (c == null)
		{
			if (t.getExistInDB())
			{
				if (t.getColumn(Ident.RESERVED_NAME_PREFIX + columnName) != null)
				{
					addDifferenceMarker(new TableDifference(t, columnName, TableDifference.COLUMN_MISSING_FROM_DB, null, cid, true));
				}
				else addDifferenceMarker(new TableDifference(t, columnName, TableDifference.COLUMN_MISSING_FROM_DB, null, cid)); // else table is probably being created as we speak - and it's save/sync with DB will reload/rewrite the column info anyway
				// if we would add these markers even when table is being created, warnings for writing dbi files with error markers will appear
			}
		}
		else
		{
			ColumnInfoDef dbCid = new ColumnInfoDef();
			dbCid.columnType = c.getColumnType();
			dbCid.allowNull = c.getAllowNull();
			dbCid.flags = c.isDatabasePK() ? IBaseColumn.PK_COLUMN : 0;
			addDifferenceMarker(new TableDifference(t, columnName, TableDifference.COLUMN_CONFLICT, dbCid, cid));
		}
	}

	public String serializeTable(ITable t) throws JSONException
	{
		return serializeTable(t, true);
	}

	public String serializeTable(ITable t, boolean onlyStoredColumns) throws JSONException
	{
		TableDef tableInfo = new TableDef();
		tableInfo.name = t.getName();
		tableInfo.hiddenInDeveloper = t.isMarkedAsHiddenInDeveloper();
		tableInfo.isMetaData = t.isMarkedAsMetaData();
		tableInfo.tableType = t.getTableType();

		List<String> colNames = new ArrayList<String>();
		Collection<Column> col = t.getColumns();
		for (Column column : col)
		{
			colNames.add(column.getName());
		}

		Iterator<Column> it = null;
		String dbiSortingKey = new DbiPreferences().getDbiSortingKey();
		it = (DbiPreferences.DBI_SORT_BY_INDEX.equals(dbiSortingKey)) ? t.getColumnsSortedByIndex(colNames) : t.getColumnsSortedByName();
		while (it.hasNext())
		{
			Column column = it.next();
			ColumnInfoDef cid = getColumnInfoDef(column, (DbiPreferences.DBI_SORT_BY_INDEX.equals(dbiSortingKey) ? colNames.indexOf(column.getName()) : -1),
				onlyStoredColumns);
			if (cid != null)
			{
				if (!tableInfo.columnInfoDefSet.contains(cid))
				{
					tableInfo.columnInfoDefSet.add(cid);
				}
			}
		}
		return serializeTableInfo(tableInfo);
	}

	public static ColumnInfoDef getColumnInfoDef(Column column, int creationOrderIndex)
	{
		return getColumnInfoDef(column, creationOrderIndex, true);
	}

	/**
	 * Puts all information about the given column into a ColumnInfoDef object, if the give column has column info.
	 *
	 * @param column the column to be inspected.
	 * @param creationOrderIndex the creationOrderIndex for this column.
	 * @return all information about the given column into a ColumnInfoDef object, if the give column has column info and null otherwise.
	 */
	public static ColumnInfoDef getColumnInfoDef(IColumn column, int creationOrderIndex, boolean onlyStoredColumns)
	{
		ColumnInfoDef cid = null;
		ColumnInfo ci = column.getColumnInfo();
		if (ci != null && (column.getExistInDB() || !onlyStoredColumns))
		{
			cid = new ColumnInfoDef();
			cid.name = column.getName();
			cid.columnType = column.getConfiguredColumnType(); // may copy db column type from column
			cid.compatibleColumnTypes = ci.getCompatibleColumnTypes(); // may copy db column type from column
			cid.allowNull = column.getAllowNull();
			cid.flags = column.getFlags();
			cid.creationOrderIndex = creationOrderIndex;
			cid.autoEnterType = ci.getAutoEnterType();
			cid.autoEnterSubType = ci.getAutoEnterSubType();
			cid.sequenceStepSize = ci.getSequenceStepSize();
			cid.preSequenceChars = ci.getPreSequenceChars();
			cid.postSequenceChars = ci.getPostSequenceChars();
			cid.defaultValue = ci.getDefaultValue();
			cid.lookupValue = ci.getLookupValue();
			cid.databaseSequenceName = ci.getDatabaseSequenceName();
			cid.titleText = ci.getTitleText();
			cid.description = ci.getDescription();
			cid.foreignType = ci.getForeignType();
			cid.converterName = ci.getConverterName();
			cid.converterProperties = ci.getConverterProperties();
			cid.validatorProperties = ci.getValidatorProperties();
			cid.validatorName = ci.getValidatorName();
			cid.defaultFormat = ci.getDefaultFormat();
			cid.dataProviderID = ci.getDataProviderID();
			cid.containsMetaData = ci.getContainsMetaData();
			cid.elementTemplateProperties = ci.getElementTemplateProperties();
			cid.sortIgnorecase = ci.getSortIgnorecase();
			cid.sortingNullprecedence = ci.getSortingNullprecedence();
		}
		else if (!onlyStoredColumns)
		{
			cid = new ColumnInfoDef();
			cid.name = column.getName();
			cid.columnType = column.getConfiguredColumnType(); // may copy db column type from column
			cid.allowNull = column.getAllowNull();
			cid.flags = column.getFlags();
			cid.creationOrderIndex = creationOrderIndex;
		}
		return cid;
	}

	/**
	 * Creates a .dbi (JSON format) file like structured String from the given table information.
	 *
	 * @param tableInfo the information about the table to be transformed into a JSON String.
	 * @return the JSON representation of tableInfo.
	 * @throws JSONException if something goes wrong with the serialize.
	 */
	public String serializeTableInfo(TableDef tableInfo) throws JSONException
	{
		ServoyJSONObject tobj = new ServoyJSONObject();
		tobj.put(SolutionSerializer.PROP_NAME, tableInfo.name);
		tobj.put(TableDef.PROP_TABLE_TYPE, tableInfo.tableType);
		if (tableInfo.hiddenInDeveloper) tobj.put(TableDef.HIDDEN_IN_DEVELOPER, true);
		if (Boolean.TRUE.equals(tableInfo.isMetaData)) tobj.put(TableDef.IS_META_DATA, true);

		JSONArray carray = new ServoyJSONArray();
		for (ColumnInfoDef cid : tableInfo.columnInfoDefSet)
		{
			ServoyJSONObject obj = new ServoyJSONObject();
			obj.put(SolutionSerializer.PROP_NAME, cid.name);
			obj.put(ColumnInfoDef.DATA_TYPE, cid.columnType.getSqlType());
			if (cid.columnType.getLength() != 0) obj.put(ColumnInfoDef.LENGTH, cid.columnType.getLength());
			if (cid.columnType.getScale() != 0) obj.put(ColumnInfoDef.SCALE, cid.columnType.getScale());
			if (cid.columnType.getSubType() != 0) obj.put(ColumnInfoDef.SUB_TYPE, cid.columnType.getSubType());
			String compatibleColumnTypesStr = XMLUtils.serializeColumnTypeArray(cid.compatibleColumnTypes);
			if (compatibleColumnTypesStr != null)
			{
				obj.put(ColumnInfoDef.COMPATIBLE_COLUMN_TYPES, compatibleColumnTypesStr);
			}
			obj.put(ColumnInfoDef.ALLOW_NULL, cid.allowNull);
			if (cid.flags != 0) obj.put(ColumnInfoDef.FLAGS, cid.flags);

			obj.put(ColumnInfoDef.CREATION_ORDER_INDEX, cid.creationOrderIndex);

			if (cid.autoEnterType != ColumnInfo.NO_AUTO_ENTER)
			{
				obj.put(ColumnInfoDef.AUTO_ENTER_TYPE, cid.autoEnterType);
				if (cid.autoEnterSubType != ColumnInfo.NO_SEQUENCE_SELECTED) obj.put(ColumnInfoDef.AUTO_ENTER_SUB_TYPE, cid.autoEnterSubType);
			}
			if (cid.sequenceStepSize > 1) obj.put(ColumnInfoDef.SEQUENCE_STEP_SIZE, cid.sequenceStepSize);
			obj.putOpt(ColumnInfoDef.PRE_SEQUENCE_CHARS, cid.preSequenceChars);
			obj.putOpt(ColumnInfoDef.POST_SEQUENCE_CHARS, cid.postSequenceChars);
			obj.putOpt(ColumnInfoDef.DEFAULT_VALUE, cid.defaultValue);
			obj.putOpt(ColumnInfoDef.LOOKUP_VALUE, cid.lookupValue);
			obj.putOpt(ColumnInfoDef.DATABASE_SEQUENCE_NAME, cid.databaseSequenceName);
			obj.putOpt(ColumnInfoDef.TITLE_TEXT, cid.titleText);
			obj.putOpt(ColumnInfoDef.DESCRIPTION, cid.description);
			obj.putOpt(ColumnInfoDef.FOREIGN_TYPE, cid.foreignType);
			obj.putOpt(ColumnInfoDef.CONVERTER_NAME, cid.converterName);
			obj.putOpt(ColumnInfoDef.CONVERTER_PROPERTIES, cid.converterProperties);
			obj.putOpt(ColumnInfoDef.VALIDATOR_PROPERTIES, cid.validatorProperties);
			obj.putOpt(ColumnInfoDef.VALIDATOR_NAME, cid.validatorName);
			obj.putOpt(ColumnInfoDef.DEFAULT_FORMAT, cid.defaultFormat);
			obj.putOpt(ColumnInfoDef.ELEMENT_TEMPLATE_PROPERTIES, cid.elementTemplateProperties);
			obj.putOpt(ColumnInfoDef.DATA_PROVIDER_ID, cid.dataProviderID);
			obj.putOpt(ColumnInfoDef.CONTAINS_META_DATA, cid.containsMetaData);
			if (cid.sortIgnorecase != null)
			{
				obj.put(ColumnInfoDef.SORT_IGNORECASE, cid.sortIgnorecase);
			}
			if (cid.sortingNullprecedence != null)
			{
				obj.put(ColumnInfoDef.SORTING_NULLPRECEDENCE, cid.sortingNullprecedence.name());
			}
			carray.put(obj);
		}
		tobj.put(TableDef.PROP_COLUMNS, carray);
		return tobj.toString(true);
	}

	public static String getDBIFileName(String tableName)
	{
		return tableName + COLUMN_INFO_FILE_EXTENSION_WITH_DOT;
	}

	public static String getSecurityFileName(String name)
	{
		return name + SECURITY_FILE_EXTENSION_WITH_DOT;
	}

	public static String getRelativeServerPath(String serverName)
	{
		if (serverName == null) return "";
		return SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR + serverName;
	}

	public IFile getDBIFile(String dataSource)
	{
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		if (stn == null)
		{
			return null;
		}
		return getDBIFile(stn[0], stn[1]);
	}

	public IFile getDBIFile(String serverName, String tableName)
	{
		if (resourceProject == null)
		{
			return null;
		}
		IPath path = new Path(getRelativeServerPath(serverName) + IPath.SEPARATOR + getDBIFileName(tableName));
		return resourceProject.getFile(path);
	}

	public IFile getSecurityFile(String serverName, String tableName)
	{
		if (resourceProject == null)
		{
			return null;
		}
		IPath path = new Path(getRelativeServerPath(serverName) + IPath.SEPARATOR + getSecurityFileName(tableName));
		return resourceProject.getFile(path);
	}

	public IFile getServerDBIFile(String serverName)
	{
		if (resourceProject == null)
		{
			return null;
		}
		IPath path = new Path(getRelativeServerPath(serverName) + COLUMN_INFO_FILE_EXTENSION_WITH_DOT);
		return resourceProject.getFile(path);
	}

	public IFile getSecurityFile()
	{
		if (resourceProject == null)
		{
			return null;
		}
		IPath path = new Path(SECURITY_DIRECTORY + IPath.SEPARATOR + SECURITY_FILENAME);
		return resourceProject.getFile(path);
	}

	public IFile getMetaDataFile(String dataSource)
	{
		if (resourceProject == null)
		{
			return null;
		}
		String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
		if (stn == null)
		{
			return null;
		}
		IPath path = new Path(getRelativeServerPath(stn[0]) + IPath.SEPARATOR + stn[1] + TABLE_DATA_FILE_EXTENSION_WITH_DOT);
		return resourceProject.getFile(path);
	}

	public IFolder getServerInformationFolder(String serverName)
	{
		if (resourceProject == null)
		{
			return null;
		}
		IPath path = new Path(getRelativeServerPath(serverName));
		return resourceProject.getFolder(path);
	}

	public void setWritesEnabled(boolean b)
	{
		writeDBIFiles = b;
	}

	public void dispose()
	{
		clearProblemMarkers(true);
		sm.removeServerListener(serverListener);
		// add listeners to initial server list
		String[] array = sm.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal server = (IServerInternal)sm.getServer(server_name, false, false);
			server.removeTableListener(tableListener);
		}
	}

	public void addMissingDBIMarker(String serverName, String tableName, boolean removeMarkersFirst)
	{
		if (removeMarkersFirst) removeErrorMarker(serverName, tableName);
		addDifferenceMarker(new TableDifference(serverName, tableName, null, TableDifference.MISSING_DBI_FILE, null, null));
	}

	public void updateMarkerStatesForMissingTable(IResourceDelta fileRd, String serverName, String tableName)
	{
		if (fileRd == null || (fileRd.getFlags() & IResourceDelta.MOVED_TO) != 0) // this is usually caused by a delete or by the synchronize with db wizard
		{
			// it's existence was noticed, not necessarily the first time (the dbi file) or
			// it was moved somewhere - remove previous markers (this will clear the difference, but not the real marker, as file probably changed name/location)
			removeErrorMarker(serverName, tableName);
		}
		else if ((fileRd.getFlags() & IResourceDelta.MOVED_FROM) != 0)
		{
			// it was moved from somewhere - remove previous markers (this will clear real markers, cause difference was probably removed when MOVED_TO arrived)
			removeErrorMarker(serverName, tableName);
		}

		if (fileRd == null || fileRd.getKind() == IResourceDelta.ADDED) // added or moved from somewhere, or it's existence was just noticed
		{
			// a .dbi file was added for a table that does not exist - add problem marker
			addDifferenceMarker(new TableDifference(serverName, tableName, null, TableDifference.MISSING_TABLE, null, null));
		}
	}

	private void addDifferenceMarker(final TableDifference columnDifference)
	{
		addDifferenceMarker(columnDifference, false);
	}

	private void addDifferenceMarker(final TableDifference columnDifference, boolean onlyRestoreMarker)
	{
		int markerSeverity = columnDifference.getSeverity();
		if (markerSeverity < 0) return;
		else
		{
			String datasource = DataSourceUtils.createDBTableDataSource(columnDifference.getServerName(), columnDifference.getTableName());
			if ((!ServoyBuilder.getDataSourceCollectorVisitor().getDataSources().contains(datasource)) && markerSeverity > IMarker.SEVERITY_WARNING)
			{
				markerSeverity = IMarker.SEVERITY_WARNING;
			}
		}
		final int severity = markerSeverity;

		if (!onlyRestoreMarker) differences.addDifference(columnDifference);
		final IResource resource;
		if (columnDifference.type == TableDifference.MISSING_DBI_FILE)
		{
			resource = resourceProject; // missing dbi file markers are added to the resources project directly
		}
		else
		{
			resource = getDBIFile(columnDifference.getServerName(), columnDifference.getTableName());
		}
		updateProblemMarkers(new Runnable()
		{
			public void run()
			{
				if (resource != null && resource.exists())
				{
					// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
					// the project might have disappeared before this job was started... (delete)
					try
					{
						boolean hiddenInDeveloper;
						if (columnDifference.getTable() != null)
						{
							hiddenInDeveloper = columnDifference.getTable().isMarkedAsHiddenInDeveloper();
						}
						else
						{
							// this does actual parsing of the .dbi file, but it seems like the Table doesn't exist in the DB so the flag is not cached anywhere;
							// currently this will only happen when a missing table is used in a solution or when the user manually chooses to sync tables
							TableMetaInfo tableMetaInfo = getTableMetainfo((IServerInternal)sm.getServer(columnDifference.getServerName()),
								columnDifference.getTableName());
							hiddenInDeveloper = tableMetaInfo.hiddenInDeveloper;
						}

						if (!hiddenInDeveloper)
						{
							IMarker marker = resource.createMarker(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
							marker.setAttribute(IMarker.MESSAGE, columnDifference.getUserFriendlyMessage());
//						int adjustedSeverity = severity;
//						if (adjustedSeverity == IMarker.SEVERITY_ERROR)
//						{
//							DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
//							for (ServoyProject sp : ServoyModelFinder.getServoyModel().getModulesOfActiveProject())
//							{
//								sp.getSolution().acceptVisitor(datasourceCollector);
//							}
//							String datasource = DataSourceUtils.createDBTableDataSource(columnDifference.getServerName(), columnDifference.getTableName());
//							if (!datasourceCollector.getDataSources().contains(datasource))
//							{
//								adjustedSeverity = IMarker.SEVERITY_WARNING;
//							}
//						}
							marker.setAttribute(IMarker.SEVERITY, severity);
							marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
							marker.setAttribute(IMarker.LOCATION, "JSON file");
							marker.setAttribute(TableDifference.ATTRIBUTE_SERVERNAME, columnDifference.getServerName());
							marker.setAttribute(TableDifference.ATTRIBUTE_TABLENAME, columnDifference.getTableName());
							marker.setAttribute(TableDifference.ATTRIBUTE_COLUMNNAME, columnDifference.getColumnName());

							// missing DBI file markers are added to the resources project itself (do not have the dbi file to add markers to);
							// this means that there might be more markers like this on the resources project - from different tables;
							// and we need to keep track of them individually - so remember the ID of the marker for each table
							if (columnDifference.type == TableDifference.MISSING_DBI_FILE)
							{
								missingDbiFileMarkerIds.put(columnDifference.getServerName() + '.' + columnDifference.getTableName(),
									Long.valueOf(marker.getId()));
								marker.setAttribute(TableDifference.ATTRIBUTE_ISMISSINGDBIFILEMARKER, true);
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logWarning("Cannot create problem marker", e);
					}

				}
			}
		});
	}

	private void addTableDeserializeErrorMarker(String serverName, String tableName, String message)
	{
		addTableDeserializeErrorMarker(serverName, tableName, message, false);
	}

	private void addTableDeserializeErrorMarker(String serverName, String tableName, String message, boolean onlyRestoreMarker)
	{
		if (!onlyRestoreMarker) differences.addDifference(new TableDifference(serverName, tableName, message));
		updateProblemMarkers(() -> addDBIBadDBInfoMarker(getDBIFile(serverName, tableName), message));
	}

	private void addServerDeserializeErrorMarker(String serverName, String message)
	{
		updateProblemMarkers(() -> addDBIBadDBInfoMarker(getServerDBIFile(serverName), message));
	}


	private static void addDBIBadDBInfoMarker(IFile file, String message)
	{
		if (file.exists())
		{
			// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
			// the project might have disappeared before this job was started... (delete)
			// find out where the error occurred if possible...
			int charNo = -1;
			int idx = message.indexOf("character");
			if (idx >= 0)
			{
				StringTokenizer st = new StringTokenizer(message.substring(idx + 9), " ");
				if (st.hasMoreTokens())
				{
					String charNoString = st.nextToken();
					try
					{
						charNo = Integer.parseInt(charNoString);
					}
					catch (NumberFormatException e)
					{
						// cannot fine character number... this is not a tragedy
					}
				}
			}

			// we have an active solution with a resources project but with invalid security info; add problem marker
			ServoyMarker mk = MarkerMessages.DBIBadDBInfo.fill(message);
			ServoyBuilder.addMarker(file, mk.getType(), mk.getText(), charNo, ServoyBuilder.DBI_BAD_INFO, IMarker.PRIORITY_NORMAL, "JSON file");
		}
	}

	private static int getErrorSeverity(String name)
	{
		if (name.toUpperCase().startsWith(TEMP_UPPERCASE_PREFIX)) return IMarker.SEVERITY_WARNING; // this will normally not happen as column info reads/writes for temp_ tables are ignored in Server class; but just in case make sure no error markers appear for them
		return IMarker.SEVERITY_ERROR;
	}

	private void removeErrorMarker(final String serverName, final String tableName)
	{
		differences.removeDifferences(serverName, tableName);
		final IFile file = getDBIFile(serverName, tableName);
		updateProblemMarkers(new Runnable()
		{
			public void run()
			{
				try
				{
					if (file.exists() && file.findMarkers(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE, false, IResource.DEPTH_ZERO).length > 0)
					{
						// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
						// the project might have disappeared before this job was started... (delete)
						ServoyBuilder.deleteMarkers(file, ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
					}

					deleteMissingDBIFileMarkerIfNeeded(serverName, tableName);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		});
	}

	public void removeAllMissingDBIFileMarkers()
	{
		if (resourceProject != null)
		{
			try
			{
				IMarker[] markers = resourceProject.findMarkers(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				if (markers != null && markers.length > 0)
				{
					for (IMarker marker : markers)
					{
						String serverName = marker.getAttribute(TableDifference.ATTRIBUTE_SERVERNAME, null);
						String tableName = marker.getAttribute(TableDifference.ATTRIBUTE_TABLENAME, null);
						if (serverName != null && tableName != null)
						{
							deleteMissingDBIFileMarkerIfNeeded(serverName, tableName);
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void deleteMissingDBIFileMarkerIfNeeded(String serverName, String tableName) throws CoreException
	{
		// missing .dbi file markers are set to the resources project directly - see if such a marker exists for the table
		List<String> keys = new ArrayList<String>();
		String serverPrefix = serverName + '.';
		if (tableName == null)
		{
			// remove all for the server
			for (String key : missingDbiFileMarkerIds.keySet())
			{
				if (key.startsWith(serverPrefix))
				{
					keys.add(key);
				}
			}
		}
		else
		{
			keys.add(serverPrefix + tableName);
		}
		for (String key : keys)
		{
			Long id = missingDbiFileMarkerIds.remove(key);
			if (resourceProject != null && id != null)
			{
				IMarker marker = resourceProject.findMarker(id.longValue());
				if (marker != null)
				{
					marker.delete();
				}
				else
				{
					ServoyLog.logError("Cannot find 'missing .dbi file' marker for a problem that should exist", null);
				}
			}
		}
	}

	private void removeErrorMarkers(final String serverName)
	{
		differences.removeDifferences(serverName);
		final IFolder folder = getServerInformationFolder(serverName);

		updateProblemMarkers(() -> {
			try
			{
				if (folder != null && folder.exists() &&
					folder.findMarkers(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE, false, IResource.DEPTH_INFINITE).length > 0)
				{
					// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
					// the project might have disappeared before this job was started... (delete)
					ServoyBuilder.deleteMarkers(folder, ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
				}
				deleteMissingDBIFileMarkerIfNeeded(serverName, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		});
	}


	private void clearProblemMarkers(boolean clearAlsoDifferences)
	{
		if (clearAlsoDifferences) differences.removeAllDifferences();
		updateProblemMarkers(() -> {
			if (resourceProject != null && resourceProject.exists())
			{
				// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
				// the project might have disappeared before this job was started... (delete)
				ServoyBuilder.deleteMarkers(resourceProject, ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
			}
			missingDbiFileMarkerIds.clear();
		});
	}

	public TableDifference getColumnDifference(String serverName, String tableName, String columnName)
	{
		return differences.getDifference(serverName, tableName, columnName);
	}

	private void updateProblemMarkers(final Runnable r)
	{
		if (doNotShowProblemMarkers) return;

		if (updateMarkersJob == null)
		{
			updateMarkersJob = new UpdateMarkersJob("Updating database information problem markers", resourceProject, true);
		}

		updateMarkersJob.addRunner(r);
	}

	/**
	 * Class used to store the information needed by column difference markers.<br>
	 * It is also able to generate a user friendly error message for this difference.
	 */
	public static class TableDifference
	{
		public static final String ATTRIBUTE_SERVERNAME = "CDServerNameAttribute";
		public static final String ATTRIBUTE_TABLENAME = "CDTableNameAttribute";
		public static final String ATTRIBUTE_COLUMNNAME = "CDColumnNameAttribute";
		public static final String ATTRIBUTE_ISMISSINGDBIFILEMARKER = "CDIsDBIFileMissingMarker";

		public static final int COLUMN_MISSING_FROM_DB = 1;
		public static final int COLUMN_MISSING_FROM_DBI_FILE = 2;
		public static final int COLUMN_CONFLICT = 3;
		public static final int DESERIALIZE_PROBLEM = 4;
		public static final int MISSING_TABLE = 5;
		public static final int MISSING_DBI_FILE = 6;
		public static final int COLUMN_SEQ_TYPE_OVERRIDEN = 7;

		private final String serverName;
		private final String tableName;
		private ITable table;
		private final String columnName;
		private final int type;
		private final ColumnInfoDef tableDefinition;
		private final ColumnInfoDef dbiFileDefinition;
		private boolean renamable;
		private final String customMessage; // initially used only for .dbi deserialize error markers

		private TableDifference(String serverName, String tableName, String customDBIDeserializeErrorMessage)
		{
			this(serverName, tableName, null, DESERIALIZE_PROBLEM, null, null, customDBIDeserializeErrorMessage);
		}

		private TableDifference(String serverName, String tableName, String columnName, int type, ColumnInfoDef tableDefinition,
			ColumnInfoDef dbiFileDefinition)
		{
			this(serverName, tableName, columnName, type, tableDefinition, dbiFileDefinition, null);
		}

		private TableDifference(String serverName, String tableName, String columnName, int type, ColumnInfoDef tableDefinition,
			ColumnInfoDef dbiFileDefinition, String customDBIDeserializeErrorMessage)
		{
			this.serverName = serverName;
			this.tableName = tableName;
			this.columnName = columnName;
			this.type = type;
			this.tableDefinition = tableDefinition;
			this.dbiFileDefinition = dbiFileDefinition;
			this.customMessage = customDBIDeserializeErrorMessage;
		}

		private TableDifference(ITable table, String columnName, int type, ColumnInfoDef tableDefinition, ColumnInfoDef dbiFileDefinition)
		{
			this(table.getServerName(), table.getName(), columnName, type, tableDefinition, dbiFileDefinition);
			this.table = table;
		}

		private TableDifference(ITable table, String columnName, int type, ColumnInfoDef tableDefinition, ColumnInfoDef dbiFileDefinition, boolean renamable)
		{
			this(table, columnName, type, tableDefinition, dbiFileDefinition);
			this.renamable = renamable;
		}

		public String getTableName()
		{
			return tableName;
		}

		public String getServerName()
		{
			return serverName;
		}

		public ITable getTable()
		{
			return table;
		}

		public String getColumnName()
		{
			return columnName;
		}

		public int getType()
		{
			return type;
		}

		public ColumnInfoDef getTableDefinition()
		{
			return tableDefinition;
		}

		public ColumnInfoDef getDbiFileDefinition()
		{
			return dbiFileDefinition;
		}

		public String getCustomMessage()
		{
			return customMessage;
		}

		public boolean isRenamable()
		{
			return renamable;
		}

		public String getUserFriendlyMessage()
		{
			ServoyMarker mk;
			if (type == COLUMN_MISSING_FROM_DB)
			{
				mk = MarkerMessages.DBIColumnMissingFromDB.fill(getColumnString());
			}
			else if (type == COLUMN_MISSING_FROM_DBI_FILE)
			{
				mk = MarkerMessages.DBIColumnMissingFromDBIFile.fill(getColumnString());
			}
			else if (type == COLUMN_CONFLICT)
			{
				mk = MarkerMessages.DBIColumnConflict.fill(getColumnString(), getColumnDefinition(tableDefinition), getColumnDefinition(dbiFileDefinition));
			}
			else if (type == MISSING_TABLE)
			{
				mk = MarkerMessages.DBITableMissing.fill(serverName + "->" + tableName);
			}
			else if (type == MISSING_DBI_FILE)
			{
				mk = MarkerMessages.DBIFileMissing.fill(serverName + "->" + tableName);
			}
			else if (type == COLUMN_SEQ_TYPE_OVERRIDEN)
			{
				mk = MarkerMessages.DBIColumnSequenceTypeOverride.fill(columnName, tableName);
			}
			else
			{
				// this should not be reached in normal execution...
				mk = MarkerMessages.DBIGenericError.fill(getColumnString());
			}
			return mk.getText();
		}

		private int computeCustomSeverity(Pair<String, ProblemSeverity> problem)
		{
			int severity = -1;
			String customSeverity = ServoyBuilder.getSeverity(problem.getLeft(), problem.getRight().name(),
				ServoyModelFinder.getServoyModel().getActiveProject().getProject());
			if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
			{
				severity = ServoyBuilder.getTranslatedSeverity(customSeverity, problem.getRight());
			}
			return severity;
		}

		private String getColumnDefinition(ColumnInfoDef definition)
		{
			StringBuffer message = new StringBuffer();
			message.append("(");
			if ((definition.flags & IBaseColumn.PK_COLUMN) != 0)
			{
				message.append("pk, ");
			}
			else if ((definition.flags & IBaseColumn.USER_ROWID_COLUMN) != 0)
			{
				message.append("row_ident, ");
			}
			message.append(Column.getDisplayTypeString(definition.columnType.getSqlType()));
			message.append("(id:");
			message.append(definition.columnType.getSqlType());
			message.append("), length: ");
			message.append(definition.columnType.getLength());
			if (definition.columnType.getScale() != 0)
			{
				message.append(", scale: ");
				message.append(definition.columnType.getScale());
			}
			message.append(", allowNull: ");
			message.append(definition.allowNull);
			message.append(")");
			return message.toString();
		}

		public String getColumnString()
		{
			StringBuffer message = new StringBuffer();
			message.append(serverName);
			message.append("->");
			message.append(tableName);
			message.append("->");
			message.append(columnName);
			return message.toString();
		}

		public int getSeverity()
		{
			if (tableName.toUpperCase().startsWith(TEMP_UPPERCASE_PREFIX)) return IMarker.SEVERITY_WARNING;

			int severity = -1;
			if (type == COLUMN_MISSING_FROM_DB)
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_MISSING_FROM_DB);
			}
			else if (type == COLUMN_MISSING_FROM_DBI_FILE)
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_MISSING_FROM_DB_FILE);
			}
			else if (type == MISSING_TABLE)
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_TABLE_MISSING);
			}
			else if (type == MISSING_DBI_FILE)
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_FILE_MISSING);
			}
			else if (type == COLUMN_CONFLICT)
			{
				// TODO check, now we always return computeCustomSeverity(ServoyBuilder.DBI_COLUMN_CONFLICT); do we want different ProblemSeverity here for the different situations?
				Column c = table.getColumn(columnName);
				ColumnType colColumnType = c.getColumnType(); // compare db column type with dbi file column type
				ColumnType dbiColumnType = dbiFileDefinition.columnType;

				if (!colColumnType.equals(dbiColumnType) && (c.getColumnInfo() == null || !c.getColumnInfo().isCompatibleColumnType(colColumnType) ||
					!c.getColumnInfo().isCompatibleColumnType(dbiColumnType)))
				{
					int t1 = Column.mapToDefaultType(colColumnType.getSqlType());
					int t2 = Column.mapToDefaultType(dbiColumnType.getSqlType());

					if ((t1 == t2 && colColumnType.getLength() == dbiColumnType.getLength()) ||
						(t1 == IColumnTypes.NUMBER && colColumnType.getScale() == 0 && t2 == IColumnTypes.INTEGER))
					{
						severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_CONFLICT);
					}
					else
					{
						boolean compatibleLengths = (t1 == t2) && (t1 == IColumnTypes.MEDIA || t1 == IColumnTypes.TEXT) &&
							(Math.abs(colColumnType.getLength() - (float)dbiColumnType.getLength()) > (Integer.MAX_VALUE / 2));
						if (compatibleLengths && (colColumnType.getLength() == Integer.MAX_VALUE || dbiColumnType.getLength() == Integer.MAX_VALUE) &&
							colColumnType.getLength() > 0 && dbiColumnType.getLength() > 0)
						{
							// one is max value, the other one way smaller; not compatible
							compatibleLengths = false;
						}
						// this check is for -1 and big value lengths
						if (!compatibleLengths)
						{
							severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_CONFLICT);
						}
					}
				}
				else if (c.getAllowNull() != dbiFileDefinition.allowNull)
				{
					severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_CONFLICT);
				}

				if (severity != IMarker.SEVERITY_ERROR) // if we already discovered an error, no use checking further
				{
					// real column can only know if it's pk or not (doesn't know about USER_ROWID_COLUMN)
					boolean columnInfoIsPk = ((dbiFileDefinition.flags & IBaseColumn.PK_COLUMN) != 0);
					if (c.isDatabasePK() != columnInfoIsPk)
					{
						if ((c.isDatabasePK() && (dbiFileDefinition.flags & IBaseColumn.IDENT_COLUMNS) == 0) || columnInfoIsPk)
						{
							// column is pk, but columninfo knows it as normal column, or column is not pk and columninfo knows it as pk
							severity = getErrorSeverity(tableName);
						}
						else if (c.isDatabasePK() && ((dbiFileDefinition.flags & IBaseColumn.USER_ROWID_COLUMN) != 0))
						{
							// columns is pk, column info says it's USER_ROWID_COLUMN - both ident columns, but not quite the same
							severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_CONFLICT);
						} // else no other case should be left
					}
				}
			}
			else if (type == COLUMN_SEQ_TYPE_OVERRIDEN)
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_COLUMN_INFO_SEQ_TYPE_OVERRIDE);
			}
			else
			{
				severity = computeCustomSeverity(ServoyBuilder.DBI_BAD_INFO);
			}

			return severity;
		}
	}

	private static class TableDifferencesHolder
	{
		private final HashMap<String, Integer> differenceTypes = new HashMap<String, Integer>();
		private final List<TableDifference> differences = new ArrayList<TableDifference>();

		public synchronized void addDifference(TableDifference d)
		{
			differenceTypes.put(d.getServerName() + '.' + d.getTableName(), Integer.valueOf(d.getType()));
			if (!differences.contains(d))
			{
				differences.add(d);
			}
		}

		public synchronized TableDifference getDifference(String serverName, String tableName, String columnName)
		{
			for (TableDifference d : differences)
			{
				if (d.getServerName().equals(serverName) && d.getTableName().equals(tableName) &&
					(d.getColumnName() == null || d.getColumnName().equals(columnName)))
				{
					return d;
				}
			}
			return null;
		}

		public synchronized void removeAllDifferences()
		{
			differenceTypes.clear();
			differences.clear();
		}

		public synchronized void removeDifferences(String serverName)
		{
			Iterator<TableDifference> it = differences.iterator();
			while (it.hasNext())
			{
				TableDifference dif = it.next();
				if (dif.getServerName().equals(serverName))
				{
					differenceTypes.remove(dif.getServerName() + '.' + dif.getTableName());
					it.remove();
				}
			}
		}

		public synchronized void removeDifferences(String serverName, String tableName)
		{
			Iterator<TableDifference> it = differences.iterator();
			while (it.hasNext())
			{
				TableDifference dif = it.next();
				if (dif.getServerName().equals(serverName) && dif.getTableName().equals(tableName))
				{
					differenceTypes.remove(dif.getServerName() + '.' + dif.getTableName());
					it.remove();
				}
			}
		}

		public synchronized int getDifferenceTypeForTable(ITable t)
		{
			Integer result = differenceTypes.get(t.getServerName() + '.' + t.getName());
			return result == null ? -1 : result.intValue();
		}

		public synchronized List<TableDifference> getDifferences()
		{
			return new ArrayList<TableDifference>(differences);
		}
	}

	public void columInfoSequenceOverriden(Column c)
	{
		ColumnInfoDef dbCid = new ColumnInfoDef();
		dbCid.name = c.getName();
		dbCid.columnType = c.getColumnType();
		dbCid.allowNull = c.getAllowNull();
		dbCid.flags = c.getFlags();
		dbCid.autoEnterSubType = c.getColumnInfo().getAutoEnterSubType();
		dbCid.autoEnterType = c.getColumnInfo().getAutoEnterType();

		try
		{
			IFile file = getDBIFile(c.getTable().getServerName(), c.getTable().getName());
			if (file.exists())
			{
				InputStream is = file.getContents(true);
				String dbiFileContent = null;
				try
				{
					dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				}
				finally
				{
					Utils.closeInputStream(is);
				}

				if (dbiFileContent != null)
				{
					TableDef tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent);
					if (!c.getTable().getName().equals(tableInfo.name))
					{
						throw new RepositoryException("Table name does not match dbi file name for " + c.getTable().getName());
					}
					ColumnInfoDef cid = null;
					for (int i = tableInfo.columnInfoDefSet.size() - 1; i >= 0; i--)
					{
						cid = tableInfo.columnInfoDefSet.get(i);
						if (cid.name.equals(c.getName()))
						{
							break;
						}
					}
					addDifferenceMarker(new TableDifference(c.getTable(), c.getName(), TableDifference.COLUMN_SEQ_TYPE_OVERRIDEN, dbCid, cid));
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}

	/** Get the DataModelManager installed as ServerInfoManager in the servermanager.
	 * @param serverName
	 * @return
	 */
	public static DataModelManager getColumnInfoManager(IServerManagerInternal serverManager)
	{
		IServerInfoManager[] cims = serverManager.getServerInfoManagers();
		if (cims != null)
		{
			for (IServerInfoManager cim : cims)
			{
				if (cim instanceof DataModelManager)
				{
					return (DataModelManager)cim;
				}
			}
		}
		return null;
	}


	public void addAllMissingDBIFileMarkersForDataSources(Set<String> dataSources)
	{
		Set<String> serverNames = new HashSet<String>();
		Map<String, List<String>> serversTables = new HashMap<String, List<String>>();
		for (String dataSource : dataSources)
		{
			String[] ds = DataSourceUtilsBase.getDBServernameTablename(dataSource);
			if (ds != null && serverNames.add(ds[0]))
			{
				serversTables.put(ds[0], DataSourceUtils.getServerTablenames(dataSources, ds[0]));
			}
		}

		for (String serverName : serverNames)
		{
			try
			{
				IServer server = ApplicationServerRegistry.get().getServerManager().getServer(serverName);
				if (server != null)
				{
					List<String> tableNames = serversTables.get(serverName);
					IFolder serverInformationFolder = getServerInformationFolder(server.getName());
					for (String tableName : server.getTableAndViewNames(true))
					{
						if (!tableNames.contains(tableName)) continue;
						if (!serverInformationFolder.getFile(tableName + DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT).exists())
						{
							addMissingDBIMarker(server.getName(), tableName, true);
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	@Override
	public void createNewColumnInfo(Column c, boolean createMissingServoySequence) throws RepositoryException
	{
		DatabaseUtils.createNewColumnInfo(ApplicationServerRegistry.get().getDeveloperRepository().getNewElementID(null), c, createMissingServoySequence);
	}

	@Override
	public void setTableColumnInfos(ITable t, HashMap<String, ColumnInfoDef> columnInfoDefinitions) throws RepositoryException
	{
		DatabaseUtils.updateTableColumnInfos(ApplicationServerRegistry.get().getDeveloperRepository(), t, columnInfoDefinitions);
	}

	/**
	 * Call this method to temporarily suppress generation of problem markers about dbi files - this is useful to not show temporary error markers during solution import for example.
	 * @param solutionImportInProgress
	 */
	public void suppressProblemMarkers(boolean shouldSuppress)
	{
		if (shouldSuppress)
		{
			clearProblemMarkers(false);
			doNotShowProblemMarkers = true; // set this only after clearing problem markers - so that it does not block markers
		}
		else
		{
			doNotShowProblemMarkers = false; // set this before adding markers - so that it does not block markers
			restoreProblemMarkersFromDifferences();
		}
	}

	private void restoreProblemMarkersFromDifferences()
	{
		for (TableDifference difference : differences.getDifferences())
		{
			if (difference.getType() == TableDifference.DESERIALIZE_PROBLEM)
				addTableDeserializeErrorMarker(difference.getServerName(), difference.getTableName(), difference.getCustomMessage(), true);
			else addDifferenceMarker(difference, true);
		}
	}

}
