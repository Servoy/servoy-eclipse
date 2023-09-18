package com.servoy.eclipse.aibridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
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
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.util.Utils;

public class AiBridgeManager {
	
	//TODO: put this options in Servoy preferences (also with organization key
	private static final int MAX_SIM_REQUESTS = 10;
	
    private final static ExecutorService executorService = Executors.newFixedThreadPool(MAX_SIM_REQUESTS);
    
    private static final Map<UUID, Completion> requestMap = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static String aiBridgePath = Platform.getLocation().toOSString() + File.separator + ".metadata" + File.separator + ".plugins" + File.separator + "com.servoy.eclipse.aibridge" + java.io.File.separator;
    private static final Object SAVE_LOCK = new Object();
    
    public static Map<UUID, Completion> getRequestMap() {
    	return requestMap;
    }
    
    public static void sendRequest(String cmdName, String endpoint, String inputData, String source, int offset, int length, String context) {   
    	final String loginToken = logIn();
    	if (!Utils.stringIsEmpty(loginToken)) {
    		CompletableFuture.runAsync(() -> { 
            	UUID uuid = UUID.randomUUID();
            	Completion completion = new Completion(uuid, cmdName, endpoint, inputData, context, source, offset, length);
                try {
                    completion.setStatus(AiBridgeStatus.SUBMITTED);
                    requestMap.put(uuid, completion);
                    AiBridgeView.refresh();
                    	
    	            completion = sendHttpRequest(loginToken, completion);
    	                                    
                } catch (Exception e) {
                	e.printStackTrace();
                	completion.setMessage(e.getMessage());
                	completion.setStatus(AiBridgeStatus.ERROR);
                } finally {
                	AiBridgeView.refresh();
                	saveData(AiBridgeView.getSolutionName());
                }
            }, executorService);
    	}
    }
    
    public static void sendCompletion(Completion completion) {   
    	final String loginToken = logIn();
    	
    	if (!Utils.stringIsEmpty(loginToken)) {
    		CompletableFuture.runAsync(() -> { 
    			Completion myCompletion = completion;
                try {
                	myCompletion.reset();
                	myCompletion.setStatus(AiBridgeStatus.SUBMITTED);
                    AiBridgeView.refresh();
                    myCompletion = sendHttpRequest(loginToken, myCompletion);                  
                } catch (Exception e) {
                	e.printStackTrace();
                	myCompletion.setMessage(e.getMessage());
                	myCompletion.setStatus(AiBridgeStatus.ERROR);
                } finally {
                	requestMap.put(myCompletion.getId(), myCompletion);
                	AiBridgeManager.saveData(AiBridgeView.getSolutionName());
                	AiBridgeView.refresh();
                }
            }, executorService);
    	}
    }
    
    private static String logIn()
	{

    	String loginToken = ServoyLoginDialog.getLoginToken();
    	if (loginToken == null) loginToken = new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
    	System.out.println(loginToken);
		return loginToken;
//		return "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJkaXNwbGF5LW5hbWUiOiJWaWQgTWFyaWFuIiwiY29tcGFueSI6IlMuQy4gU2Vydm95IFNybCIsInFhcGFhc19uYW1lc3BhY2UiOiJjcm0kJCIsImV4cCI6MTY5NTYzNDUxMSwiY29udGFjdC1pZCI6IjEwODY0MyIsImZpcnN0LW5hbWUiOiJWaWQiLCJlbWFpbCI6Im12aWRAc2Vydm95LmNvbSIsInVzZXJuYW1lIjoibXZpZEBzZXJ2b3kuY29tIiwibGFzdC1uYW1lIjoiTWFyaWFuIn0.9TiRvRJnnfPCuB59YfBK9N82mvHHSnyu2UdIgjlphlM";
	}
    
    private static StringEntity createEntity(String loginToken, String queryData, String queryContext) throws UnsupportedEncodingException {
    	//this method need to be rewritten according to the JSON expected by the endpoint in cloud
        JSONObject jsonObj = new JSONObject();
	    jsonObj.put("loginToken", loginToken);
	    jsonObj.put("question", queryData);
	    return new StringEntity(jsonObj.toString(), ContentType.APPLICATION_JSON);
    }
    
    private static Completion sendHttpRequest(String loginToken, Completion request) throws IOException {
        CloseableHttpResponse httpResponse = null;
        try {
            HttpClientBuilder httpBuilder = HttpClientBuilder.create();
            CloseableHttpClient httpClient = httpBuilder.build();
            
            HttpPost postRequest = new HttpPost(request.getEndpoint());
            StringEntity entity = createEntity(loginToken, request.getSelection(), request.getContext());
            postRequest.setEntity(entity);
            
            httpResponse = httpClient.execute(postRequest);
            request.setHttpCode(httpResponse.getCode());
            
            if (httpResponse.getCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + httpResponse.getCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));
            StringBuilder sbResult = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null) {
                sbResult.append(output);
            }
            
            JSONObject jsonObj = new JSONObject(sbResult.toString());
            Response response = new Response(jsonObj);
            request.setResponse(response);
            request.setStatus(AiBridgeStatus.COMPLETE);
            request.setEndTime(Calendar.getInstance().getTime());
            if (response.isEmptyResponse()) {
            	//something error appeared in the cloud
            	request.setStatus(AiBridgeStatus.ERROR);
            	request.setMessage("No response ...");
            }
            
            
        } catch (RuntimeException | IOException e) {
        	request.setStatus(AiBridgeStatus.ERROR);
        	request.setMessage(e.getMessage());
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
        return request;
    }
    
    public static void saveData(String solutionName) {
        synchronized (SAVE_LOCK) {
            try {
            	if (Files.notExists(Paths.get(aiBridgePath))) {
            		System.out.println("Creating path: " + aiBridgePath);
            		Files.createDirectories(Paths.get(aiBridgePath));
            	}
            	
            	System.out.println("Save data to: " + aiBridgePath + File.separator + solutionName + "-completions.json");
            	
                String json = mapper.writeValueAsString(AiBridgeManager.getRequestMap());
                Files.write(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json"), json.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void loadData(String solutionName) {
    	try {
    		requestMap.clear();
    		
    		System.out.println("Load data from: " + aiBridgePath + File.separator + solutionName + "-completions.json");
    		
			if (Files.exists(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json"))) {
				String json = new String(Files.readAllBytes(Paths.get(aiBridgePath + File.separator + solutionName + "-completions.json")));
				
		        Map<UUID, Completion> tempMap = mapper.readValue(json, new TypeReference<Map<UUID, Completion>>() {});
		        
		        for (Map.Entry<UUID, Completion> entry : tempMap.entrySet()) {
	                Completion completion = entry.getValue();
	                if (AiBridgeStatus.SUBMITTED.equals(completion.getStatus())) {
	                    completion.setStatus(AiBridgeStatus.INCOMPLETE);
	                }
	            }
		        requestMap.putAll(tempMap);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }

}
