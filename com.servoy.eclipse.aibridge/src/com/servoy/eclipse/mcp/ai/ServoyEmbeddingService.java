package com.servoy.eclipse.mcp.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.extensions.OrtxPackage;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * Main service for managing embeddings and semantic search for Servoy context.
 * Uses local ONNX-based embedding model (BGE-small-en-v1.5) with ONNX tokenizer.
 */
public class ServoyEmbeddingService
{

	private static ServoyEmbeddingService instance;

	private OrtEnvironment env;
	private OrtSession modelSession;
	private OrtSession tokenizerSession;
	private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
	private int embeddingCount = 0;

	public ServoyEmbeddingService()
	{
		this.embeddingStore = new InMemoryEmbeddingStore<>();
	}

	/**
	 * Initialize the service.
	 * Called once at plugin startup by Activator to pre-load models and knowledge base.
	 */
	private void initialize()
	{
		System.out.println("[ServoyEmbeddings] Initializing ONNX embedding service with ONNX tokenizer...");

		try
		{
			// Initialize ONNX Runtime
			env = OrtEnvironment.getEnvironment();
			System.out.println("[ServoyEmbeddings] ONNX Runtime initialized");

			// Load embedding model from OSGi bundle
			System.out.println("[ServoyEmbeddings] Loading ONNX embedding model from bundle...");
			Bundle modelsBundle = Platform.getBundle("onnx-models-bge-small-en");
			if (modelsBundle == null)
			{
				throw new RuntimeException("Models bundle not found: onnx-models-bge-small-en");
			}

			URL modelURL = modelsBundle.getEntry("models/bge-small-en-v1.5/model.onnx");
			if (modelURL == null)
			{
				throw new RuntimeException("Model file not found in bundle");
			}
			InputStream modelStream = modelURL.openStream();
			byte[] modelBytes = modelStream.readAllBytes();
			modelStream.close();
			System.out.println("[ServoyEmbeddings] Model loaded (" + (modelBytes.length / 1024 / 1024) + " MB), creating session...");
			modelSession = env.createSession(modelBytes);
			System.out.println("[ServoyEmbeddings] ONNX model session created");

			// Load ONNX tokenizer from OSGi bundle
			System.out.println("[ServoyEmbeddings] Loading ONNX tokenizer from bundle...");
			URL tokenizerURL = modelsBundle.getEntry("models/bge-small-en-v1.5/tokenizer.onnx");
			if (tokenizerURL == null)
			{
				throw new RuntimeException("Tokenizer file not found in bundle");
			}
			InputStream tokenizerStream = tokenizerURL.openStream();
			byte[] tokenizerBytes = tokenizerStream.readAllBytes();
			tokenizerStream.close();
			System.out.println("[ServoyEmbeddings] Tokenizer loaded (" + (tokenizerBytes.length / 1024) + " KB), creating session...");

			// Register ONNX Runtime Extensions for custom operators (BertTokenizer)
			OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
			sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
			System.out.println("[ServoyEmbeddings] ONNX Runtime Extensions registered");

			// Create tokenizer session with ONNX Runtime Extensions
			tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
			System.out.println("[ServoyEmbeddings] ONNX tokenizer session created");

			System.out.println("[ServoyEmbeddings] ONNX model and tokenizer loaded successfully");

			// Pre-load Servoy knowledge base (happens at startup via Activator)
			initializeKnowledgeBase();

			System.out.println("[ServoyEmbeddings] Embedding service ready!");
		}
		catch (Exception e)
		{
			System.err.println("[ServoyEmbeddings] Failed to initialize: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException("Failed to initialize embedding service", e);
		}
	}

	/**
	 * Get singleton instance.
	 * Note: Instance is created and initialized at plugin startup by Activator,
	 * so this will return the already-initialized instance with pre-loaded knowledge base.
	 */
	public static synchronized ServoyEmbeddingService getInstance()
	{
		if (instance == null)
		{
			instance = new ServoyEmbeddingService();
			instance.initialize();
		}
		return instance;
	}

	/**
	 * Pre-load Servoy-specific knowledge into the embedding store.
	 * Reads embeddings.list to find all .txt files in /embeddings/ directory.
	 */
	private void initializeKnowledgeBase()
	{
		System.out.println("[ServoyEmbeddings] Loading knowledge base from /embeddings/ directory...");

		try (InputStream listStream = getClass().getResourceAsStream("/main/resources/embeddings/embeddings.list");
			BufferedReader listReader = new BufferedReader(new InputStreamReader(listStream, StandardCharsets.UTF_8)))
		{
			String filename;
			while ((filename = listReader.readLine()) != null)
			{
				filename = filename.trim();
				if (!filename.isEmpty() && !filename.startsWith("#") && filename.endsWith(".txt"))
				{
					// Extract intent from filename: relation_create.txt -> RELATION_CREATE
					String intentKey = filename.substring(0, filename.lastIndexOf('.')).toUpperCase();

					// Load examples from file
					loadEmbeddingsFromFile("/main/resources/embeddings/" + filename, intentKey);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("[ServoyEmbeddings] Failed to load embedding files: " + e.getMessage());
			e.printStackTrace();
		}

		System.out.println("[ServoyEmbeddings] Knowledge base loaded with " + getEmbeddingCount() + " examples");
	}

	/**
	 * Load embedding examples from a file (one example per line)
	 */
	private void loadEmbeddingsFromFile(String resourcePath, String intentKey)
	{
		try (InputStream is = getClass().getResourceAsStream(resourcePath);
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
		{
			String line;
			int count = 0;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#")) // Skip empty lines and comments
				{
					embed(line, "intent", intentKey);
					count++;
				}
			}
			System.out.println("[ServoyEmbeddings] Loaded " + count + " examples for intent: " + intentKey);
		}
		catch (Exception e)
		{
			System.err.println("[ServoyEmbeddings] Failed to load embeddings from " + resourcePath + ": " + e.getMessage());
		}
	}

	/**
	 * Embed text with metadata using ONNX model
	 */
	private void embed(String text, String metadataKey, String metadataValue)
	{
		try
		{
			float[] embeddingArray = generateEmbedding(text);
			Embedding embedding = new Embedding(embeddingArray);

			Metadata metadata = Metadata.from(metadataKey, metadataValue);
			TextSegment segment = TextSegment.from(text, metadata);
			embeddingStore.add(embedding, segment);
			embeddingCount++;
		}
		catch (Exception e)
		{
			System.err.println("[ServoyEmbeddings] Failed to embed text: " + e.getMessage());
		}
	}

	/**
	 * Generate embedding for text using ONNX model with ONNX tokenizer
	 */
	private float[] generateEmbedding(String text) throws OrtException
	{
		System.out.println("[ServoyEmbeddings] Tokenizing with ONNX tokenizer: \"" + text + "\"");

		// Step 1: Tokenize using ONNX tokenizer
		Map<String, OnnxTensor> tokenizerInputs = new HashMap<>();
		String[] textArray = new String[] { text };
		OnnxTensor textTensor = OnnxTensor.createTensor(env, textArray);
		tokenizerInputs.put("text", textTensor);

		OrtSession.Result tokenizerResults = tokenizerSession.run(tokenizerInputs);

		// Extract tokenizer outputs (1D arrays)
		long[] inputIds = (long[])tokenizerResults.get(0).getValue();
		long[] attentionMask = (long[])tokenizerResults.get(2).getValue();
		long[] tokenTypeIds = (long[])tokenizerResults.get(1).getValue();

		System.out.println("[ServoyEmbeddings] Token count: " + inputIds.length);

		// Cleanup tokenizer tensors
		textTensor.close();
		tokenizerResults.close();

		// Step 2: Wrap in 2D arrays for embedding model (add batch dimension)
		long[][] inputIdsArray = new long[][] { inputIds };
		long[][] attentionMaskArray = new long[][] { attentionMask };
		long[][] tokenTypeIdsArray = new long[][] { tokenTypeIds };

		OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsArray);
		OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskArray);
		OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenTypeIdsArray);

		// Step 3: Run embedding model inference
		Map<String, OnnxTensor> modelInputs = new HashMap<>();
		modelInputs.put("input_ids", inputIdsTensor);
		modelInputs.put("attention_mask", attentionMaskTensor);
		modelInputs.put("token_type_ids", tokenTypeIdsTensor);

		OrtSession.Result modelResults = modelSession.run(modelInputs);

		// Extract embeddings (last_hidden_state output)
		float[][][] output = (float[][][])modelResults.get(0).getValue();

		// Mean pooling over sequence length (first and only item in batch)
		float[] embedding = meanPooling(output[0], attentionMask);

		// Normalize
		normalize(embedding);

		System.out.println("[ServoyEmbeddings] Generated embedding vector (384 dimensions): [" + embedding[0] + ", " + embedding[1] + ", ... " +
			embedding[embedding.length - 1] + "]");

		// Cleanup
		inputIdsTensor.close();
		attentionMaskTensor.close();
		tokenTypeIdsTensor.close();
		modelResults.close();

		return embedding;
	}

