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

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;

/**
 * Stores and provides the export-relevant arguments the product was started with.
 * @author gboros
 */
public class WarArgumentChest extends AbstractArgumentChest
{

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
			+ "        -as <app_server_dir> ... specifies where to find the 'application_server' directory.\n"
			+ "             Default: '../../application_server'.\n"
			+ "        -pl alternate project locations; solution  and  resources projects will  be searched\n"
			+ "             for in direct subfolders of the given 'workspace_location'.     Example: if the\n"
			+ "             workspace needs to contain projects from different git repositories,  those can\n"
			+ "             be checked out in '<workspace_location>/a', '<workspace_location>/b' and so on.\n"
			+ "        -ie ignore build errors.  CAUTION! the use of this flag is discouraged; it can cause\n"
			+ "             invalid solutions to be exported.";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest#parseArguments(java.lang.String[])
	 */
	@Override
	protected void parseArguments(String[] args)
	{
	}
}
