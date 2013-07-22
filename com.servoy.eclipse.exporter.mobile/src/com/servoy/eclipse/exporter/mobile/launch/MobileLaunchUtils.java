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

package com.servoy.eclipse.exporter.mobile.launch;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;

/**
 * Utility methods useful for launching a mobile client/mobile test client.
 * 
 * @author acostescu
 */
@SuppressWarnings("nls")
public class MobileLaunchUtils
{

	public static String getWarFileName(String solutionName, boolean testURL)
	{
		return solutionName + (testURL ? MobileExporter.TEST_WAR_SUFFIX : "");
	}

	public static String getDefaultApplicationURL(String warFileName, int port)
	{
		// this could be enhanced a bit for https usage (based on port? or some more parsing of the server.xml)
		return getApplicationURL("http://localhost:" + port + "/", warFileName);
	}

	/**
	 * @param containerBaseURL must end with "/"
	 */
	public static String getApplicationURL(String containerBaseURL, String warFileName)
	{
		// this could be enhanced a bit for https usage (based on port? or some more parsing of the server.xml)
		return containerBaseURL + warFileName + "/index.html";
	}

	public static String getTestApplicationURL(String baseURL, boolean nodebug, int bridgeID)
	{
		String appURL = baseURL;

		boolean hasArgs = (appURL.indexOf("?") != -1);
		appURL = appURL + (hasArgs ? "&" : "?") + "log_level=DEBUG&noinitsmc=true&bid=" + bridgeID;
		if (nodebug && !appURL.contains("nodebug=true"))
		{
			appURL = appURL + "&nodebug=true";
		}

		return appURL;
	}

}
