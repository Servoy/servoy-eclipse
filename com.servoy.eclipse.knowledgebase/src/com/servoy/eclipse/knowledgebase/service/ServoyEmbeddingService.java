package com.servoy.eclipse.knowledgebase.service;

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
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.model.util.ServoyLog;

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
	private static final double SCORE_THRESHOLD = 0.8; // Minimum similarity score percentage

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
	 * Initialize the service with ONNX models.
	 * Called once at plugin startup by Activator.
	 * Knowledge bases are loaded separately via loadKnowledgeBaseFromReader().
	 */
	private void initialize()
	{
		ServoyLog.logInfo("[ServoyEmbeddings] Initializing ONNX embedding service with ONNX tokenizer...");

		try
		{
			env = OrtEnvironment.getEnvironment();

			ServoyLog.logInfo("[ServoyEmbeddings] Loading ONNX embedding model from bundle...");
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

			modelSession = env.createSession(modelBytes);
			URL tokenizerURL = modelsBundle.getEntry("models/bge-small-en-v1.5/tokenizer.onnx");
			if (tokenizerURL == null)
			{
				throw new RuntimeException("Tokenizer file not found in bundle");
			}
			InputStream tokenizerStream = tokenizerURL.openStream();
			byte[] tokenizerBytes = tokenizerStream.readAllBytes();
			tokenizerStream.close();

			OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
			sessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath());
			tokenizerSession = env.createSession(tokenizerBytes, sessionOptions);
			ServoyLog.logInfo("[ServoyEmbeddings] ONNX model and tokenizer loaded successfully");
			
			ServoyLog.logInfo("[ServoyEmbeddings] Embedding service ready! Knowledge bases will be loaded from workspace packages.");
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to initialize: " + e.getMessage());
			throw new RuntimeException("Failed to initialize embedding service", e);
		}
	}

	/**
	 * Get singleton instance (lazy initialization).
	 * Models are initialized on first call.
	 * Knowledge bases loaded separately via loadKnowledgeBaseFromReader().
	 * 
	 * @return the singleton instance
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
			ServoyLog.logError("[ServoyEmbeddings] Failed to embed text: " + e.getMessage());
		}
	}
	
	/**
	 * Extract category/intent key from file path.
	 * E.g., "embeddings/forms.txt" -> "FORMS"
	 * 
	 * @param path the file path
	 * @return the category key in uppercase
	 */
	private String extractCategoryFromPath(String path)
	{
		String filename = path.substring(path.lastIndexOf('/') + 1);
		String baseName = filename.substring(0, filename.lastIndexOf('.'));
		return baseName.toUpperCase();
	}

	/**
	 * Generate embedding for text using ONNX model with ONNX tokenizer
	 */
	private float[] generateEmbedding(String text) throws OrtException
	{
		Map<String, OnnxTensor> tokenizerInputs = new HashMap<>();
		String[] textArray = new String[] { text };
		OnnxTensor textTensor = OnnxTensor.createTensor(env, textArray);
		tokenizerInputs.put("text", textTensor);

		OrtSession.Result tokenizerResults = tokenizerSession.run(tokenizerInputs);

		long[] inputIds = (long[])tokenizerResults.get(0).getValue();
		long[] attentionMask = (long[])tokenizerResults.get(2).getValue();
		long[] tokenTypeIds = (long[])tokenizerResults.get(1).getValue();
		textTensor.close();
		tokenizerResults.close();

		long[][] inputIdsArray = new long[][] { inputIds };
		long[][] attentionMaskArray = new long[][] { attentionMask };
		long[][] tokenTypeIdsArray = new long[][] { tokenTypeIds };

		OnnxTensor inputIdsTensor = OnnxTensor.createTensor(env, inputIdsArray);
		OnnxTensor attentionMaskTensor = OnnxTensor.createTensor(env, attentionMaskArray);
		OnnxTensor tokenTypeIdsTensor = OnnxTensor.createTensor(env, tokenTypeIdsArray);

		Map<String, OnnxTensor> modelInputs = new HashMap<>();
		modelInputs.put("input_ids", inputIdsTensor);
		modelInputs.put("attention_mask", attentionMaskTensor);
		modelInputs.put("token_type_ids", tokenTypeIdsTensor);

		OrtSession.Result modelResults = modelSession.run(modelInputs);

		float[][][] output = (float[][][])modelResults.get(0).getValue();

		float[] embedding = meanPooling(output[0], attentionMask);

		normalize(embedding);

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
			float[] queryEmbeddingArray = generateEmbedding(query);
			Embedding queryEmbedding = new Embedding(queryEmbeddingArray);

			EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
				.queryEmbedding(queryEmbedding)
				.maxResults(maxResults)
				.minScore(SCORE_THRESHOLD) // Only return >70% similarity
				.build();

			List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();

			// Convert to SearchResult format
			List<SearchResult> results = new ArrayList<>();
			for (EmbeddingMatch<TextSegment> match : matches)
			{
				TextSegment segment = match.embedded();
				String matchText = segment.text();

				Map<String, String> metadata = new HashMap<>();
				for (Map.Entry<String, Object> entry : segment.metadata().toMap().entrySet())
				{
					metadata.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
				}

				double score = match.score();
				results.add(new SearchResult(score, matchText, metadata));
			}

			return results;
		}
		catch (Exception e)
		{
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
	
	/**
	 * Reload all knowledge bases from package readers (workspace projects).
	 * Clears all embeddings and rules, then reloads from scratch.
	 * 
	 * @param packageReaders array of package readers to load from
	 */
	public void reloadAllKnowledgeBasesFromReaders(IPackageReader[] packageReaders)
	{
		ServoyLog.logInfo("[ServoyEmbeddings] Reloading all knowledge bases from package readers...");
		
		this.embeddingStore.removeAll();
		this.embeddingCount = 0;
		
		RulesCache.clear();
		
		int totalEmbeddings = 0;
		int totalRules = 0;
		
		for (IPackageReader reader : packageReaders)
		{
			String packageName = reader.getPackageName();
			
			int embeddingCount = loadKnowledgeBaseFromReader(reader);
			totalEmbeddings += embeddingCount;
			
			int ruleCount = RulesCache.loadFromPackageReader(reader);
			totalRules += ruleCount;
		}
		
		ServoyLog.logInfo("[ServoyEmbeddings] Reload complete - Total: " + totalEmbeddings + " embeddings, " + 
			totalRules + " rules from " + packageReaders.length + " package(s)");
	}
	
	/**
	 * Load knowledge base from a single package reader (workspace project).
	 * This is an ADDITIVE load - does NOT clear existing embeddings.
	 * Reads embeddings/embeddings.list to find all .txt files to load.
	 * 
	 * @param reader the package reader
	 * @return number of embeddings loaded from this package
	 */
	public int loadKnowledgeBaseFromReader(IPackageReader reader)
	{
		int loadedCount = 0;
		
		try
		{
			String packageName = reader.getPackageName();
			
			URL embeddingsListURL = reader.getUrlForPath("embeddings/embeddings.list");
			if (embeddingsListURL == null)
			{
				ServoyLog.logInfo("[ServoyEmbeddings] No embeddings/embeddings.list found in package: " + packageName);
				return 0;
			}
			
			List<String> embeddingFiles = new ArrayList<>();
			try (BufferedReader reader2 = new BufferedReader(
				new InputStreamReader(embeddingsListURL.openStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader2.readLine()) != null)
				{
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#"))
					{
						embeddingFiles.add(line);
					}
				}
			}
			
			for (String embeddingFile : embeddingFiles)
			{
				String path = "embeddings/" + embeddingFile;
				int count = loadEmbeddingsFromReader(reader, path);
				loadedCount += count;
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to load knowledge base from package reader: " + 
				e.getMessage(), e);
		}
		
		return loadedCount;
	}
	
	/**
	 * Load embeddings from a specific file in a package reader.
	 * 
	 * @param reader the package reader
	 * @param path the path to the embeddings file (e.g., "embeddings/forms.txt")
	 * @return number of embeddings loaded
	 */
	private int loadEmbeddingsFromReader(IPackageReader reader, String path)
	{
		int count = 0;
		
		try
		{
			URL fileURL = reader.getUrlForPath(path);
			if (fileURL == null)
			{
				return 0;
			}
			
			String category = extractCategoryFromPath(path);
			
			try (BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(fileURL.openStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = bufferedReader.readLine()) != null)
				{
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#"))
					{
						embed(line, "intent", category);
						count++;
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to load embeddings from " + path + ": " + e.getMessage(), e);
		}
		
		return count;
	}
}
