package com.servoy.eclipse.exporter.ngdesktop.utils;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.wicket.validation.validator.UrlValidator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;

public class NgDesktopClientConnection implements Closeable
{
	private String service_url = "https://ngdesktop-builder.servoy.com";
	private String statusMessage = null;
	private int buildCurrentSize;
	private int binarySize;
	private String binaryName;
	private int buildRefSize;
	private int buildRefDuration;

	private CloseableHttpClient httpClient = null;

	private static final int BUFFER_SIZE = 8192;

	private static String BUILD_ENDPOINT = "/build/start";
	private static String STATUS_ENDPOINT = "/build/status/";
	private static String DOWNLOAD_ENDPOINT = "/build/download/";
	private static String BINARY_NAME_ENDPOINT = "/build/name/";
	private static String DELETE_ENDPOINT = "/build/delete/";
	private static String CANCEL_ENDPOINT = "/build/cancel/";

	// START sync - this block need to be identical with the similar error codes
	// from the NgDesktopMonitor in ngdesktop-service project
	public final static int REQUESTS_FULL = 2;
	public final static int BUILDS_FULL = 3;
	public final static int PROCESSING = 4; // installer is currently created
	public final static int ERROR = 5; // creating installer process has run into an error
	public final static int READY = 6; // installer is ready for download
	public final static int WAITING = 7; // waiting in the requests queue
	public final static int CANCELED = 8; // the build has been cancelled
	public final static int NOT_FOUND = 9;
	public final static int ALREADY_STARTED = 10;
	public final static int OK = 11; // no error
	// END sync

	public NgDesktopClientConnection() throws MalformedURLException
	{

		final String srvAddress = System.getProperty("ngclient.service.address");// if no port specified here (address:port) -
		// defaulting to 443
		if (srvAddress != null)
		{// validate format
			final UrlValidator urlValidator = new UrlValidator();
			if (!urlValidator.isValid(srvAddress))
				throw new MalformedURLException("URI is not valid: " + srvAddress);
			service_url = srvAddress;
		}

		final HttpClientBuilder httpBuilder = HttpClientBuilder.create();
		httpClient = httpBuilder.build();
	}

	private String getEncodedData(String resourcePath) throws IOException
	{// expect absolute path
		if (resourcePath != null) try (FileInputStream fis = new FileInputStream(new File(resourcePath)))
		{
			return Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
		}
		return null;
	}

	@Override
	public void close() throws IOException
	{
		if (httpClient != null)
		{
			httpClient.close();
			httpClient = null;
		}
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

		JSONObject jsonObj = new JSONObject();
		if (platform != null)
			jsonObj.put("platform", platform);
		if (settings.get("icon_path") != null && settings.get("icon_path").trim().length() > 0)
			jsonObj.put("icon", getEncodedData(settings.get("icon_path")));
		if (settings.get("image_path") != null && settings.get("image_path").trim().length() > 0)
			jsonObj.put("image", getEncodedData(settings.get("image_path")));
		if (settings.get("copyright") != null && settings.get("image_path").trim().length() > 0)
			jsonObj.put("copyright", settings.get("copyright"));
		if (settings.get("app_url") != null && settings.get("app_url").trim().length() > 0)
			jsonObj.put("url", settings.get("app_url"));
		if (settings.get("ngdesktop_width") != null && settings.get("ngdesktop_width").trim().length() > 0)
			jsonObj.put("width", settings.get("ngdesktop_width"));
		if (settings.get("ngdesktop_height") != null && settings.get("ngdesktop_height").trim().length() > 0)
			jsonObj.put("height", settings.get("ngdesktop_height"));
		if (settings.get("ngdesktop_version") != null && settings.get("ngdesktop_version").trim().length() > 0)
			jsonObj.put("version", settings.get("ngdesktop_version"));
		if (settings.get("ngdesktop_include_update") != null && settings.get("ngdesktop_include_update").trim().length() > 0)
			jsonObj.put("includeUpdate", settings.get("ngdesktop_include_update"));

		final StringEntity input = new StringEntity(jsonObj.toString());
		input.setContentType("application/json");

		final HttpPost postRequest = new HttpPost(service_url + BUILD_ENDPOINT);
		postRequest.setEntity(input);
		ServoyLog.logInfo("Build request for " + service_url + BUILD_ENDPOINT);
		jsonObj = processRequest(postRequest);
		System.out.println(jsonObj.getString("statusMessage"));
		buildRefSize = jsonObj.optInt("buildRefSize", 0);
		buildRefDuration = jsonObj.optInt("buildRefDuration", 0);

		return jsonObj.getString("tokenId");
	}

