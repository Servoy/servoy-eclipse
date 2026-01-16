package com.servoy.eclipse.knowledgebase.mcp.handlers.utility;

import com.servoy.eclipse.knowledgebase.mcp.handlers.AbstractToolHandler;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.DatabaseSchemaService;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * Database tools handler - tools for querying database schema information
 * Tools: listTables, getTableInfo
 */
public class DatabaseToolHandler extends AbstractToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "DatabaseToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	@Override
	protected Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
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

	// =============================================
	// TOOL: listTables
	// =============================================

	private McpSchema.CallToolResult handleListTables(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		String serverName = extractString(args, "serverName");

		// Validate required parameter
		String error = validateRequired(serverName, "serverName");
		if (error != null)
		{
			return errorResult(error);
		}

		try
		{
			IServerInternal server = DatabaseSchemaService.getServer(serverName);
			if (server == null)
			{
				return errorResult("Database server '" + serverName + "' not found");
			}

			List<String> tables = DatabaseSchemaService.getTableNames(server);
			StringBuilder result = new StringBuilder();
			result.append("Database Server: ").append(serverName).append("\n");
			result.append("Tables (").append(tables.size()).append("):\n\n");

			if (tables.isEmpty())
			{
				result.append("(No tables found)\n");
			}
			else
			{
				for (String tableName : tables)
				{
					result.append("  - ").append(tableName).append("\n");
				}
			}

			return successResult(result.toString());
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseToolHandler] Error in handleListTables", e);
			return errorResult(e.getMessage());
		}
	}

	// =============================================
	// TOOL: getTableInfo
	// =============================================

	private McpSchema.CallToolResult handleGetTableInfo(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		String serverName = extractString(args, "serverName");
		String tableName = extractString(args, "tableName");

		// Validate required parameters
		String error = combineErrors(
			validateRequired(serverName, "serverName"),
			validateRequired(tableName, "tableName"));
		if (error != null)
		{
			return errorResult(error);
		}

		try
		{
			IServerInternal server = DatabaseSchemaService.getServer(serverName);
			if (server == null)
			{
				return errorResult("Database server '" + serverName + "' not found");
			}

			ITable table = DatabaseSchemaService.getTable(server, tableName);
			if (table == null)
			{
				return errorResult("Table '" + tableName + "' not found in server '" + serverName + "'");
			}

			StringBuilder result = new StringBuilder();
			result.append("Table: ").append(table.getSQLName()).append("\n");
			result.append("DataSource: ").append(table.getDataSource()).append("\n\n");
			result.append("Columns:\n\n");

			Collection<Column> columns = DatabaseSchemaService.getColumns(table);
			if (columns != null && !columns.isEmpty())
			{
				int colNum = 1;
				Set<String> pkNames = DatabaseSchemaService.getPrimaryKeyNames(table);

				for (Column col : columns)
				{
					result.append(colNum).append(". ");
					result.append("Name: ").append(col.getName()).append("\n");

					String colTypeName = col.getColumnType() != null ? col.getColumnType().toString() : "UNKNOWN";
					result.append("   Type: ").append(colTypeName).append("\n");

					boolean isPK = pkNames.contains(col.getName());
					result.append("   Primary Key: ").append(isPK).append("\n\n");
					colNum++;
				}
			}
			else
			{
				result.append("(No columns found)\n");
			}

			return successResult(result.toString());
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseToolHandler] Error in handleGetTableInfo", e);
			return errorResult(e.getMessage());
		}
	}
}
