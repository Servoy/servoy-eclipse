package com.servoy.eclipse.exporter.ngdesktop.wsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;

public class NgDesktopClientConnection
{
	private String service_url = "https://ngdesktop-builder.servoy.com";
	private String statusMessage = null;

	HttpClientBuilder httpBuilder = null;
	private CloseableHttpClient httpClient = null;

	private static final int BUFFER_SIZE = 8192;

	private static String BUILD_ENDPOINT = "/build/start";
	private static String STATUS_ENDPOINT = "/build/status/";
	private static String DOWNLOAD_ENDPOINT = "/build/download/";
	private static String BINARY_NAME_ENDPOINT = "/build/name/"; 
	private static String CANCEL_ENDPOINT = "/build/cancel/";//TODO: add cancel support

	//START sync - this block need to be identical with the similar error codes from the NgDesktopMonitor in ngdesktop-service project
	public final static int REQUESTS_FULL = 2;
	public final static int BUILDS_FULL = 3;
	public final static int PROCESSING = 4; // installer is currently created
	public final static int ERROR = 5; // creating installer process has run into an error
	public final static int READY = 6; // installer is ready for download
	public final static int WAITING = 7; // waiting in the requests queue
	public final static int CANCELED = 8; // the build has been cancelled
	public final static int NOT_FOUND = 9;
	public final static int ALREADY_STARTED = 10;
	//END sync

	public NgDesktopClientConnection() throws MalformedURLException
	{
		
		String srvAddress = System.getProperty("ngclient.service.address");//if no port specified here (address:port) - defaulting to 443
		if (srvAddress != null) {//validate format
			UrlValidator urlValidator = new UrlValidator();
			if (!urlValidator.isValid(srvAddress)) throw new MalformedURLException(srvAddress);
			service_url = srvAddress;
		}
		
		
		httpBuilder = HttpClientBuilder.create();
		httpClient = httpBuilder.build();
	}

	private String getEncodedData(String resourcePath) throws IOException
	{//expect absolute path
		if (resourcePath != null)
		{
			return Base64.getEncoder().encodeToString(IOUtils.toByteArray(new FileInputStream(new File(resourcePath))));
		}
		return null;
	}

	public void closeConnection() throws IOException
	{
		if (httpClient != null)
		{
			httpClient.close();
			httpClient = null;
		}
		httpBuilder = null;
	}

	/**
	 * 
	 * @param platform
	 * @param iconPath
	 * @param imagePath
	 * @param copyright
	 * @return tokenId - string id to be used in future queries
	 * @throws IOException
	 */
	public String startBuild(String platform, IDialogSettings settings) throws IOException
	{
		HttpPost postRequest = new HttpPost(service_url + BUILD_ENDPOINT);
		JSONObject jsonObj = new JSONObject();
		if (platform != null) jsonObj.put("platform", platform);
		if (settings.get("icon_path") != null && settings.get("icon_path").trim().length() > 0) jsonObj.put("icon", getEncodedData(settings.get("icon_path")));
		if (settings.get("image_path") != null && settings.get("image_path").trim().length() > 0) jsonObj.put("image", getEncodedData(settings.get("image_path")));
		if (settings.get("copyright") != null && settings.get("image_path").trim().length() > 0) jsonObj.put("copyright", settings.get("copyright"));
		if (settings.get("app_url") != null && settings.get("app_url").trim().length() > 0) jsonObj.put("url", settings.get("app_url"));
		if (settings.get("ngdesktop_width") != null && settings.get("ngdesktop_width").trim().length() > 0) jsonObj.put("width", settings.get("ngdesktop_width"));
		if (settings.get("ngdesktop_height") != null && settings.get("ngdesktop_height").trim().length() > 0) jsonObj.put("height", settings.get("ngdesktop_height"));

		StringEntity input = new StringEntity(jsonObj.toString());
		input.setContentType("application/json");
		postRequest.setEntity(input);

		ServoyLog.logInfo("Build request for " + service_url + BUILD_ENDPOINT);
		
		try (CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())))) {
			
			//verify status code
			if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK)
			{
				throw new IOException("Http error: " + httpResponse.getStatusLine().getStatusCode() + ": " + httpResponse.getStatusLine().getReasonPhrase());
			}
			
			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null)
				sb.append(output);

			jsonObj = new JSONObject(sb.toString());
			if (jsonObj.getInt("statusCode") != WAITING)
			{ //this is the first status set on the service on a normal processing
				throw new IOException(jsonObj.getString("statusMessage"));
			}
		} finally {
			postRequest.reset();
		}
		return (String)jsonObj.get("tokenId");
	}

	/**
	 * 
	 * @param tokenId
	 * @return
	 * 			running - the build is currently running
	 * 			error - build run into an error; use getErrorDetails(tokenId) for get error details
	 * 			ready - build is ready to download
	 * @throws IOException
	 */
	public int getStatus(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(service_url + STATUS_ENDPOINT + tokenId);
		try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest);
			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())))) {
			
			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null)
				sb.append(output);
			JSONObject jsonObj = new JSONObject(sb.toString());
			int statusCode = jsonObj.getInt("statusCode");
			statusMessage = (String)jsonObj.get("statusMessage");
			return statusCode;
		} finally {
			getRequest.reset();
		}
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public String getBinaryName(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(service_url + BINARY_NAME_ENDPOINT + tokenId);
		try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest);
			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())))) {
			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null)
				sb.append(output);
	
			JSONObject jsonObj = new JSONObject(sb.toString());
			return (String)jsonObj.get("binaryName");
		} finally {
			getRequest.reset();
		}
	}

	public void download(String tokenId, String savePath) throws IOException //expect absolutePath
	{
		String binaryName = getBinaryName(tokenId);
		HttpGet getRequest = new HttpGet(service_url + DOWNLOAD_ENDPOINT + tokenId);

		ServoyLog.logInfo(service_url + DOWNLOAD_ENDPOINT + tokenId);

		int amount = 0;
		try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest);
			InputStream is = httpResponse.getEntity().getContent();
			FileOutputStream fos = new FileOutputStream(savePath + binaryName)) { 
			
			byte[] inputFile = new byte[BUFFER_SIZE];
			
			int n = is.read(inputFile, 0, BUFFER_SIZE);
			
			while (n != -1)
			{
				if (n > 0)
				{
					fos.write(inputFile, 0, n);
					amount += n;
				}
				n = is.read(inputFile, 0, BUFFER_SIZE);
			}
		} finally {
			getRequest.reset();
		}
		ServoyLog.logInfo("Downloaded bytes: " + amount);
	}
}
