package com.servoy.eclipse.exporter.ngdesktop.wsclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;

public class NgDesktopClientConnection
{
	private String protocol = null;
	private InetAddress serverAddress = null;
	private int serverPort = 0;

	HttpClientBuilder httpBuilder = null;
	private CloseableHttpClient httpClient = null;

	private static final int BUFFER_SIZE = 8192;

	private static String BUILD_ENDPOINT = "/ngdesktopws/build";
	private static String STATUS_ENDPOINT = "/ngdesktopws/status/";
	private static String DOWNLOAD_ENDPOINT = "/ngdesktopws/download/";
	private static String BINARY_NAME_ENDPOINT = "/ngdesktopws/binary/";
	private static String BUILD_ERROR_ENDPOINT = "/ngdesktopws/error/";
	private static String DELETE_ENDPOINT = "/ngdesktopws/delete/";

	public final static String REQUEST_ALREADY_REGISTERED = "Already registered";
	public final static String BUILD_ALREADY_RUNNING = "Already running";
	public final static String REQUEST_STACK_FULL = "Requests full";
	public final static String PROCESSING = "processing"; // installer is currently created
	public final static String ERROR = "error"; // creating installer process has run into an error
	public final static String READY = "ready"; // installer is ready for download
	public final static String WAITING = "waiting"; // waiting in the requests queue
	public final static String DELETED = "deleted"; // the build has been deleted

	public NgDesktopClientConnection(String protocol, InetAddress serverAddress, int serverPort)
	{
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		this.protocol = protocol;
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
	public String startBuild(String platform, String iconPath, String imagePath, String copyright, String appUrl) throws IOException
	{
		try
		{
			HttpPost postRequest = new HttpPost(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + BUILD_ENDPOINT);
			JSONObject jsonObj = new JSONObject();
			if (platform != null) jsonObj.put("platform", platform);
			if (iconPath != null) jsonObj.put("icon", getEncodedData(iconPath));
			if (imagePath != null) jsonObj.put("image", getEncodedData(imagePath));
			if (copyright != null) jsonObj.put("copyright", copyright);
			if (appUrl != null) jsonObj.put("url", appUrl);

			StringEntity input = new StringEntity(jsonObj.toString());
			input.setContentType("application/json");
			postRequest.setEntity(input);

			ServoyLog.logInfo("Build request for " + protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + BUILD_ENDPOINT);

			HttpResponse httpResponse = httpClient.execute(postRequest);

			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

			String output;
			StringBuffer sb = new StringBuffer();
			while ((output = br.readLine()) != null)
				sb.append(output);

			jsonObj = new JSONObject(sb.toString());
			//TODO: if tokenid = RESOURCE_BUSY - throw new IOException(stack service is full. Try again later)
			return (String)jsonObj.get("tokenId");
		}
		catch (UnsupportedEncodingException | ClientProtocolException e)
		{
			// not the case
		}
		return null;
	}

	public String getErrorDetails(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + BUILD_ERROR_ENDPOINT + tokenId);
		HttpResponse httpResponse = httpClient.execute(getRequest);

		BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
		String output;
		StringBuffer sb = new StringBuffer();
		while ((output = br.readLine()) != null)
			sb.append(output);

		JSONObject jsonObj = new JSONObject(sb.toString());
		String errorMessage = (String)jsonObj.get("errorMessage");
		ServoyLog.logError(errorMessage, new Exception(errorMessage));
		return errorMessage;
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
	public String getStatus(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + STATUS_ENDPOINT + tokenId);
		HttpResponse httpResponse = httpClient.execute(getRequest);

		BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
		String output;
		StringBuffer sb = new StringBuffer();
		while ((output = br.readLine()) != null)
			sb.append(output);

		JSONObject jsonObj = new JSONObject(sb.toString());
		//TODO: return data for a progress bar. NGDesktop service need to be modified to provide this information
		return (String)jsonObj.get("status");
	}

	public String getBinaryName(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + BINARY_NAME_ENDPOINT + tokenId);
		HttpResponse httpResponse = httpClient.execute(getRequest);

		BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
		String output;
		StringBuffer sb = new StringBuffer();
		while ((output = br.readLine()) != null)
			sb.append(output);

		JSONObject jsonObj = new JSONObject(sb.toString());
		return (String)jsonObj.get("binaryName");
	}

	public void download(String tokenId, String savePath) throws IOException //expect absolutePath
	{
		String binaryName = getBinaryName(tokenId);
		HttpGet getRequest = new HttpGet(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + DOWNLOAD_ENDPOINT + tokenId);

		ServoyLog.logInfo("Download request: " + protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + DOWNLOAD_ENDPOINT + tokenId);

		HttpResponse httpResponse = httpClient.execute(getRequest);

		InputStream is = httpResponse.getEntity().getContent();
		byte[] inputFile = new byte[BUFFER_SIZE];
		FileOutputStream fos = new FileOutputStream(savePath + "/" + binaryName);
		int n = is.read(inputFile, 0, BUFFER_SIZE);
		int amount = 0;
		while (n != -1)
		{
			if (n > 0)
			{
				fos.write(inputFile, 0, n);
				amount += n;
			}
			n = is.read(inputFile, 0, BUFFER_SIZE);
		}
		fos.flush();
		is.close();
		fos.close();

		ServoyLog.logInfo("Downloaded bytes: " + amount);
	}

	public void delete(String tokenId) throws IOException
	{
		HttpGet getRequest = new HttpGet(protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + DELETE_ENDPOINT + tokenId);
		ServoyLog.logInfo("Delete request: " + protocol + serverAddress.getHostAddress() + ":" + Integer.toString(serverPort) + DELETE_ENDPOINT + tokenId);
		HttpResponse httpResponse = httpClient.execute(getRequest);
		BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
		String output;
		StringBuffer sb = new StringBuffer();
		while ((output = br.readLine()) != null)
			sb.append(output);

		JSONObject jsonObj = new JSONObject(sb.toString());
		if (!DELETED.equals(jsonObj.get("status")))
		{
			String errorMessage = "Service error: delete failed for token " + tokenId;
			IOException exception = new IOException(errorMessage);
			ServoyLog.logError(errorMessage, exception);
			throw exception;
		}
	}
}
