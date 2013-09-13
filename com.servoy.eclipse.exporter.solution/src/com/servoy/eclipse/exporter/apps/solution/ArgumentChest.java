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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
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
	private static final String FILE_EXTENSION = ".servoy"; //$NON-NLS-1$

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
	@SuppressWarnings("nls")
	protected void parseArguments(String[] args)
	{
		if (!mustShowHelp())
		{
			int i = 0;
			while (i < args.length)
			{
				if ("-md".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						String mdarg = args[++i];
						if ("ws".equals(mdarg))
						{
							metadataSource = META_DATA_WS;
						}
						else if ("db".equals(mdarg))
						{
							metadataSource = META_DATA_DB;
						}
						else if ("none".equals(mdarg))
						{
							metadataSource = META_DATA_NONE;
						}
						else if ("both".equals(mdarg))
						{
							metadataSource = META_DATA_BOTH;
						}
						else
						{
							info("unknown meta data source '" + mdarg + "'", ILogLevel.ERROR);
							markInvalid();
						}
					}
					else
					{
						info("meta data source was not specified after '-md' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-sd".equalsIgnoreCase(args[i]))
				{
					exportSampleData = true;
				}
				else if ("-sdcount".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						try
						{
							sampleDataCount = Integer.parseInt(args[++i]);
							if (sampleDataCount < 1)
							{
								sampleDataCount = 1;
								info("Number of rows to export per table cannot be < 1. Corrected to 1.", ILogLevel.ERROR);
							}
							else if (sampleDataCount > IDataServerInternal.MAX_ROWS_TO_RETRIEVE)
							{
								sampleDataCount = IDataServerInternal.MAX_ROWS_TO_RETRIEVE;
								info("Number of rows to export per table cannot be > " + IDataServerInternal.MAX_ROWS_TO_RETRIEVE + ". Corrected.",
									ILogLevel.ERROR);
							}
						}
						catch (NumberFormatException e)
						{
							info("Number of rows to export per table specified after '-sdcount' argument is not an integer value.", ILogLevel.ERROR);
							markInvalid();
						}
					}
					else
					{
						info("Number of rows to export per table was not specified after '-sdcount' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-i18n".equalsIgnoreCase(args[i]))
				{
					exportI18N = true;
				}
				else if ("-users".equalsIgnoreCase(args[i]))
				{
					exportUsers = true;
				}
				else if ("-tables".equalsIgnoreCase(args[i]))
				{
					exportAllTablesFromReferencedServers = true;
				}
				else if ("-pwd".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						protectionPassword = args[++i];
					}
					else
					{
						info("Protection password was not specified after '-pwd' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-modules".equalsIgnoreCase(args[i]))
				{
					exportModules = true;
					if (i < (args.length - 1))
					{
						moduleList = new ArrayList<String>();
						while ((++i) < args.length)
						{
							moduleList.add(args[i]);
						}
					}
				}
				i++;
			}
		}
	}

	@Override
	@SuppressWarnings("nls")
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
			+ "        -modules [<module1_name> <module2_name> ... <moduleN_name>] ... MUST   be  the  last\n"
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
			allModules.retainAll(moduleList);
			if (allModules.size() != moduleList.size())
			{
				info("Some of the modules specified for export were not actually modules of exported solution.", ILogLevel.ERROR); //$NON-NLS-1$
			}
		}
		return allModules;
	}

	public String getProtectionPassword()
	{
		return protectionPassword;
	}

	@SuppressWarnings("nls")
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