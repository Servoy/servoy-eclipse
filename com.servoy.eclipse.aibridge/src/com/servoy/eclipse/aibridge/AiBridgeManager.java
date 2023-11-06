package com.servoy.eclipse.aibridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
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
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.aibridge.dto.Response;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.util.Utils;

public class AiBridgeManager
{

	private static final String ENDPOINT = "https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/llm/AIBridge";
	private static final int MAX_SIM_REQUESTS = 10;

	private final static ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIM_REQUESTS);

	private final Map<UUID, Completion> requestMap = new ConcurrentHashMap<>();
	private final ObjectMapper mapper = new ObjectMapper();
	private final String aiBridgePath = Platform.getLocation().toOSString() + File.separator + ".metadata" + File.separator + ".plugins" + File.separator +
		"com.servoy.eclipse.aibridge" + java.io.File.separator;
	private final Object SAVE_LOCK = new Object();

	private final String devToken = "e3b70a550c39cce86c88951f43005a740993d84f8903568919f83bd97984ab5b98485c0c5e7ce5df64e7f7ec714bf73a3db891542acd27aa69da044f81458249";

	private final boolean debug = false;

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
					System.out.println("Debug: " + debug);
					saveData(AiBridgeView.getSolutionName());
					if (debug)
					{
						completion = sendDebugRequest(loginToken, completion);
					}
					else
					{
						completion = sendHttpRequest(loginToken, completion);
					}

				}
				catch (Exception e)
				{
					completion.setMessage(e.getMessage());
					completion.setStatus(AiBridgeStatus.ERROR);
					ServoyLog.logError(e);
				}
				finally
				{
					saveData(AiBridgeView.getSolutionName());
					AiBridgeView.refresh();
				}
			}, executorService);
		}
	}

	private Completion sendDebugRequest(String loginToken, Completion request) throws IOException, InterruptedException
	{

		final long INITIAL_WAIT_TIME = 120000; // 2 minutes in milliseconds
		final long DELAY_AFTER_CHANGE = 2000; // 2 seconds
		long startTime = System.currentTimeMillis();
		long lastChangeTime = 0;
		//Path tempFile = Paths.get(aiBridgePath, request.getCmdName() + "_" + request.getId().toString() + ".json");
		Path tempFile = Paths.get("/Users/vidmarian/work/tmp", request.getCmdName() + "_" + request.getId().toString() + ".json");

		boolean initialChangeDetected = false;
		try
		{
			// Step 1: Create the JSON payload
			StringEntity entity = createEntity(loginToken, request);
			String jsonPayload;
			jsonPayload = EntityUtils.toString(entity);
			// Step 2: Write the JSON payload to a temporary file

			byte[] payload = jsonPayload.getBytes(StandardCharsets.UTF_8);
			Files.write(tempFile, payload);

			long initialTimestamp = Files.getLastModifiedTime(tempFile).toMillis();

			while (true)
			{
				Thread.sleep(1000); // poll every 1 second
				long newTimestamp = Files.getLastModifiedTime(tempFile).toMillis();

				if (!initialChangeDetected && newTimestamp > initialTimestamp)
				{
					// Initial change detected
					initialChangeDetected = true;
					lastChangeTime = System.currentTimeMillis();
					initialTimestamp = newTimestamp;
					System.out.println("Initial change detected: " + lastChangeTime);
				}
				else if (initialChangeDetected && newTimestamp > initialTimestamp)
				{
					// Subsequent changes after the initial change
					lastChangeTime = System.currentTimeMillis();
					initialTimestamp = newTimestamp;
					System.out.println("File change detected: " + lastChangeTime);
				}
				else if (initialChangeDetected && (System.currentTimeMillis() - lastChangeTime) >= DELAY_AFTER_CHANGE)
				{
					// No changes have been detected for at least 2 seconds after the last modification.
					System.out.println("File is stable: " + lastChangeTime);
					break;
				}

				// If no initial change is detected within the 2-minute window, handle accordingly
				if (!initialChangeDetected && (System.currentTimeMillis() - startTime) >= INITIAL_WAIT_TIME)
				{
					// Timeout occurred, so create an empty response and assign to the request.
					request.setResponse(new Response());
					request.setStatus(AiBridgeStatus.ERROR);
					request.setMessage("Timeout occurred. No response ...");
					System.out.println("Time out occured ...!");
					return request;
				}
			}

			String jsonResponse = new String(Files.readAllBytes(tempFile), StandardCharsets.UTF_8);

			// Remove leading and trailing double quotes and unescape any escaped quotes.
			jsonResponse = jsonResponse.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
			// Check if the jsonResponse is valid. If it's empty or not a valid JSON, create an empty response
			if (jsonResponse.isEmpty() || !isValidJSON(jsonResponse))
			{
				request.setResponse(new Response());
				request.setStatus(AiBridgeStatus.ERROR);
				request.setMessage("Invalid or empty JSON response...");
				return request;
			}

			JSONObject jsonObj = new JSONObject(jsonResponse);
			Response response = new Response(jsonObj);

			request.setResponse(response);
			request.setStatus(AiBridgeStatus.COMPLETE);
			request.setEndTime(Calendar.getInstance().getTime());
			if (response.isEmptyResponse())
			{
				request.setStatus(AiBridgeStatus.ERROR);
				request.setMessage("No response ...");
			}

			System.out.println(request.getResponse().getResponseMessage());

			return request;
		}
		catch (ParseException | IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			//Files.deleteIfExists(tempFile);
		}

		return null;
	}

	private boolean isValidJSON(String test)
	{
		try
		{
			new JSONObject(test);
		}
		catch (JSONException ex)
		{
			return false;
		}
		return true;
	}


	public void sendCompletion(Completion completion)
	{
		final String loginToken = logIn();

		if (!Utils.stringIsEmpty(loginToken))
		{
			CompletableFuture.runAsync(() -> {
				Completion myCompletion = completion;
				try
				{
					myCompletion.reset();
					myCompletion.setStatus(AiBridgeStatus.SUBMITTED);
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
					requestMap.put(myCompletion.getId(), myCompletion);
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

	private StringEntity createEntity(String loginToken, Completion request) throws UnsupportedEncodingException
	{
		//this method need to be in sync with the json object expected by the endpoint
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("loginToken", loginToken);
		jsonObj.put("requestType", getRequestType(request.getCmdName()));
		jsonObj.put("selection", request.getSelection());
		jsonObj.put("context", request.getContext());
		jsonObj.put("selectionTokensCount", request.getSelectionTokensCount());
		jsonObj.put("contextTokensCount", request.getContextTokensCount());

		return new StringEntity(jsonObj.toString(), ContentType.APPLICATION_JSON);
	}

	private String getRequestType(String cmdName)
	{
		//TODO: need to rewrite in a more comprehensive manner (maybe use returnTypeId from the command?)
		//implement this quickly for Rene to be able to work out the cloud part
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
			StringEntity entity = createEntity(loginToken, request);
			postRequest.setEntity(entity);
			postRequest.setHeader("token", devToken);

			CloseableHttpResponse httpResponse = httpClient.execute(postRequest);
			request.setHttpCode(httpResponse.getCode());

			if (httpResponse.getCode() != 200)
			{
				throw new RuntimeException("Failed : HTTP error code : " + httpResponse.getCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
			StringBuilder sbResult = new StringBuilder();
			String output;
			while ((output = br.readLine()) != null)
			{
				sbResult.append(output);
			}

			System.out.println(sbResult.toString());
			JSONObject jsonObj = new JSONObject(sbResult.toString());
			Response response = new Response(jsonObj);
			request.setResponse(response);
			request.setStatus(AiBridgeStatus.COMPLETE);
			request.setEndTime(Calendar.getInstance().getTime());
			if (response.isEmptyResponse())
			{
				//some error appeared in the cloud
				request.setStatus(AiBridgeStatus.ERROR);
				request.setMessage("No response ...");
			}


		}
		catch (RuntimeException | IOException e)
		{
			request.setStatus(AiBridgeStatus.ERROR);
			request.setMessage(e.getMessage());
		}
		return request;
	}

	public void saveData(String solutionName)
	{
		synchronized (SAVE_LOCK)
		{
			try
			{
				if (Files.notExists(Paths.get(aiBridgePath)))
				{
					Files.createDirectories(Paths.get(aiBridgePath));
				}

				String json = mapper.writeValueAsString(getRequestMap());
				Files.write(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json"), json.getBytes());
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public void loadData(String solutionName)
	{
		try
		{
			requestMap.clear();

			if (Files.exists(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json")))
			{
				String json = new String(Files.readAllBytes(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json")));

				Map<UUID, Completion> tempMap = mapper.readValue(json, new TypeReference<Map<UUID, Completion>>()
				{
				});

				for (Map.Entry<UUID, Completion> entry : tempMap.entrySet())
				{
					Completion completion = entry.getValue();
					if (AiBridgeStatus.SUBMITTED.equals(completion.getStatus()))
					{
						completion.setStatus(AiBridgeStatus.INCOMPLETE);
					}
				}
				requestMap.putAll(tempMap);
			}
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}

	}

}
