package com.servoy.build.documentation.ai;

//import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.embedding.EmbeddingRequest;
import com.theokanning.openai.embedding.EmbeddingResult;
import com.theokanning.openai.service.OpenAiService;

import io.pinecone.PineconeClient;
import io.pinecone.PineconeClientConfig;
import io.pinecone.PineconeConnection;
import io.pinecone.PineconeConnectionConfig;
import io.pinecone.PineconeException;
import io.pinecone.proto.DeleteRequest;
import io.pinecone.proto.DeleteResponse;
import io.pinecone.proto.QueryRequest;
import io.pinecone.proto.QueryResponse;
import io.pinecone.proto.QueryVector;
import io.pinecone.proto.UpsertRequest;
import io.pinecone.proto.UpsertRequest.Builder;
import io.pinecone.proto.UpsertResponse;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.asynchttpclient.*;
import io.pinecone.proto.Vector;

public class AIClient
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AIClient.class.getName());
	private final String model;
	private final String embeddingModel;
	private final OpenAiService service;
//	private AsyncHttpClient client;

	private static String openai_key = System.getProperty("openai.apikey", "example-api-key");

	// if the link you see on pinecone.io site for your index is:
	// https://test-787abcd.svc.asia-southeast1-gcp-free.pinecone.io
	// then
	//     pinecone_environment = asia-southeast1-gcp-free
	//     pinecone_project_name = 787abcd
	// and the pinecone_index would be "test"
	private static String pinecone_key = System.getProperty("pinecone.apikey", "example-api-key");
	private static String pinecone_environment = System.getProperty("pinecone.environment", "example-environment");
	private static String pinecone_project_name = System.getProperty("pinecone.projectName", "example-project-name");

	private final String pinecone_index;

	public AIClient(String indexName, String model, String embeddingModel)
	{
		this.model = model;
		this.embeddingModel = embeddingModel;
		this.service = new OpenAiService(openai_key);
		this.pinecone_index = indexName;
//		this.client = Dsl.asyncHttpClient();
	}

	public void populateIndex(List<PineconeItem> data, List<String> metadataKeys, String namespace)
	{
		clearIndex(namespace);

		if (metadataKeys == null)
		{
			metadataKeys = new ArrayList<>();
		}

		for (PineconeItem item : data)
		{
//			if (item.getEmbeddings() == null) item.setEmbeddings(createEmbedding(item.getText()));
			// else embedding is already know for this item

			Map<String, String> metadata = new HashMap<>();
			for (String key : metadataKeys)
			{
				metadata.put(key, item.getMetadata().get(key));
			}
			item.setMetadata(metadata);
		}
		try
		{
//			upsert(data, namespace);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to populate index", e);
		}
	}

	public String answer(String question, List<PineconeItem> data, String namespace)
	{
		try
		{
			String extraContext = getSimilarContext(question, data, 3, namespace);
			return complete(question, extraContext);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to get similar context", e);
		}
		return "Couldn't answer.";
	}

	private void clearIndex(String namespace)
	{
		PineconeClientConfig configuration = new PineconeClientConfig()
			.withApiKey(pinecone_key)
			.withEnvironment(pinecone_environment)
			.withProjectName(pinecone_project_name);

		PineconeClient pineconeClient = new PineconeClient(configuration);

		PineconeConnectionConfig connectionConfig = new PineconeConnectionConfig().withIndexName(pinecone_index);

		try (PineconeConnection connection = pineconeClient.connect(connectionConfig))
		{
			io.pinecone.proto.DeleteRequest.Builder deleteRequest = DeleteRequest.newBuilder();
			deleteRequest.setDeleteAll(true);
			deleteRequest.setNamespace(namespace);

			LOGGER.info("Sending pinecone clear/delete all from index request...");
			LOGGER.debug(String.valueOf(deleteRequest));

			DeleteResponse deleteResponse = connection.getBlockingStub().delete(deleteRequest.build());

			LOGGER.info("Got pinecone clear response...");
			LOGGER.debug(String.valueOf(deleteResponse));
		}
		catch (PineconeException e)
		{
			LOGGER.error("Failed to complete pinecone upsert", e);
		}
	}

	private void upsert(List<PineconeItem> data, String namespace) throws Exception
	{
//		List<Map<String, Object>> vectors = new ArrayList<>();
//
//		for (PineconeItem item : data) {
//			Map<String, Object> vectorData = new HashMap<>();
//			vectorData.put("id", item.getId());
//			vectorData.put("values", item.getEmbeddings());
//			vectors.add(vectorData);
//		}
//
//		Map<String, Object> requestBody = new HashMap<>();
//		requestBody.put("vectors", vectors);
//		requestBody.put("namespace", namespace);

		PineconeClientConfig configuration = new PineconeClientConfig()
			.withApiKey(pinecone_key)
			.withEnvironment(pinecone_environment)
			.withProjectName(pinecone_project_name);

		PineconeClient pineconeClient = new PineconeClient(configuration);

		PineconeConnectionConfig connectionConfig = new PineconeConnectionConfig().withIndexName(pinecone_index);

		try (PineconeConnection connection = pineconeClient.connect(connectionConfig))
		{
			Builder upsertRequest = UpsertRequest.newBuilder();
			for (PineconeItem item : data)
			{
				upsertRequest.addVectors(Vector.newBuilder()
					.setId(String.valueOf(item.getId()))
					.addAllValues(item.getEmbeddings())
					.build());
			}

			upsertRequest.setNamespace(namespace);

			LOGGER.info("Sending pinecone upsert request...");
			LOGGER.debug(String.valueOf(upsertRequest));

			UpsertResponse upsertResponse = connection.getBlockingStub().upsert(upsertRequest.build());

			LOGGER.info("Got pinecone upsert response...");
			LOGGER.debug(String.valueOf(upsertResponse));
		}
		catch (PineconeException e)
		{
			LOGGER.error("Failed to complete pinecone upsert", e);
		}

//		String requestJson = new ObjectMapper().writeValueAsString(requestBody);

//		String url = String.format("https://%s-%s.svc.%s.pinecone.io/vectors/upsert", pinecone_index,
//				pinecone_project_name, pinecone_environment);
//
//		client.preparePost(url)
//			.addHeader("Accept", "application/json")
//			.addHeader("Content-Type", "application/json")
//			.addHeader("Api-Key", pinecone_key)
//			.setBody(requestJson)
//			.execute().toCompletableFuture()
//			.thenAccept(System.out::println).join();
	}

	public String getSimilarContext(String question, List<PineconeItem> data, int topK, String namespace) throws Exception
	{
		List<Float> questionEmbedding = createEmbedding(question);
//
//		Map<String, Object> requestBody = new HashMap<>();
//		requestBody.put("vector", questionEmbedding);
//		requestBody.put("topK", topK);
//
//		String requestJson = new ObjectMapper().writeValueAsString(requestBody);
//
//		String url = String.format("https://%s-%s.svc.%s.pinecone.io/query", pinecone_index, pinecone_project_name,
//				pinecone_environment);
//
//		List<String> ids = new ArrayList<>();
//		String responseBody = client.preparePost(url)
//				.addHeader("Accept", "application/json")
//				.addHeader("Content-Type", "application/json")
//				.addHeader("Api-Key", pinecone_key)
//				.setBody(requestJson)
//				.execute().toCompletableFuture()
//				.get().getResponseBody();
//
//		// Parse the response
//		JsonNode responseJson = new ObjectMapper().readTree(responseBody);
//		if (responseJson.has("results")) {
//			for (JsonNode result : responseJson.get("results")) {
//				if (result.has("matches")) {
//					for (JsonNode match : result.get("matches")) {
//						if (match.has("id")) {
//							ids.add(match.get("id").asText());
//						}
//					}
//				}
//			}
//		}

		PineconeClientConfig configuration = new PineconeClientConfig()
			.withApiKey(pinecone_key)
			.withEnvironment(pinecone_environment)
			.withProjectName(pinecone_project_name);

		PineconeClient pineconeClient = new PineconeClient(configuration);
		PineconeConnectionConfig connectionConfig = new PineconeConnectionConfig().withIndexName(pinecone_index);

		List<String> similarIds = new ArrayList<>();

		try (PineconeConnection connection = pineconeClient.connect(connectionConfig))
		{
			QueryVector queryVector = QueryVector
				.newBuilder()
				.addAllValues(questionEmbedding)
				.setTopK(topK)
				.setNamespace(namespace)
				.build();

			QueryRequest queryRequest = QueryRequest
				.newBuilder()
				.addQueries(queryVector)
				.setTopK(topK)
				.build();

			LOGGER.info("Sending pinecone query request...");
			LOGGER.debug(String.valueOf(queryRequest));

			QueryResponse queryResponse = connection.getBlockingStub().query(queryRequest);

			LOGGER.info("Got pinecone query response...");
			LOGGER.debug(String.valueOf(queryResponse));

			if (queryResponse.getResultsCount() > 0)
			{
				queryResponse.getResultsList().forEach(result -> result.getMatchesList().forEach(match -> {
					similarIds.add(match.getId());
				}));
			}
			else
			{
				LOGGER.warn("Pinecone found 0 similar results!");
			}
		}
		catch (PineconeException e)
		{
			LOGGER.error("Error finding pinecone similarities: ", e);
		}
		List<String> context = new ArrayList<>();
		for (String id : similarIds)
		{
			for (PineconeItem item : data)
			{
				if (item.getId() == Integer.parseInt(id))
				{
					context.add(item.getText());
				}
			}
		}

		if (context.isEmpty())
		{
			return null;
		}

		return String.join("\n\n", context);
	}

	public String complete(String prompt, String extraContext)
	{
		try
		{
			String content = prompt + (extraContext != null ? ("\n\n === extra_context ===\n\n" + extraContext + "\n\n") : "");
			List<ChatMessage> messages = Arrays.asList(
				new ChatMessage("system", "You are chatting with an AI trained by OpenAI."),
				new ChatMessage("user", content));

			// Define the request
			ChatCompletionRequest request = ChatCompletionRequest.builder().model(this.model).messages(messages).n(1)
				.maxTokens(3000).logitBias(new HashMap<>()).build();

			LOGGER.info("Sending AI complete request with the following prompt:\n" + messages);
			LOGGER.debug(String.valueOf(request));

			// Define the response type
			ChatCompletionResult response = this.service.createChatCompletion(request);

			LOGGER.info("Received AI complete response...");
			LOGGER.debug(String.valueOf(response));

			if (response != null && response.getChoices() != null && !response.getChoices().isEmpty())
			{
				return response.getChoices().get(0).getMessage().getContent();
			}
			else
			{
				return null;
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to complete text", e);
			return null;
		}
	}

	private List<Float> createEmbedding(String text)
	{
		// OpenAI's text-embedding-ada-002 generates vectors with 1536 output dimensions; so existing pinecone index should use that!
		try
		{

			List<String> inputTexts = Collections.singletonList(text);

			EmbeddingRequest request = EmbeddingRequest.builder().model(embeddingModel).input(inputTexts)
				.user("default") // replace with the actual user identifier
				.build();

			LOGGER.info("Asking ada AI to create embedding...");
			LOGGER.debug(String.valueOf(request));

			EmbeddingResult response = this.service.createEmbeddings(request);

			LOGGER.info("Embedding creation response received...");
			LOGGER.debug(String.valueOf(response));

			if (response != null && response.getData() != null && !response.getData().isEmpty())
			{
				List<Double> embeddings = response.getData().get(0).getEmbedding();
				List<Float> float_embeddings = new ArrayList<>();
				for (Double d : embeddings)
				{
					float_embeddings.add(d.floatValue());
				}
				return float_embeddings;
			}
			else
			{
				return null;
			}
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to create embedding", e);
			return null;
		}
	}

//	public void closeHttpClient() {
//		try {
//			client.close();
//		} catch (IOException e) {
//			LOGGER.log(Level.SEVERE, "Failed to close HTTP Client", e);
//		}
//	}

}