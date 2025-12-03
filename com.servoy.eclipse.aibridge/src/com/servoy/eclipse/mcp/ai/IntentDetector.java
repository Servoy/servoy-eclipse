package com.servoy.eclipse.mcp.ai;

import java.util.List;

import com.servoy.eclipse.mcp.ai.ServoyEmbeddingService.SearchResult;
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

		// Get top 5 results to compare and apply keyword boosting
		List<ServoyEmbeddingService.SearchResult> results = embeddingService.search(prompt, 5);

		if (!results.isEmpty())
		{
			// Log all top matches for debugging
			for (SearchResult result : results)
			{
			}

			// Apply keyword boosting to disambiguate similar scores
			bestMatch = applyKeywordBoosting(prompt, results);
			intent = bestMatch.metadata.get("intent");
		}

		ServoyLog.logInfo("[IntentDetector] Detected intent: " + intent + " (score: " + (bestMatch != null ? bestMatch.score : 0) + ")");
		return intent;
	}

	/**
	 * Apply keyword boosting to resolve ambiguity when multiple intents have similar scores.
	 * This helps disambiguate cases like "create form" vs "create relation".
	 */
	private ServoyEmbeddingService.SearchResult applyKeywordBoosting(String prompt, List<ServoyEmbeddingService.SearchResult> results)
	{
		String promptLower = prompt.toLowerCase();

		// Define intent-specific keywords that should boost the score
		double formBoost = 0.0;
		double relationBoost = 0.0;
		double valuelistBoost = 0.0;

		// Check for form-specific keywords
		if (promptLower.contains("form") || promptLower.contains("frm") ||
			promptLower.contains("screen") || promptLower.contains("ui") ||
			promptLower.contains("layout") || promptLower.contains("responsive") ||
			promptLower.contains("css"))
		{
			formBoost = 0.15; // 15% boost for form keywords
		}

		// Check for relation-specific keywords
		if (promptLower.contains("relation") || promptLower.contains("link") ||
			promptLower.contains("join") || promptLower.contains("connect") ||
			promptLower.contains("between") || promptLower.contains("foreign key"))
		{
			relationBoost = 0.15; // 15% boost for relation keywords
		}

		// Check for valuelist-specific keywords
		if (promptLower.contains("valuelist") || promptLower.contains("value list") ||
			promptLower.contains("dropdown") || promptLower.contains("lookup"))
		{
			valuelistBoost = 0.15; // 15% boost for valuelist keywords
		}

		// Find best result after applying boosts
		ServoyEmbeddingService.SearchResult best = results.get(0);
		double bestScore = best.score;

		for (ServoyEmbeddingService.SearchResult result : results)
		{
			String intent = result.metadata.get("intent");
			double boostedScore = result.score;

			if ("FORMS".equals(intent))
			{
				boostedScore += formBoost;
			}
			else if ("RELATIONS".equals(intent))
			{
				boostedScore += relationBoost;
			}
			else if ("VALUELISTS".equals(intent))
			{
				boostedScore += valuelistBoost;
			}

			if (boostedScore > bestScore)
			{
				best = result;
				bestScore = boostedScore;
			}
		}

		return best;
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
