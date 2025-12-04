package com.servoy.eclipse.mcp.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.mcp.services.BootstrapComponentService;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Button component handler - Complete CRUD operations for bootstrap buttons
 * Tools: addButton, updateButton, deleteButton, listButtons, getButtonInfo
 */
public class ButtonComponentHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "ButtonComponentHandler";
	}

	/**
	 * Define all tools for button operations
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("addButton", new ToolHandlerRegistry.ToolDefinition(
			"Adds a bootstrap button component to a form. " +
				"Required: formName (string), name (string), cssPosition (string). " +
				"cssPosition format: 'top,right,bottom,left,width,height' where first 4 values are DISTANCES from edges (not coordinates). " +
				"Use -1 for unconstrained edges. Example: '20,-1,-1,25,80,30' means 20px from top, 25px from left, 80x30 size. " +
				"Optional: text (string, default 'Button'), styleClass (string), variant (string), " +
				"imageStyleClass (string - icon to the left), trailingImageStyleClass (string - icon to the right), " +
				"showAs (string: 'text', 'html', 'trusted_html'), tabSeq (number), " +
				"enabled (boolean), visible (boolean), toolTipText (string).",
			this::handleAddButton));

		tools.put("updateButton", new ToolHandlerRegistry.ToolDefinition(
			"Updates an existing button component on a form. " +
				"Required: formName (string), name (string). " +
				"Optional: Any property to update - text, cssPosition, styleClass, variant, imageStyleClass, " +
				"trailingImageStyleClass, showAs, tabSeq, enabled, visible, toolTipText. Only specified properties will be updated.",
			this::handleUpdateButton));

		tools.put("deleteButton", new ToolHandlerRegistry.ToolDefinition(
			"Deletes a button component from a form. " +
				"Required: formName (string), name (string).",
			this::handleDeleteButton));

		tools.put("listButtons", new ToolHandlerRegistry.ToolDefinition(
			"Lists all button components in a form with their details. " +
				"Required: formName (string). " +
				"Returns: JSON array of button information (name, cssPosition, text, styleClass).",
			this::handleListButtons));

		tools.put("getButtonInfo", new ToolHandlerRegistry.ToolDefinition(
			"Gets detailed information about a specific button component. " +
				"Required: formName (string), name (string). " +
				"Returns: Full JSON object with all button properties.",
			this::handleGetButtonInfo));

		return tools;
	}

	/**
	 * Register all button tools with MCP server
	 */
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
	// TOOL: addButton
	// =============================================

	private McpSchema.CallToolResult handleAddButton(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[ButtonComponentHandler] handleAddButton CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			// Extract required parameters
			String formName = extractString(args, "formName", null);
			String name = extractString(args, "name", null);
			String cssPosition = extractString(args, "cssPosition", null);

			// Extract optional parameters
			String text = extractString(args, "text", "Button");
			String styleClass = extractString(args, "styleClass", null);
			String variant = extractString(args, "variant", null);
			String imageStyleClass = extractString(args, "imageStyleClass", null);
			String trailingImageStyleClass = extractString(args, "trailingImageStyleClass", null);
			String showAs = extractString(args, "showAs", null);
			Integer tabSeq = extractInteger(args, "tabSeq", null);
			Boolean enabled = extractBoolean(args, "enabled", null);
			Boolean visible = extractBoolean(args, "visible", null);
			String toolTipText = extractString(args, "toolTipText", null);

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				return errorResult("'formName' parameter is required");
			}

			if (name == null || name.trim().isEmpty())
			{
				return errorResult("'name' parameter is required");
			}

			if (cssPosition == null || cssPosition.trim().isEmpty())
			{
				return errorResult("'cssPosition' parameter is required (format: 'top,right,bottom,left,width,height')");
			}

			// Build properties map
			Map<String, Object> properties = new HashMap<>();
			properties.put("text", text);
			if (styleClass != null) properties.put("styleClass", styleClass);
			if (variant != null) properties.put("variant", variant);
			if (imageStyleClass != null) properties.put("imageStyleClass", imageStyleClass);
			if (trailingImageStyleClass != null) properties.put("trailingImageStyleClass", trailingImageStyleClass);
			if (showAs != null) properties.put("showAs", showAs);
			if (tabSeq != null) properties.put("tabSeq", tabSeq);
			if (enabled != null) properties.put("enabled", enabled);
			if (visible != null) properties.put("visible", visible);
			if (toolTipText != null) properties.put("toolTipText", toolTipText);

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String error = BootstrapComponentService.addComponentToForm(
					projectPath, formName, name, "bootstrapcomponents-button", cssPosition, properties);

				if (error != null)
				{
					return errorResult(error);
				}

				return successResult("Successfully added button '" + name + "' to form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error adding button", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleAddButton", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: updateButton
	// =============================================

	private McpSchema.CallToolResult handleUpdateButton(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[ButtonComponentHandler] handleUpdateButton CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			// Extract required parameters
			String formName = extractString(args, "formName", null);
			String name = extractString(args, "name", null);

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				return errorResult("'formName' parameter is required");
			}

			if (name == null || name.trim().isEmpty())
			{
				return errorResult("'name' parameter is required");
			}

			// Extract optional update parameters
			Map<String, Object> updates = new HashMap<>();
			
			String text = extractString(args, "text", null);
			String cssPosition = extractString(args, "cssPosition", null);
			String styleClass = extractString(args, "styleClass", null);
			String variant = extractString(args, "variant", null);
			String imageStyleClass = extractString(args, "imageStyleClass", null);
			String trailingImageStyleClass = extractString(args, "trailingImageStyleClass", null);
			String showAs = extractString(args, "showAs", null);
			Integer tabSeq = extractInteger(args, "tabSeq", null);
			Boolean enabled = extractBoolean(args, "enabled", null);
			Boolean visible = extractBoolean(args, "visible", null);
			String toolTipText = extractString(args, "toolTipText", null);

			if (text != null) updates.put("text", text);
			if (cssPosition != null) updates.put("cssPosition", cssPosition);
			if (styleClass != null) updates.put("styleClass", styleClass);
			if (variant != null) updates.put("variant", variant);
			if (imageStyleClass != null) updates.put("imageStyleClass", imageStyleClass);
			if (trailingImageStyleClass != null) updates.put("trailingImageStyleClass", trailingImageStyleClass);
			if (showAs != null) updates.put("showAs", showAs);
			if (tabSeq != null) updates.put("tabSeq", tabSeq);
			if (enabled != null) updates.put("enabled", enabled);
			if (visible != null) updates.put("visible", visible);
			if (toolTipText != null) updates.put("toolTipText", toolTipText);

			if (updates.isEmpty())
			{
				return errorResult("No properties specified to update. Provide at least one property to update.");
			}

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String error = BootstrapComponentService.updateComponent(projectPath, formName, name, updates);

				if (error != null)
				{
					return errorResult(error);
				}

				return successResult("Successfully updated button '" + name + "' on form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error updating button", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleUpdateButton", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: deleteButton
	// =============================================

	private McpSchema.CallToolResult handleDeleteButton(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[ButtonComponentHandler] handleDeleteButton CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			// Extract required parameters
			String formName = extractString(args, "formName", null);
			String name = extractString(args, "name", null);

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				return errorResult("'formName' parameter is required");
			}

			if (name == null || name.trim().isEmpty())
			{
				return errorResult("'name' parameter is required");
			}

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String error = BootstrapComponentService.deleteComponent(projectPath, formName, name);

				if (error != null)
				{
					return errorResult(error);
				}

				return successResult("Successfully deleted button '" + name + "' from form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error deleting button", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteButton", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: listButtons
	// =============================================

	private McpSchema.CallToolResult handleListButtons(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[ButtonComponentHandler] handleListButtons CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String formName = extractString(args, "formName", null);

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				return errorResult("'formName' parameter is required");
			}

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String result = BootstrapComponentService.listComponentsByType(projectPath, formName, "bootstrapcomponents-button");

				return successResult(result);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error listing buttons", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleListButtons", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: getButtonInfo
	// =============================================

	private McpSchema.CallToolResult handleGetButtonInfo(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[ButtonComponentHandler] handleGetButtonInfo CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			String formName = extractString(args, "formName", null);
			String name = extractString(args, "name", null);

			// Validate required parameters
			if (formName == null || formName.trim().isEmpty())
			{
				return errorResult("'formName' parameter is required");
			}

			if (name == null || name.trim().isEmpty())
			{
				return errorResult("'name' parameter is required");
			}

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String result = BootstrapComponentService.getComponentInfo(projectPath, formName, name);

				return successResult(result);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error getting button info", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleGetButtonInfo", e);
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

	/**
	 * Gets the name of the currently opened form in the active editor.
	 * Returns null if no form is currently open in the editor.
	 * Must be called from UI thread or wrapped in Display.syncExec.
	 */
	private String getCurrentFormName()
	{
		final String[] result = new String[1];
		
		Display.getDefault().syncExec(() -> {
			try
			{
				IWorkbenchPage activePage = EditorUtil.getActivePage();
				if (activePage != null)
				{
					IEditorPart activeEditor = activePage.getActiveEditor();
					if (activeEditor != null)
					{
						Form form = EditorUtil.getForm(activeEditor);
						if (form != null)
						{
							result[0] = form.getName();
							System.out.println("[ButtonComponentHandler] Current form detected: " + result[0]);
						}
						else
						{
							System.out.println("[ButtonComponentHandler] Active editor does not contain a form");
						}
					}
					else
					{
						System.out.println("[ButtonComponentHandler] No active editor found");
					}
				}
				else
				{
					System.out.println("[ButtonComponentHandler] No active workbench page found");
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error detecting current form", e);
			}
		});
		
		return result[0];
	}

	private String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key)) return defaultValue;
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}

	private Boolean extractBoolean(Map<String, Object> args, String key, Boolean defaultValue)
	{
		if (args == null || !args.containsKey(key)) return defaultValue;
		Object value = args.get(key);
		if (value instanceof Boolean) return (Boolean)value;
		if (value != null) return Boolean.parseBoolean(value.toString());
		return defaultValue;
	}

	private Integer extractInteger(Map<String, Object> args, String key, Integer defaultValue)
	{
		if (args == null || !args.containsKey(key)) return defaultValue;
		Object value = args.get(key);
		if (value instanceof Number) return ((Number)value).intValue();
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
}