	/**
	 * Mean pooling with attention mask
	 */
	private float[] meanPooling(float[][] tokenEmbeddings, long[] attentionMask)
	{
		int embeddingDim = tokenEmbeddings[0].length;
		float[] result = new float[embeddingDim];
		int count = 0;

		for (int i = 0; i < tokenEmbeddings.length && i < attentionMask.length; i++)
		{
			if (attentionMask[i] == 1)
			{
				for (int j = 0; j < embeddingDim; j++)
				{
					result[j] += tokenEmbeddings[i][j];
				}
				count++;
			}
		}

		if (count > 0)
		{
			for (int j = 0; j < embeddingDim; j++)
			{
				result[j] /= count;
			}
		}

		return result;
	}

	/**
	 * Normalize embedding vector
	 */
	private void normalize(float[] embedding)
	{
		float norm = 0;
		for (float v : embedding)
		{
			norm += v * v;
		}
		norm = (float)Math.sqrt(norm);
		if (norm > 0)
		{
			for (int i = 0; i < embedding.length; i++)
			{
				embedding[i] /= norm;
			}
		}
	}

	/**
	 * Search for similar text segments
	 */
	public List<SearchResult> search(String query, int maxResults)
	{
		try
		{
			System.out.println("\n[ServoyEmbeddings] === SEMANTIC SEARCH ===");
			System.out.println("[ServoyEmbeddings] Query: \"" + query + "\"");

			float[] queryEmbeddingArray = generateEmbedding(query);
			Embedding queryEmbedding = new Embedding(queryEmbeddingArray);

			System.out.println("[ServoyEmbeddings] Comparing against stored examples...");

			// Use LangChain4j's search with minimum score threshold
			EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
				.queryEmbedding(queryEmbedding)
				.maxResults(maxResults)
				.minScore(0.7) // Only return >70% similarity
				.build();

			List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

			// Convert to our SearchResult format
			List<SearchResult> results = new ArrayList<>();
			for (EmbeddingMatch<TextSegment> match : matches)
			{
				TextSegment segment = match.embedded();
				String matchText = segment.text();

				// Convert Metadata to Map<String, String>
				Map<String, String> metadata = new HashMap<>();
				for (Map.Entry<String, Object> entry : segment.metadata().toMap().entrySet())
				{
					metadata.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
				}

				double score = match.score();
				results.add(new SearchResult(score, matchText, metadata));
				System.out.println("[ServoyEmbeddings]   ✓ Match: \"" + matchText + "\" (similarity: " + String.format("%.3f", score) + ")");
			}

			if (results.isEmpty())
			{
				System.out.println("[ServoyEmbeddings] No matches found above 0.7 threshold");
			}
			else
			{
				System.out
					.println("[ServoyEmbeddings] Best match: \"" + results.get(0).text + "\" (score: " + String.format("%.3f", results.get(0).score) + ")");
			}
			System.out.println("[ServoyEmbeddings] === END SEARCH ===\n");

			return results;
		}
		catch (Exception e)
		{
			System.err.println("[ServoyEmbeddings] Search failed: " + e.getMessage());
			e.printStackTrace();
			return new ArrayList<>();
		}
	}

	/**
	 * Search result class
	 */
	public static class SearchResult
	{
		public final double score;
		public final String text;
		public final Map<String, String> metadata;

		public SearchResult(double score, String text, Map<String, String> metadata)
		{
			this.score = score;
			this.text = text;
			this.metadata = metadata;
		}
	}

	/**
	 * Get the number of embeddings in the store
	 */
	public int getEmbeddingCount()
	{
		return embeddingCount;
	}

	/**
	 * Add new text to the knowledge base
	 */
	public void addKnowledge(String text, String metadataKey, String metadataValue)
	{
		embed(text, metadataKey, metadataValue);
	}
}
