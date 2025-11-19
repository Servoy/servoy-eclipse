package com.servoy.eclipse.mcp.ai;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Stores and manages prompt enrichment rules loaded from resource files.
 * Rules are loaded once at initialization and cached in memory for fast access.
 * Auto-discovers all .md files in /rules/ directory.
 */
public class RulesCache
{
	// Cache: intent key -> rule content
	private static final Map<String, String> rulesCache = new HashMap<>();

	static
	{
		autoDiscoverRules();
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
					String baseName = filename.substring(0, filename.lastIndexOf('.'));
					String intentKey = baseName.toUpperCase();
					loadRule(intentKey, "/main/resources/rules/" + filename);
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RuleStore] Failed to load rules: " + e.getMessage());
		}
	}

	/**
	 * Load a rule from resources into the cache
	 */
	//TODO: initialize at developer startup
	private static void loadRule(String intentKey, String resourcePath)
	{
		try (InputStream is = RulesCache.class.getResourceAsStream(resourcePath))
		{
			if (is == null)
			{
				ServoyLog.logError("[RuleStore] Rule file not found: " + resourcePath);
				return;
			}

			String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			rulesCache.put(intentKey, content);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RuleStore] Failed to load rule from " + resourcePath + ": " + e.getMessage());
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
