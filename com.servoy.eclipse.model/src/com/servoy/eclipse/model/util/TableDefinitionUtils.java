/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.json.JSONException;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsAndSecurityBasedOnWorkspaceFiles;
import com.servoy.j2db.util.xmlxport.MetadataDef;
import com.servoy.j2db.util.xmlxport.ServerDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * @author acostache
 *
 */
public class TableDefinitionUtils
{
	public static Pair<ITableDefinitionsAndSecurityBasedOnWorkspaceFiles, IMetadataDefManager> getTableDefinitionsFromDBI(IServerInternal server)
		throws CoreException, JSONException, IOException
	{
		final Map<String, List<String>> neededServersTables = new HashMap<String, List<String>>();
		neededServersTables.put(server.getName(), new ArrayList<String>());
		// just retrieve all tables from this server
		return getTableDefinitionsFromDBI(neededServersTables, true, true);
	}

	public static TableDef loadTableDef(IFile file) throws IOException, CoreException
	{
		if (file.exists())
		{
			String dbiFileContent = null;
			try (InputStream is = file.getContents(true))
			{
				dbiFileContent = Utils.getTXTFileContent(is, Charset.forName("UTF-8"));
			}

			if (dbiFileContent != null)
			{
				return DatabaseUtils.deserializeTableInfo(dbiFileContent);
			}
		}
		return null;
	}

	private static Pair<ITableDefinitionsAndSecurityBasedOnWorkspaceFiles, IMetadataDefManager> getTableDefinitionsFromDBI(
		Map<String, List<String>> neededServersTables, boolean exportAllTablesFromReferencedServers, boolean exportMetaData)
		throws CoreException, JSONException, IOException
	{
		DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();

		if (dmm == null) return null;
		// B. for needed tables, get dbi files (db is down)
		Map<String, List<IFile>> server_tableDbiFiles = new HashMap<>();
		for (Entry<String, List<String>> neededServersTableEntry : neededServersTables.entrySet())
		{
			final String serverName = neededServersTableEntry.getKey();
			final List<String> tables = neededServersTableEntry.getValue();
			final List<IFile> dbiz = getServerTableinfo(serverName, DataModelManager.COLUMN_INFO_FILE_EXTENSION, tables, exportAllTablesFromReferencedServers);

			// minimum requirement for dbi files based export: all needed dbi files must be found
			List<String> notFoundDBIFileTableNames = allNeededDbiFilesExist(tables, dbiz);
			if (notFoundDBIFileTableNames.size() > 0)
			{
				throw new FileNotFoundException("Could not locate all needed dbi files for server '" + serverName + "', tablenames: '" +
					Arrays.toString(notFoundDBIFileTableNames.toArray()) + "'.\nPlease make sure the necessary files exist.");
			}

			server_tableDbiFiles.put(serverName, dbiz);
		}

		// C. deserialize server and table dbis to get tabledefs and metadata info
		Map<ServerDef, List<TableDef>> serverTableDefs = new HashMap<>();
		List<MetadataDef> metadataDefs = new ArrayList<MetadataDef>();
		for (Entry<String, List<IFile>> server_tableDbiFile : server_tableDbiFiles.entrySet())
		{
			String serverName = server_tableDbiFile.getKey();
			ServerDef serverDef = new ServerDef(serverName);

			IFile serverDBIFile = ServoyModelFinder.getServoyModel().getDataModelManager().getServerDBIFile(serverName);
			if (serverDBIFile.exists())
			{
				try (InputStream is = serverDBIFile.getContents(true))
				{
					serverDef.dbiFileContents = Utils.getTXTFileContent(is, Charset.forName("UTF-8"));
				}
			}

			List<TableDef> tableDefs = new ArrayList<>();
			for (IFile file : server_tableDbiFile.getValue())
			{
				TableDef tableInfo = loadTableDef(file);
				if (tableInfo != null)
				{
					tableDefs.add(tableInfo);
					if (exportMetaData && tableInfo.isMetaData != null && tableInfo.isMetaData.booleanValue())
					{
						String ds = DataSourceUtils.createDBTableDataSource(serverName, tableInfo.name);
						IFile mdf = dmm.getMetaDataFile(ds);
						if (mdf != null && mdf.exists())
						{
							String wscontents = null;
							wscontents = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getUTF8Contents(mdf.getFullPath().toString());
							if (wscontents != null)
							{
								MetadataDef mdd = new MetadataDef(ds, wscontents);
								if (!metadataDefs.contains(mdd)) metadataDefs.add(mdd);
							}
						}
					}
				}
			}
			serverTableDefs.put(serverDef, tableDefs);
		}

		ITableDefinitionsAndSecurityBasedOnWorkspaceFiles tableDefManager = null;
		IMetadataDefManager metadataDefManager = null;

		// D. make use of tabledef info and metadata for the managers
		tableDefManager = new ITableDefinitionsAndSecurityBasedOnWorkspaceFiles()
		{
			private DBSecurityDefinitionsBasedOnFiles dBSecurityDefinitionsBasedOnFiles;

			public Map<ServerDef, List<TableDef>> getServerTableDefs()
			{
				return serverTableDefs;
			}

			@Override
			public List<SecurityInfo> getDatabaseSecurityInfoByGroup(String groupName)
			{
				if (dBSecurityDefinitionsBasedOnFiles == null) dBSecurityDefinitionsBasedOnFiles = new DBSecurityDefinitionsBasedOnFiles(this);
				return dBSecurityDefinitionsBasedOnFiles.getDatabaseSecurityInfoByGroup(groupName);
			}
		};

		metadataDefManager = new IMetadataDefManager()
		{
			private List<MetadataDef> metadataDefList = new ArrayList<MetadataDef>();

			public void setMetadataDefsList(List<MetadataDef> metadataDefList)
			{
				this.metadataDefList = metadataDefList;
			}

			public List<MetadataDef> getMetadataDefsList()
			{
				return this.metadataDefList;
			}
		};
		metadataDefManager.setMetadataDefsList(metadataDefs);

		return new Pair<ITableDefinitionsAndSecurityBasedOnWorkspaceFiles, IMetadataDefManager>(tableDefManager, metadataDefManager);
	}

