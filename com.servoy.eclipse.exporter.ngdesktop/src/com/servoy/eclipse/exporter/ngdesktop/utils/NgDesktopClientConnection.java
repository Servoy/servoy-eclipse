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
	private int downloadSize;
	private int buildCurrentSize;
	private int buildRefSize;
	private int buildRefDuration;

	private CloseableHttpClient httpClient = null;

	private static final int BUFFER_SIZE = 8192;

	private static String BUILD_ENDPOINT = "/build/start";
	private static String STATUS_ENDPOINT = "/build/status/";
	private static String DOWNLOAD_ENDPOINT = "/build/download/";
	private static String BUILD_NAME_ENDPOINT = "/build/name/";
	private static String DELETE_ENDPOINT = "/build/delete/";
	private static String CANCEL_ENDPOINT = "/build/cancel/";

	// START sync - this block need to be identical with the similar error codes
	// from the NgDesktopMonitor in ngdesktop-service project
	public final static int RUNNING = 0;
	public final static int NOT_RUNNING = 1;
	public final static int REQUESTS_FULL = 2;
	public final static int BUILDS_FULL = 3;
	public final static int PROCESSING = 4; // installer is currently created
	public final static int ERROR = 5; // creating installer process has run into an error
	public final static int WARNING = 12;
	public final static int READY = 6; // installer is ready for download
	public final static int WAITING = 7; // waiting in the requests queue
	public final static int CANCELED = 8; // the build has been cancelled
	public final static int NOT_FOUND = 9;
	public final static int ALREADY_STARTED = 10;
	public final static int OK = 11; // no error
	public final static int ACCESS_DENIED = 13; //authorization failed
	public final static int DOWNLOAD_ARCHIVE = 14; //Build not supported. Download binary instead.
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
	public JSONObject startBuild(IDialogSettings settings) throws IOException
	{

		JSONObject jsonObj = new JSONObject();
		if (settings.get("platform") != null && settings.get("platform").trim().length() > 0)
			jsonObj.put("platform", settings.get("platform"));
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
		if (settings.get("include_update") != null && settings.get("include_update").trim().length() > 0)
			jsonObj.put("includeUpdate", settings.get("include_update"));
		if (settings.get("update_url") != null && settings.get("update_url").trim().length() > 0)
			jsonObj.put("updateUrl", settings.get("update_url"));
		if (settings.get("login_token") != null)
			jsonObj.put("loginToken", settings.get("login_token"));
		if (settings.get("application_name") != null)
			jsonObj.put("applicationName", settings.get("application_name"));

		final StringEntity input = new StringEntity(jsonObj.toString());
		input.setContentType("application/json");

		final HttpPost postRequest = new HttpPost(service_url + BUILD_ENDPOINT);
		postRequest.setEntity(input);
		ServoyLog.logInfo("Build request for " + service_url + BUILD_ENDPOINT);
		jsonObj = processRequest(postRequest);

		buildRefSize = jsonObj.optInt("buildRefSize", 0);
		buildRefDuration = jsonObj.optInt("buildRefDuration", 0);

		//return jsonObj.getString("tokenId");
		return jsonObj;
	}

	/**
	 *
	 * @param tokenId
	 * @return running - the build is currently running error - build has ended with
	 *         errors; ready - build is ready to download
	 * @throws IOException
	 */
	public JSONObject getStatus(String tokenId) throws IOException
	{
		final JSONObject jsonObj = processRequest(new HttpGet(service_url + STATUS_ENDPOINT + tokenId));
		buildCurrentSize = jsonObj.optInt("buildCurrentSize", 0);
		statusMessage = jsonObj.getString("statusMessage");
		return jsonObj;
	}

	public String getStatusMessage()
	{
		return statusMessage;
	}

	//expect absolute path
	public int download(String tokenId, String savePath, NgDesktopServiceMonitor monitor) throws IOException
	{
		final JSONObject jsonObj = processRequest(new HttpGet(service_url + BUILD_NAME_ENDPOINT + tokenId));
		final String binaryName = jsonObj.getString("binaryName");
		final String updateName = jsonObj.optString("updateName", null);
		final String yamlName = "latest.yml";
		final String readmeName = "readme.txt";
		downloadSize = jsonObj.optInt("binarySize", 0); // size in MB; value contain also update size if requested

		monitor.beginTask("Download...", downloadSize);
		int downloadedMBytes = downloadFile(binaryName, tokenId, savePath, monitor, false);
		if (updateName != null)
		{
			downloadedMBytes += downloadFile(updateName, tokenId, savePath, monitor, true);
			downloadedMBytes += downloadFile(yamlName, tokenId, savePath, monitor, true);
			downloadedMBytes += downloadFile(readmeName, tokenId, savePath, monitor, true);
		}
		monitor.fillRemainingSteps();
		monitor.done();
		ServoyLog.logInfo("Downloaded (Mb): " + downloadedMBytes);
		return downloadedMBytes;
	}

	private int downloadFile(String fileName,
		String tokenId,
		String savePath,
		NgDesktopServiceMonitor monitor,
		boolean isUpdate)
		throws IOException
	{
		String strUrl = service_url + DOWNLOAD_ENDPOINT + tokenId;
		if (isUpdate) strUrl = service_url + DOWNLOAD_ENDPOINT + tokenId + "/" + fileName;
		final HttpGet getRequest = new HttpGet(strUrl);

		ServoyLog.logInfo(service_url + DOWNLOAD_ENDPOINT + tokenId);
		monitor.setTaskName("Download " + fileName + "...");

		int totalBytesRead = 0;
		int bytesCountForMonitor = 0;
		try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest);
			InputStream is = httpResponse.getEntity().getContent();
			FileOutputStream fos = new FileOutputStream(savePath + File.separator + fileName))
		{

			final byte[] inputFile = new byte[BUFFER_SIZE];
			int readBytes = is.read(inputFile, 0, BUFFER_SIZE);
			while (readBytes != -1)
			{
				if (monitor.isCanceled())
				{
					is.close();
					fos.close();
					new File(savePath).delete();
					return 0; // download failed, cancel was pressed
				}
				if (readBytes > 0)
				{
					fos.write(inputFile, 0, readBytes);
					totalBytesRead += readBytes;
					bytesCountForMonitor += readBytes;
				}
				readBytes = is.read(inputFile, 0, BUFFER_SIZE);

				final float countMB = (float)bytesCountForMonitor / (1024 * 1024);// bytes => MB;
				final float fractionalMB = countMB % 1;
				final int decimalMB = Math.round(countMB - fractionalMB);
				if (decimalMB > 0)
				{
					monitor.worked(decimalMB);
					bytesCountForMonitor = 0;
				}
			}
		}
		finally
		{
			getRequest.reset();
		}
		return totalBytesRead / (1024 * 1024); //bytes to Mb
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