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
		this.intentDetector = new IntentDetector();
	}

	/**
	 * Process prompt: detect intent and enrich if needed
	 * @param prompt The user's original prompt
	 * @return Enriched prompt or "PASS_THROUGH"
	 */
	public String processPrompt(String prompt)
	{
		String intent = intentDetector.detectIntent(prompt);

		if (!intentDetector.isServoyIntent(intent))
		{
			return "PASS_THROUGH";
		}
		String enriched = enrichPrompt(intent, prompt);

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
			return "PASS_THROUGH";
		}

		return prompt + "\n\n" + rules + "\n\nNow process the original request above using these rules.\n";
	}
}
