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

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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

	public String loadPhoneGapAcount(String username, String password)
	{
		try
		{
			String url = "https://build.phonegap.com/api/v1/me";

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
		}
		catch (Exception ex)
		{
			return ex.getMessage();
		}
		return null;
	}

	public String createNewPhoneGapApplication(PhoneGapApplication application, String solutionName, String serverURL, String outputFolder)
	{
		File exportedFile = null;
		try
		{
			String url = "https://build.phonegap.com/api/v1/apps";

			HttpPost post = new HttpPost(url);
			MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			entity.addPart("data", new StringBody(application.getJSON()));

			exportedFile = mobileExporter.doExport(new File(outputFolder), serverURL, solutionName, true);
			entity.addPart("file", new FileBody(exportedFile));

			post.setEntity(entity);

			HttpResponse response = client.execute(post);
			String content = EntityUtils.toString(response.getEntity());
			ServoyJSONObject jsonResponse = new ServoyJSONObject(content, false);
			if (jsonResponse.has("error") && jsonResponse.get("error") instanceof String)
			{
				return jsonResponse.getString("error");
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
}
