package com.servoy.eclipse.mcp.handlers;

import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.mcp.services.FormFileService;
import com.servoy.eclipse.model.util.ServoyLog;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Component handler - Bootstrap component operations
 * Tools: addButton
 */
public class ComponentToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "ComponentToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("addButton", new ToolHandlerRegistry.ToolDefinition(
			"Adds a bootstrap button component to a form. Required: formName (string), buttonName (string), text (string). Optional: cssPosition (string, format: 'top,right,bottom,left,width,height').",
			this::handleAddButton));

		return tools;
	}

	/**
	 * Register all component tools with MCP server
	 */
	@Override
	public void registerTools(McpSyncServer server)
	{
		// Map-based approach: iterate through tool definitions and register each
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
	// TOOL: addButton
	// =============================================

	private McpSchema.CallToolResult handleAddButton(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String formName = null;
		String buttonName = null;
		String buttonText = null;
		String cssPosition = null;
		String errorMessage = null;

		try
		{
			Map<String, Object> args = request.arguments();

			if (args != null)
			{
				// Extract formName parameter (required)
				if (args.containsKey("formName"))
				{
					Object formObj = args.get("formName");
					if (formObj != null)
					{
						formName = formObj.toString();
					}
				}

				// Extract buttonName parameter (required)
				if (args.containsKey("buttonName"))
				{
					Object nameObj = args.get("buttonName");
					if (nameObj != null)
					{
						buttonName = nameObj.toString();
					}
				}

				// Extract text parameter (required)
				if (args.containsKey("text"))
				{
					Object textObj = args.get("text");
					if (textObj != null)
					{
						buttonText = textObj.toString();
					}
				}

				// Extract cssPosition parameter (optional)
				if (args.containsKey("cssPosition"))
				{
					Object posObj = args.get("cssPosition");
					if (posObj != null)
					{
						cssPosition = posObj.toString();
					}
				}
			}

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				errorMessage = "The 'formName' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			if (buttonName == null || buttonName.trim().isEmpty())
			{
				errorMessage = "The 'buttonName' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			if (buttonText == null)
			{
				errorMessage = "The 'text' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// If cssPosition not provided, use default auto-positioning
			if (cssPosition == null || cssPosition.trim().isEmpty())
			{
				// Default position: top-left area with standard button size
				cssPosition = "39,-1,-1,60,80,30";
				ServoyLog.logInfo("[ComponentToolHandler] Using default cssPosition: " + cssPosition);
			}

			// Get active project path
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			if (servoyModel == null || servoyModel.getActiveProject() == null)
			{
				errorMessage = "No active Servoy project found.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			String projectPath = servoyModel.getActiveProject().getProject().getLocation().toOSString();

			// Call FormFileService to add button
			boolean success = FormFileService.addButtonToForm(projectPath, formName, buttonName, buttonText, cssPosition);

			if (!success)
			{
				errorMessage = "Failed to add button. Check that form '" + formName + "' exists and uses CSS positioning.";
			}
			else
			{
				ServoyLog.logInfo("[ComponentToolHandler] Button '" + buttonName + "' added successfully to form '" + formName + "'");
			}
		}
		catch (Exception e)
		{
			errorMessage = "Error: " + e.getMessage();
			ServoyLog.logError("[ComponentToolHandler] Error in handleAddButton: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage
			: "Button '" + buttonName + "' added successfully to form '" + formName + "' with text '" + buttonText + "'.";

		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
