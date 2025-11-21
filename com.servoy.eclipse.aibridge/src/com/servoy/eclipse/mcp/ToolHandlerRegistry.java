package com.servoy.eclipse.mcp;

import java.util.function.BiFunction;

import com.servoy.eclipse.mcp.handlers.CommonToolHandler;
import com.servoy.eclipse.mcp.handlers.RelationToolHandler;
import com.servoy.eclipse.mcp.handlers.ValueListToolHandler;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Central registry of all MCP tool handlers.
 * To add a new handler, simply add it to the array below.
 */
public class ToolHandlerRegistry
{
	/**
	 * Helper class to hold tool definition (description + handler).
	 * Used for map-based tool registration.
	 */
	public static class ToolDefinition
	{
		public final String description;
		public final BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler;

		public ToolDefinition(String description, BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler)
		{
			this.description = description;
			this.handler = handler;
		}
	}

	/**
	 * Get all registered tool handlers.
	 *
	 * ADD NEW HANDLERS HERE - just add one line to the array.
	 *
	 * @return Array of tool handler instances
	 */
	public static IToolHandler[] getHandlers()
	{
		// @formatter:off
		return new IToolHandler[] {
			new RelationToolHandler(),
			new ValueListToolHandler(),
			new CommonToolHandler()
		};
		// @formatter:on
	}

	/**
	 * Helper method to register a tool with the MCP server.
	 * Eliminates boilerplate code in handler classes.
	 *
	 * @param server The MCP server to register the tool with
	 * @param toolName The name of the tool
	 * @param description The tool description
	 * @param handler The handler function for this tool
	 */
	public static void registerTool(
		McpSyncServer server,
		String toolName,
		String description,
		BiFunction<McpSyncServerExchange, McpSchema.CallToolRequest, McpSchema.CallToolResult> handler)
	{
		Tool tool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name(toolName)
			.description(description)
			.build();

		SyncToolSpecification spec = SyncToolSpecification.builder()
			.tool(tool)
			.callHandler(handler)
			.build();

		server.addTool(spec);
	}
}
