package com.servoy.eclipse.mcp.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores and manages prompt enrichment rules loaded from resource files.
 * Rules are loaded once at initialization and cached in memory for fast access.
 * Auto-discovers all .md files in /rules/ directory.
 */
public class RulesCache
{
	// Cache: intent key -> rule content
	private static final Map<String, String> rulesCache = new HashMap<>();

	// Static initializer - loads all rules at class initialization
	static
	{
		System.out.println("[RuleStore] Initializing rule store...");
		autoDiscoverRules();
		System.out.println("[RuleStore] Rule store initialized with " + rulesCache.size() + " rules");
	}

	/**
	 * Auto-discover and load all .md files from /rules/ directory
	 * Reads rules.list manifest file to find all rule files
	 */
	private static void autoDiscoverRules()
	{
		try (InputStream listStream = RulesCache.class.getResourceAsStream("/main/resources/rules/rules.list");
			BufferedReader listReader = new BufferedReader(new InputStreamReader(listStream, StandardCharsets.UTF_8)))
		{
			String filename;
			while ((filename = listReader.readLine()) != null)
			{
				filename = filename.trim();
				if (!filename.isEmpty() && !filename.startsWith("#") && filename.endsWith(".md"))
				{
					// Extract filename without extension: valuelist_create.md -> valuelist_create
					String baseName = filename.substring(0, filename.lastIndexOf('.'));
					
					// Convert to uppercase with underscores: valuelist_create -> VALUELIST_CREATE
					String intentKey = baseName.toUpperCase();
					
					// Load the rule
					loadRule(intentKey, "/main/resources/rules/" + filename);
				}
			}
		}
		catch (Exception e)
		{
			System.err.println("[RuleStore] Failed to load rules: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Load a rule from resources into the cache
	 */
	private static void loadRule(String intentKey, String resourcePath)
	{
		try (InputStream is = RulesCache.class.getResourceAsStream(resourcePath))
		{
			if (is == null)
			{
				System.err.println("[RuleStore] Rule file not found: " + resourcePath);
				return;
			}

			String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			rulesCache.put(intentKey, content);
			System.out.println("[RuleStore] Loaded rule: " + intentKey + " (" + content.length() + " chars)");
		}
		catch (Exception e)
		{
			System.err.println("[RuleStore] Failed to load rule from " + resourcePath + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Get rules for a specific intent
	 * @param intent The intent key (e.g., "RELATION_CREATE")
	 * @return The rule content, or empty string if not found
	 */
	public static String getRules(String intent)
	{
		String rules = rulesCache.get(intent);
		if (rules == null)
		{
			System.out.println("[RuleStore] No rules found for intent: " + intent);
			return "";
		}
		return rules;
	}

	/**
	 * Check if rules exist for an intent
	 */
	public static boolean hasRules(String intent)
	{
		return rulesCache.containsKey(intent);
	}

	/**
	 * Get the number of loaded rules
	 */
	public static int getRuleCount()
	{
		return rulesCache.size();
	}

	/**
	 * Get all available intent keys
	 */
	public static String[] getAvailableIntents()
	{
		return rulesCache.keySet().toArray(new String[0]);
	}
}
