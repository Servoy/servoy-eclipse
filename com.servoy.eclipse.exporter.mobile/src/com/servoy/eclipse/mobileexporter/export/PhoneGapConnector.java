/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.mobileexporter.export;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author lvostinar
 *
 */
public class PhoneGapConnector
{
	private ServoyJSONObject jsonContent;
	private final DefaultHttpClient client = new DefaultHttpClient();
	private final MobileExporter mobileExporter = new MobileExporter();
	private JSONArray iosCertificates;
	private JSONArray androidCertificates;
	private JSONArray blackberryCertificates;

	public String loadPhoneGapAcount(String username, String password)
	{
		try
		{
			String url = "https://build.phonegap.com/api/v1/apps";

			BasicCredentialsProvider bcp = new BasicCredentialsProvider();
			URL _url = new URL(url);
			bcp.setCredentials(new AuthScope(_url.getHost(), _url.getPort()), new UsernamePasswordCredentials(username, password));
			client.setCredentialsProvider(bcp);

			HttpResponse response = client.execute(new HttpGet(url));
			String content = EntityUtils.toString(response.getEntity());
			jsonContent = new ServoyJSONObject(content, false);
			if (jsonContent.has("error"))
			{
				return jsonContent.getString("error");
			}

			response = client.execute(new HttpGet("https://build.phonegap.com/api/v1/keys"));
			JSONObject keys = new ServoyJSONObject(EntityUtils.toString(response.getEntity()), false);
			if (keys.has("keys"))
			{
				keys = keys.getJSONObject("keys");
			}
			if (keys.has("error"))
			{
				return keys.getString("error");
			}
			if (keys.has("ios"))
			{
				iosCertificates = keys.getJSONObject("ios").getJSONArray("all");
			}
			if (keys.has("android"))
			{
				androidCertificates = keys.getJSONObject("android").getJSONArray("all");
			}
			if (keys.has("blackberry"))
			{
				blackberryCertificates = keys.getJSONObject("blackberry").getJSONArray("all");
			}

			if ((iosCertificates != null && iosCertificates.length() > 0) || (androidCertificates != null && androidCertificates.length() > 0) ||
				(blackberryCertificates != null && blackberryCertificates.length() > 0))
			{
				if (jsonContent.has("apps"))
				{
					try
					{
						JSONArray apps = (JSONArray)jsonContent.get("apps");
						for (int i = 0; i < apps.length(); i++)
						{
							int appID = apps.getJSONObject(i).getInt("id");
							url = "https://build.phonegap.com/api/v1/apps/" + appID;
							response = client.execute(new HttpGet(url));
							JSONObject jsonApp = new ServoyJSONObject(EntityUtils.toString(response.getEntity()), false);
							if (jsonApp.has("keys"))
							{
								apps.getJSONObject(i).put("keys", jsonApp.getJSONObject("keys"));
							}
						}
					}
					catch (JSONException e)
					{
						ServoyLog.logError(e);
					}

				}
			}
		}
		catch (Exception ex)
		{
			return ex.getMessage();
		}
		return null;
	}

	public String[] getExistingApps()
	{
		List<String> pgApps = new ArrayList<String>();
		if (jsonContent.has("apps"))
		{
			try
			{
				JSONArray apps = (JSONArray)jsonContent.get("apps");
				for (int i = 0; i < apps.length(); i++)
				{
					pgApps.add(apps.getJSONObject(i).getString("title"));
				}
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}

		}
		return pgApps.toArray(new String[0]);
	}

	public PhoneGapApplication getApplication(String title)
	{
		if (jsonContent.has("apps"))
		{
			try
			{
				JSONArray appArray = jsonContent.getJSONArray("apps");
				for (int i = 0; i < appArray.length(); i++)
				{
					if (title.equals(appArray.getJSONObject(i).getString("title")))
					{
						String version = appArray.getJSONObject(i).has("version") ? appArray.getJSONObject(i).getString("version") : "";
						String description = appArray.getJSONObject(i).has("description") ? appArray.getJSONObject(i).getString("description") : "";
						boolean publicApplication = appArray.getJSONObject(i).has("private") ? !appArray.getJSONObject(i).getBoolean("private") : false;
						String iconPath = null;
						if (appArray.getJSONObject(i).has("icon"))
						{
							iconPath = appArray.getJSONObject(i).getJSONObject("icon").getString("filename");
						}
						int id = appArray.getJSONObject(i).has("id") ? appArray.getJSONObject(i).getInt("id") : -1;
						List<String> certificates = new ArrayList<String>();
						if (appArray.getJSONObject(i).has("keys"))
						{
							addPlatformKey(appArray.getJSONObject(i), "ios", certificates);
							addPlatformKey(appArray.getJSONObject(i), "android", certificates);
							addPlatformKey(appArray.getJSONObject(i), "blackberry", certificates);
						}
						return new PhoneGapApplication(title, version, description, publicApplication, iconPath, certificates.toArray(new String[0]), id);
					}
				}
			}
			catch (JSONException e)
			{
				ServoyLog.logError(e);
			}

		}
		return null;
	}

