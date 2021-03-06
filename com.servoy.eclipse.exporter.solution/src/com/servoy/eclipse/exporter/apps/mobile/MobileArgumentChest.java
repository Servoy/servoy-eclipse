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

package com.servoy.eclipse.exporter.apps.mobile;

import java.util.HashMap;

import com.servoy.eclipse.exporter.apps.common.AbstractArgumentChest;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.j2db.util.ILogLevel;

/**
 * Stores and provides the export-relevant arguments the product was started with.
 * @author acostescu
 */
public class MobileArgumentChest extends AbstractArgumentChest
{

	private boolean testing; // test war by default
	private String serverURL;
	private int syncTimeout;
	private String serviceSolutionName = null;
	private boolean useLongTestMethodNames = false;

	public MobileArgumentChest(String[] args)
	{
		super();
		initialize(args);
	}

	@Override
	protected void parseArguments(HashMap<String, String> argsMap)
	{
		// set defaults; these can't be set when declaring the member because this method is called from the super class constructor
		testing = true; // test war by default
		syncTimeout = MobileExporter.DEFAULT_SYNC_TIMEOUT;
		serverURL = parseArg("server_url", "Server url was not specified after '-server_url' argument.", argsMap, false);
		serviceSolutionName = parseArg("service_solution", "Service solution was not specified after '-service_solution' argument.", argsMap, false);
		if (argsMap.containsKey("production")) testing = false;
		if (argsMap.containsKey("sync_timeout")) parseSyncTimeout(argsMap.get("sync_timeout"));
		if (argsMap.containsKey("long_test_names")) useLongTestMethodNames = true;
	}

	private void parseSyncTimeout(String value)
	{
		if (!value.equals(""))
		{
			try
			{
				syncTimeout = Integer.parseInt(value);
				if (syncTimeout < 1)
				{
					syncTimeout = 1;
					info("Sync timeout < 1. Corrected to 1.", ILogLevel.ERROR);
				}
			}
			catch (NumberFormatException e)
			{
				info("Sync timeout specified after '-sync_timeout' argument is not an integer value.", ILogLevel.ERROR);
				markInvalid();
			}
		}
		else
		{
			info("Sync timeout was not specified after '-sync_timeout' argument.", ILogLevel.ERROR);
			markInvalid();
		}
	}

	@Override
	public String getHelpMessage()
	{
		// @formatter:off
		return "Workspace exporter for mobile solutions. Exports mobile workspace solutions in .war format.\n"
			+ super.getHelpMessageCore()
			+ "        -production ... export normal mobile client. Default: false (exports unit test war).\n"
			+ "        -server_url <url> ... application server URL.  Used to find mobile service solution.\n"
			+ "             Default: http://localhost:[detectedInstallationPortNumber]\n"
			+ "        -service_solution ... name    of    the   service   solution    (default   will   be\n"
			+ "                              mySolutionName_service).\n"
			+ "        -sync_timeout <seconds> ... client sync call timeout. Default: " + MobileExporter.DEFAULT_SYNC_TIMEOUT + " sec.\n"
			+ "        -long_test_names ... only if '-production'  is not set; it will generate 'long' test\n"
			+ "                           method names that include solution and form/scope name;  this can\n"
			+ "                           help with  dumb junit tools  that ignore junit test suite nesting\n"
			+ "                           when showing test results - thus loosing that information.\n"
			+ getHelpMessageExitCodes();
		// @formatter:on
	}

	@Override
	protected String getHelpMessageDbi()
	{
		return "";
	}

	public boolean shouldExportForTesting()
	{
		return testing;
	}

	public String getServerURL()
	{
		if (serverURL == null)
		{
			serverURL = MobileExporter.getDefaultServerURL(); // at this point the app. server should be initialized; if really needed before initilize we could always use WebServer.getHTTPPort() directly
		}
		return serverURL;
	}

	/**
	 * @return the serviceSolutionName
	 */
	public String getServiceSolutionName()
	{
		return serviceSolutionName;
	}

	public int getSyncTimeout()
	{
		return syncTimeout;
	}

	@Override
	public boolean shouldExportUsingDbiFileInfoOnly()
	{
		return true; // mobile exporter doesn't use table info
	}

	public boolean useLongTestMethodNames()
	{
		return useLongTestMethodNames;
	}

}