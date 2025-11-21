package com.servoy.eclipse.mcp;

import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Interface for MCP tool handlers.
 * Each handler implements this to register its tools with the MCP server.
 */
public interface IToolHandler
{
	/**
	 * Register all tools provided by this handler with the MCP server.
	 * 
	 * @param server The MCP server to register tools with
	 */
	void registerTools(McpSyncServer server);

	/**
	 * Get the handler name for logging purposes.
	 * 
	 * @return The handler name
	 */
	String getHandlerName();
}
