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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.BaseSpecProvider;
import org.sablo.specification.BaseSpecProvider.ISpecReloadListener;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class GetAllInstalledPackages implements IDeveloperService, ISpecReloadListener
{
	public static final String CLIENT_SERVER_METHOD = "requestAllInstalledPackages";
	private final WebPackageManagerEndpoint endpoint;

	public GetAllInstalledPackages(WebPackageManagerEndpoint endpoint)
	{
		this.endpoint = endpoint;
		WebComponentSpecProvider.getInstance().addSpecReloadListener(null, this);
		WebServiceSpecProvider.getInstance().addSpecReloadListener(null, this);
	}

	public JSONArray executeMethod(JSONObject msg)
	{
		String activeSolutionName = ServoyModelFinder.getServoyModel().getFlattenedSolution().getName();
		BaseSpecProvider componentSpecProvider = WebComponentSpecProvider.getInstance();
		WebServiceSpecProvider serviceSpecProvider = WebServiceSpecProvider.getInstance();
		JSONArray result = new JSONArray();
		try
		{
			List<JSONObject> remotePackages = getRemotePackages();

			for (JSONObject pack : remotePackages)
			{
				String name = pack.getString("name");
				IPackageReader reader = componentSpecProvider.getPackageReader(name);
				if (reader == null) reader = serviceSpecProvider.getPackageReader(name);
				if (reader != null)
				{
					pack.put("installed", reader.getVersion());
					String parentSolutionName = getParentProjectNameForPackage(reader.getResource());
					pack.put("activeSolution", parentSolutionName != null ? parentSolutionName : activeSolutionName);
				}
				else
				{
					pack.put("activeSolution", msg != null && msg.has("solution") ? msg.get("solution") : activeSolutionName);
				}
				// TODO add the solution where this package is installed in.
				result.put(pack);
			}
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return result;
	}

	private String getParentProjectNameForPackage(File packageFile)
	{
		if (packageFile != null && packageFile.isFile())
		{
			IWorkspaceRoot root = ServoyModel.getWorkspace().getRoot();
			IFile[] files = root.findFilesForLocationURI(packageFile.toURI());
			if (files.length == 1 && files[0] != null && files[0].exists())
			{
				return files[0].getProject().getName();
			}
		}

		return null;
	}

	private List<JSONObject> getRemotePackages() throws Exception
	{
		List<JSONObject> result = new ArrayList<>();
		String repositoriesIndex = getUrlContents("http://servoy.github.io/webpackageindex");

		JSONArray repoArray = new JSONArray(repositoriesIndex);
		for (int i = repoArray.length(); i-- > 0;)
		{
			Object repo = repoArray.get(i);
			if (repo instanceof JSONObject)
			{
				JSONObject repoObject = (JSONObject)repo;
				String packageResponse = getUrlContents(repoObject.getString("url"));
				if (packageResponse == null)
				{
					Debug.log("Couldn't get the package contents of: " + repoObject);
					continue;
				}
				try
				{
					JSONObject packageObject = new JSONObject(packageResponse);
					JSONArray jsonArray = packageObject.getJSONArray("releases");
					List<JSONObject> toSort = new ArrayList<>();
					for (int k = jsonArray.length(); k-- > 0;)
					{
						toSort.add(jsonArray.getJSONObject(k));
					}
					Collections.sort(toSort, new Comparator<JSONObject>()
					{
						@Override
						public int compare(JSONObject o1, JSONObject o2)
						{
							return o2.optString("version", "").compareTo(o1.optString("version", ""));
						}
					});
					packageObject.put("releases", toSort);
					result.add(packageObject);
				}
				catch (Exception e)
				{
					Debug.log("Couldn't get the package contents of: " + repoObject + " error parsing: " + packageResponse, e);
					continue;
				}
			}
		}
		return result;
	}

	private String getUrlContents(String urlToRead)
	{
		try
		{
			StringBuilder result = new StringBuilder();
			URL url = new URL(urlToRead);
			HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null)
			{
				result.append(line);
			}
			rd.close();
			conn.disconnect();
			return result.toString();
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return null;
	}

	@Override
	public void dispose()
	{
		WebComponentSpecProvider.getInstance().removeSpecReloadListener(null, this);
		WebServiceSpecProvider.getInstance().removeSpecReloadListener(null, this);
	}

	@Override
	public void webObjectSpecificationReloaded()
	{
		JSONArray packages = executeMethod(null);
		JSONObject jsonResult = new JSONObject();
		jsonResult.put("method", CLIENT_SERVER_METHOD);
		jsonResult.put("result", packages);
		endpoint.send(jsonResult.toString());
	}

}
