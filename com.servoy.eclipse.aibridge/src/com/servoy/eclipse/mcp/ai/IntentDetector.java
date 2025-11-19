package com.servoy.eclipse.mcp.ai;

import java.util.List;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Detects user intent from prompts using semantic similarity search.
 * Uses ServoyEmbeddingService with ONNX tokenizer to find the closest matching intent.
 */
public class IntentDetector
{
	private final ServoyEmbeddingService embeddingService;

	public IntentDetector()
	{
		this.embeddingService = ServoyEmbeddingService.getInstance();
	}

	/**
	 * Detect intent from user prompt using semantic search
	 * @param prompt The user's prompt
	 * @return Intent string (e.g., "RELATION_CREATE", "VALUELIST_CREATE", "PASS_THROUGH")
	 */
	public String detectIntent(String prompt)
	{
		String intent = "PASS_THROUGH";
		ServoyEmbeddingService.SearchResult bestMatch = null;
		List<ServoyEmbeddingService.SearchResult> results = embeddingService.search(prompt, 1);
		if (!results.isEmpty())
		{
			bestMatch = results.get(0);
			intent = bestMatch.metadata.get("intent");
		}
		ServoyLog.logInfo("[IntentDetector] Detected intent: " + intent + " (score: " + (bestMatch != null ? bestMatch.score : 0) + ")");
		return intent;
	}

	/**
	 * Check if the detected intent is Servoy-related
	 * @param intent The detected intent
	 * @return true if Servoy-related, false otherwise
	 */
	public boolean isServoyIntent(String intent)
	{
		return !intent.equals("PASS_THROUGH");
	}
}
