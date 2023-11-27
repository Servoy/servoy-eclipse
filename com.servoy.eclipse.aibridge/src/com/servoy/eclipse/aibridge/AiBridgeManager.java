package com.servoy.eclipse.aibridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.javascript.parser.JavascriptParserPreferences;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.aibridge.dto.Response;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Utils;

public class AiBridgeManager
{

	private static final String ENDPOINT = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/llm/AIBridge";
	private static final int MAX_SIM_REQUESTS = 10;

	private final static ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIM_REQUESTS);

	private final Map<UUID, Completion> requestMap = new ConcurrentHashMap<>();
	private final ObjectMapper mapper = new ObjectMapper();
	private final String aiBridgePath = Platform.getLocation().toOSString() + File.separator + ".metadata" + File.separator + ".plugins" + File.separator +
		"com.servoy.eclipse.aibridge" + java.io.File.separator;

	private static AiBridgeManager instance = null;

	private AiBridgeManager()
	{
		super();
	}

	public static AiBridgeManager getInstance()
	{
		if (instance == null)
		{
			instance = new AiBridgeManager();
		}
		return instance;
	}

	public Map<UUID, Completion> getRequestMap()
	{
		return requestMap;
	}

	public void sendRequest(
		String cmdName, String inputData,
		String source, int offset, int length, String context)
	{
		final String loginToken = logIn();
		if (!Utils.stringIsEmpty(loginToken))
		{
			CompletableFuture.runAsync(() -> {
				UUID uuid = UUID.randomUUID();
				Completion completion = new Completion(uuid, cmdName, ENDPOINT, inputData, context, source, offset, length);
				try
				{
					requestMap.put(uuid, completion);
					AiBridgeView.refresh();
					completion = sendHttpRequest(loginToken, completion);

				}
				catch (Exception e)
				{
					completion.setMessage(e.getMessage());
					completion.setStatus(AiBridgeStatus.ERROR);
					ServoyLog.logError(e);
				}
				finally
				{
					saveCompletion(AiBridgeView.getSolutionName(), uuid, completion);
					AiBridgeView.refresh();
				}
			}, executorService);
		}
	}

	public void sendCompletion(Completion completion)
	{
		final String loginToken = logIn();

		if (!Utils.stringIsEmpty(loginToken))
		{
			CompletableFuture.runAsync(() -> {
				//load full context and selection
				Completion myCompletion = completion.getFullCompletion();
				try
				{
					deleteFile(AiBridgeView.getSolutionName(), myCompletion.getId());
					requestMap.put(myCompletion.getId(), myCompletion.fullReset());
					myCompletion.setStatus(AiBridgeStatus.SUBMITTED);
					myCompletion.setResponse(new Response());
					myCompletion.setMessage("Processing...");
					AiBridgeView.refresh();
					myCompletion = sendHttpRequest(loginToken, myCompletion);
				}
				catch (Exception e)
				{
					myCompletion.setMessage(e.getMessage());
					myCompletion.setStatus(AiBridgeStatus.ERROR);
					ServoyLog.logError(e);
				}
				finally
				{
					saveCompletion(AiBridgeView.getSolutionName(), myCompletion.getId(), myCompletion);
					AiBridgeView.refresh();

				}
			}, executorService);
		}
	}

	private String logIn()
	{

		String loginToken = ServoyLoginDialog.getLoginToken();
		if (loginToken == null) loginToken = new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
		return loginToken;
	}

	private StringEntity createEntity(Completion request) throws UnsupportedEncodingException
	{
		//this method need to be in sync with the json object expected by the endpoint
		JSONObject jsonObj = new JSONObject();
		//jsonObj.put("loginToken", logIn());
		jsonObj.put("requestType", getRequestType(request.getCmdName()));
		jsonObj.put("selection", request.getSelection());
		jsonObj.put("context", request.getContext());
		jsonObj.put("selectionTokensCount", request.getSelectionTokensCount());
		jsonObj.put("contextTokensCount", request.getContextTokensCount());
		jsonObj.put("servoyVersion", ClientVersion.getBundleVersion());
		jsonObj.put("useEcmaScriptParser", new JavascriptParserPreferences().useES6Parser());


		return new StringEntity(jsonObj.toString(), ContentType.APPLICATION_JSON);
	}

	private String getRequestType(String cmdName)
	{
		//TODO: need to rewrite in a more comprehensive manner (maybe use returnTypeId from the command?)
		//implement this to be able to quickly work out the cloud part
		//for now it can stay as is but in future releases this may change depending on the use cases
		return switch (cmdName)
		{
			case "Explain selection" -> "explain";
			case "Add inline comments" -> "comment";
			case "Debug" -> "debug";
			default -> "";
		};
	}

	private Completion sendHttpRequest(String loginToken, Completion request)
	{
		HttpClientBuilder httpBuilder = HttpClientBuilder.create();
		try (CloseableHttpClient httpClient = httpBuilder.build())
		{
			HttpPost postRequest = new HttpPost(request.getEndpoint());
			StringEntity entity = createEntity(request);
			postRequest.setEntity(entity);
			postRequest.setHeader("token", loginToken);

			CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
			request.setHttpCode(httpResponse.getCode());

			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
			StringBuilder sbResult = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null)
			{
				sbResult.append(output);
			}

			//avoid processing the responseMessage (this may create a bottleneck);
			String jsonString = sbResult.toString();

			int startKeyLength = "\"responseMessage\"".length();
			int startIndex = jsonString.indexOf("\"responseMessage\"") + startKeyLength;
			int endIndex = jsonString.indexOf("\"responseFunction\"");
			String responseMessage = "";
			if (startIndex >= startKeyLength && endIndex > startIndex)
			{
				responseMessage = jsonString.substring(startIndex, endIndex).trim();

				// Remove leading colon, spaces and trailing comma if present
				responseMessage = responseMessage.replaceAll("^[\\s:]+|[\\s,]+$", "");
				if (responseMessage.startsWith("\"") && responseMessage.endsWith("\""))
				{
					responseMessage.substring(1, responseMessage.length() - 1); //cut off the beginning and ending double quotes (if any)
				}
				jsonString = jsonString.substring(0, startIndex) + ":\"\"," + jsonString.substring(endIndex);
				responseMessage = responseMessage.equals("null") ? "" : responseMessage;
			}

			JSONObject jsonObj = new JSONObject(jsonString);
			Response response = new Response(jsonObj);
			request.setResponse(response);
			request.setMessage(responseMessage);
			request.setEndTime(Calendar.getInstance().getTime());

			request.setStatus(AiBridgeStatus.ERROR);
			String errorMessage = switch (httpResponse.getCode())
			{
				case 200 ->
				{
					request.setStatus(AiBridgeStatus.COMPLETE);
					yield null; // No error message for success
				}
				case 500 -> "Service is temporary down ...!";
				case 403 -> "Unrecognized sender ...!";
				case 401 -> "Invalid credentials ...!";
				default -> "Unexpected error: " + httpResponse.getCode();
			};

			if (errorMessage != null)
			{
				request.setMessage(errorMessage);
			}

		}
		catch (RuntimeException | IOException e)
		{
			request.setStatus(AiBridgeStatus.ERROR);
			request.setMessage(e.getMessage());
		}
		return request;
	}

	private Completion saveCompletion(String solutionName, UUID uuid, Completion completion)
	{
		Path directoryPath = Paths.get(aiBridgePath + File.separator + solutionName);
		try
		{
			if (Files.notExists(directoryPath))
			{
				Files.createDirectories(directoryPath);
			}
			Path filePath = directoryPath.resolve(uuid.toString() + ".json");
			Files.deleteIfExists(filePath);
			String jsonStr = mapper.writeValueAsString(completion);
			Files.write(filePath, jsonStr.getBytes());
			return completion.partialReset();

		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
		return completion;
	}

	/*
	 * Save persistent data.
	 */
	public void saveData(String solutionName)
	{
		Path directoryPath = Paths.get(aiBridgePath + File.separator + solutionName);
		try
		{
			if (Files.notExists(directoryPath))
			{
				Files.createDirectories(directoryPath);
			}
			for (Map.Entry<UUID, Completion> entry : requestMap.entrySet())
			{
				Path filePath = directoryPath.resolve(entry.getKey().toString() + ".json");
				if (!Files.exists(filePath))
				{
					String json = mapper.writeValueAsString(entry.getValue());
					Files.write(filePath, json.getBytes());
				}
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void loadData(String solutionName)
	{
		Path directoryPath = Paths.get(aiBridgePath + File.separator + solutionName);

		try
		{
			requestMap.clear();
			if (Files.exists(directoryPath))
			{
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.json"))
				{
					for (Path entry : stream)
					{
						String json = new String(Files.readAllBytes(entry));
						String fileName = entry.getFileName().toString();
						UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 5)); // Remove ".json"
						Completion completion = mapper.readValue(json, Completion.class);
						completion = completion.partialReset();
						if (AiBridgeStatus.SUBMITTED.equals(completion.getStatus()))
						{
							//messages just submitted and with no response
							completion.setStatus(AiBridgeStatus.INCOMPLETE);
							completion.setMessage("Stopped...");
						}
						requestMap.put(uuid, completion);
					}
				}
			}
		}
		catch (IOException | IllegalArgumentException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void deleteFile(String solutionName, UUID uuid)
	{
		Path filePath = Paths.get(aiBridgePath, solutionName, uuid.toString() + ".json");
		try
		{
			Files.deleteIfExists(filePath);
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}

	public void deleteFiles(String solutionName, List<UUID> uuidList)
	{
		Path directoryPath = Paths.get(aiBridgePath + File.separator + solutionName);

		for (UUID uuid : uuidList)
		{
			try
			{
				Path filePath = directoryPath.resolve(uuid.toString() + ".json");
				Files.deleteIfExists(filePath);
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public Path getCompletionPath(UUID uuid)
	{
		return Paths.get(aiBridgePath, AiBridgeView.getSolutionName(), uuid.toString() + ".json");
	}
}
