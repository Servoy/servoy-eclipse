package com.servoy.eclipse.knowledgebase.ai;

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
		}
	}

	/**
	 * Get rules for a specific intent
	 * @param intent The intent key (e.g., "RELATION_CREATE")
	 * @return The rule content, or empty string if not found
	 */
	public static String getRules(String intent)
	{
		return getRules(intent, null);
	}
	
	/**
	 * Get rules for a specific intent with variable substitution
	 * @param intent The intent key (e.g., "RELATION_CREATE")
	 * @param projectName The active project name for variable substitution (can be null)
	 * @return The rule content with variables replaced, or empty string if not found
	 */
	public static String getRules(String intent, String projectName)
	{
		String rules = rulesCache.get(intent);
		if (rules == null)
		{
			return "";
		}
		
		// Replace variables if project name provided
		if (projectName != null && !projectName.isEmpty())
		{
			rules = rules.replace("{{PROJECT_NAME}}", projectName);
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

	/**
	 * Clear all cached rules.
	 * Used when reloading knowledge bases.
	 */
	public static void clear()
	{
		rulesCache.clear();
		ServoyLog.logInfo("[RulesCache] All rules cleared");
	}

	/**
	 * Load rules from a single bundle.
	 * Overwrites existing rules with the same intent key.
	 * Reads rules/rules.list to find all .md files to load.
	 * 
	 * @param bundle the knowledge base bundle
	 * @return number of rules loaded from this bundle
	 */
	public static int loadFromBundle(org.osgi.framework.Bundle bundle)
	{
		int loadedCount = 0;
		String bundleName = bundle.getSymbolicName();

		try
		{
			java.net.URL rulesListURL = bundle.getEntry("rules/rules.list");
			if (rulesListURL == null)
			{
				ServoyLog.logError("[RulesCache] No rules.list found in bundle: " + bundleName);
				return 0;
			}

			try (java.io.InputStream listStream = rulesListURL.openStream();
				java.io.BufferedReader listReader = new java.io.BufferedReader(
					new java.io.InputStreamReader(listStream, StandardCharsets.UTF_8)))
			{
				String filename;
				while ((filename = listReader.readLine()) != null)
				{
					filename = filename.trim();
					if (!filename.isEmpty() && !filename.startsWith("#") && filename.endsWith(".md"))
					{
						// Extract intent key from filename: forms.md -> FORMS
						String baseName = filename.substring(0, filename.lastIndexOf('.'));
						String intentKey = baseName.toUpperCase();
						
						// Load rule from bundle (overwrites if exists)
						if (loadRuleFromBundle(bundle, "rules/" + filename, intentKey))
						{
							loadedCount++;
						}
					}
				}
			}

			ServoyLog.logInfo("[RulesCache] Loaded " + loadedCount + " rules from bundle: " + bundleName);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rules from bundle " + bundleName + ": " + e.getMessage(), e);
		}

		return loadedCount;
	}

	/**
	 * Load a single rule from a bundle and store in cache.
	 * Overwrites existing rule with same intent key.
	 * 
	 * @param bundle the bundle containing the rule
	 * @param path path within the bundle (e.g., "rules/forms.md")
	 * @param intentKey the intent/category key (e.g., "FORMS")
	 * @return true if loaded successfully
	 */
	private static boolean loadRuleFromBundle(org.osgi.framework.Bundle bundle, String path, String intentKey)
	{
		try
		{
			java.net.URL fileURL = bundle.getEntry(path);
			if (fileURL == null)
			{
				ServoyLog.logError("[RulesCache] File not found in bundle " + bundle.getSymbolicName() + ": " + path);
				return false;
			}

			try (java.io.InputStream is = fileURL.openStream())
			{
				String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				rulesCache.put(intentKey, content); // Overwrites if key exists
				return true;
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rule from " + path + " in bundle " + 
				bundle.getSymbolicName() + ": " + e.getMessage());
			return false;
		}
	}
	
	// ========================================
	// NEW: IPackageReader-based methods for workspace packages
	// ========================================
	
	/**
	 * Load rules from a package reader (workspace project).
	 * Reads rules/rules.list to find all .md files to load.
	 * 
	 * @param reader the package reader
	 * @return number of rules loaded
	 */
	public static int loadFromPackageReader(org.sablo.specification.Package.IPackageReader reader)
	{
		int loadedCount = 0;
		
		try
		{
			String packageName = reader.getPackageName();
			
			java.net.URL rulesListURL = reader.getUrlForPath("rules/rules.list");
			if (rulesListURL == null)
			{
				ServoyLog.logInfo("[RulesCache] No rules/rules.list found in package: " + packageName);
				return 0;
			}
			
			java.util.List<String> ruleFiles = new java.util.ArrayList<>();
			try (BufferedReader reader2 = new BufferedReader(
				new InputStreamReader(rulesListURL.openStream(), StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader2.readLine()) != null)
				{
					line = line.trim();
					if (!line.isEmpty() && !line.startsWith("#") && line.endsWith(".md"))
					{
						ruleFiles.add(line);
					}
				}
			}
			
			for (String ruleFile : ruleFiles)
			{
				String path = "rules/" + ruleFile;
				
				if (loadRuleFromReader(reader, path))
				{
					loadedCount++;
				}
			}
			
			ServoyLog.logInfo("[RulesCache] Loaded " + loadedCount + " rules from package: " + packageName);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rules from package reader: " + e.getMessage(), e);
		}
		
		return loadedCount;
	}
	
	/**
	 * Load a single rule from a package reader.
	 * 
	 * @param reader the package reader
	 * @param path the path to the rule file (e.g., "rules/forms.md")
	 * @return true if loaded successfully
	 */
	private static boolean loadRuleFromReader(org.sablo.specification.Package.IPackageReader reader, String path)
	{
		try
		{
			java.net.URL fileURL = reader.getUrlForPath(path);
			if (fileURL == null)
			{
				return false;
			}
			
			// Extract intent key from filename
			String filename = path.substring(path.lastIndexOf('/') + 1);
			String baseName = filename.substring(0, filename.lastIndexOf('.'));
			String intentKey = baseName.toUpperCase();
			
			try (java.io.InputStream is = fileURL.openStream())
			{
				String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				rulesCache.put(intentKey, content); // Overwrites if key exists
				return true;
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rule from " + path + ": " + e.getMessage(), e);
			return false;
		}
	}
}
