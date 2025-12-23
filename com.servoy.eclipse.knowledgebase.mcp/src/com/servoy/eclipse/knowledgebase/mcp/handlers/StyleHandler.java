package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.FormService;
import com.servoy.eclipse.knowledgebase.mcp.services.StyleService;
import com.servoy.eclipse.model.util.ServoyLog;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Style handler - CRUD operations for CSS/LESS styles
 * Tools: addStyle, getStyle, listStyles, deleteStyle
 */
public class StyleHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "StyleHandler";
	}

	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("addStyle", new ToolHandlerRegistry.ToolDefinition(
			"Adds or updates a CSS class in a LESS file. " +
				"Required: className (string - without dot), cssContent (string - CSS rules). " +
				"Optional: lessFileName (string - file to add style to, defaults to <solution-name>.less). " +
				"If lessFileName is provided and different from main solution file, import is automatically added.",
			this::handleAddStyle));

		tools.put("getStyle", new ToolHandlerRegistry.ToolDefinition(
			"Gets the CSS content of a class from a LESS file. " +
				"Required: className (string - without dot). " +
				"Optional: lessFileName (string - file to search in, defaults to <solution-name>.less).",
			this::handleGetStyle));

		tools.put("listStyles", new ToolHandlerRegistry.ToolDefinition(
			"Lists all CSS class names in a LESS file. " +
				"Optional: lessFileName (string - file to list from, defaults to <solution-name>.less). " +
				"Returns: Comma-separated list of class names.",
			this::handleListStyles));

		tools.put("deleteStyle", new ToolHandlerRegistry.ToolDefinition(
			"Deletes a CSS class from a LESS file. " +
				"Required: className (string - without dot). " +
				"Optional: lessFileName (string - file to delete from, defaults to <solution-name>.less).",
			this::handleDeleteStyle));

		return tools;
	}

	@Override
	public void registerTools(McpSyncServer server)
	{
		for (Map.Entry<String, ToolHandlerRegistry.ToolDefinition> entry : getToolDefinitions().entrySet())
		{
			ToolHandlerRegistry.registerTool(
				server,
				entry.getKey(),
				entry.getValue().description,
				entry.getValue().handler);
		}
	}

	// =============================================
	// TOOL: addStyle
	// =============================================

	private McpSchema.CallToolResult handleAddStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[StyleHandler] handleAddStyle CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String cssContent = extractString(args, "cssContent", null);
			String lessFileName = extractString(args, "lessFileName", null);

			// Validate required parameters
			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			if (cssContent == null || cssContent.trim().isEmpty())
			{
				return errorResult("'cssContent' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				String projectPath = getProjectPath();
				String solutionName = getSolutionName();
				
				String error = StyleService.addOrUpdateStyle(projectPath, solutionName, lessFileName, className, cssContent);

				if (error != null)
				{
					return errorResult(error);
				}

				String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
					? lessFileName 
					: solutionName + ".less";
				
				if (!targetFile.endsWith(".less"))
				{
					targetFile += ".less";
				}
				
				// Refresh any open form to reflect style changes
				refreshCurrentForm(targetFile);
				
				return successResult("Successfully added/updated style '." + className + "' in " + targetFile);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error adding style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleAddStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: getStyle
	// =============================================

	private McpSchema.CallToolResult handleGetStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[StyleHandler] handleGetStyle CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String lessFileName = extractString(args, "lessFileName", null);

			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				String projectPath = getProjectPath();
				String solutionName = getSolutionName();
				
				String result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);

				return successResult(result);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error getting style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleGetStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: listStyles
	// =============================================

	private McpSchema.CallToolResult handleListStyles(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[StyleHandler] handleListStyles CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String lessFileName = extractString(args, "lessFileName", null);

			try
			{
				String projectPath = getProjectPath();
				String solutionName = getSolutionName();
				
				String result = StyleService.listStyles(projectPath, solutionName, lessFileName);

				return successResult(result);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error listing styles", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleListStyles", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: deleteStyle
	// =============================================

	private McpSchema.CallToolResult handleDeleteStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[StyleHandler] handleDeleteStyle CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String lessFileName = extractString(args, "lessFileName", null);

			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				String projectPath = getProjectPath();
				String solutionName = getSolutionName();
				
				String error = StyleService.deleteStyle(projectPath, solutionName, lessFileName, className);

				if (error != null)
				{
					return errorResult(error);
				}

				String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
					? lessFileName 
					: solutionName + ".less";
				
				if (!targetFile.endsWith(".less"))
				{
					targetFile += ".less";
				}
				
				// Refresh any open form to reflect style changes
				refreshCurrentForm(targetFile);
				
				return successResult("Successfully deleted style '." + className + "' from " + targetFile);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error deleting style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// HELPER METHODS
	// =============================================

	private String getProjectPath() throws Exception
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		if (servoyModel == null || servoyModel.getActiveProject() == null)
		{
			throw new Exception("No active Servoy project found");
		}
		return servoyModel.getActiveProject().getProject().getLocation().toOSString();
	}

	private String getSolutionName() throws Exception
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		if (servoyModel == null || servoyModel.getActiveProject() == null)
		{
			throw new Exception("No active Servoy project found");
		}
		if (servoyModel.getActiveProject().getSolution() == null)
		{
			throw new Exception("No active solution found");
		}
		return servoyModel.getActiveProject().getSolution().getName();
	}

	private String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key)) return defaultValue;
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	private McpSchema.CallToolResult successResult(String message)
	{
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(message)))
			.build();
	}

	private McpSchema.CallToolResult errorResult(String message)
	{
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent("Error: " + message)))
			.isError(true)
			.build();
	}
	
	/**
	 * Refreshes the currently open form (if any) to reflect style changes.
	 * This triggers a resource change notification without closing/reopening the editor.
	 * 
	 * @param lessFileName Name of the LESS file that was modified
	 */
	private void refreshCurrentForm(String lessFileName)
	{
		try
		{
			System.err.println("[StyleHandler] Triggering refresh after style change in: " + lessFileName);
			
			String projectPath = getProjectPath();
			
			// Refresh on UI thread
			Display.getDefault().asyncExec(() -> {
				try
				{
					boolean refreshed = FormService.refreshFormAfterStyleChange(projectPath, lessFileName);
					if (refreshed)
					{
						System.err.println("[StyleHandler] Style refresh triggered successfully");
					}
					else
					{
						System.err.println("[StyleHandler] Style refresh could not be triggered");
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError("[StyleHandler] Error triggering style refresh: " + e.getMessage(), e);
				}
			});
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleHandler] Error in refreshCurrentForm: " + e.getMessage(), e);
		}
	}
}