	public static Pair<ITableDefinitionsAndSecurityBasedOnWorkspaceFiles, IMetadataDefManager> getTableDefinitionsFromDBI(Solution activeSolution,
		boolean exportReferencedModules, boolean exportI18NData, boolean exportAllTablesFromReferencedServers, boolean exportMetaData)
		throws CoreException, JSONException, IOException
	{
		// A. get only the needed servers (and tables)
		final Map<String, List<String>> neededServersTables = getNeededServerTables(activeSolution, exportReferencedModules, exportI18NData);
		return getTableDefinitionsFromDBI(neededServersTables, exportAllTablesFromReferencedServers, exportMetaData);
	}

	public static List<IFile> getServerTableinfo(String serverName, String fileExtension, List<String> tablesNeeded, boolean exportAll)
	{
		IFolder serverInformationFolder = ServoyModelFinder.getServoyModel().getDataModelManager().getServerInformationFolder(serverName);
		List<IFile> files = new ArrayList<>();

		if (serverInformationFolder.exists())
		{
			try
			{
				serverInformationFolder.accept(new IResourceVisitor()
				{
					public boolean visit(IResource resource) throws CoreException
					{
						String extension = resource.getFileExtension();
						if (resource instanceof IFile && fileExtension.equalsIgnoreCase(extension))
						{
							// we found a table file
							String tableName = resource.getName().substring(0, resource.getName().length() - (fileExtension.length() + 1));
							if ((tablesNeeded != null && tablesNeeded.contains(tableName)) || exportAll)
							{
								files.add((IFile)resource);
							}
						}
						return true;
					}

				}, IResource.DEPTH_ONE, false);
			}
			catch (CoreException e)
			{
				Debug.error(e);
			}
		}
		return files;
	}

	private static List<String> allNeededDbiFilesExist(List<String> neededTableNames, List<IFile> existingDbiFiles)
	{
		if (neededTableNames != null && existingDbiFiles != null && neededTableNames.size() > 0 && existingDbiFiles.size() == 0) return neededTableNames;

		List<String> dbiFileNames = new ArrayList<String>(existingDbiFiles.size());
		for (IFile f : existingDbiFiles)
		{
			if (f != null)
			{
				int i = f.getName().lastIndexOf(".dbi");
				dbiFileNames.add(f.getName().substring(0, i));
			}
		}

		List<String> notFoundDbiForTable = new ArrayList<String>();
		for (String tableName : neededTableNames)
		{
			if (!dbiFileNames.contains(tableName))
			{
				notFoundDbiForTable.add(tableName);
			}
		}

		return notFoundDbiForTable;
	}

