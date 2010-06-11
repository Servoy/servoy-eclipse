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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.dbcp.DbcpException;
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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.builder.MarkerMessages;
import com.servoy.eclipse.core.builder.ServoyBuilder;
import com.servoy.eclipse.core.util.ResourcesUtils;
import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UpdateMarkersJob;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerListener;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITableListener;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

public class DataModelManager implements IColumnInfoManager
{
	public static final String COLUMN_INFO_FILE_EXTENSION = "dbi";
	public static final String COLUMN_INFO_FILE_EXTENSION_WITH_DOT = '.' + COLUMN_INFO_FILE_EXTENSION;
	private static final String PROP_TABLE_TYPE = "tableType";
	private static final String PROP_COLUMNS = "columns";

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

	public DataModelManager(IProject resourceProject, IServerManagerInternal sm)
	{
		this.resourceProject = resourceProject;
		this.sm = sm;

		tableListener = new ITableListener()
		{
			public void tablesAdded(IServer server, String tableNames[])
			{
				// not interested in this
			}

			public void tablesRemoved(IServer server, Table tables[], boolean deleted)
			{
				// not interested in this
			}

			public void serverStateChanged(IServer server, int oldState, int newState)
			{
				if ((oldState & ITableListener.ENABLED) == 0 && (newState & ITableListener.ENABLED) != 0 && (newState & ITableListener.VALID) != 0)
				{
					// this means that a server that was disabled became enabled and valid - so reload column info to regenerate problem markers if needed
					reloadAllColumnInfo(server);
				}
				else if ((newState & ITableListener.ENABLED) == 0 && (oldState & ITableListener.ENABLED) != 0 && (oldState & ITableListener.VALID) != 0)
				{
					// an enabled, valid server was disabled, so remove any markers it might have
					removeErrorMarkers(((IServerInternal)server).getName());
				}
			}

			public void tableInitialized(Table t)
			{
				for (Column column : t.getColumns())
				{
					if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
						!column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
					{
						try
						{
							IFile dbiFile = getDBIFile(t.getServerName(), t.getName());
							if (dbiFile.exists())
							{
								IMarker marker = dbiFile.createMarker(ServoyBuilder.COLUMN_MARKER_TYPE);
								String msg = MarkerMessages.getMessage(MarkerMessages.Marker_Column_UUIDFlagNotSet, t.getName(), column.getName());
								marker.setAttribute(IMarker.MESSAGE, msg);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
								marker.setAttribute(IMarker.LOCATION, "JSON file");
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
			public void serverAdded(IServer s)
			{
				((IServerInternal)s).addTableListener(tableListener);
				if (((IServerInternal)s).getConfig().isEnabled() && ((IServerInternal)s).isValid()) reloadAllColumnInfo(s);
			}

			public void serverRemoved(IServer s)
			{
				// remove error markers for server s
				((IServerInternal)s).removeTableListener(tableListener);
				removeErrorMarkers(((IServerInternal)s).getName());
			}
		};
		sm.addServerListener(serverListener);
	}

	public void loadAllColumnInfo(final Table t) throws RepositoryException
	{
		if (t == null) return;
		removeErrorMarker(t.getServerName(), t.getName());
		Iterator<Column> it = t.getColumns().iterator();
		while (it.hasNext())
		{
			it.next().removeColumnInfo();
		}
		IFile file = getDBIFile(t.getServerName(), t.getName());
		int existingColumnInfo = 0;
		boolean addMissingColumnMarkersIfNeeded = true;
		if (file.exists())
		{
			InputStream is = null;
			try
			{
				is = file.getContents(true);
				String json_table = Utils.getTXTFileContent(is, Charset.forName("UTF8"));
				IServerInternal s = (IServerInternal)sm.getServer(t.getServerName());
				if (s != null && s.getConfig().isEnabled() && s.isValid() && json_table != null)
				{
					existingColumnInfo = deserializeTable(t, json_table);
				}
			}
			catch (JSONException e)
			{
				// maybe the .dbi file content is corrupt... add an error marker
				addDeserializeErrorMarker(t, e.getMessage());
				throw new RepositoryException(e);
			}
			catch (CoreException e)
			{
				// maybe the .dbi file content is corrupt... add an error marker
				addDeserializeErrorMarker(t, e.getMessage());
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
		else
		{
			addMissingColumnMarkersIfNeeded = false; // no need adding missing column information markers if the file is missing altogether...
			addDifferenceMarker(new TableDifference(t, null, TableDifference.MISSING_DBI_FILE, null, null), IMarker.SEVERITY_ERROR);
		}

		Iterator<Column> columns = t.getColumns().iterator();
		while (columns.hasNext())
		{
			Column c = columns.next();
			if (c.getColumnInfo() == null)
			{
				if (addMissingColumnMarkersIfNeeded) addDifferenceMarker(new TableDifference(t, c.getName(), TableDifference.COLUMN_MISSING_FROM_DBI_FILE,
					null, null), IMarker.SEVERITY_ERROR);
				// only create servoy sequences when this was a new table and there is only 1 pk column
				createNewColumnInfo(c, existingColumnInfo == 0 && t.getPKColumnTypeRowIdentCount() == 1);//was missing - create automatic sequences if missing
			}
		}
	}

	public void createNewColumnInfo(Column c, boolean createMissingServoySequence) throws RepositoryException
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		int element_id = ServoyModel.getDeveloperRepository().getNewElementID(null);
		ColumnInfo ci = new ColumnInfo(element_id, false);
		if (createMissingServoySequence && c.getRowIdentType() != Column.NORMAL_COLUMN && c.getSequenceType() == ColumnInfo.NO_SEQUENCE_SELECTED &&
			(Column.mapToDefaultType(c.getType()) == IColumnTypes.INTEGER || Column.mapToDefaultType(c.getType()) == IColumnTypes.NUMBER))
		{
			ci.setAutoEnterType(ColumnInfo.SEQUENCE_AUTO_ENTER);
			ci.setAutoEnterSubType(ColumnInfo.SERVOY_SEQUENCE);
			ci.setSequenceStepSize(1);
		}
		c.setColumnInfo(ci);
	}

	// delete file
	public void removeAllColumnInfo(Table t) throws RepositoryException
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
	}

	// delete item
	public void removeColumnInfo(Column c) throws RepositoryException
	{
		Table t = c.getTable();
		c.removeColumnInfo();
		updateAllColumnInfo(t);
	}

	public void serializeAllColumnInfo(Table t, IFileAccess fileAccess, String projectName) throws RepositoryException
	{
		if (t == null) return;

		try
		{
			String out = serializeTable(t);
			fileAccess.setUTF8Contents(projectName + '/' + getRelativeServerPath(t.getServerName()) + IPath.SEPARATOR + getFileName(t.getName()), out);
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
		catch (IOException e)
		{
			throw new RepositoryException(e);
		}
	}

	public static void reloadAllColumnInfo(IServer server)
	{
		try
		{
			Iterator<String> tables = ((IServerInternal)server).getTableAndViewNames().iterator();
			while (tables.hasNext())
			{
				String tableName = tables.next();
				if (((IServerInternal)server).isTableLoaded(tableName))
				{
					((IServerInternal)server).reloadTableColumnInfo(((IServerInternal)server).getTable(tableName));
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		catch (DbcpException ex)
		{
			// the initialize of servers might not be completed at this point; so we may have an invalid server
			// don't do anything, this error should not exist 
		}
	}

	public void updateAllColumnInfo(final Table t) throws RepositoryException
	{
		if (t == null || !writeDBIFiles) return;

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
			// create file contents as string
			String out = serializeTable(t);
			try
			{
				InputStream source = new ByteArrayInputStream(out.getBytes("UTF8"));
				IFile file = getDBIFile(t.getServerName(), t.getName());
				writingMarkerFreeDBIFile = file;
//				boolean fileContentsWritten = false;
				if (file.exists())
				{
					int differenceType = differences.getDifferenceTypeForTable(t);
					if (differenceType != -1)
					{
						writingMarkerFreeDBIFile = null;
					}
					if (differenceType == TableDifference.COLUMN_CONFLICT || differenceType == TableDifference.COLUMN_MISSING_FROM_DB ||
						differenceType == TableDifference.COLUMN_MISSING_FROM_DBI_FILE)
					{
						// a .dbi file that has difference markers is about to be overwritten with information from memory;
						// in order to avoid an accidental overwrite, we will ask the user if he really wants to do this;
						// normally difference markers will be solved by quick fixes - not by editing database information from
						// memory and then saving it...
						ReturnValueRunnable asker = new ReturnValueRunnable()
						{
							public void run()
							{
								returnValue = new Boolean(
									MessageDialog.openQuestion(
										Display.getCurrent().getActiveShell(),
										"Unexpected database information file write",
										"The database information file (.dbi) contents for table '" +
											t.getName() +
											"' of server '" +
											t.getServerName() +
											"' are about to be written. This table currently has associated error markers for problems that might have prevented the loading of .dbi information in the first place. This means that you could be overwriting the current .dbi file contents with defaults.\nIf you are not sure why this happened, you should choose 'No', check the 'Problems' view for these error markers and try to solve them (see if context menu - Quick Fix is enabled).\n\nDo you wish to continue with the write?"));
							}
						};
						if (Display.getCurrent() != null)
						{
							asker.run();
						}
						else
						{
							Display.getDefault().syncExec(asker);
						}
						if (((Boolean)asker.getReturnValue()).booleanValue() == true)
						{
							// if the user chose 'Yes', write the contents
							file.setContents(source, true, false, null);
//							fileContentsWritten = true;
						}
					}
					else
					{
						file.setContents(source, true, false, null);
//						fileContentsWritten = true;
					}
				}
				else
				{
					writingMarkerFreeDBIFile = null; // do not inhibit reload of dbi file - because we could have error markers saying that the file does not exist - and they need to be cleared
					ResourcesUtils.createFileAndParentContainers(file, source, true);
//					fileContentsWritten = true;
				}
//				if (fileContentsWritten)
//				{
//					// remove changed flag for column infos that were written to file
//					for (Column c : t.getColumns())
//					{
//						ColumnInfo ci = c.getColumnInfo();
//						if (ci != null && c.getExistInDB()) // null would be strange here - but safer to check
//						{
//							ci.flagStored();
//						}
//					}
//				}
			}
			finally
			{
				writingMarkerFreeDBIFile = null;
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
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}
	}

	/**
	 * Will save the dbi file later or now, depending on the parameters.
	 * 
	 * @param t the table.
	 * @param writeBackLater specifies whether the column info should be updater now or later.
	 * @throws RepositoryException
	 */
	public void updateAllColumnInfo(final Table t, boolean writeBackLater) throws RepositoryException
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
						updateAllColumnInfo(t);
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
			updateAllColumnInfo(t);
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

	private int deserializeTable(Table t, String json_table) throws RepositoryException, JSONException
	{
		int existingColumnInfo = 0;
		TableDef tableInfo = deserializeTableInfo(json_table);
		if (!t.getName().equals(tableInfo.name))
		{
			throw new RepositoryException("Table name does not match dbi file name for " + t.getName());
		}
		if (tableInfo.columnInfoDefSet.size() > 0)
		{
			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			for (int j = 0; j < tableInfo.columnInfoDefSet.size(); j++)
			{
				ColumnInfoDef cid = tableInfo.columnInfoDefSet.get(j);

				String cname = cid.name;
				Column c = t.getColumn(cname);
				addDifferenceMarkersIfNecessary(c, cid, t, cname);

				if (c != null)
				{
					existingColumnInfo++;
					int element_id = ServoyModel.getDeveloperRepository().getNewElementID(null);
					ColumnInfo ci = new ColumnInfo(element_id, true);
					ci.setAutoEnterType(cid.autoEnterType);
					ci.setAutoEnterSubType(cid.systemValue);
					ci.setSequenceStepSize(cid.sequenceStepSize);
					ci.setPreSequenceChars(cid.preSequenceChars);
					ci.setPostSequenceChars(cid.postSequenceChars);
					ci.setDefaultValue(cid.defaultValue);
					ci.setLookupValue(cid.lookupValue);
					ci.setDatabaseSequenceName(cid.databaseSequenceName);
					ci.setTitleText(cid.titleText);
					ci.setDescription(cid.description);
					ci.setForeignType(cid.foreignType);
					ci.setConverterName(cid.converterName);
					ci.setConverterProperties(cid.converterProperties);
					ci.setValidatorProperties(cid.validatorProperties);
					ci.setValidatorName(cid.validatorName);
					ci.setDefaultFormat(cid.defaultFormat);
					ci.setElementTemplateProperties(cid.elementTemplateProperties);
					c.setColumnInfo(ci);
					c.setFlags(cid.flags); // updates rowident columns in Table as well
				}
			}
		}
		return existingColumnInfo;
	}

	private void addDifferenceMarkersIfNecessary(Column c, ColumnInfoDef cid, Table t, String columnName)
	{
		if (c == null)
		{
			if (t.getExistInDB()) addDifferenceMarker(new TableDifference(t, columnName, TableDifference.COLUMN_MISSING_FROM_DB, null, cid),
				IMarker.SEVERITY_ERROR); // else table is probably being created as we speak - and it's save/sync with DB will reload/rewrite the column info anyway
			// if we would add these markers even when table is being created, warnings for writing dbi files with error markers will appear
		}
		else
		{
			int severity = -1;
			if (c.getType() != cid.datatype || c.getLength() != cid.length)
			{
				int t1 = Column.mapToDefaultType(c.getType());
				int t2 = Column.mapToDefaultType(cid.datatype);
				if ((t1 == t2 && c.getLength() == cid.length) ||
					(t1 == IColumnTypes.NUMBER && c.getScale() == 0 && t2 == IColumnTypes.INTEGER) ||
					(t1 == t2 && (t1 == IColumnTypes.MEDIA || t1 == IColumnTypes.TEXT) && (Math.abs((float)c.getLength() - (float)cid.length) > (Integer.MAX_VALUE / 2)))) // this check is for -1 and big value lengths
				{
					severity = IMarker.SEVERITY_WARNING; // somewhat compatible types... but still different
				}
				else
				{
					severity = IMarker.SEVERITY_ERROR;
				}
			}
			else if (c.getAllowNull() != cid.allowNull)
			{
				severity = IMarker.SEVERITY_WARNING;
			}

			if (severity != IMarker.SEVERITY_ERROR) // if we already discovered an error, no use checking further
			{
				// real column can only know if it's pk or not (doesn't know about USER_ROWID_COLUMN)
				boolean columnInfoIsPk = ((cid.flags & Column.PK_COLUMN) != 0);
				if (c.isDatabasePK() != columnInfoIsPk)
				{
					if ((c.isDatabasePK() && ((cid.flags & Column.IDENT_COLUMNS) == 0)) || columnInfoIsPk)
					{
						// column is pk, but columninfo knows it as normal column, or column is not pk and columninfo knows it as pk
						severity = IMarker.SEVERITY_ERROR;
					}
					else if (c.isDatabasePK() && ((cid.flags & Column.USER_ROWID_COLUMN) != 0))
					{
						// columns is pk, column info says it's USER_ROWID_COLUMN - both ident columns, but not quite the same
						severity = IMarker.SEVERITY_WARNING;
					} // else no other case should be left
				}
			}

			if (severity != -1)
			{
				ColumnInfoDef dbCid = new ColumnInfoDef();
				dbCid.datatype = c.getType();
				dbCid.length = c.getLength();
				dbCid.allowNull = c.getAllowNull();
				dbCid.flags = c.getFlags();
				addDifferenceMarker(new TableDifference(t, columnName, TableDifference.COLUMN_CONFLICT, dbCid, cid), severity);
			}
		}
	}

	/**
	 * Gets the table information from a .dbi (JSON format) file like structured String.
	 * 
	 * @param stringDBIContent the table information in .dbi format
	 * @return the deserialized table information.
	 * @throws JSONException if the structure of the JSON in String stringDBIContent is bad.
	 */
	public TableDef deserializeTableInfo(String stringDBIContent) throws JSONException
	{
		ServoyJSONObject dbiContents = new ServoyJSONObject(stringDBIContent, true);
		TableDef tableInfo = new TableDef();
		tableInfo.name = dbiContents.getString(SolutionSerializer.PROP_NAME);
		tableInfo.tableType = dbiContents.getInt(PROP_TABLE_TYPE);

		if (dbiContents.has(PROP_COLUMNS))
		{
			JSONArray columns = dbiContents.getJSONArray(PROP_COLUMNS);
			for (int i = 0; i < columns.length(); i++)
			{
				JSONObject cobj = columns.getJSONObject(i);
				if (cobj == null) continue;
				ColumnInfoDef cid = new ColumnInfoDef();

				cid.creationOrderIndex = cobj.getInt("creationOrderIndex");
				cid.name = cobj.getString(SolutionSerializer.PROP_NAME);
				cid.datatype = cobj.getInt("dataType");
				cid.length = cobj.has("length") ? cobj.optInt("length") : 0;
				cid.allowNull = cobj.getBoolean("allowNull");
				cid.autoEnterType = cobj.has("autoEnterType") ? cobj.optInt("autoEnterType") : ColumnInfo.NO_AUTO_ENTER;
				cid.systemValue = cobj.has("autoEnterSubType") ? cobj.optInt("autoEnterSubType") : ColumnInfo.NO_SEQUENCE_SELECTED;
				cid.sequenceStepSize = cobj.has("sequenceStepSize") ? cobj.optInt("sequenceStepSize") : 1;
				cid.preSequenceChars = cobj.has("preSequenceChars") ? cobj.optString("preSequenceChars") : null;
				cid.postSequenceChars = cobj.has("postSequenceChars") ? cobj.optString("postSequenceChars") : null;
				cid.defaultValue = cobj.has("defaultValue") ? cobj.optString("defaultValue") : null;
				cid.lookupValue = cobj.has("lookupValue") ? cobj.optString("lookupValue") : null;
				cid.databaseSequenceName = cobj.has("databaseSequenceName") ? cobj.optString("databaseSequenceName") : null;
				cid.titleText = cobj.has("titleText") ? cobj.optString("titleText") : null;
				cid.description = cobj.has("description") ? cobj.optString("description") : null;
				cid.foreignType = cobj.has("foreignType") ? cobj.optString("foreignType") : null;
				cid.converterName = cobj.has("converterName") ? cobj.optString("converterName") : null;
				cid.converterProperties = cobj.has("converterProperties") ? cobj.optString("converterProperties") : null;
				cid.validatorProperties = cobj.has("validatorProperties") ? cobj.optString("validatorProperties") : null;
				cid.validatorName = cobj.has("validatorName") ? cobj.optString("validatorName") : null;
				cid.defaultFormat = cobj.has("defaultFormat") ? cobj.optString("defaultFormat") : null;
				cid.elementTemplateProperties = cobj.has("elementTemplateProperties") ? cobj.optString("elementTemplateProperties") : null;
				cid.flags = cobj.has("flags") ? cobj.optInt("flags") : 0;

				if (!tableInfo.columnInfoDefSet.contains(cid))
				{
					tableInfo.columnInfoDefSet.add(cid);
				}
			}
		}
		return tableInfo;
	}

	private String serializeTable(Table t) throws JSONException
	{
		TableDef tableInfo = new TableDef();
		tableInfo.name = t.getName();

		List<String> colNames = new ArrayList<String>();
		Collection<Column> col = t.getColumns();
		for (Column column : col)
		{
			colNames.add(column.getName());
		}

		Iterator<Column> it = t.getColumnsSortedByName();
		while (it.hasNext())
		{
			Column column = it.next();
			ColumnInfoDef cid = getColumnInfoDef(column, colNames.indexOf(column.getName()));
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

	/**
	 * Puts all information about the given column into a ColumnInfoDef object, if the give column has column info.
	 * 
	 * @param column the column to be inspected.
	 * @param creationOrderIndex the creationOrderIndex for this column.
	 * @return all information about the given column into a ColumnInfoDef object, if the give column has column info and null otherwise.
	 */
	public static ColumnInfoDef getColumnInfoDef(Column column, int creationOrderIndex)
	{
		ColumnInfoDef cid = null;
		ColumnInfo ci = column.getColumnInfo();
		if (ci != null && column.getExistInDB())
		{
			cid = new ColumnInfoDef();
			cid.name = column.getName();
			cid.datatype = column.getType();
			cid.length = column.getLength();
			cid.allowNull = column.getAllowNull();
			cid.flags = column.getFlags();
			cid.creationOrderIndex = creationOrderIndex;
			cid.autoEnterType = ci.getAutoEnterType();
			cid.systemValue = ci.getAutoEnterSubType();
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
			cid.elementTemplateProperties = ci.getElementTemplateProperties();
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
		tobj.put(PROP_TABLE_TYPE, tableInfo.tableType);

		JSONArray carray = new ServoyJSONArray();
		Iterator<ColumnInfoDef> it = tableInfo.columnInfoDefSet.iterator();
		while (it.hasNext())
		{
			ColumnInfoDef cid = it.next();
			ServoyJSONObject obj = new ServoyJSONObject();
			obj.put(SolutionSerializer.PROP_NAME, cid.name);
			obj.put("dataType", cid.datatype);
			if (cid.length != 0) obj.put("length", cid.length);
			obj.put("allowNull", cid.allowNull);
			if (cid.flags != 0) obj.put("flags", cid.flags);

			obj.put("creationOrderIndex", cid.creationOrderIndex);

			if (cid.autoEnterType != ColumnInfo.NO_AUTO_ENTER)
			{
				obj.put("autoEnterType", cid.autoEnterType);
				if (cid.systemValue != ColumnInfo.NO_SEQUENCE_SELECTED) obj.put("autoEnterSubType", cid.systemValue);
			}
			if (cid.sequenceStepSize > 1) obj.put("sequenceStepSize", cid.sequenceStepSize);
			obj.putOpt("preSequenceChars", cid.preSequenceChars);
			obj.putOpt("postSequenceChars", cid.postSequenceChars);
			obj.putOpt("defaultValue", cid.defaultValue);
			obj.putOpt("lookupValue", cid.lookupValue);
			obj.putOpt("databaseSequenceName", cid.databaseSequenceName);
			obj.putOpt("titleText", cid.titleText);
			obj.putOpt("description", cid.description);
			obj.putOpt("foreignType", cid.foreignType);
			obj.putOpt("converterName", cid.converterName);
			obj.putOpt("converterProperties", cid.converterProperties);
			obj.putOpt("validatorProperties", cid.validatorProperties);
			obj.putOpt("validatorName", cid.validatorName);
			obj.putOpt("defaultFormat", cid.defaultFormat);
			obj.putOpt("elementTemplateProperties", cid.elementTemplateProperties);
			carray.put(obj);
		}
		tobj.put(PROP_COLUMNS, carray);
		return tobj.toString(true);
	}

	public static String getFileName(String tableName)
	{
		return tableName + COLUMN_INFO_FILE_EXTENSION_WITH_DOT;
	}

	public static String getRelativeServerPath(String serverName)
	{
		if (serverName == null) return "";
		return SolutionSerializer.DATASOURCES_DIR_NAME + IPath.SEPARATOR + serverName + IPath.SEPARATOR;
	}

	public IFile getDBIFile(String serverName, String tableName)
	{
		IPath path = new Path(getRelativeServerPath(serverName) + IPath.SEPARATOR + getFileName(tableName));
		return resourceProject.getFile(path);
	}

	public IFolder getDBIFileContainer(String serverName)
	{
		IPath path = new Path(getRelativeServerPath(serverName));
		return resourceProject.getFolder(path);
	}

	public void setWritesEnabled(boolean b)
	{
		writeDBIFiles = b;
	}

	public void dispose()
	{
		clearProblemMarkers();
		sm.removeServerListener(serverListener);
		// add listeners to initial server list
		String[] array = sm.getServerNames(false, false, true, true);
		for (String server_name : array)
		{
			IServerInternal server = (IServerInternal)sm.getServer(server_name, false, false);
			server.removeTableListener(tableListener);
		}
	}

	public void updateMarkerStatesForMissingTable(IResourceDelta fileRd, String serverName, String tableName)
	{
		if ((fileRd.getFlags() & IResourceDelta.MOVED_TO) != 0) // this is probably a delete
		{
			// it was moved somewhere - remove previous markers (this will clear the difference, but not the real marker, as file probably changed name/location)
			removeErrorMarker(serverName, tableName);
		}
		else if ((fileRd.getFlags() & IResourceDelta.MOVED_FROM) != 0)
		{
			// it was moved from somewhere - remove previous markers (this will clear real markers, cause difference was probably removed when MOVED_TO arrived)
			removeErrorMarker(serverName, tableName);
		}

		if (fileRd.getKind() == IResourceDelta.ADDED) // added or moved from somewhere
		{
			// a .dbi file was added for a table that does not exist - add problem marker
			addDifferenceMarker(new TableDifference(serverName, tableName, null, TableDifference.MISSING_TABLE, null, null), IMarker.SEVERITY_ERROR);
		}
	}

	private void addDifferenceMarker(final TableDifference columnDifference, final int severity)
	{
		differences.addDifference(columnDifference);
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
				if (resource.exists())
				{
					// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
					// the project might have disappeared before this job was started... (delete)
					try
					{
						IMarker marker = resource.createMarker(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
						marker.setAttribute(IMarker.MESSAGE, columnDifference.getUserFriendlyMessage());
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
							missingDbiFileMarkerIds.put(columnDifference.getServerName() + '.' + columnDifference.getTableName(), marker.getId());
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

	private void addDeserializeErrorMarker(Table t, final String message)
	{
		differences.addDifference(new TableDifference(t));
		final IFile file = getDBIFile(t.getServerName(), t.getName());
		updateProblemMarkers(new Runnable()
		{
			public void run()
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
					String msg = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_BadDBInfo, message);
					ServoyBuilder.addMarker(file, ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE, msg, charNo, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
						"JSON file");
				}
			}
		});
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
			if (id != null)
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
		final IFolder folder = getDBIFileContainer(serverName);
		updateProblemMarkers(new Runnable()
		{
			public void run()
			{
				try
				{
					if (folder.exists() && folder.findMarkers(ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE, false, IResource.DEPTH_INFINITE).length > 0)
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
			}
		});
	}


	private void clearProblemMarkers()
	{
		differences.removeAllDifferences();
		updateProblemMarkers(new Runnable()
		{
			public void run()
			{
				if (resourceProject.exists())
				{
					// because this is executed async (problem markers cannot be added/removed when on resource change notification thread)
					// the project might have disappeared before this job was started... (delete)
					ServoyBuilder.deleteMarkers(resourceProject, ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
				}
				missingDbiFileMarkerIds.clear();
			}
		});
	}

	public TableDifference getColumnDifference(String serverName, String tableName, String columnName)
	{
		return differences.getDifference(serverName, tableName, columnName);
	}

	private void updateProblemMarkers(final Runnable r)
	{
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

		public static final int COLUMN_MISSING_FROM_DB = 1;
		public static final int COLUMN_MISSING_FROM_DBI_FILE = 2;
		public static final int COLUMN_CONFLICT = 3;
		public static final int DESERIALIZE_PROBLEM = 4;
		public static final int MISSING_TABLE = 5;
		public static final int MISSING_DBI_FILE = 6;

		private final String serverName;
		private final String tableName;
		private Table table;
		private final String columnName;
		private final int type;
		private final ColumnInfoDef tableDefinition;
		private final ColumnInfoDef dbiFileDefinition;

		private TableDifference(Table table)
		{
			this(table, null, DESERIALIZE_PROBLEM, null, null);
		}

		private TableDifference(String serverName, String tableName, String columnName, int type, ColumnInfoDef tableDefinition, ColumnInfoDef dbiFileDefinition)
		{
			this.serverName = serverName;
			this.tableName = tableName;
			this.columnName = columnName;
			this.type = type;
			this.tableDefinition = tableDefinition;
			this.dbiFileDefinition = dbiFileDefinition;
		}

		private TableDifference(Table table, String columnName, int type, ColumnInfoDef tableDefinition, ColumnInfoDef dbiFileDefinition)
		{
			this(table.getServerName(), table.getName(), columnName, type, tableDefinition, dbiFileDefinition);
			this.table = table;
		}

		public String getTableName()
		{
			return tableName;
		}

		public String getServerName()
		{
			return serverName;
		}

		public Table getTable()
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

		public String getUserFriendlyMessage()
		{
			String message;
			if (type == COLUMN_MISSING_FROM_DB)
			{
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_ColumnMissingFromDB, getColumnString());
			}
			else if (type == COLUMN_MISSING_FROM_DBI_FILE)
			{
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_ColumnMissingFromDBIFile, getColumnString());
			}
			else if (type == COLUMN_CONFLICT)
			{
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_ColumnConflict, getColumnString(), getColumnDefinition(tableDefinition),
					getColumnDefinition(dbiFileDefinition));
			}
			else if (type == MISSING_TABLE)
			{
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_TableMissing, serverName + "->" + tableName); //$NON-NLS-1$
			}
			else if (type == MISSING_DBI_FILE)
			{
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_DBIFileMissing, serverName + "->" + tableName); //$NON-NLS-1$
			}
			else
			{
				// this should not be reached in normal execution...
				message = MarkerMessages.getMessage(MarkerMessages.Marker_DBI_GenericError, getColumnString());
			}
			return message;
		}

		private String getColumnDefinition(ColumnInfoDef definition)
		{
			StringBuffer message = new StringBuffer();
			message.append("("); //$NON-NLS-1$
			if ((definition.flags & Column.PK_COLUMN) != 0)
			{
				message.append("pk, "); //$NON-NLS-1$
			}
			else if ((definition.flags & Column.USER_ROWID_COLUMN) != 0)
			{
				message.append("row_ident, "); //$NON-NLS-1$
			}
			message.append(Column.getDisplayTypeString(definition.datatype));
			message.append("(id:"); //$NON-NLS-1$
			message.append(definition.datatype);
			message.append("), length: "); //$NON-NLS-1$
			message.append(definition.length);
			message.append(", allowNull: "); //$NON-NLS-1$
			message.append(definition.allowNull);
			message.append(")"); //$NON-NLS-1$
			return message.toString();
		}

		public String getColumnString()
		{
			StringBuffer message = new StringBuffer();
			message.append(serverName);
			message.append("->"); //$NON-NLS-1$
			message.append(tableName);
			message.append("->"); //$NON-NLS-1$
			message.append(columnName);
			return message.toString();
		}
	}

	private static class TableDifferencesHolder
	{
		HashMap<String, Integer> differenceTypes = new HashMap<String, Integer>();
		List<TableDifference> differences = new ArrayList<TableDifference>();

		public synchronized void addDifference(TableDifference d)
		{
			differenceTypes.put(d.getServerName() + '.' + d.getTableName(), new Integer(d.getType()));
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

		public void removeDifferences(String serverName)
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

		public void removeDifferences(String serverName, String tableName)
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

		public synchronized int getDifferenceTypeForTable(Table t)
		{
			Integer result = differenceTypes.get(t.getServerName() + '.' + t.getName());
			return result == null ? -1 : result.intValue();
		}

	}

}