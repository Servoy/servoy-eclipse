/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.designer.webpackage.endpoint;

import java.util.HashMap;

import org.json.JSONObject;

/**
 * @author gganea
 *
 */
public class WebPackagesServiceHandler
{
	private final HashMap<String, IDeveloperService> configuredHandlers = new HashMap<>();

	public WebPackagesServiceHandler()
	{
		configuredHandlers.put("requestAllInstalledPackages", new GetAllInstalledPackages());
		configuredHandlers.put("install", new InstallWebPackageHandler());
	}

	public JSONObject handleMessage(String message)
	{
		JSONObject msg = new JSONObject(message);
		IDeveloperService iServerService = configuredHandlers.get(msg.get("method"));
		JSONObject args = null;
		if (msg.has("args")) args = msg.getJSONObject("args");
		return iServerService.executeMethod(message, args);
	}

}

