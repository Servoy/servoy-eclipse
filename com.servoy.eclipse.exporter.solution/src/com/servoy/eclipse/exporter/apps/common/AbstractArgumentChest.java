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
	private boolean workspaceIsSplit = false;
	private String settingsFile = null;
	private String appServerDir = "../../application_server";
	private boolean exportUsingDbiFileInfoOnly = false;
	private boolean exportIfDBDown = false;

	// this must not be done in constructor as it calls an abstract method that can end up setting fields in an extending class - fields that are not yet set to default values and will be after the constructor of this class finishes
	public void initialize(String[] args)
	{
		if (args.length == 0) mustShowHelp = true;
		else
		{
			int i = 0;
			while (i < args.length)
			{
				if ("-help".equalsIgnoreCase(args[i]) || "-?".equals(args[i]) || "/?".equals(args[i]))
				{
					mustShowHelp = true;
					i = args.length - 1;
				}
				else if ("-s".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						solutionNames = args[++i];
					}
					else
					{
						info("Solution name(s) was(were) not specified after '-s' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-o".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						exportFilePath = args[++i];
					}
					else
					{
						info("Export file path was not specified after '-o' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-verbose".equalsIgnoreCase(args[i]))
				{
					verbose = true;
				}
				else if ("-p".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						settingsFile = args[++i];
					}
					else
					{
						info("Properties file was not specified after '-p' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-as".equalsIgnoreCase(args[i]))
				{
					if (i < (args.length - 1))
					{
						appServerDir = args[++i];
					}
					else
					{
						info("Application server directory was not specified after '-as' argument.", ILogLevel.ERROR);
						markInvalid();
					}
				}
				else if ("-sw".equalsIgnoreCase(args[i]))
				{
					workspaceIsSplit = true;
				}
				else if ("-dbi".equalsIgnoreCase(args[i]))
				{
					exportUsingDbiFileInfoOnly = true;
				}
				else if ("-dbd".equalsIgnoreCase(args[i]))
				{
					exportIfDBDown = true;
				}
				i++;
			}

			parseArguments(args);

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
			+ "        -sw assume split workspace. Solution and resources projects will be  searched for in\n"
			+ "             direct subfolders of the  given 'workspace_location'. Example: if the workspace\n"
			+ "             needs to contain projects from different git repositories, those can be checked\n"
			+ "             out in '<workspace_location>/repo1', '<workspace_location>/repo2' and so on.\n"
			+ getHelpMessageDbiDbd();
		// @formatter:on
	}

	// dbi and dbd are implemented by mobile exporter, but hardcoded, not configurable - so not part of the help message; allow extending classes to suppress these
	protected String getHelpMessageDbiDbd()
	{
		// @formatter:off
		return
			  "        -dbi ... export based on dbi files (even if database servers are available)\n"
		    + "        -dbd ... try to export even if a needed database is offline, using the .dbi files\n";
		// @formatter:on
	}

	protected String getHelpMessageExistCodes()
	{
		return "\nEXIT codes: 0 - normal, 1 - export stopped by user, 2 - export failed, 3 - invalid arguments";
	}

	/**
	 * Called inside the constructor. So you can count on this method being called in child classes as part of the super(...) call. 
	 */
	protected abstract void parseArguments(String[] args);

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

	public boolean isWorkspaceSplit()
	{
		return workspaceIsSplit;
	}

	public String[] getSolutionNames()
	{
		return solutionNames.split(",");
	}

	public String getSolutionNamesAsString()
	{
		return solutionNames;
	}

	public boolean getExportUsingDbiFileInfoOnly()
	{
		return exportUsingDbiFileInfoOnly;
	}

	public boolean exportIfDBDown()
	{
		return exportIfDBDown;
	}

	public void info(String message, int priority)
	{
		if (priority > ILogLevel.WARNING || verbose)
		{
			System.out.println(message);
		}
	}

}