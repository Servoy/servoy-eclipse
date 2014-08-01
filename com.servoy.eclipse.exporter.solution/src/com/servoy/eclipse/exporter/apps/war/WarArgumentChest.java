/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.exporter.apps.war;

import java.util.HashMap;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.j2db.util.Utils;

/**
 * Stores and provides the export-relevant arguments the product was started with.
 * @author gboros
 */
public class WarArgumentChest extends AbstractArgumentChest
{

	private String plugins;
	private String beans;
	private String lafs;
	private String drivers;
	private boolean isExportActiveSolutionOnly;
	private String pluginLocations;

	public WarArgumentChest(String[] args)
	{
		super();
		initialize(args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest#getHelpMessage()
	 */
	@Override
	public String getHelpMessage()
	{
		// @formatter:off
		return "WAR exporter. Exports workspace solutions into .war files.\n"
			+ "USAGE:\n\n"
			+ "   -help or -? or /? or no arguments ... shows current help message.\n\n"
			+ "                  OR\n\n"
			+ "   -s <solution> -o <out_dir> -data <workspace_location> [optional_args]\n\n"
			
			+ "        Optional arguments:\n\n"
			+ "        -verbose ... prints more info to console\n"
			+ "        -p <properties_file> ... path and name of properties file.\n"
			+ "             Default: the 'servoy.properties' file  from 'application_server'  will be used.\n"
			+ "        -pi <plugin_names> ... the list of plugins to export e.g. -pi plugin1.jar plugin2.zip\n" 
			+			    "Default: all plugins from application_server/plugins are exported.\n"
			+ "        -b <bean_names> ... the list of beans to export \n" 
			+			   "Default: all beans from application_server/beans are exported.\n"
			+ "        -l <lafs_names> ... the list of lafs to export \n" 
			+			    "Default: all lafs from application_server/lafs are exported.\n"
			+ "        -d <jdbc_drivers> ... the list of drivers to export\n" 
			+			    "Default: all drivers from application_server/drivers are exported.\n"
			+ "        -as <app_server_dir> ... specifies where to find the 'application_server' directory.\n"
			+ "             Default: '../../application_server'.\n"
			+ "        -pl alternate project locations; solution  and  resources projects will  be searched\n"
			+ "             for in direct subfolders of the given 'workspace_location'.     Example: if the\n"
			+ "             workspace needs to contain projects from different git repositories,  those can\n"
			+ "             be checked out in '<workspace_location>/a', '<workspace_location>/b' and so on.\n"
			+ "        -ie ignore build errors.  CAUTION! the use of this flag is discouraged; it can cause\n"
			+ "             invalid solutions to be exported.\n"
			+ "        -active <true/false> export active solution (and its modules) only\n"
			+ "				Default: true\n"
			+ "        -pluginlocations absolute paths to plugin folders.\n" 
			+			    "Default: '../plugins'.\n";
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap)
	{
		System.out.println("parsing arguments map "+argsMap);
		plugins = parseArg("pi", "Plugin name(s) was(were) not specified after '-pi' argument.", argsMap);
		beans = parseArg("b", "Bean name(s) was(were) not specified after '-b' argument.", argsMap);
		lafs = parseArg("l", "Laf name(s) was(were) not specified after '-l' argument.", argsMap);
		drivers = parseArg("d","Driver name(s) was(were) not specified after '-d' argument.", argsMap);
		isExportActiveSolutionOnly = true;
		if (argsMap.containsKey("active") && !Utils.getAsBoolean(argsMap.get("active"))) isExportActiveSolutionOnly = false;
		pluginLocations = parseArg("pluginLocations", null, argsMap);
		if (pluginLocations == null) pluginLocations = "../plugins";
	}

	public String getPlugins()
	{
		return plugins;
	}

	public String getBeans()
	{
		return beans;
	}

	public String getLafs()
	{
		return lafs;
	}

	public String getDrivers()
	{
		return drivers;
	}

	public boolean isExportActiveSolutionOnly()
	{
		return isExportActiveSolutionOnly;
	}

	/**
	 * @return the paths to plugin directories
	 */
	public String getPluginLocations()
	{
		return pluginLocations;
	}
}
