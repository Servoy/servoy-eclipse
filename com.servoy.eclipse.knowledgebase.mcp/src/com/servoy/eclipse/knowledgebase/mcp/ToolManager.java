package com.servoy.eclipse.knowledgebase.mcp;

/**
 * Central manager for MCP tool operations.
 * Provides facade for accessing tool handlers.
 * 
 * @author Servoy
 * @since 2026.3
 */
public class ToolManager
{
	/**
	 * Get all tool handlers for MCP registration.
	 * Called by aibridge's McpServletProvider to register tools.
 * 
 * @return Array of all tool handler instances
 */
public static IToolHandler[] getHandlers()
{
return ToolHandlerRegistry.getHandlers();
}
}
