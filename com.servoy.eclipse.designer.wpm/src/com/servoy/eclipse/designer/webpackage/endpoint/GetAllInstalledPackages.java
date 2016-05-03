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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.BaseSpecProvider;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.j2db.util.Debug;

/**
 * @author gganea
 *
 */
public class GetAllInstalledPackages implements IDeveloperService
{

	/*
	 * (non-Javadoc)
	 *
	 * @see org.sablo.websocket.IServerService#executeMethod(java.lang.String, org.json.JSONObject)
	 */
	public JSONObject executeMethod(String methodName, JSONObject args)
	{
		BaseSpecProvider provider = WebComponentSpecProvider.getInstance();
		JSONArray result = new JSONArray();
		try
		{
			Map<String, String> packagesToVersions = provider.getPackagesToVersions();
			packagesToVersions.putAll(WebServiceSpecProvider.getInstance().getPackagesToVersions());
			Map<String, String> latestVersions = getLatestVersions();

			for (Map.Entry<String, String> entry : latestVersions.entrySet())
			{
				JSONObject pack = new JSONObject();
				pack.put("name", entry.getKey());
				if (packagesToVersions.containsKey(entry.getKey()))
				{
					pack.put("version", packagesToVersions.get(entry.getKey()));
				}
				else pack.put("version", "n/a");
				pack.put("latestVersion", entry.getValue());
				result.put(pack);
			}
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return new JSONObject().put("requestAllInstalledPackages", result);
	}

	/**
	 * @return
	 * @throws Exception
	 */
	private Map<String, String> getLatestVersions() throws Exception
	{

		HashMap<String, String> result = new HashMap<>();
		String repositoriesIndex = getListOfProjectsAsJson("http://servoy.github.io/webpackageindex");

		JSONArray repoArray = new JSONArray(repositoriesIndex);
		for (int i = repoArray.length(); i-- > 0;)
		{
			Object repo = repoArray.get(i);
			if (repo instanceof JSONObject)
			{
				JSONObject repoObject = (JSONObject)repo;
				String geturl = repoObject.getString("url").replace("github.com", "api.github.com/repos") + "/releases/latest";
				String githubResponse = callGithubAPIForLatestVersion(geturl);
				JSONObject response = new JSONObject(githubResponse);
				result.put(repoObject.getString("name"), response.getString("tag_name"));
			}
		}
		return result;
	}

	private String callGithubAPIForLatestVersion(String url)
	{
		StringBuffer result = new StringBuffer();
		try
		{
			// Create an instance of HttpClient.
			HttpClient client = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);

			// add request header
			HttpResponse response;
			response = client.execute(request);

			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";
			while ((line = rd.readLine()) != null)
			{
				result.append(line);
			}
		}
		catch (Throwable e)
		{
			Debug.log(e);
		}
		return result.toString();
	}

	private String getListOfProjectsAsJson(String urlToRead)
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
			return result.toString();
		}
		catch (Exception e)
		{
			Debug.log(e);
		}
		return null;
	}
}
