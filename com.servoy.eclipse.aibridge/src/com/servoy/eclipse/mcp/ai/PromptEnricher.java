package com.servoy.eclipse.mcp.ai;

/**
 * Enriches user prompts with Servoy-specific context and rules based on detected intent.
 * This is the bridge between intent detection and the existing enrichment methods.
 */
public class PromptEnricher
{

	private final IntentDetector intentDetector;

	public PromptEnricher()
	{
		System.out.println("[PromptEnricher] Initializing with ONNX-based semantic intent detection");
		this.intentDetector = new IntentDetector();
	}

	/**
	 * Process prompt: detect intent and enrich if needed
	 * @param prompt The user's original prompt
	 * @return Enriched prompt or "PASS_THROUGH"
	 */
	public String processPrompt(String prompt)
	{
		System.out.println("[PromptEnricher] Processing prompt: \"" + prompt + "\"");

		// Detect intent using embeddings
		String intent = intentDetector.detectIntent(prompt);

		// If not Servoy-related, pass through
		if (!intentDetector.isServoyIntent(intent))
		{
			System.out.println("[PromptEnricher] Not Servoy-related, passing through");
			return "PASS_THROUGH";
		}

		System.out.println("[PromptEnricher] Enriching prompt for intent: " + intent);

		String enriched = enrichPrompt(intent, prompt);

		System.out.println("[PromptEnricher] Enrichment complete (" + enriched.length() + " chars)");
		return enriched;
	}

	/**
	 * Enrich prompts based on user intent, using rules from RuleStore
	 */
	private String enrichPrompt(String intent, String prompt)
	{
		String rules = RulesCache.getRules(intent);
		if (rules.isEmpty())
		{
			System.err.println("[PromptEnricher] No rules found for RELATION_CREATE");
			return "PASS_THROUGH";
		}

		// Combine user prompt with rules
		return prompt + "\n\n" + rules + "\n\nNow process the original request above using these rules.\n";
	}
}
