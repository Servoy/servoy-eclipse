package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.DatabaseSchemaService;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Database tools handler - tools for querying database schema information
 * Tools: listTables, getTableInfo
 */
public class DatabaseToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "DatabaseToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("listTables", new ToolHandlerRegistry.ToolDefinition(
			"Lists all tables in a database server. Required: serverName (string) - database server name.",
			this::handleListTables));

		tools.put("getTableInfo", new ToolHandlerRegistry.ToolDefinition(
			"Retrieves comprehensive information about a database table including columns, primary keys, and metadata. Required: serverName (string) - database server name, tableName (string) - table name.",
			this::handleGetTableInfo));

		return tools;
	}

	/**
	 * Register all database tools with MCP server
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
	// TOOL: listTables
	// =============================================

	private McpSchema.CallToolResult handleListTables(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String serverName = null;
		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			Map<String, Object> args = request.arguments();

			if (args != null && args.containsKey("serverName"))
			{
				Object serverObj = args.get("serverName");
				if (serverObj != null)
				{
					serverName = serverObj.toString();
				}
			}

			// Validate required parameter
			if (serverName == null || serverName.trim().isEmpty())
			{
				errorMessage = "The 'serverName' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Use DatabaseSchemaService to get server
			IServerInternal server = DatabaseSchemaService.getServer(serverName);

			if (server == null)
			{
				errorMessage = "Database server '" + serverName + "' not found.";
			}
			else
			{
				List<String> tables = DatabaseSchemaService.getTableNames(server);

				resultBuilder.append("Database Server: ").append(serverName).append("\n");
				resultBuilder.append("Tables (").append(tables.size()).append("):\n\n");

				if (tables.isEmpty())
				{
					resultBuilder.append("(No tables found)\n");
				}
				else
				{
					for (String tableName : tables)
					{
						resultBuilder.append("  - ").append(tableName).append("\n");
					}
				}
			}
		}
		catch (Exception e)
		{
			errorMessage = "Error: " + e.getMessage();
			ServoyLog.logError("[DatabaseToolHandler] Error in handleListTables: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: getTableInfo
	// =============================================

	private McpSchema.CallToolResult handleGetTableInfo(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String serverName = null;
		String tableName = null;
		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			Map<String, Object> args = request.arguments();

			if (args != null)
			{
				if (args.containsKey("serverName"))
				{
					Object serverObj = args.get("serverName");
					if (serverObj != null)
					{
						serverName = serverObj.toString();
					}
				}

				if (args.containsKey("tableName"))
				{
					Object tableObj = args.get("tableName");
					if (tableObj != null)
					{
						tableName = tableObj.toString();
					}
				}
			}

			// Validate required parameters
			if (serverName == null || serverName.trim().isEmpty())
			{
				errorMessage = "The 'serverName' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			if (tableName == null || tableName.trim().isEmpty())
			{
				errorMessage = "The 'tableName' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Use DatabaseSchemaService to get server and table
			IServerInternal server = DatabaseSchemaService.getServer(serverName);

			if (server == null)
			{
				errorMessage = "Database server '" + serverName + "' not found.";
			}
			else
			{
				ITable table = DatabaseSchemaService.getTable(server, tableName);

				if (table == null)
				{
					errorMessage = "Table '" + tableName + "' not found in server '" + serverName + "'.";
				}
				else
				{
					resultBuilder.append("Table: ").append(table.getSQLName()).append("\n");
					resultBuilder.append("DataSource: ").append(table.getDataSource()).append("\n\n");

					// Get all columns with detailed information using service
					resultBuilder.append("Columns:\n\n");
					Collection<Column> columns = DatabaseSchemaService.getColumns(table);

					if (columns != null && !columns.isEmpty())
					{
						int colNum = 1;
						Set<String> pkNames = DatabaseSchemaService.getPrimaryKeyNames(table);

						for (Column col : columns)
						{
							resultBuilder.append(colNum).append(". ");
							resultBuilder.append("Name: ").append(col.getName()).append("\n");

							String colTypeName = col.getColumnType() != null ? col.getColumnType().toString() : "UNKNOWN";
							resultBuilder.append("   Type: ").append(colTypeName).append("\n");

							// Primary key status
							boolean isPK = pkNames.contains(col.getName());
							resultBuilder.append("   Primary Key: ").append(isPK).append("\n");

							resultBuilder.append("\n");
							colNum++;
						}
					}
					else
					{
						resultBuilder.append("(No columns found)\n");
					}
				}
			}
		}
		catch (Exception e)
		{
			errorMessage = "Error: " + e.getMessage();
			ServoyLog.logError("[DatabaseToolHandler] Error in handleGetTableInfo: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
