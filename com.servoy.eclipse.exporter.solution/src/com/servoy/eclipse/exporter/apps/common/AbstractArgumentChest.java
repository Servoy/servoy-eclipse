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

package com.servoy.eclipse.exporter.apps.common;

import java.util.HashMap;

import com.servoy.j2db.util.ILogLevel;

/**
 * Stores and provides the export-app-relevant arguments (reads arguments from eclipse app. arguments).
 * @author acostescu
 */
public abstract class AbstractArgumentChest implements IArgumentChest
{

	private boolean invalidArguments = false;
	private boolean mustShowHelp = false;
	private String solutionNames = null;
	private String exportFilePath = null;
	private boolean verbose = false;
	private boolean aggregateWorkspace = false;
	private String settingsFile = null;
	private String appServerDir = "../../application_server";
	private boolean exportUsingDbiFileInfoOnly = false;
	private boolean ignoreBuildErrors = false;

	// this must not be done in constructor as it calls an abstract method that can end up setting fields in an extending class - fields that are not yet set to default values and will be after the constructor of this class finishes
	public void initialize(String[] args)
	{
		//printArguments(args);
		if (args.length == 0) mustShowHelp = true;
		else
		{
			HashMap<String, String> argsMap = getArgsAsMap(args);
			if (argsMap.containsKey("help") || argsMap.containsKey("?")) mustShowHelp = true;
			solutionNames = parseArg("s", "Solution name(s) was(were) not specified after '-s' argument.", argsMap);
			exportFilePath = parseArg("o", "Export file path was not specified after '-o' argument.", argsMap);
			if (argsMap.containsKey("verbose")) verbose = true;
			settingsFile = parseArg("p", "Properties file was not specified after '-p' argument.", argsMap);
			if (argsMap.containsKey("as"))
			{
				appServerDir = parseArg("as", "Application server directory was not specified after '-as' argument.", argsMap);
			}
			if (argsMap.containsKey("pl")) aggregateWorkspace = true;
			if (argsMap.containsKey("dbi") || argsMap.containsKey("dbd")) exportUsingDbiFileInfoOnly = true;
			if (argsMap.containsKey("ie")) ignoreBuildErrors = true;

			if (!mustShowHelp) parseArguments(argsMap);

			// check that the required arguments are provided
			if (!mustShowHelp && !invalidArguments)
			{
				if (solutionNames == null || exportFilePath == null)
				{
					info("Required arguments are missing. Please provide both '-s'and '-o' arguments.", ILogLevel.ERROR);
					markInvalid();
				}
				else if (solutionNames.split(",").length == 0)
				{
					info("No solutions to export -> '" + solutionNames +
						"'; unable to get solution names from that string. It must be a list of solution names separated by comma.", ILogLevel.ERROR);
					markInvalid();
				}
			}
		}
	}

	private HashMap<String, String> getArgsAsMap(String[] args)
	{
		StringBuilder argsString = new StringBuilder();
		for (String arg : args)
		{
			argsString.append(arg + " ");
		}
		String a = argsString.toString();

		HashMap<String, String> argsMap = new HashMap<String, String>();
		if (a.contains("/?"))
		{
			a = a.replace("/?", "");
			argsMap.put("?", "");
		}

		String[] argss = argsString.toString().split("-");
		for (String arg : argss)
		{
			if (arg != null && !arg.trim().equals(""))
			{
				int idx = arg.indexOf(" ");
				if (idx > 0 && arg.length() > idx)
				{
					argsMap.put(arg.substring(0, idx).toLowerCase(), arg.substring(idx + 1).trim());
				}
				else
				{
					argsMap.put(arg.toLowerCase(), "");
				}
			}
		}
		return argsMap;
	}

	protected String parseArg(String argName, String errMsg, HashMap<String, String> argsMap)
	{
		if (argsMap.containsKey(argName))
		{
			String val = argsMap.get(argName);
			if (val != null && !val.trim().equals("")) return val;

			if (errMsg != null)
			{
				info(errMsg, ILogLevel.ERROR);
				markInvalid();
			}
		}
		return null;
	}

	void printArguments(String args[])
	{
		StringBuilder sb = new StringBuilder();
		for (String str : args)
		{
			sb.append(str).append(" ");
		}

		info("Arguments: " + sb.toString(), ILogLevel.ERROR);
	}

	public abstract String getHelpMessage();

	protected String getHelpMessageCore()
	{
		// @formatter:off
		return "USAGE:\n\n"
			+ "   -help or -? or /? or no arguments ... shows current help message.\n\n"
			+ "                  OR\n\n"
			+ "   -s <solutions_separated_by_comma> -o <out_dir> -data <workspace_location> [optional_args]\n\n"
			
			+ "        Optional arguments:\n\n"
			+ "        -verbose ... prints more info to console\n"
			+ "        -p <properties_file> ... path and name of properties file.\n"
			+ "             Default: the 'servoy.properties' file  from 'application_server'  will be used.\n"
			+ "        -as <app_server_dir> ... specifies where to find the 'application_server' directory.\n"
			+ "             Default: '../../application_server'.\n"
			+ "        -pl alternate project locations; solution  and  resources projects will  be searched\n"
			+ "             for in direct subfolders of the given 'workspace_location'.     Example: if the\n"
			+ "             workspace needs to contain projects from different git repositories,  those can\n"
			+ "             be checked out in '<workspace_location>/a', '<workspace_location>/b' and so on.\n"
			+ "        -ie ignore build errors.  CAUTION! the use of this flag is discouraged; it can cause\n"
			+ "             invalid solutions to be exported.\n"
			+ getHelpMessageDbi();
		// @formatter:on
	}

	// dbi and dbd are implemented by mobile exporter, but hardcoded, not configurable - so not part of the help message; allow extending classes to suppress these
	protected String getHelpMessageDbi()
	{
		// @formatter:off
		return
			  "        -dbi ... export based on dbi files (even if database servers are available)\n";
		// @formatter:on
	}

	protected String getHelpMessageExistCodes()
	{
		return "\nEXIT codes: 0 - normal, 1 - export stopped by user, 2 - export failed, 3 - invalid arguments";
	}

	/**
	 * Called inside the constructor. So you can count on this method being called in child classes as part of the super(...) call. 
	 */
	protected abstract void parseArguments(HashMap<String, String> argsMap);

	protected void markInvalid()
	{
		invalidArguments = true;
	}

	public String getAppServerDir()
	{
		return appServerDir;
	}

	public String getSettingsFileName()
	{
		return settingsFile;
	}

	public boolean isInvalid()
	{
		return invalidArguments;
	}

	public boolean mustShowHelp()
	{
		return mustShowHelp;
	}

	public String getExportFilePath()
	{
		return exportFilePath;
	}

	public boolean isVerbose()
	{
		return verbose;
	}

	public boolean shouldAggregateWorkspace()
	{
		return aggregateWorkspace;
	}

	public String[] getSolutionNames()
	{
		return solutionNames.split(",");
	}

	public String getSolutionNamesAsString()
	{
		return solutionNames;
	}

	public boolean shouldExportUsingDbiFileInfoOnly()
	{
		return exportUsingDbiFileInfoOnly;
	}

	public void info(String message, int priority)
	{
		if (priority > ILogLevel.WARNING || verbose)
		{
			System.out.println(message);
		}
	}

	@Override
	public boolean shouldIgnoreBuildErrors()
	{
		return ignoreBuildErrors;
	}

}