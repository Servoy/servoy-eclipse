package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.List;
import java.util.Map;

/**
 * Base class for tool handlers providing common parameter extraction utilities.
 * Eliminates code duplication across handlers.
 */
public abstract class ToolHandlerBase
{
	/**
	 * Extract string parameter from arguments map with default value.
	 * 
	 * @param args Arguments map
	 * @param key Parameter key
	 * @param defaultValue Default value if not found or null
	 * @return String value or default
	 */
	protected String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	/**
	 * Extract map parameter from arguments.
	 * 
	 * @param args Arguments map
	 * @param key Parameter key
	 * @return Map value or null if not found or wrong type
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, Object> extractMap(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof Map)
		{
			return (Map<String, Object>)value;
		}
		return null;
	}

	/**
	 * Extract string array parameter from arguments.
	 * 
	 * @param args Arguments map
	 * @param key Parameter key
	 * @return List of strings or null if not found or wrong type
	 */
	@SuppressWarnings("unchecked")
	protected List<String> extractStringArray(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof List)
		{
			List<String> result = new java.util.ArrayList<>();
			for (Object item : (List<?>)value)
			{
				if (item != null)
				{
					result.add(item.toString());
				}
			}
			return result;
		}
		return null;
	}
}
