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

package com.servoy.eclipse.exporter.mobile.ui.wizard;

import java.io.File;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.http.MultipartFormDataBodyPublisher;

/**
 * @author lvostinar
 *
 */
public class PhoneGapConnector
{

	private static final String AUTH_TOKEN_PARAM = "?auth_token=";
	private static final String URL_PHONEGAP_KEYS = "https://build.phonegap.com/api/v1/keys";
	private static final String URL_PHONEGAP_CLOUD = "https://build.phonegap.com/api/v1/apps";

	private ServoyJSONObject jsonContent;
	private final HttpClient client = HttpClient.newBuilder().authenticator(new Authenticator()
	{
		@Override
		protected PasswordAuthentication getPasswordAuthentication()
		{
			return passwordAuthentication;
		}
	}).build(); // Use HttpClientBuilder for HttpClient 11+
	private PasswordAuthentication passwordAuthentication;
	private JSONArray iosCertificates;
	private JSONArray androidCertificates;
	private JSONArray blackberryCertificates;
	private String authToken = null;

	public String loadPhoneGapAcount(String authorisationToken)
	{
		authToken = authorisationToken;
		return loadPhoneGapAccount();
	}

	protected URI getURL(String url)
	{
		return authToken != null ? URI.create(url + AUTH_TOKEN_PARAM + authToken) : URI.create(url);
	}

	public String loadPhoneGapAcount(String username, String password)
	{
		if (username == null || "".equals(username))
		{
			return "Please provide your PhoneGap username";
		}
		if (password == null || "".equals(password))
		{
			return "Please provide your PhoneGap account password";
		}
		passwordAuthentication = new PasswordAuthentication(username, password.toCharArray());

		return loadPhoneGapAccount();
	}

	private ServoyJSONObject getJSONResponse(HttpRequest method) throws Exception
	{
		return client.send(method, responseInfo -> BodySubscribers.mapping(
			BodySubscribers.ofString(StandardCharsets.UTF_8), // Upstream subscriber gives us a String
			(String content) -> { // This function converts the String to JSONObject
				int status = responseInfo.statusCode();

				if (status != HttpURLConnection.HTTP_OK)
				{
					String errorMsg = null;
					String contentType = responseInfo.headers().firstValue("Content-Type").orElse(null);
					if (contentType != null && contentType.contains("json"))
					{
						try
						{
							ServoyJSONObject json = new ServoyJSONObject(content, false);
							if (json.has("error") && json.get("error") instanceof String)
							{
								errorMsg = json.getString("error");
							}
						}
						catch (JSONException ex)
						{
							ServoyLog.logError(ex);
						}
					}

					if (errorMsg == null && status >= 400)//error with no json error message (e.g. 504 Gateway Timeout)
					{
						errorMsg = "Cannot connect to Phonegap. Please try again later";
					}
					if (errorMsg != null)
					{
						String reason = "Status code " + status;
						throw new RuntimeException(errorMsg + " " + reason);
					}
				}

				return new ServoyJSONObject(content, false);
			})).body();

	}

	protected String loadPhoneGapAccount()
	{
		try
		{
			jsonContent = getJSONResponse(HttpRequest.newBuilder(getURL(URL_PHONEGAP_CLOUD)).build());
			JSONObject keys = getJSONResponse(HttpRequest.newBuilder(getURL(URL_PHONEGAP_KEYS)).build());
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
							String url = URL_PHONEGAP_CLOUD + "/" + appID;
							JSONObject jsonApp = getJSONResponse(HttpRequest.newBuilder(getURL(url)).build());
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
			ServoyLog.logError(ex);
			return "Cannot load Phonegap account. Please try again later, message: " + ex.getMessage();
		}
		return null;
	}

	public String[] getExistingApps()
	{
		List<String> pgApps = new ArrayList<String>();
		if (jsonContent != null && jsonContent.has("apps"))
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
		if (jsonContent != null && jsonContent.has("apps"))
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

	public String createOrUpdatePhoneGapApplication(String username, String password, PhoneGapApplication application, MobileExporter mobileExporter,
		File configFile)
	{
		//make sure we have loaded the account and have the latest data before export
		String loadError = loadPhoneGapAcount(username, password);
		if (loadError != null)
		{
			return loadError;
		}

		File exportedFile = null;
		File tempConfigFile = null;
		try
		{
			String url = URL_PHONEGAP_CLOUD;

			PhoneGapApplication existingApplication = getApplication(application.getTitle());
			int appId = -1;
			if (existingApplication != null)
			{
				appId = existingApplication.getId();
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
			MultipartFormDataBodyPublisher entity = new MultipartFormDataBodyPublisher();
			entity.add("data", application.getJSON());

			mobileExporter.setOutputFolder(new File(System.getProperty("java.io.tmpdir")));
			if (!configFile.exists())
			{
				String configTemplate = Utils.getTXTFileContent(getClass().getResourceAsStream("config.xml"), Charset.forName("UTF-8"));
				tempConfigFile = new File(System.getProperty("java.io.tmpdir"), "servoy_mobile_config.xml");
				String appTitle = application.getTitle();
				String sAppId = appTitle != null ? appTitle.replace(" ", "") : "app";
				configTemplate = configTemplate.replace("%%ID%%", sAppId).replace("%%VERSION%%", application.getVersion()).replace("%%NAME%%",
					appTitle).replace("%%DESCRIPTION%%", application.getDescription());
				Utils.writeTXTFile(tempConfigFile, configTemplate, Charset.forName("UTF-8"));
				configFile = tempConfigFile;
			}
			mobileExporter.setConfigFile(configFile);
			exportedFile = mobileExporter.doExport(true, new NullProgressMonitor());
			entity.addFile("file", exportedFile.toPath());

			HttpRequest request;
			if (appId >= 0)
			{
				url += "/" + appId;
				request = HttpRequest.newBuilder(getURL(url)).setHeader("Content-Type", entity.contentType()).PUT(entity).build();
			}
			else
			{
				request = HttpRequest.newBuilder(getURL(url)).setHeader("Content-Type", entity.contentType()).POST(entity).build();
			}

			ServoyJSONObject jsonResponse = getJSONResponse(request);
			appId = jsonResponse.getInt("id");

			if (application.getIconPath() != null && !"".equals(application.getIconPath()))
			{
				File file = Paths.get(application.getIconPath()).normalize().toFile();
				if (file.exists() && file.isFile())
				{
					url = URL_PHONEGAP_CLOUD + "/" + appId + "/icon";
					entity = new MultipartFormDataBodyPublisher();
					entity.addFile("icon", file.toPath());
					request = HttpRequest.newBuilder(getURL(url)).POST(entity).build();
					jsonResponse = getJSONResponse(request);
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
			return "Cannot connect to Phonegap. Please try again later, message: " + ex.getMessage();
		}
		finally
		{
			if (exportedFile != null)
			{
				exportedFile.delete();
			}
			if (tempConfigFile != null)
			{
				tempConfigFile.delete();
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