	private static void addServerTable(Map<String, List<String>> srvTbl, String serverName, String tableName)
	{
		List<String> tablesForServer = srvTbl.get(serverName);
		if (tablesForServer == null)
		{
			tablesForServer = new ArrayList<String>();
		}
		if (!tablesForServer.contains(tableName)) tablesForServer.add(tableName);
		srvTbl.put(serverName, tablesForServer);
	}

	/**
	 * @param mainActiveSolution if null all tables from all servers are specified
	 * @param includeModules
	 * @param includeI18NData
	 * @return
	 */
	public static Map<String, List<String>> getNeededServerTables(Solution mainActiveSolution, boolean includeModules, boolean includeI18NData)
	{
		DataSourceCollectorVisitor collector = new DataSourceCollectorVisitor();
		//IF NO SOLUTION SPECIFFIED GET ALL TABLES FROM ALL SERVERS
		if (mainActiveSolution == null)
		{
			IServerManagerInternal sm = ApplicationServerRegistry.get().getServerManager();
			String[] serverNames = sm.getServerNames(true, true, false, false);
			Map<String, List<String>> neededServersTablesMap = new HashMap<String, List<String>>();
			try
			{
				for (String serverName : serverNames)
				{
					for (IFile tableDbiFile : getServerTableinfo(serverName, DataModelManager.COLUMN_INFO_FILE_EXTENSION, null, true))
					{
						String name = tableDbiFile.getName();
						String tableName = name.substring(0, name.indexOf(DataModelManager.COLUMN_INFO_FILE_EXTENSION_WITH_DOT)).toLowerCase();
						addServerTable(neededServersTablesMap, serverName, tableName);
					}
				}
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
			return neededServersTablesMap;
		}
		//NORMAL CASE WHENSOLUTION SPECIFIED AS PARAMETER
		//get modules to export if needed, or just the active project
		if (includeModules)
		{
			ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject(); // TODO shouldn't this be done selectively/after the developer chooses what modules he wants exported?
			for (ServoyProject module : modules)
			{
				module.getEditingSolution().acceptVisitor(collector);
			}
		}
		else mainActiveSolution.acceptVisitor(collector);

		Map<String, List<String>> neededServersTablesMap = new HashMap<String, List<String>>();
		Set<String> dataSources = collector.getDataSources();
		Set<String> serverNames = DataSourceUtils.getServerNames(dataSources);

		for (String serverName : serverNames)
		{
			for (String tableName : DataSourceUtils.getServerTablenames(dataSources, serverName))
			{
				addServerTable(neededServersTablesMap, serverName, tableName);
			}
		}

		// check if i18n info is needed
		if (mainActiveSolution.getI18nDataSource() != null && includeI18NData)
			addServerTable(neededServersTablesMap, mainActiveSolution.getI18nServerName(), mainActiveSolution.getI18nTableName());

		return neededServersTablesMap;
	}

	private static boolean isDatabaseDownErrorMarkerThatCouldBeIgnoredOnExport(IMarker marker)
	{
		try
		{
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR && ServoyBuilder.MISSING_SERVER.equals(marker.getType()))
			{
				IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager()
					.getServer((String)marker.getAttribute("missingServer"), false, false);
				return server != null && server.getConfig().isEnabled(); //if the server is not present in the servoy.properties file or if it is disabled, we don't want to skip this marker
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return false;
	}

	/**
	 * Checks if there are db error markers that can be ignored on export (due to db not started).
	 * For a db marker to be ignored, the server must be present in the servoy.properties file and it must be enabled.
	 * Otherwise it cannot be ignored on export.
	 * @return true if the database is down but present in .properties file and enabled
	 */
	public static boolean hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(String[] projects)
	{
		if (projects != null && projects.length > 0)
		{

			for (String moduleName : projects)
			{
				ServoyProject module = ServoyModelFinder.getServoyModel().getServoyProject(moduleName);
				if (module != null)
				{
					try
					{
						IMarker[] markers = module.getProject().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
						for (IMarker marker : markers)
						{
							// db down errors = missing server (what other cases?)
							if (isDatabaseDownErrorMarkerThatCouldBeIgnoredOnExport(marker))
							{
								return true;
							}
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}

				}
			}

		}
		return false;
	}
}
