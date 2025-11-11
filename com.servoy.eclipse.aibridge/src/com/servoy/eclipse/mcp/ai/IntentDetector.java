package com.servoy.eclipse.mcp.ai;

import java.util.List;

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
		System.out.println("[IntentDetector] Prompt: \"" + prompt + "\"");

		// Search for similar prompts in knowledge base
		List<ServoyEmbeddingService.SearchResult> results = embeddingService.search(prompt, 1);

		if (results.isEmpty())
		{
			System.out.println("[IntentDetector] No matches found, defaulting to PASS_THROUGH");
			return "PASS_THROUGH";
		}

		// Get the best match
		ServoyEmbeddingService.SearchResult bestMatch = results.get(0);
		String intent = bestMatch.metadata.get("intent");

		System.out.println("[IntentDetector] Best match: \"" + bestMatch.text + "\" (score: " + String.format("%.3f", bestMatch.score) + ")");
		System.out.println("[IntentDetector] Detected intent: " + intent);

		return intent != null ? intent : "PASS_THROUGH";
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
