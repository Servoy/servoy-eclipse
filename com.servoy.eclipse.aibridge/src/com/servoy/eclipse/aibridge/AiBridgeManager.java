package com.servoy.eclipse.aibridge;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodySubscribers;
import java.nio.charset.StandardCharsets;
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

import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.javascript.parser.JavascriptParserPreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
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

	private static final URI ENDPOINT = URI.create(System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/llm/AIBridge");
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
		ServoyLoginDialog.getLoginToken(loginToken -> {
			if (!Utils.stringIsEmpty(loginToken))
			{
				CompletableFuture.runAsync(() -> {
					UUID uuid = UUID.randomUUID();
					Completion completion = new Completion(uuid, cmdName, inputData, context, source, offset, length);
					try
					{
						requestMap.put(uuid, completion);
						AiBridgeView.setSelectionId(uuid);
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
						AiBridgeView.setSelectionId(uuid);
						AiBridgeView.refresh();
					}
				}, executorService);
			}
			else showConnectMessage();
		});
	}

	public void sendCompletion(Completion completion)
	{
		ServoyLoginDialog.getLoginToken(loginToken -> {
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
						AiBridgeView.setSelectionId(myCompletion.getId());
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
						AiBridgeView.setSelectionId(myCompletion.getId());
						AiBridgeView.refresh();

					}
				}, executorService);
			}
			else showConnectMessage();
		});
	}

	private void showConnectMessage()
	{

		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openInformation(
					Display.getDefault().getActiveShell(),
					"Login Required",
					"You need to log in to Servoy Cloud if you want to use Servoy AI.");
			}
		});
	}

	private String createEntity(Completion request)
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


		return jsonObj.toString();
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

	private Completion sendHttpRequest(String loginToken, final Completion request)
	{
		try (HttpClient httpClient = HttpClient.newHttpClient())
		{
			HttpRequest post = HttpRequest.newBuilder(ENDPOINT)
				.setHeader("token", loginToken)
				.setHeader("Content-Type", "application/json")
				.POST(BodyPublishers.ofString(createEntity(request)))
				.build();

			return httpClient.send(post, responseInfo -> BodySubscribers.mapping(
				BodySubscribers.ofString(StandardCharsets.UTF_8), // Upstream subscriber gives us a String
				(String jsonString) -> { // This function converts the String to JSONObject
					request.setHttpCode(responseInfo.statusCode());

					String errorMessage = switch (responseInfo.statusCode())
					{
						case 200 -> {
							request.setStatus(AiBridgeStatus.COMPLETE);
							yield null; // No error message for success
						}
						case 500 -> "Service is temporary down ...!";
						case 403 -> "Unrecognized sender ...!";
						case 401 -> "Invalid credentials ...!";
						default -> "Unexpected error: " + responseInfo.statusCode();
					};
					if (errorMessage != null)
					{
						request.setStatus(AiBridgeStatus.ERROR);
						request.setMessage(errorMessage);
						return request;
					}

					JSONObject jsonObj = new JSONObject(jsonString);
					Response response = new Response(jsonObj);
					request.setResponse(response);
					request.setEndTime(Calendar.getInstance().getTime());
					return request;
				})).body();
		}
		catch (RuntimeException | IOException | InterruptedException e)
		{
			e.printStackTrace();
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
				if (!Files.exists(filePath)) //save only new / re-submitted data
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
		//this is called at developer's startup; for now we avoid persistence beteen restarts

		Path directoryPath = Paths.get(aiBridgePath + File.separator + solutionName);

		try
		{
			requestMap.clear();
			if (Files.exists(directoryPath) && Files.isDirectory(directoryPath))
			{
//				try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath, "*.json"))
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(directoryPath))
				{
					for (Path entry : stream)
					{
						if (Files.isRegularFile(entry))
						{
							Files.delete(entry);
						}
//						String json = new String(Files.readAllBytes(entry));
//						String fileName = entry.getFileName().toString();
//						UUID uuid = UUID.fromString(fileName.substring(0, fileName.length() - 5)); // Remove ".json"
//						Completion completion = mapper.readValue(json, Completion.class);
//						completion = completion.partialReset();
//						if (AiBridgeStatus.SUBMITTED.equals(completion.getStatus()))
//						{
//							//messages just submitted and with no response
//							completion.setStatus(AiBridgeStatus.INCOMPLETE);
//							completion.setMessage("Stopped...");
//						}
//						requestMap.put(uuid, completion);
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