	/**
	 *
	 * @param tokenId
	 * @return running - the build is currently running error - build has ended with
	 *         errors; ready - build is ready to download
	 * @throws IOException
	 */
	public int getStatus(String tokenId) throws IOException
	{
		final JSONObject jsonObj = processRequest(new HttpGet(service_url + STATUS_ENDPOINT + tokenId));
		buildCurrentSize = jsonObj.optInt("buildCurrentSize", 0);
		statusMessage = jsonObj.getString("statusMessage");
		return jsonObj.getInt("statusCode");
	}

	public String getStatusMessage()
	{
		return statusMessage;
	}

	public int download(String tokenId, String savePath, NgDesktopServiceMonitor monitor) throws IOException // expect
	// absolutePath
	{
		final HttpGet getRequest = new HttpGet(service_url + DOWNLOAD_ENDPOINT + tokenId);
		final JSONObject jsonObj = processRequest(new HttpGet(service_url + BINARY_NAME_ENDPOINT + tokenId));
		binaryName = jsonObj.getString("binaryName");
		binarySize = jsonObj.optInt("binarySize", 0); // MB

		int downloadedBytes = 0;
		int currentSize = 0;

		ServoyLog.logInfo(service_url + DOWNLOAD_ENDPOINT + tokenId);
		monitor.beginTask("Download " + binaryName, binarySize);
		int amount = 0;
		try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest);
			InputStream is = httpResponse.getEntity().getContent();
			FileOutputStream fos = new FileOutputStream(savePath + File.separator + binaryName))
		{

			final byte[] inputFile = new byte[BUFFER_SIZE];

			int n = is.read(inputFile, 0, BUFFER_SIZE);
			downloadedBytes = n;
			while (n != -1)
			{
				if (monitor.isCanceled())
				{
					is.close();
					fos.close();
					new File(savePath).delete();
					return 0; // download failed, cancel was pressed
				}
				if (n > 0)
				{
					monitor.worked(Math.round((float)n / (1024 * 1024)));
					fos.write(inputFile, 0, n);
					amount += n;
				}
				n = is.read(inputFile, 0, BUFFER_SIZE);
				downloadedBytes += n;

				final int bytesToMegaBytes = Math.round((float)downloadedBytes / (1024 * 1024));// bytes => MB
				if (bytesToMegaBytes > 0)
				{// if BUFFER_SIZE is 8kb => 1MB at every 128 steps
					currentSize += bytesToMegaBytes;
					monitor.worked(bytesToMegaBytes);
					downloadedBytes = 0;
				}
			}
			if (binarySize > currentSize) monitor.worked(binarySize - currentSize);
			monitor.done();
		}
		finally
		{
			getRequest.reset();
		}
		ServoyLog.logInfo("Downloaded bytes: " + amount);
		return amount;
	}

	public void delete(String tokenId) throws IOException
	{
		processRequest(new HttpDelete(service_url + DELETE_ENDPOINT + tokenId));
	}

	public void cancel(String tokenId) throws IOException
	{
		processRequest(new HttpPost(service_url + CANCEL_ENDPOINT + tokenId));
	}

	private JSONObject processRequest(HttpRequestBase request) throws IOException
	{
		try (CloseableHttpResponse httpResponse = httpClient.execute(request);
			BufferedReader br = new BufferedReader(
				new InputStreamReader(httpResponse.getEntity().getContent())))
		{
			String output;
			final StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null)
				sb.append(output);
			final JSONObject jsonObj = new JSONObject(sb.toString());
			final int statusCode = jsonObj.optInt("statusCode", OK);
			if (statusCode == ERROR)
			{
				ServoyLog.logInfo("Request: " + request.getRequestLine().toString());
				ServoyLog.logInfo("Error received: " + jsonObj.getString("statusMessage"));
			}
			return jsonObj;
		}
		finally
		{
			request.reset();
		}
	}

	public int getNgDesktopBuildRefSize()
	{
		return buildRefSize;
	}

	public int getNgDesktopBuildRefDuration()
	{
		return buildRefDuration;
	}

	public int getNgDesktopBuildCurrentSize()
	{
		return buildCurrentSize;
	}
}