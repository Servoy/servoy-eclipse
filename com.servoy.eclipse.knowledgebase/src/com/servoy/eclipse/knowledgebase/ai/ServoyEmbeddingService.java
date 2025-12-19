package com.servoy.eclipse.knowledgebase.ai;

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
	 * Initialize the service with ONNX models and knowledge base bundles.
	 * Called once at plugin startup by Activator.
	 * 
	 * @param knowledgeBaseBundles array of knowledge base bundles to load (can be empty)
	 */
	private void initialize(Bundle[] knowledgeBaseBundles)
	{
		ServoyLog.logInfo("[ServoyEmbeddings] Initializing ONNX embedding service with ONNX tokenizer...");

		try
		{
			env = OrtEnvironment.getEnvironment();

			// Load embedding model from OSGi bundle
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

			// Load knowledge base from bundles
			if (knowledgeBaseBundles != null && knowledgeBaseBundles.length > 0)
			{
				initializeKnowledgeBase(knowledgeBaseBundles);
			}
			else
			{
				ServoyLog.logInfo("[ServoyEmbeddings] No knowledge base bundles provided - service ready but empty");
			}
			
			ServoyLog.logInfo("[ServoyEmbeddings] Embedding service ready!");
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to initialize: " + e.getMessage());
			throw new RuntimeException("Failed to initialize embedding service", e);
		}
	}

	/**
	 * Get singleton instance without knowledge base.
	 * Use getInstance(Bundle[]) to initialize with knowledge base bundles.
	 * 
	 * @return the singleton instance (initialized with models only)
	 */
	public static synchronized ServoyEmbeddingService getInstance()
	{
		return getInstance(new Bundle[0]);
	}

	/**
	 * Get singleton instance and initialize with knowledge base bundles.
	 * Can be called multiple times - subsequent calls will be ignored (singleton).
	 * 
	 * @param knowledgeBaseBundles array of knowledge base bundles to load
	 * @return the singleton instance
	 */
	public static synchronized ServoyEmbeddingService getInstance(Bundle[] knowledgeBaseBundles)
	{
		if (instance == null)
		{
			instance = new ServoyEmbeddingService();
			instance.initialize(knowledgeBaseBundles);
		}
		return instance;
	}

	/**
	 * Load knowledge base from a single bundle.
	 * Loads embeddings (additive - may create duplicates) and rules (overwrites by intent key).
	 * 
	 * @param bundle the knowledge base bundle to load
	 */
	public void loadKnowledgeBase(Bundle bundle)
	{
		String bundleName = bundle.getSymbolicName();
		ServoyLog.logInfo("[ServoyEmbeddings] Loading knowledge base from bundle: " + bundleName);
		
		// Load embeddings (additive)
		int embeddingCount = loadKnowledgeBaseFromBundle(bundle);
		
		// Load rules (overwrites by intent key)
		int ruleCount = RulesCache.loadFromBundle(bundle);
		
		ServoyLog.logInfo("[ServoyEmbeddings] Loaded " + embeddingCount + " embeddings and " + 
			ruleCount + " rules from " + bundleName);
	}

	/**
	 * Reload all knowledge bases from all installed bundles.
	 * Clears all embeddings and rules, then reloads from scratch.
	 * This prevents duplicates and ensures clean state.
	 * 
	 * @param knowledgeBaseBundles array of knowledge base bundles to load
	 */
	public void reloadAllKnowledgeBases(Bundle[] knowledgeBaseBundles)
	{
		ServoyLog.logInfo("[ServoyEmbeddings] Reloading all knowledge bases...");
		
		// Clear all embeddings
		this.embeddingStore.removeAll();
		this.embeddingCount = 0;
		
		// Clear all rules
		RulesCache.clear();
		
		// Reload embeddings and rules from all bundles
		initializeKnowledgeBase(knowledgeBaseBundles);
		
		ServoyLog.logInfo("[ServoyEmbeddings] Reload complete");
	}

	/**
	 * Load knowledge base from multiple bundles.
	 * Each bundle's embeddings are loaded and merged into the embedding store.
	 * Rules with same intent key will be overwritten.
	 * 
	 * @param knowledgeBaseBundles array of knowledge base bundles
	 */
	private void initializeKnowledgeBase(Bundle[] knowledgeBaseBundles)
	{
		int totalEmbeddings = 0;
		int totalRules = 0;
		
		for (Bundle bundle : knowledgeBaseBundles)
		{
			String bundleName = bundle.getSymbolicName();
			ServoyLog.logInfo("[ServoyEmbeddings] Loading knowledge base from bundle: " + bundleName);
			
			// Load embeddings
			int embeddingCount = loadKnowledgeBaseFromBundle(bundle);
			totalEmbeddings += embeddingCount;
			
			// Load rules (overwrites if intent key exists)
			int ruleCount = RulesCache.loadFromBundle(bundle);
			totalRules += ruleCount;
			
			ServoyLog.logInfo("[ServoyEmbeddings] Loaded " + embeddingCount + " embeddings and " + 
				ruleCount + " rules from " + bundleName);
		}
		
		ServoyLog.logInfo("[ServoyEmbeddings] Total knowledge base loaded: " + totalEmbeddings + " embeddings and " + 
			totalRules + " rules from " + knowledgeBaseBundles.length + " bundle(s)");
	}

	/**
	 * Load knowledge base from a single bundle.
	 * Reads embeddings/embeddings.list to find all .txt files to load.
	 * 
	 * @param bundle the knowledge base bundle
	 * @return number of embeddings loaded from this bundle
	 */
	private int loadKnowledgeBaseFromBundle(Bundle bundle)
	{
		int loadedCount = 0;
		
		try
		{
			URL embeddingsListURL = bundle.getEntry("embeddings/embeddings.list");
			if (embeddingsListURL == null)
			{
				ServoyLog.logError("[ServoyEmbeddings] No embeddings.list found in bundle: " + bundle.getSymbolicName());
				return 0;
			}

			try (InputStream listStream = embeddingsListURL.openStream();
				BufferedReader listReader = new BufferedReader(new InputStreamReader(listStream, StandardCharsets.UTF_8)))
			{
				String filename;
				while ((filename = listReader.readLine()) != null)
				{
					filename = filename.trim();
					if (!filename.isEmpty() && !filename.startsWith("#") && filename.endsWith(".txt"))
					{
						// Extract intent from filename: relations.txt -> RELATIONS, valuelists.txt -> VALUELISTS
						String intentKey = filename.substring(0, filename.lastIndexOf('.')).toUpperCase();

						// Load examples from bundle
						int count = loadEmbeddingsFromBundle(bundle, "embeddings/" + filename, intentKey);
						loadedCount += count;
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to load knowledge base from bundle " + 
				bundle.getSymbolicName() + ": " + e.getMessage());
		}
		
		return loadedCount;
	}

	/**
	 * Load embedding examples from a bundle file (one example per line).
	 * 
	 * @param bundle the bundle containing the embeddings
	 * @param path path within the bundle (e.g., "embeddings/forms.txt")
	 * @param intentKey the intent/category key (e.g., "FORMS")
	 * @return number of embeddings loaded from this file
	 */
	private int loadEmbeddingsFromBundle(Bundle bundle, String path, String intentKey)
	{
		int count = 0;
		
		try
		{
			URL fileURL = bundle.getEntry(path);
			if (fileURL == null)
			{
				ServoyLog.logError("[ServoyEmbeddings] File not found in bundle " + bundle.getSymbolicName() + ": " + path);
				return 0;
			}

			try (InputStream is = fileURL.openStream();
				BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#")) // Skip empty lines and comments
					{
						embed(line, "intent", intentKey);
						count++;
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ServoyEmbeddings] Failed to load embeddings from " + path + 
				" in bundle " + bundle.getSymbolicName() + ": " + e.getMessage());
		}
		
		return count;
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
			float[] queryEmbeddingArray = generateEmbedding(query);
			Embedding queryEmbedding = new Embedding(queryEmbeddingArray);

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
	
	// ========================================
	// NEW: IPackageReader-based methods for workspace packages
	// ========================================
	
	/**
	 * Reload all knowledge bases from package readers (workspace projects).
	 * Clears all embeddings and rules, then reloads from scratch.
	 * 
	 * @param packageReaders array of package readers to load from
	 */
	public void reloadAllKnowledgeBasesFromReaders(IPackageReader[] packageReaders)
	{
		ServoyLog.logInfo("[ServoyEmbeddings] Reloading all knowledge bases from package readers...");
		
		// Clear all embeddings
		this.embeddingStore.removeAll();
		this.embeddingCount = 0;
		
		// Clear all rules
		RulesCache.clear();
		
		// Reload embeddings and rules from all package readers
		int totalEmbeddings = 0;
		int totalRules = 0;
		
		for (IPackageReader reader : packageReaders)
		{
			String packageName = reader.getPackageName();
			ServoyLog.logInfo("[ServoyEmbeddings] Loading knowledge base from package: " + packageName);
			
			// Load embeddings
			int embeddingCount = loadKnowledgeBaseFromReader(reader);
			totalEmbeddings += embeddingCount;
			
			// Load rules
			int ruleCount = RulesCache.loadFromPackageReader(reader);
			totalRules += ruleCount;
			
			ServoyLog.logInfo("[ServoyEmbeddings] Loaded " + embeddingCount + " embeddings and " + 
				ruleCount + " rules from " + packageName);
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
						// CRITICAL: Use "intent" not "category" to match McpServletProvider expectations
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
