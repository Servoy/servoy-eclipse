/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author jcompagner
 * @since 8.2
 *
 */
public class HandleRepositories implements IDeveloperService
{
	public static final String GET_REPOSITORIES = "getRepositories";

	public static final String SET_SELECTED_REPOSITORY = "setSelectedRepository";

	public static final String ADD_REPOSITORY = "addRepository";

	public static final String REMOVE_REPOSITORY = "removeRepository";

	private final GetAllInstalledPackages getAllInstalledPackagesService;

	public HandleRepositories(GetAllInstalledPackages getAllInstalledPackagesService)
	{
		this.getAllInstalledPackagesService = getAllInstalledPackagesService;
	}

	@Override
	public Object executeMethod(JSONObject message)
	{
		String method = message.getString("method");
		if (GET_REPOSITORIES.equals(method))
		{
			return getRepositoryList();
		}
		else if (ADD_REPOSITORY.equals(method))
		{
			JSONObject values = message.getJSONObject("values");
			String url = values.getString("url");
			JSONObject result = new JSONObject();

			List<JSONObject> allpackages = getAllInstalledPackagesService.setSelectedWebPackageIndex(url);
			result.put("packages", allpackages);
			JSONArray storedIndexes = null;
			String indexes = Activator.getEclipsePreferences().node("wpm").get("indexes", null);
			if (indexes == null) storedIndexes = new JSONArray();
			else storedIndexes = new JSONArray(indexes);
			storedIndexes.put(storedIndexes.length(), values);
			Activator.getEclipsePreferences().node("wpm").put("indexes", storedIndexes.toString());
			try
			{
				Activator.getEclipsePreferences().flush();
			}
			catch (BackingStoreException e)
			{
				ServoyLog.logError(e);
			}

			result.put("repositories", getRepositoryList());
			return result;
		}
		else if (SET_SELECTED_REPOSITORY.equals(method))
		{
			String name = message.getString("name");
			if (name.equals("Servoy Default"))
			{
				return getAllInstalledPackagesService.setSelectedWebPackageIndex(GetAllInstalledPackages.MAIN_WEBPACKAGEINDEX);
			}
			else
			{
				String indexes = Activator.getEclipsePreferences().node("wpm").get("indexes", null);
				if (indexes != null)
				{
					JSONArray storedIndexes = new JSONArray(indexes);
					for (int i = 0; i < storedIndexes.length(); i++)
					{
						if (storedIndexes.getJSONObject(i).getString("name").equals(name))
						{
							return getAllInstalledPackagesService.setSelectedWebPackageIndex(storedIndexes.getJSONObject(i).getString("url"));
						}
					}
				}
			}
		}
		else if (REMOVE_REPOSITORY.equals(method))
		{
			String name = message.getString("name");

			JSONArray storedIndexes = null;
			String indexes = Activator.getEclipsePreferences().node("wpm").get("indexes", null);
			if (indexes == null) storedIndexes = new JSONArray();
			else storedIndexes = new JSONArray(indexes);
			for (int i = 0; i < storedIndexes.length(); i++)
			{
				JSONObject repo = storedIndexes.getJSONObject(i);
				if (name.equals(repo.optString("name", null)))
				{
					storedIndexes.remove(i);
					if (GetAllInstalledPackages.getSelectedWebPackageIndex().equals(repo.optString("url")))
					{
						getAllInstalledPackagesService.setSelectedWebPackageIndex(GetAllInstalledPackages.MAIN_WEBPACKAGEINDEX);
					}
					break;
				}
			}
			Activator.getEclipsePreferences().node("wpm").put("indexes", storedIndexes.toString());
			try
			{
				Activator.getEclipsePreferences().flush();
			}
			catch (BackingStoreException e)
			{
				ServoyLog.logError(e);
			}
			return getRepositoryList();
		}
		return null;
	}

	private JSONArray getRepositoryList()
	{
		String selectedUrl = GetAllInstalledPackages.getSelectedWebPackageIndex();

		JSONArray result = new JSONArray();
		JSONObject defaultServoy = new JSONObject();
		defaultServoy.put("name", "Servoy Default");
		defaultServoy.put("selected", selectedUrl.equals(GetAllInstalledPackages.MAIN_WEBPACKAGEINDEX));
		result.put(defaultServoy);

		String indexes = Activator.getEclipsePreferences().node("wpm").get("indexes", null);
		if (indexes != null)
		{
			JSONArray stored = new JSONArray(indexes);
			for (int i = 0; i < stored.length(); i++)
			{
				JSONObject jsonObject = stored.getJSONObject(i);
				jsonObject.put("selected", selectedUrl.equals(jsonObject.getString("url")));
				result.put(jsonObject);
			}
		}
		return result;
	}

	@Override
	public void dispose()
	{
	}

}
