package com.servoy.eclipse.knowledgebase.mcp.handlers.utility;

import com.servoy.eclipse.knowledgebase.mcp.handlers.AbstractToolHandler;

import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.ContextService;
import com.servoy.eclipse.model.nature.ServoyProject;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Context handler - manages which solution/module receives write operations.
 * Tools: getContext, setContext
 */
public class ContextToolHandler extends AbstractToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "ContextToolHandler";
	}

	@Override
	public void registerTools(McpSyncServer server)
	{
		// Tool: getContext
		ToolHandlerRegistry.registerTool(
			server,
			"getContext",
			"Gets the current context and available solutions/modules. " +
				"The current context determines where new items (relations, forms, valuelists) will be created. " +
				"No parameters required. " +
				"Returns: Current context ('active' or module name) and list of available contexts.",
			this::handleGetContext);

		// Tool: setContext
		ToolHandlerRegistry.registerTool(
			server,
			"setContext",
			"Sets the current context for write operations. " +
				"Required: context (string) - 'active' for active solution, or module name like 'Module_A'. " +
				"All subsequent create operations will target this context unless overridden. " +
				"Returns: Confirmation with new current context.",
			this::handleSetContext);
	}

	private McpSchema.CallToolResult handleGetContext(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject == null)
		{
			return errorResult("No active Servoy solution");
		}

		ContextService contextService = ContextService.getInstance();
		String currentContext = contextService.getCurrentContext();
		List<String> availableContexts = contextService.getAvailableContexts(activeProject);

		StringBuilder result = new StringBuilder();
		result.append("Current Context: ").append(currentContext).append("\n\n");
		result.append("Active Solution: ").append(activeProject.getProject().getName()).append("\n\n");
		result.append("Available Contexts:\n");
		for (String context : availableContexts)
		{
			String marker = context.equals(currentContext) ? " [CURRENT]" : "";
			result.append("  - ").append(context).append(marker).append("\n");
		}
		result.append("\nAll create operations (relations, forms, valuelists) will target: ").append(currentContext);

		return successResult(result.toString());
	}

	private McpSchema.CallToolResult handleSetContext(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		String targetContext = extractString(args, "context");

		// Validate required parameter
		String error = validateRequired(targetContext, "context");
		if (error != null)
		{
			return errorResult(error);
		}

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();

		if (activeProject == null)
		{
			return errorResult("No active Servoy solution");
		}

		// Validate context exists
		ContextService contextService = ContextService.getInstance();
		List<String> availableContexts = contextService.getAvailableContexts(activeProject);

		if (!availableContexts.contains(targetContext))
		{
			return errorResult("Context '" + targetContext + "' not found. Available: " + String.join(", ", availableContexts));
		}

		// Set context
		contextService.setCurrentContext(targetContext);

		String displayName = "active".equals(targetContext) ? activeProject.getProject().getName() + " (active solution)" : targetContext;

		return successResult("Context switched to: " + displayName + "\n\nAll subsequent create operations will target this context.");
	}
}
