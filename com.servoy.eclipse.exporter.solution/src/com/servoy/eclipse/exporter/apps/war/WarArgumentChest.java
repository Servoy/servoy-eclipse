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
import com.servoy.j2db.util.xmlxport.IXMLImportUserChannel;

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
	private String selectedComponents;
	private String selectedServices;
	private String excludedComponentPackages;
	private String excludedServicePackages;

	private boolean exportMetaData = false;
	private boolean exportSampleData = false;
	private int sampleDataCount = 5000;
	private boolean exportI18N = false;
	private boolean exportAllTablesFromReferencedServers = false;
	private boolean checkMetadataTables = false;
	private boolean exportUsers = false;
	private String warFileName = null;

	private static final String overwriteGroups = "overwriteGroups";//  overwrites Groups\n"
	private static final String allowSQLKeywords = "allowSQLKeywords";// allows SQLKeywords \n"
	private static final String overrideSequenceTypes = "overrideSequenceTypes";// overrides Sequence Types \n"
	private static final String overrideDefaultValues = "overrideDefaultValues";// overrides Default Values \n"
	private static final String insertNewI18NKeysOnly = "insertNewI18NKeysOnly";// inserts NewI18NKeysOnly \n"

	private static final String importUserPolicy = "importUserPolicy";// int \n"
	private static final String addUsersToAdminGroup = "addUsersToAdminGroup";// adds Users To Admin Group \n"
	private static final String updateSequences = "updateSequences";// updates Sequences \n";
	private static final String upgradeRepository = "upgradeRepository";

	private static final String createTomcatContextXML = "createTomcatContextXML";
	private static final String antiResourceLocking = "antiResourceLocking";
	private static final String clearReferencesStatic = "clearReferencesStatic";
	private static final String clearReferencesStopThreads = "clearReferencesStopThreads";
	private static final String clearReferencesStopTimerThreads = "clearReferencesStopTimerThreads";

	private HashMap<String, String> argumentsMap;

	public WarArgumentChest(String[] args)
	{
		super();
		initialize(args);
	}

	@Override
	public String getHelpMessage()
	{
		// @formatter:off
		return  "WAR exporter. Exports workspace solutions into .war files.\n"
			+ super.getHelpMessageCore()
			+ "        -b <bean_names> ... the list of beans to export\n"
			+ "             Default: all beans from application_server/beans are exported.\n"
			+ "        -l <lafs_names> ... the list of lafs to export \n"
			+ "             Default: all lafs from application_server/lafs are exported.\n"
			+ "        -d <jdbc_drivers> ... the list of drivers to export\n"
			+ "             Default: all drivers from application_server/drivers are exported.\n"
			+ "        -pi <plugin_names> ... the list of plugins to export e.g -pi plugin1.jar plugin2.zip\n"
			+ "             Default: all plugins from application_server/plugins are exported.\n"
			+ "        -active <true/false> ... export active solution (and its modules) only\n"
			+ "				Default: true\n"
			+ "        -pluginLocations <absolute paths to plugins folder>\n"
			+ "             Default: '../plugins'.\n"
			+ "        -crefs ... exports only the components used by the solution.\n"
			+ "             Default: all components are exported.\n"
			+ "        -crefs <additional_component_names> ... can be 'all' (no ') or a list of components;\n"
			+ "             exports  the  components  used  by  the solution  and  the  components  in  the\n"
			+ "             additional components list.\n"
			+ "             Default: all components are exported.\n"
			+ "        -excludeComponentPkgs ... space separated list of excluded component packages\n"
			+ "             Default: none is excluded.\n"
			+ "        -srefs exports only the services used by the solution.\n"
			+ "             Default: all services are exported.\n"
			+ "        -srefs <additional_service_names> ... can  be  'all' (no ')  or  a list of services;\n"
			+ "             exports  the services  used by the solution  and the services in the additional\n"
			+ "             services list.\n"
			+ "             Default: all services are exported.\n"
			+ "        -excludeServicePkgs ... space separated list of excluded service packages\n"
			+ "             Default: none is excluded.\n"
			+ "        -md ws|db|none|both ... take table  metadata from workspace / database / both+check.\n"
			+ "             Usually you will want to use 'ws'.\n"
			+ "        -checkmd ... check metadata tables, default false\n"
			+ "        -sd ... exports sample data. IMPORTANT all needed DB servers must already be started\n"
			+ "        -sdcount <count> ... number of rows to  export per table. Only  makes sense when -sd\n"
			+ "             is also present. Can be 'all' (without the '). Default: 10000\n"
			+ "        -i18n ... exports i18n data\n"
			+ "        -users ... exports users\n"
			+ "        -tables ... export  all table  information  about  tables from  referenced  servers.\n"
			+ "             IMPORTANT: all needed DB servers must already be started\n"
			+ "        -warFileName ... the name of the war file\n"
			+ "             Default: the solution name\n"
			+ "        -" + overwriteGroups + " ...  overwrites Groups\n"
			+ "        -" + allowSQLKeywords + " ... allows SQLKeywords\n"
			+ "        -" + overrideSequenceTypes + " ... overrides Sequence Types\n"
			+ "        -" + overrideDefaultValues + " ... overrides Default Values\n"
			+ "        -" + insertNewI18NKeysOnly + " ... inserts NewI18NKeysOnly\n"
			+ "        -" + importUserPolicy + " ... 0/1/2 where:\n"
			+ "             don't = 0\n"
			+ "             create users & update groups = 1 (default)\n"
			+ "             overwrite completely = 2\n"
			+ "        -" + addUsersToAdminGroup + " ... adds Users To Admin Group\n"
			+ "        -" + updateSequences + " ... updates Sequences\n"
			+ "        -" + upgradeRepository + " ... automatically upgrade repository if needed\n"
			+ "        -" + createTomcatContextXML + " ... create   a   META-INF/context.xml  file;   please  see\n"
			+ "             https://tomcat.apache.org/tomcat-8.0-doc/config/context.html#Standard_Impleme\n"
			+ "             ntation\n"
			+ "             for more information\n"
			+ "        -" + antiResourceLocking + " ... add  antiResourceLocking=\"true\"  to  Context element; may\n"
			+ "             only be used with createTomcatContextXML\n"
			+ "        -" + clearReferencesStatic + " ... add  clearReferencesStatic=\"true\"  to  Context element;\n"
			+ "             may only be used with createTomcatContextXML\n"
			+ "        -" + clearReferencesStopThreads + " ... add  clearReferencesStopThreads=\"true\"  to Context\n"
			+ "             element; may only be used with createTomcatContextXML\n"
			+ "        -" + clearReferencesStopTimerThreads + " ... add clearReferencesStopTimerThreads=\"true\" to\n"
			+ "             Context element; may only be used with createTomcatContextXML.\n";
		// @formatter:on
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap)
	{
		System.out.println("parsing arguments map " + argsMap);
		plugins = parseArg("pi", "Plugin name(s) was(were) not specified after '-pi' argument.", argsMap);
		beans = parseArg("b", "Bean name(s) was(were) not specified after '-b' argument.", argsMap);
		lafs = parseArg("l", "Laf name(s) was(were) not specified after '-l' argument.", argsMap);
		drivers = parseArg("d", "Driver name(s) was(were) not specified after '-d' argument.", argsMap);
		isExportActiveSolutionOnly = true;
		if (argsMap.containsKey("active") && !Utils.getAsBoolean(argsMap.get("active"))) isExportActiveSolutionOnly = false;
		pluginLocations = parseArg("pluginLocations", null, argsMap);
		if (pluginLocations == null) pluginLocations = "developer/../plugins";
		selectedComponents = parseComponentsArg("crefs", argsMap);
		selectedServices = parseComponentsArg("srefs", argsMap);
		excludedComponentPackages = parseComponentsArg("excludeComponentPkgs", argsMap);
		excludedServicePackages = parseComponentsArg("excludeServicePkgs", argsMap);

		if (argsMap.containsKey("md")) exportMetaData = true;
		if (argsMap.containsKey("checkmd")) checkMetadataTables = true;
		if (argsMap.containsKey("sd"))
		{
			exportSampleData = true;
			sampleDataCount = parseSampleDataCount(argsMap);
		}
		if (argsMap.containsKey("i18n")) exportI18N = true;
		if (argsMap.containsKey("users")) exportUsers = true;
		if (argsMap.containsKey("tables")) exportAllTablesFromReferencedServers = true;
		if (argsMap.containsKey("warFileName")) warFileName = parseArg("warFileName", null, argsMap);
		argumentsMap = argsMap;
	}

	private String parseComponentsArg(String argName, HashMap<String, String> argsMap)
	{
		if (argsMap.containsKey(argName))
		{
			if (argsMap.get(argName) == null)
			{
				return "";
			}
			return argsMap.get(argName);
		}
		return null;

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

	public String getPluginLocations()
	{
		return pluginLocations;
	}

	public String getSelectedComponents()
	{
		return selectedComponents;
	}

	public String getSelectedServices()
	{
		return selectedServices;
	}

	public String getExcludedComponentPackages()
	{
		return excludedComponentPackages;
	}

	public String getExcludedServicePackages()
	{
		return excludedServicePackages;
	}

	public boolean isExportSampleData()
	{
		return exportSampleData;
	}

	public boolean isExportI18NData()
	{
		return exportI18N;
	}

	public int getNumberOfSampleDataExported()
	{
		return sampleDataCount;
	}

	public boolean isExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	public boolean shouldExportMetadata()
	{
		return exportMetaData;
	}

	public boolean checkMetadataTables()
	{
		return checkMetadataTables;
	}


	public boolean exportUsers()
	{
		return exportUsers;
	}

	public String getWarFileName()
	{
		return warFileName;
	}

	public boolean isOverwriteGroups()
	{
		return argumentsMap.containsKey(overwriteGroups);
	}

	public boolean isAllowSQLKeywords()
	{
		return argumentsMap.containsKey(allowSQLKeywords);
	}

	public boolean isInsertNewI18NKeysOnly()
	{
		return argumentsMap.containsKey(insertNewI18NKeysOnly);
	}

	public boolean isOverrideDefaultValues()
	{
		return argumentsMap.containsKey(overrideDefaultValues);
	}

	public int getImportUserPolicy()
	{
		if (argumentsMap.containsKey(importUserPolicy)) return Utils.getAsInteger(argumentsMap.get(importUserPolicy));
		return IXMLImportUserChannel.IMPORT_USER_POLICY_CREATE_U_UPDATE_G;
	}

	public boolean isAddUsersToAdminGroup()
	{
		return argumentsMap.containsKey(addUsersToAdminGroup);
	}

	public boolean isUpdateSequences()
	{
		return argumentsMap.containsKey(updateSequences);
	}

	public boolean automaticallyUpdateRepository()
	{
		return argumentsMap.containsKey(upgradeRepository);
	}

	public boolean isCreateTomcatContextXML()
	{
		return argumentsMap.containsKey(createTomcatContextXML);
	}

	public boolean isClearReferencesStatic()
	{
		return argumentsMap.containsKey(clearReferencesStatic);
	}

	public boolean isClearReferencesStopThreads()
	{
		return argumentsMap.containsKey(clearReferencesStopThreads);
	}

	public boolean isClearReferencesStopTimerThreads()
	{
		return argumentsMap.containsKey(clearReferencesStopTimerThreads);
	}

	public boolean isAntiResourceLocking()
	{
		return argumentsMap.containsKey(antiResourceLocking);
	}
}
