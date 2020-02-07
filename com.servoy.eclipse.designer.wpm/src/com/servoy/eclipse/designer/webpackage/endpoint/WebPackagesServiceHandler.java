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

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author gganea
 *
 */
public class WebPackagesServiceHandler
{
	private final HashMap<String, IDeveloperService> configuredHandlers = new HashMap<>();

	public WebPackagesServiceHandler(WebPackageManagerEndpoint endpoint)
	{
		GetAllInstalledPackages getAllInstalledPackages = new GetAllInstalledPackages(endpoint);
		configuredHandlers.put(GetAllInstalledPackages.CLIENT_SERVER_METHOD, getAllInstalledPackages);
		configuredHandlers.put("install", new InstallWebPackageHandler());
		configuredHandlers.put("showurl", new ShowUrllWebPackageHandler());
		configuredHandlers.put("remove", new RemoveWebPackageHandler());
		configuredHandlers.put(GetSolutionList.GET_SOLUTION_LIST_METHOD, new GetSolutionList(endpoint));
		HandleRepositories handleRepositories = new HandleRepositories(getAllInstalledPackages);
		configuredHandlers.put(HandleRepositories.GET_REPOSITORIES, handleRepositories);
		configuredHandlers.put(HandleRepositories.SET_SELECTED_REPOSITORY, handleRepositories);
		configuredHandlers.put(HandleRepositories.ADD_REPOSITORY, handleRepositories);
		configuredHandlers.put(HandleRepositories.REMOVE_REPOSITORY, handleRepositories);
	}

	public String handleMessage(String message)
	{
		JSONObject msg = new JSONObject(message);
		String method = msg.getString("method");
		IDeveloperService iServerService = configuredHandlers.get(method);
		Object result = iServerService.executeMethod(msg);
		if (result == null) return null;
		JSONObject jsonResult = new JSONObject();

		if ("install".equals(method)) // there is an error during install, the result contains the error message
		{
			jsonResult.put("method", GetAllInstalledPackages.INSTALL_ERROR_METHOD);
		}
		else if (GetAllInstalledPackages.CLIENT_SERVER_METHOD.equals(method) && ((JSONArray)result).length() == 0)
		{
			jsonResult.put("method", GetAllInstalledPackages.CONTENT_NOT_AVAILABLE_METHOD);
		}
		else
		{
			jsonResult.put("method", method);
			jsonResult.put("result", result);
		}

		return jsonResult.toString();
	}

	public void dispose()
	{
		for (IDeveloperService service : configuredHandlers.values())
		{
			service.dispose();
		}
	}
}

