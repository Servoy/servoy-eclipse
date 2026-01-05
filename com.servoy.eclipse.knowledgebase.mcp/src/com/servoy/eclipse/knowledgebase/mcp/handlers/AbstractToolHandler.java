package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.model.util.ServoyLog;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Abstract base class for all tool handlers.
 * Provides common functionality for parameter extraction, result building,
 * UI thread execution, and tool registration.
 */
public abstract class AbstractToolHandler implements IToolHandler
{
	/**
	 * Register all tools provided by this handler.
	 * Default implementation uses getToolDefinitions() map-based approach.
	 */
	@Override
	public void registerTools(McpSyncServer server)
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> definitions = getToolDefinitions();
		if (definitions != null && !definitions.isEmpty())
		{
			for (Map.Entry<String, ToolHandlerRegistry.ToolDefinition> entry : definitions.entrySet())
			{
				ToolHandlerRegistry.registerTool(
					server,
					entry.getKey(),
					entry.getValue().description,
					entry.getValue().handler);
			}
		}
	}

	/**
	 * Get tool definitions for this handler.
	 * Override this to provide map-based tool registration.
	 * 
	 * @return Map of tool name to ToolDefinition, or null if using manual registration
	 */
	protected Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		return new LinkedHashMap<>();
	}

	// =============================================
	// PARAMETER EXTRACTION UTILITIES
	// =============================================

	/**
	 * Extract string parameter with default value.
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
	 * Extract string parameter (no default).
	 */
	protected String extractString(Map<String, Object> args, String key)
	{
		return extractString(args, key, null);
	}

	/**
	 * Extract integer parameter with default value.
	 */
	protected int extractInt(Map<String, Object> args, String key, int defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		if (value instanceof Number number)
		{
			return number.intValue();
		}
		if (value != null)
		{
			try
			{
				return Integer.parseInt(value.toString());
			}
			catch (NumberFormatException e)
			{
				return defaultValue;
			}
		}
		return defaultValue;
	}

	/**
	 * Extract boolean parameter with default value.
	 */
	protected boolean extractBoolean(Map<String, Object> args, String key, boolean defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		if (value instanceof Boolean boolValue)
		{
			return boolValue;
		}
		if (value != null)
		{
			return Boolean.parseBoolean(value.toString());
		}
		return defaultValue;
	}

	/**
	 * Extract map parameter.
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
	 * Extract string list parameter.
	 */
	@SuppressWarnings("unchecked")
	protected List<String> extractStringList(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof List< ? >)
		{
			List<String> result = new java.util.ArrayList<>();
			for (Object item : (List< ? >)value)
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

	// =============================================
	// RESULT BUILDERS
	// =============================================

	/**
	 * Create success result with message.
	 */
	protected McpSchema.CallToolResult successResult(String message)
	{
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(message)))
			.build();
	}

	/**
	 * Create error result with message.
	 */
	protected McpSchema.CallToolResult errorResult(String message)
	{
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent("Error: " + message)))
			.isError(true)
			.build();
	}

	/**
	 * Create approval needed result with message.
	 * Used for context-aware operations requiring user confirmation.
	 */
	protected McpSchema.CallToolResult approvalNeededResult(String message)
	{
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(message)))
			.build();
	}

	// =============================================
	// UI THREAD EXECUTION
	// =============================================

	/**
	 * Execute operation on UI thread and handle exceptions.
	 * Simplifies the common pattern of syncExec with exception handling.
	 * 
	 * @param operation Operation to execute
	 * @param errorContext Context description for error logging
	 * @return CallToolResult with success or error
	 */
	protected McpSchema.CallToolResult executeOnUIThread(Callable<String> operation, String errorContext)
	{
		final String[] result = new String[1];
		final Exception[] exception = new Exception[1];

		Display.getDefault().syncExec(() -> {
			try
			{
				result[0] = operation.call();
			}
			catch (Exception e)
			{
				exception[0] = e;
			}
		});

		if (exception[0] != null)
		{
			ServoyLog.logError(errorContext, exception[0]);
			return errorResult(exception[0].getMessage());
		}

		return successResult(result[0]);
	}

	// =============================================
	// VALIDATION UTILITIES
	// =============================================

	/**
	 * Validate required string parameter.
	 * 
	 * @param value Parameter value
	 * @param paramName Parameter name for error message
	 * @return Error message if invalid, null if valid
	 */
	protected String validateRequired(String value, String paramName)
	{
		if (value == null || value.trim().isEmpty())
		{
			return "'" + paramName + "' parameter is required";
		}
		return null;
	}

	/**
	 * Validate enum parameter against allowed values.
	 * 
	 * @param value Parameter value
	 * @param paramName Parameter name for error message
	 * @param allowedValues Array of allowed values
	 * @return Error message if invalid, null if valid
	 */
	protected String validateEnum(String value, String paramName, String... allowedValues)
	{
		if (value == null)
		{
			return null; // Use validateRequired for required checks
		}

		for (String allowed : allowedValues)
		{
			if (allowed.equals(value))
			{
				return null;
			}
		}

		return "Invalid " + paramName + " value: '" + value + "'. Must be one of: " + String.join(", ", allowedValues);
	}

	/**
	 * Collect multiple validation errors into single message.
	 * 
	 * @param errors Array of error messages (null entries are ignored)
	 * @return Combined error message, or null if no errors
	 */
	protected String combineErrors(String... errors)
	{
		StringBuilder combined = new StringBuilder();
		for (String error : errors)
		{
			if (error != null)
			{
				if (combined.length() > 0)
				{
					combined.append("; ");
				}
				combined.append(error);
			}
		}
		return combined.length() > 0 ? combined.toString() : null;
	}
}
