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

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.eclipse.model.export.IExportSolutionModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
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
	private boolean isExportActiveSolution;
	private String exportNG2Mode;
	private boolean exportNG1;
	private String pluginLocations;
	private String selectedComponents;
	private String selectedServices;
	private String excludedComponentPackages;
	private String excludedServicePackages;
	private String excludedPlugins;
	private String excludedBeans;
	private String excludedLafs;
	private String excludedDrivers;

	private boolean exportMetaData = false;
	private boolean exportSampleData = false;
	private int sampleDataCount = 5000;
	private boolean exportI18N = false;
	private boolean exportAllTablesFromReferencedServers = false;
	private boolean checkMetadataTables = false;
	private boolean exportUsers = false;
	private String warFileName = null;
	private String warSettingsFile = null;

	private static final String overwriteGroups = "overwriteGroups";//  overwrites Groups\n"
	private static final String allowSQLKeywords = "allowSQLKeywords";// allows SQLKeywords \n"
	private static final String stopOnDataModelChanges = "stopOnDataModelChanges";// allow data model changes \n"
	private static final String overrideSequenceTypes = "overrideSequenceTypes";// overrides Sequence Types \n"
	private static final String overrideDefaultValues = "overrideDefaultValues";// overrides Default Values \n"
	private static final String insertNewI18NKeysOnly = "insertNewI18NKeysOnly";// inserts NewI18NKeysOnly \n"
	private static final String allowDataModelChanges = "allowDataModelChanges";// allow data model changes \n"
	private static final String skipDatabaseViewsUpdate = "skipDatabaseViewsUpdate";// skip database views update \n"

	private static final String importUserPolicy = "importUserPolicy";// int \n"
	private static final String addUsersToAdminGroup = "addUsersToAdminGroup";// adds Users To Admin Group \n"
	private static final String updateSequences = "updateSequences";// updates Sequences \n";
	private static final String upgradeRepository = "upgradeRepository";

	private static final String contextFileName = "contextFileName";
	private static final String createTomcatContextXML = "createTomcatContextXML";
	private static final String antiResourceLocking = "antiResourceLocking";
	private static final String clearReferencesStatic = "clearReferencesStatic";
	private static final String clearReferencesStopThreads = "clearReferencesStopThreads";
	private static final String clearReferencesStopTimerThreads = "clearReferencesStopTimerThreads";

	private static final String defaultAdminUser = "defaultAdminUser";
	private static final String defaultAdminPassword = "defaultAdminPassword";
	private static final String useAsRealAdminUser = "useAsRealAdminUser";
	private static final String licenses = "licenses";
	private static final String license = "license";
	private static final String license_name_suffix = ".company_name";
	private static final String license_code_suffix = ".code";
	private static final String license_nr_suffix = ".licenses";

	private static final String userHomeDirectory = "userHomeDirectory";
	private static final String doNotOverwriteDBServerProperties = "doNotOverwriteDBServerProperties";
	private static final String overwriteAllProperties = "overwriteAllProperties";

	private static final String webXmlFileName = "webXmlFileName";
	private static final String log4jConfigurationFile = "log4jConfigurationFile";

	private static final String noneActiveSolutions = "nas";
	private static final String excludeDrivers = "excludeDrivers";
	private static final String excludeLafs = "excludeLafs";
	private static final String excludeBeans = "excludeBeans";
	private static final String excludePlugins = "excludePlugins";

	private HashMap<String, String> argumentsMap;
	private Map<String, License> licenseMap = new HashMap<>();

	public WarArgumentChest(String[] args)
	{
		super();
		initialize(args);
	}

	@Override
	protected String getMandatoryArgumentsMessage()
	{
		StringBuilder message = new StringBuilder(super.getMandatoryArgumentsMessage(false));
		message.append("\n");
		message.append(MANDATORY_ARGS_INDENT);
		message.append("Note: <solution_name> can  also  contain multiple solution names  separated  by comma. In\n" + MANDATORY_ARGS_INDENT +
			"      that case, one .war file will be generated for each solution in that  list. Some of\n" + MANDATORY_ARGS_INDENT +
			"      the options below  (for example -warFileName)  do not make sense when multiple .war\n" + MANDATORY_ARGS_INDENT +
			"      files are to be created. There is a note on each such option.\n\n");
		message.append(MANDATORY_ARGS_INDENT);
		message.append("-");
		message.append(defaultAdminUser);
		message.append(" <user name for admin page when no admin user exists>");
		message.append("\n");
		message.append(MANDATORY_ARGS_INDENT);
		message.append("-");
		message.append(defaultAdminPassword);
		message.append(" <password for defaultAdminUser>");
		return message.toString();
	}

	@Override
	public String getHelpMessage()
	{
		// @formatter:off
		return  "WAR exporter. Exports workspace solutions into .war files.\n"
			+ super.getHelpMessageCore()
			+ "        -active <true/false> ... export  given solution  (and its modules) as part  of .war.\n"
			+ "             That means the (current if more) solution  from -s  parameter and  its modules.\n"
			+ "             If false,  it will create a war with no solution  included. If false,  you will\n"
			+ "             have to import  solutions using the admin page. It does  not make sense  to use\n"
			+ "             false if multiple solutions were given at -s.\n"
			+ "             Default: true\n"
			+ "        -pfw <properties_file> ... path & name of properties file to be included in the war.\n"
			+ "             Default: the 'servoy.properties' file  from 'application_server'  will be used.\n"
			+ "        -b <bean_names> ... the space separated list of (smart / web client) beans to export\n"
			+ "             Default: all beans from application_server/beans are exported.\n"
			+ "             You can use '-b <none>' to avoid exporting beans.\n"
			+ "        -" + excludeBeans + " <bean_names> ... the  list of beans to excluded from  the export e.g.:\n"
			+ "             -" + excludeBeans + " bean1.jar bean2.zip\n"
			+ "             Default: none is excluded.\n"
			+ "        -l <lafs_names> ... the space separated list of look-and-feels (smart cl.) to export\n"
			+ "             Default: all lafs from application_server/lafs are exported.\n"
			+ "             You can use '-l <none>' to avoid exporting lafs.\n"
			+ "        -" + excludeLafs + " <lafs_names> ... the list of lafs to be excluded  from  the export e.g:\n"
			+ "             -" + excludeLafs + " laf1.jar laf2.zip\n"
			+ "             Default: none is excluded.\n"
			+ "        -d <jdbc_drivers> ... the space separated list of  jdbc (database) drivers to export\n"
			+ "             Default: all drivers from application_server/drivers are exported.\n"
			+ "             You can use '-d <none>' to avoid exporting drivers.\n"
			+ "        -" + excludeDrivers + " <jdbc_drivers> ... the  list of drivers  to exclude from  the export\n"
			+ "             e.g.: -" + excludeDrivers + " driver1.jar driver2.zip\n"
			+ "             Default: none is excluded.\n"
			+ "        -pi <plugin_names> ... the list of plugins to export e.g -pi plugin1.jar plugin2.zip\n"
			+ "             Default: all plugins from application_server/plugins are exported.\n"
			+ "             You can use '-pi <none>' to avoid exporting plugins.\n"
			+ "        -" + excludePlugins + " <plugin_names> ... the  list of plugins  to exclude from  the export\n"
			+ "             e.g.: -" + excludePlugins + " plugin1.jar plugin2.zip\n"
			+ "             Default: none is excluded.\n"
			+ "        -" + noneActiveSolutions + " ... space separated list of solutions that must be exported but are not in the\n"
			+ "             current solution's modules (for example solutions for batch processors).\n"
			+ "             If active is false  no solution will be exported  in war. It might be  wrong to\n"
			+ "             use this flag if you also specify multiple solutions in -s.\n"
			+ "             Default: only active solution and its modules are exported.\n"
			+ "        -pluginLocations <ABSOLUTE paths to developer 'plugins' folder> ...  needed in  case\n"
			+ "             you don't run the exporter from [servoy_install]/developer/exporter\n"
			+ "             Default: '../plugins'.\n"
			+ "        -crefs ... exports only the components used by the (current if -s has more) solution\n"
			+ "        -crefs <additional_component_names> ... can be 'all'  (without the ')  or a  list of\n"
			+ "             components separated by spaces; exports the  components  used  by  the solution\n"
			+ "             (current if -s has more) and the components in this additional components list.\n"
			+ "             Default: only the used components exported.\n"
			+ "        -excludeComponentPkgs ... space separated list of  excluded component packages (from\n"
			+ "             all available component packages).\n"
			+ "             Default: none is excluded.\n"
			+ "        -srefs ... exports  only the services used by  the (current if -s has more) solution\n"
			+ "        -srefs <additional_service_names> ... can  be  'all' (no ')  or  a list  of services\n"
			+ "             separated by spaces; exports the services used by the  (current if -s has more)\n"
			+ "             solution  and the services in this additional services list.\n"
			+ "             Default: only the used services are exported.\n"
			+ "        -excludeServicePkgs ... space separated list of  excluded service packages (from all\n"
			+ "             available service packages).\n"
			+ "             Default: none is excluded.\n"
			+ "        -md ... export metadata tables (tables marked as metadata); uses the metadata stored\n"
			+ "             in workspace files for each metadata table.\n"
			+ "        -checkmd ... check that metadata for metadata tables is the same for each table both\n"
			+ "             in the according workspace file and in the actual table in the  database before\n"
			+ "             exporting; this only makes sense if -md was specified\n"
			+ "             Default: false\n"
			+ "        -sd ... exports sample data. IMPORTANT all needed DB servers must already be started\n"
			+ "        -sdcount <count> ... number of rows to  export per table. Only  makes sense when -sd\n"
			+ "             is also present. Can be 'all' (without the ') in which  case  it will  still be\n"
			+ "             limited but to a very high number: " + IDataServerInternal.MAX_ROWS_TO_RETRIEVE + "\n"
			+ "             Default: " + IExportSolutionModel.DEFAULT_NUMBER_OF_SAMPLE_DATA_ROWS_IF_DATA_IS_EXPORTED + "\n"
			+ "        -i18n ... exports i18n data\n"
			+ "        -users ... exports users\n"
			+ "        -tables ... export  all table  information  about  tables from  referenced  servers.\n"
			+ "             IMPORTANT: all needed DB servers must already be started\n"
			+ "        -warFileName ... the name of the  war file; do *NOT* use this if  multiple solutions\n"
			+ "             were given in -s argument (it does not make sense then as multiple wars will be\n"
			+ "             generated).\n"
			+ "             Default: the (current if -s has more) solution name\n"
			+ "        -" + overwriteGroups + " ...  overwrites Groups\n"
			+ "        -" + allowSQLKeywords + " ... allows SQLKeywords\n"
			+ "        -" + stopOnDataModelChanges + " ... stops import if data model changes.\n"
			+ "             This option is ignored if " + allowDataModelChanges + " is present.\n"
			+ "        -" + allowDataModelChanges +" ... (optionally) a space separated list of server names  that\n"
			+ "             allow data model changes.  If the list is missing,  then data model changes are\n"
			+ "             allowed on all servers.\n"
			+ "        -" + skipDatabaseViewsUpdate +"... skips database views update \n"
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
			+ "        -" + contextFileName + " ... a path to a tomcat context.xml  that should be included\n"
			+ "             into the WAR/META-INF/context.xml\n"
//			+ "        -" + createTomcatContextXML + " ... create   a   META-INF/context.xml   file;   please   see\n"
//			+ "             https://tomcat.apache.org/tomcat-8.0-doc/config/context.html#Standard_Implement\n"
//			+ "             ation for more information.\n"
//			+ "        -" + antiResourceLocking + " ... add antiResourceLocking=\"true\" to Context element; may only\n"
//			+ "             be used with createTomcatContextXML.\n"
//			+ "        -" + clearReferencesStatic + " ... add clearReferencesStatic=\"true\" to  Context element; may\n"
//			+ "             only be used with createTomcatContextXML.\n"
//			+ "        -" + clearReferencesStopThreads + " ... add   clearReferencesStopThreads=\"true\"   to Context\n"
//			+ "             element; may only be used with createTomcatContextXML.\n"
//			+ "        -" + clearReferencesStopTimerThreads + " ... add  clearReferencesStopTimerThreads=\"true\"  to\n"
//			+ "             Context element; may only be used with createTomcatContextXML.\n"
			+ "        -" + useAsRealAdminUser + " ... the  default admin user login  given via   -" + defaultAdminUser + "\n"
			+ "             above will  be available for  admin page login even  if there are  Servoy users\n"
			+ "             with the Administrators permission set.\n"
			+ "        -" + license + license_name_suffix + " OR " + license+".<i>" + license_name_suffix + ",\n"
			+ "             The name of the company that has the license,  where <i> is used when there are\n"
			+ "             multiple licenses:\n"
			+ "             -" + license + ".1" + license_name_suffix + " name1" + " -" + license + ".2" + license_name_suffix + " name2\n"
			+ "        -" + license + license_code_suffix +" OR " + license + ".<i>" + license_code_suffix + ",\n"
			+ "             The license code, where <i> is used when there are multiple licenses:\n"
			+ "             -" + license + ".1" + license_code_suffix + " XXXX-XXXX-XXXX" + " -" + license + ".2" + license_code_suffix + "  XXXX-XXXX-XXXX\n"
			+ "        -" + license+license_nr_suffix + " OR " + license + ".<i>" + license_nr_suffix + ",\n"
			+ "             The number of licenses, where <i> is used when there are multiple licenses:\n"
			+ "             -" + license + ".1" + license_nr_suffix +" SERVER" + " -" + license + ".2" + license_nr_suffix +" 1000\n"
			+ "        -" + userHomeDirectory + " <user_home_directory> ... this must be a writable directory where\n"
			+ "             Servoy application  related files  will be stored;  if not set, then the system\n"
			+ "             user home directory will be used.\n"
			+ "        -" + doNotOverwriteDBServerProperties + " ... SKIP overwrite of old DBserver properties - if\n"
			+ "             they were  stored separately  by  a previously deployed war  (due to changes to\n"
			+ "             properties via admin page) - with ones from the war export's servoy.properties.\n"
			+ "             If -overwriteAllProperties below is set then this flag has no effect.  Prior to\n"
			+ "             Servoy 2019.09,  '-overwriteDBServerProperties'  was used instead but it is now\n"
			+ "             removed in order to have the same default value as in UI export wizard.\n"
			+ "        -" + overwriteAllProperties + " ... overwrite  all  (potentially  changed  via  admin  page)\n"
			+ "             properties  of a previously deployed war  application  with the values from the\n"
			+ "             servoy.properties of this war export.\n"
			+ "        -" + log4jConfigurationFile + " ... a  path  to a log4j  configuration file  that  should be\n"
			+ "             included nstead of the default one.\n"
			+ "        -" + webXmlFileName + " ... a path to a web.xml  that should be included instead  of default\n"
			+ "             one; it should be a web.xml file previously generated via a Servoy WAR export.\n"
			+ "        -ng2 true / false / sourcemaps ... export Titanium NG2 binaries.  If 'sourcemaps' is\n"
			+ "             given, sourcemaps will be generated for .ts files - useful for debugging.\n"
			+ "             Default: true\n"
			+ "        -ng1 ... export NG1 client resources; not exported by default.\n"
			+ getHelpMessageExitCodes();
		// @formatter:on
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap)
	{
		printArgsMap(System.out, argsMap);
		warSettingsFile = parseArg("pfw", "Properties file was not specified after '-pfw' argument.", argsMap, false);
		plugins = parseArg("pi", "Plugin name(s) was(were) not specified after '-pi' argument.", argsMap, false);
		excludedPlugins = parseArg(excludePlugins, null, argsMap, false);
		beans = parseArg("b", "Bean name(s) was(were) not specified after '-b' argument.", argsMap, false);
		excludedBeans = parseArg(excludeBeans, null, argsMap, false);
		lafs = parseArg("l", "Laf name(s) was(were) not specified after '-l' argument.", argsMap, false);
		excludedLafs = parseArg(excludeLafs, null, argsMap, false);
		drivers = parseArg("d", "Driver name(s) was(were) not specified after '-d' argument.", argsMap, false);
		excludedDrivers = parseArg(excludeDrivers, null, argsMap, false);
		isExportActiveSolution = true;
		if (argsMap.containsKey("active") && !Utils.getAsBoolean(argsMap.get("active"))) isExportActiveSolution = false;
		exportNG2Mode = "true";
		if (argsMap.containsKey("ng2")) exportNG2Mode = argsMap.get("ng2");
		exportNG1 = false;
		if (argsMap.containsKey("ng1")) exportNG1 = true;
		pluginLocations = parseArg("pluginLocations", null, argsMap, false);
		if (pluginLocations == null) pluginLocations = "../plugins";
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
		if (argsMap.containsKey("warFileName")) warFileName = parseArg("warFileName", null, argsMap, false);
		parseArg(webXmlFileName, null, argsMap, false);
		parseArg(log4jConfigurationFile, null, argsMap, false);

		parseArg("defaultAdminUser", "Parameters'-defaultAdminUser' and '-defaultAdminPassword' are required.", argsMap, true);
		parseArg("defaultAdminPassword", "Parameters'-defaultAdminUser' and '-defaultAdminPassword' are required.", argsMap, true);

		if (argsMap.containsKey(licenses) || argsMap.containsKey(license + license_name_suffix) || argsMap.containsKey(license + ".1" + license_name_suffix))
		{
			licenseMap = parseLicensesArg(argsMap);
		}

		argumentsMap = argsMap;
	}

	private Map<String, License> parseLicensesArg(HashMap<String, String> argsMap)
	{
		Map<String, License> result = new HashMap<>();
		if (argsMap.containsKey(license + license_name_suffix))
		{
			String name = argsMap.get(license + license_name_suffix);
			String code = argsMap.get(license + license_code_suffix);
			String nrLicenses = argsMap.get(license + license_nr_suffix);
			if (checkLicensePart(name, license + license_name_suffix) && checkLicensePart(code, license + license_code_suffix) &&
				checkLicensePart(nrLicenses, license + license_nr_suffix))
			{
				result.put(code, new License(name, code, nrLicenses));
			}
		}
		else if (argsMap.containsKey(license + ".1" + license_name_suffix))
		{
			int i = 1;
			while (argsMap.containsKey(license + "." + i + license_name_suffix))
			{
				String name = argsMap.get(license + "." + i + license_name_suffix);
				String code = argsMap.get(license + "." + i + license_code_suffix);
				String nrLicenses = argsMap.get(license + "." + i + license_nr_suffix);
				if (!checkLicensePart(name, license + "." + i + license_name_suffix) || !checkLicensePart(code, license + "." + i + license_code_suffix) ||
					!checkLicensePart(nrLicenses, license + "." + i + license_nr_suffix))
				{
					break;
				}
				result.put(code, new License(name, code, nrLicenses));
				i++;
			}
		}
		if (argsMap.containsKey(licenses))
		{
			String l = argsMap.get(licenses);
			String[] l_array = l.split(",");
			for (String license1 : l_array)
			{
				String[] parts = license1.trim().split(" ");
				String company = null;
				String code = null;
				String numLicenses = null;
				if (parts.length != 3 || parts[0].startsWith("\""))
				{
					Pattern p = Pattern.compile("\"(.+)\" (.+) (.+)");
					Matcher m = p.matcher(license1.trim());
					if (m.matches())
					{
						company = m.group(1);
						numLicenses = m.group(2);
						code = m.group(3);
					}
					else
					{
						ServoyLog.logError(new Exception(
							"Please specify license as <company_name> <number_of_licenses> <code> or \\\"<company_name>\\\" <number_of_licenses> <code>. \"" +
								license1 + "\" is not valid"));
						continue;
					}
				}
				else
				{
					company = parts[0].trim();
					numLicenses = parts[1].trim();
					code = parts[2].trim();
				}
				result.put(code, new License(company, code, numLicenses));
			}
		}
		return result;
	}

	protected boolean checkLicensePart(String value, String key)
	{
		if (value == null || "".equals(value.trim()))
		{
			ServoyLog.logError(
				new Exception("Please specify license as -license.company_name <name> -license.code <code> -license.licenses <licenses number> " +
					" or -license.<i>.company_name <name> -license.<i>.code <code> -license.<i>.licenses <licenses number>. The key '" + key +
					"' is not specified."));
			return false;
		}
		return true;
	}

	/**
	 * @param out
	 * @param argsMap
	 */
	private static void printArgsMap(PrintStream out, Map<String, String> argsMap)
	{
		out.print("parsing arguments map {");

		StringBuilder sb = new StringBuilder();

		for (Entry<String, String> entry : argsMap.entrySet())
		{
			if (sb.length() > 0)
			{
				sb.append(", ");
			}
			sb.append(entry.getKey()).append('=');
			if (entry.getKey().toLowerCase().indexOf("passw") >= 0)
			{
				sb.append("*********");
			}
			else
			{
				sb.append(entry.getValue());
			}
		}

		out.println(sb.append('}').toString());
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

	public String getExcludedPlugins()
	{
		return excludedPlugins;
	}

	public String getBeans()
	{
		return beans;
	}

	public String getExcludedBeans()
	{
		return excludedBeans;
	}

	public String getLafs()
	{
		return lafs;
	}

	public String getExcludedLafs()
	{
		return excludedLafs;
	}

	public String getDrivers()
	{
		return drivers;
	}

	public String getExcludedDrivers()
	{
		return excludedDrivers;
	}

	public boolean isExportActiveSolutionOnly()
	{
		return isExportActiveSolution;
	}

	public String exportNG2Mode()
	{
		return exportNG2Mode;
	}

	public boolean exportNG1()
	{
		return exportNG1;
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

	public String getWarSettingsFileName()
	{
		return warSettingsFile;
	}

	public boolean isOverrideSequenceTypes()
	{
		return argumentsMap.containsKey(overrideSequenceTypes);
	}

	public boolean isOverwriteGroups()
	{
		return argumentsMap.containsKey(overwriteGroups);
	}

	public boolean isAllowSQLKeywords()
	{
		return argumentsMap.containsKey(allowSQLKeywords);
	}

	public boolean isStopOnAllowDataModelChanges()
	{
		return argumentsMap.containsKey(stopOnDataModelChanges);
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

	public String getTomcatContextXMLFileName()
	{
		return argumentsMap.get(contextFileName);
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

	public String getDefaultAdminUser()
	{
		return argumentsMap.get(defaultAdminUser);
	}

	public String getDefaultAdminPassword()
	{
		return argumentsMap.get(defaultAdminPassword);
	}

	public boolean isUseAsRealAdminUser()
	{
		return argumentsMap.containsKey(useAsRealAdminUser);
	}

	public Map<String, License> getLicenses()
	{
		return licenseMap;
	}

	public boolean isOverwriteDeployedDBServerProperties()
	{
		return !argumentsMap.containsKey(doNotOverwriteDBServerProperties);
	}

	public boolean isOverwriteDeployedServoyProperties()
	{
		return argumentsMap.containsKey(overwriteAllProperties);
	}

	public String getUserHome()
	{
		return argumentsMap.get(userHomeDirectory);
	}

	public String getWebXMLFileName()
	{
		return argumentsMap.get(webXmlFileName);
	}

	public String getLog4jConfigurationFile()
	{
		return argumentsMap.get(log4jConfigurationFile);
	}

	/**
	 * @return
	 */
	public String getNoneActiveSolutions()
	{
		return argumentsMap.get(noneActiveSolutions);
	}

	public String getAllowDataModelChanges()
	{
		if (argumentsMap.containsKey(allowDataModelChanges))
		{
			if (argumentsMap.get(allowDataModelChanges) != null)
			{
				return argumentsMap.get(allowDataModelChanges).replaceAll(" ", ",");
			}
			return Boolean.toString(true);
		}
		return null;
	}

	public boolean skipDatabaseViewsUpdate()
	{
		return argumentsMap.containsKey(skipDatabaseViewsUpdate);
	}
}
