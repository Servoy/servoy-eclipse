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

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Stores and manages prompt enrichment rules loaded from SPM packages.
 * Rules are loaded dynamically from knowledge base packages in the workspace.
 * See KnowledgeBaseManager for loading logic.
 */
public class RulesCache
{
	private static final Map<String, String> rulesCache = new HashMap<>();

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
			
			URL rulesListURL = reader.getUrlForPath("rules/rules.list");
			if (rulesListURL != null)
			{
				List<String> ruleFiles = new ArrayList<>();
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
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rules from package reader: " + e.getMessage());
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
			URL fileURL = reader.getUrlForPath(path);
			if (fileURL == null)
			{
				return false;
			}
			
			String filename = path.substring(path.lastIndexOf('/') + 1);
			String baseName = filename.substring(0, filename.lastIndexOf('.'));
			String intentKey = baseName.toUpperCase();
			
			try (InputStream is = fileURL.openStream())
			{
				String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
				rulesCache.put(intentKey, content); // Overwrites if key exists
				return true;
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RulesCache] Failed to load rule from " + path + ": " + e.getMessage(), e);
		}
		return false;
	}
}
