package com.servoy.eclipse.knowledgebase.handlers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.IToolHandler;
import com.servoy.eclipse.knowledgebase.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.services.BootstrapComponentService;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Label component handler - Complete CRUD operations for bootstrap labels
 * Tools: addLabel, updateLabel, deleteLabel, listLabels, getLabelInfo
 */
public class LabelComponentHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "LabelComponentHandler";
	}

	/**
	 * Define all tools for label operations
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("addLabel", new ToolHandlerRegistry.ToolDefinition(
			"Adds a bootstrap label component to a form. " +
				"Required: formName (string), name (string), cssPosition (string). " +
				"cssPosition format: 'top,right,bottom,left,width,height' where first 4 values are DISTANCES from edges (not coordinates). " +
				"Use -1 for unconstrained edges. Example: '20,-1,-1,25,80,30' means 20px from top, 25px from left, 80x30 size. " +
				"Optional: text (string, default 'Label'), styleClass (string), " +
				"labelFor (string), showAs (string: 'text', 'html', 'trusted_html'), " +
				"enabled (boolean), visible (boolean), toolTipText (string).",
			this::handleAddLabel));

		tools.put("updateLabel", new ToolHandlerRegistry.ToolDefinition(
			"Updates an existing label component on a form. " +
				"Required: formName (string), name (string). " +
				"Optional: Any property to update - text, cssPosition, styleClass, labelFor, showAs, " +
				"enabled, visible, toolTipText. Only specified properties will be updated.",
			this::handleUpdateLabel));

		tools.put("deleteLabel", new ToolHandlerRegistry.ToolDefinition(
			"Deletes a label component from a form. " +
				"Required: formName (string), name (string).",
			this::handleDeleteLabel));

		tools.put("listLabels", new ToolHandlerRegistry.ToolDefinition(
			"Lists all label components in a form with their details. " +
				"Required: formName (string). " +
				"Returns: JSON array of label information (name, cssPosition, text, styleClass).",
			this::handleListLabels));

		tools.put("getLabelInfo", new ToolHandlerRegistry.ToolDefinition(
			"Gets detailed information about a specific label component. " +
				"Required: formName (string), name (string). " +
				"Returns: Full JSON object with all label properties.",
			this::handleGetLabelInfo));

		return tools;
	}

	/**
	 * Register all label tools with MCP server
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
	// TOOL: addLabel
	// =============================================

	private McpSchema.CallToolResult handleAddLabel(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[LabelComponentHandler] handleAddLabel CALLED");

		try
		{
			Map<String, Object> args = request.arguments();

			// Extract required parameters
			String formName = extractString(args, "formName", null);
			String name = extractString(args, "name", null);
			String cssPosition = extractString(args, "cssPosition", null);

			// Extract optional parameters
			String text = extractString(args, "text", "Label");
			String styleClass = extractString(args, "styleClass", null);
			String labelFor = extractString(args, "labelFor", null);
			String showAs = extractString(args, "showAs", null);
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
			if (labelFor != null) properties.put("labelFor", labelFor);
			if (showAs != null) properties.put("showAs", showAs);
			if (enabled != null) properties.put("enabled", enabled);
			if (visible != null) properties.put("visible", visible);
			if (toolTipText != null) properties.put("toolTipText", toolTipText);

			// Execute
			try
			{
				String projectPath = getProjectPath();
				String error = BootstrapComponentService.addComponentToForm(
					projectPath, formName, name, "bootstrapcomponents-label", cssPosition, properties);

				if (error != null)
				{
					return errorResult(error);
				}

				return successResult("Successfully added label '" + name + "' to form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error adding label", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleAddLabel", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: updateLabel
	// =============================================

	private McpSchema.CallToolResult handleUpdateLabel(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[LabelComponentHandler] handleUpdateLabel CALLED");

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
			String labelFor = extractString(args, "labelFor", null);
			String showAs = extractString(args, "showAs", null);
			Boolean enabled = extractBoolean(args, "enabled", null);
			Boolean visible = extractBoolean(args, "visible", null);
			String toolTipText = extractString(args, "toolTipText", null);

			if (text != null) updates.put("text", text);
			if (cssPosition != null) updates.put("cssPosition", cssPosition);
			if (styleClass != null) updates.put("styleClass", styleClass);
			if (labelFor != null) updates.put("labelFor", labelFor);
			if (showAs != null) updates.put("showAs", showAs);
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

				return successResult("Successfully updated label '" + name + "' on form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error updating label", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleUpdateLabel", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: deleteLabel
	// =============================================

	private McpSchema.CallToolResult handleDeleteLabel(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[LabelComponentHandler] handleDeleteLabel CALLED");

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

				return successResult("Successfully deleted label '" + name + "' from form '" + formName + "'");
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error deleting label", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteLabel", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: listLabels
	// =============================================

	private McpSchema.CallToolResult handleListLabels(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[LabelComponentHandler] handleListLabels CALLED");

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
				String result = BootstrapComponentService.listComponentsByType(projectPath, formName, "bootstrapcomponents-label");

				return successResult(result);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error listing labels", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleListLabels", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: getLabelInfo
	// =============================================

	private McpSchema.CallToolResult handleGetLabelInfo(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[LabelComponentHandler] handleGetLabelInfo CALLED");

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
				ServoyLog.logError("Error getting label info", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleGetLabelInfo", e);
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
							System.err.println("[LabelComponentHandler] Current form detected: " + result[0]);
						}
						else
						{
							System.err.println("[LabelComponentHandler] Active editor does not contain a form");
						}
					}
					else
					{
						System.err.println("[LabelComponentHandler] No active editor found");
					}
				}
				else
				{
					System.err.println("[LabelComponentHandler] No active workbench page found");
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
