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

package com.servoy.eclipse.exporter.apps.solution;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.ILogLevel;
import com.servoy.j2db.util.xmlxport.IXMLExportUserChannel;

/**
 * Stores and provides the export-relevant arguments the product was started with.
 * @author acostescu
 */
public class ArgumentChest extends AbstractArgumentChest implements IXMLExportUserChannel
{
	private static final String FILE_EXTENSION = ".servoy";
	
	private static final int META_DATA_NONE = 0;
	private static final int META_DATA_WS = 1;
	private static final int META_DATA_DB = 2;
	private static final int META_DATA_BOTH = 3;


	private boolean exportSampleData = false;
	private int metadataSource = META_DATA_WS;
	private int sampleDataCount = 10000;
	private boolean exportI18N = false;
	private boolean exportUsers = false;
	private boolean exportModules = false;
	private List<String> moduleList = null;
	private boolean exportAllTablesFromReferencedServers = false;
	private String protectionPassword = null;

	public ArgumentChest(String[] args)
	{
		super();
		initialize(args);
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap)
	{
		metadataSource = parseMetadata(argsMap);
		if (argsMap.containsKey("sd"))
		{
			exportSampleData = true;
			sampleDataCount = parseSampleDataCount(argsMap);
		}
		if (argsMap.containsKey("i18n")) exportI18N = true;
		if (argsMap.containsKey("users")) exportUsers = true;
		if (argsMap.containsKey("tables")) exportAllTablesFromReferencedServers = true;
		protectionPassword = parseArg("pwd", "Protection password was not specified after '-pwd' argument.", argsMap);

		if (argsMap.containsKey("modules"))
		{
			exportModules = true;
			String modules = parseArg("modules", null, argsMap);
			if (modules != null) moduleList = Arrays.asList(modules.split(" "));
		}
	}

	private int parseMetadata(HashMap<String, String> argsMap)
	{
		if (argsMap.containsKey("md"))
		{
			String mdarg = argsMap.get("md");
			if (!mdarg.equals(""))
			{
				if ("ws".equals(mdarg)) return META_DATA_WS;
				if ("db".equals(mdarg)) return META_DATA_DB;
				if ("none".equals(mdarg)) return META_DATA_NONE;
				if ("both".equals(mdarg)) return META_DATA_BOTH;

				info("unknown meta data source '" + mdarg + "'", ILogLevel.ERROR);
				markInvalid();
			}
			else
			{
				info("meta data source was not specified after '-md' argument.", ILogLevel.ERROR);
				markInvalid();
			}
		}
		return META_DATA_WS;
	}


	@Override
	public String getHelpMessage()
	{
		// @formatter:off
		return "Workspace exporter. Exports workspace solutions into .servoy files.\n"
			+ super.getHelpMessageCore()
			+ "        -md ws|db|none|both ... take table  metadata from workspace / database / both+check.\n"
			+ "             Usually you will want to use 'ws'.\n"
			+ "        -sd ... exports sample data. IMPORTANT: all needed DB\n"
			+ "             servers must already be started\n"
			+ "        -sdcount <count> ... number of rows to  export per table. Only  makes sense when -sd\n"
			+ "             is also present. Can be 'all' (without the '). Default: 10000\n"
			+ "        -i18n ... exports i18n data\n"
			+ "        -users ... exports users\n"
			+ "        -tables ... export  all table  information  about  tables from  referenced  servers.\n"
			+ "             IMPORTANT: all needed DB servers must already be started\n"
			+ "        -pwd <protection_password> ... protect  the exported  solution with given  password.\n"
			+ "        -modules [<module1_name> <module2_name> ... <moduleN_name>]\n"
			+ "             argument  specified in command line. Includes all or part of referenced modules\n"
			+ "             in export. If only '-modules' is used,  it will export all  referenced modules.\n"
			+ "             If a list of  modules is also included, it  will export only  modules from this\n"
			+ "             list, provided they are referenced by exported solution.\n"
			+ getHelpMessageExistCodes();
		// @formatter:on
	}

	public boolean shouldExportMetaData()
	{
		return metadataSource != META_DATA_NONE;
	}

	public boolean shouldExportSampleData()
	{
		return exportSampleData;
	}

	public int getNumberOfSampleDataExported()
	{
		return sampleDataCount;
	}

	public boolean shouldExportI18NData()
	{
		return exportI18N;
	}

	public boolean shouldExportUsers()
	{
		return exportUsers;
	}

	public boolean shouldExportModules()
	{
		return exportModules;
	}

	public boolean shouldProtectWithPassword()
	{
		return protectionPassword != null;
	}

	// IXMLExportUserChannel methods:

	public boolean getExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	public <T extends List<String>> T getModuleIncludeList(T allModules)
	{
		if (moduleList != null)
		{
			String allModulesString = Arrays.toString(allModules.toArray());
			allModules.retainAll(moduleList);
			if (allModules.size() != moduleList.size())
			{
				info(
					"Some of the modules specified for export where not actually modules of exported solution. All solution modules: '" + allModulesString + "', to exported modules: '" + Arrays.toString(moduleList.toArray()) + "'", ILogLevel.ERROR); //$NON-NLS-1$
				return null;
			}
		}
		return allModules;
	}

	public String getProtectionPassword()
	{
		return protectionPassword;
	}

	public String getTableMetaData(ITable table) throws IOException
	{
		DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();
		if (dmm == null)
		{
			throw new IOException("Error exporting table meta data, Cannot find internal data model manager.");
		}

		String wscontents = null;
		if ((metadataSource | META_DATA_WS) != 0)
		{
			wscontents = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()).getUTF8Contents(dmm.getMetaDataFile(table.getDataSource()).getFullPath().toString());
		}
		if (metadataSource == META_DATA_WS)
		{
			return wscontents;
		}

		String dbcontents = null;
		if ((metadataSource | META_DATA_DB) != 0)
		{
			try
			{
				dbcontents = MetaDataUtils.generateMetaDataFileContents((Table)table, -1);
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				throw new IOException("Could not check table meta data from database " + e.getMessage());
			}
		}
		if (metadataSource == META_DATA_DB)
		{
			return dbcontents;
		}

		// check if current contents matches data file
		if (wscontents != null && !wscontents.equals(dbcontents))
		{
			throw new IOException("Checking table meta data failed for table '" + table.getName() + "' in server '" + table.getServerName() +
				"', current workspace contents does not match current database contents.\n" + //
				"update the meta data for this table first");
		}

		return wscontents;
	}

	public String getExportFileName(String solutionName)
	{
		File f = new File(getExportFilePath(), solutionName + FILE_EXTENSION);
		return f.getAbsolutePath();
	}

}