	private void addPlatformKey(JSONObject app, String platform, List<String> certificates) throws JSONException
	{
		if (app.has("keys"))
		{
			JSONObject platformKey = app.getJSONObject("keys").optJSONObject(platform);
			if (platformKey != null && platformKey.has("title"))
			{
				certificates.add(platformKey.getString("title"));
			}
		}
	}

	public String createOrUpdatePhoneGapApplication(PhoneGapApplication application, String solutionName, String serverURL, int timeout, File configFile)
	{
		File exportedFile = null;
		try
		{
			String url = "https://build.phonegap.com/api/v1/apps";

			PhoneGapApplication existingApplication = getApplication(application.getTitle());
			int appId = -1;
			HttpEntityEnclosingRequestBase request;
			if (existingApplication != null)
			{
				appId = existingApplication.getId();
			}
			if (appId >= 0)
			{
				url += "/" + appId;
				request = new HttpPut(url);
			}
			else
			{
				request = new HttpPost(url);
			}
			if (application.getCertificates() != null && application.getCertificates().length > 0)
			{
				Map<String, Object> selectedCertificates = new HashMap<String, Object>();
				for (String certificateTitle : application.getCertificates())
				{
					addSelectedCertificate(iosCertificates, "ios", certificateTitle, selectedCertificates);
					addSelectedCertificate(androidCertificates, "android", certificateTitle, selectedCertificates);
					addSelectedCertificate(blackberryCertificates, "blackberry", certificateTitle, selectedCertificates);
				}
				JSONObject json = new JSONObject(selectedCertificates);
				application.setSelectedCertificates(json);
			}
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			entity.addPart("data", new StringBody(application.getJSON()));

			mobileExporter.setServerURL(serverURL);
			mobileExporter.setSolutionName(solutionName);
			mobileExporter.setTimeout(timeout);
			mobileExporter.setOutputFolder(new File(System.getProperty("java.io.tmpdir")));
			mobileExporter.setConfigFile(configFile);
			exportedFile = mobileExporter.doExport(true);
			entity.addPart("file", new FileBody(exportedFile));

			request.setEntity(entity);

			HttpResponse response = client.execute(request);
			String content = EntityUtils.toString(response.getEntity());
			ServoyJSONObject jsonResponse = new ServoyJSONObject(content, false);
			if (jsonResponse.has("error") && jsonResponse.get("error") instanceof String)
			{
				return jsonResponse.getString("error");
			}
			appId = jsonResponse.getInt("id");

			if (application.getIconPath() != null && !"".equals(application.getIconPath()))
			{
				File file = new File(application.getIconPath());
				if (file.exists() && file.isFile())
				{
					url = "https://build.phonegap.com/api/v1/apps/" + appId + "/icon";
					request = new HttpPost(url);
					entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
					entity.addPart("icon", new FileBody(file));
					request.setEntity(entity);
					response = client.execute(request);
					jsonResponse = new ServoyJSONObject(EntityUtils.toString(response.getEntity()), false);
					if (jsonResponse.has("error") && jsonResponse.get("error") instanceof String)
					{
						return jsonResponse.getString("error");
					}
				}
			}
		}
		catch (Exception ex)
		{
			return ex.getMessage();
		}
		finally
		{
			if (exportedFile != null)
			{
				exportedFile.delete();
			}
		}
		return null;
	}

	private void addSelectedCertificate(JSONArray certificates, String platform, String certificateTitle, Map<String, Object> selectedCertificates)
		throws JSONException
	{
		if (certificates != null)
		{
			for (int i = 0; i < certificates.length(); i++)
			{
				if (certificateTitle.equals(certificates.getJSONObject(i).getString("title")))
				{
					selectedCertificates.put(platform, certificates.getJSONObject(i));
				}
			}
		}
	}

	public String[] getCertificates()
	{
		List<String> certificates = new ArrayList<String>();
		try
		{
			if (iosCertificates != null)
			{
				for (int i = 0; i < iosCertificates.length(); i++)
				{
					certificates.add(iosCertificates.getJSONObject(i).getString("title"));
				}
			}
			if (androidCertificates != null)
			{
				for (int i = 0; i < androidCertificates.length(); i++)
				{
					certificates.add(androidCertificates.getJSONObject(i).getString("title"));
				}
			}
			if (blackberryCertificates != null)
			{
				for (int i = 0; i < blackberryCertificates.length(); i++)
				{
					certificates.add(blackberryCertificates.getJSONObject(i).getString("title"));
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return certificates.toArray(new String[0]);
	}
}
