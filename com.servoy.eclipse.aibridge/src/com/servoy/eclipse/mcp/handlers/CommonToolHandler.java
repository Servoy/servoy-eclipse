package com.servoy.eclipse.mcp.handlers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Common tools handler - shared tools used by multiple intent domains
 * Tools: queryDatabaseSchema, getTableColumns
 */
public class CommonToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "CommonToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("queryDatabaseSchema", new ToolHandlerRegistry.ToolDefinition(
			"Queries the database schema for available servers, tables, and columns. Optional: serverName (string) - filter by specific database server, tableName (string) - filter by specific table.",
			this::handleQueryDatabaseSchema));

		tools.put("getTableColumns", new ToolHandlerRegistry.ToolDefinition(
			"Retrieves detailed column information for a specific database table. Required: serverName (string) - database server name, tableName (string) - table name.",
			this::handleGetTableColumns));

		return tools;
	}

	/**
	 * Register all common tools with MCP server
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
	// TOOL: queryDatabaseSchema
	// =============================================

	private McpSchema.CallToolResult handleQueryDatabaseSchema(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
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

			// Validate serverName is required
			if (serverName == null || serverName.trim().isEmpty())
			{
				errorMessage = "The 'serverName' argument is required. Cannot enumerate database servers - please specify the server name.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			IServerInternal server = null;
			try
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				server = (IServerInternal)serverManager.getServer(serverName, false, false);
			}
			catch (Exception e)
			{
				ServoyLog.logError("[CommonToolHandler] Failed to get server: " + e.getMessage(), e);
			}

			if (server == null)
			{
				errorMessage = "Database server '" + serverName + "' not found. Check that the server is configured and valid.";
			}
			else
			{
				if (tableName != null && !tableName.trim().isEmpty())
				{
					// Query specific table
					ITable table = server.getTable(tableName);

					if (table == null)
					{
						errorMessage = "Table '" + tableName + "' not found in server '" + serverName + "'.";
					}
					else
					{
						resultBuilder.append("Table: ").append(table.getSQLName()).append("\n");
						resultBuilder.append("DataSource: ").append(table.getDataSource()).append("\n\n");

						// Primary Key columns
						List<Column> pkColumns = table.getRowIdentColumns();
						resultBuilder.append("Primary Key Columns:\n");
						if (pkColumns != null && !pkColumns.isEmpty())
						{
							for (Column col : pkColumns)
							{
								resultBuilder.append("  - ").append(col.getName()).append("\n");
							}
						}
						else
						{
							resultBuilder.append("  (none)\n");
						}

						// Find incoming foreign keys using ColumnInfo metadata
						resultBuilder.append("\nIncoming Foreign Keys (tables referencing this table):\n");
						String targetSqlName = table.getSQLName();
						List<String> allTables = server.getTableNames(false);
						int fkCount = 0;

						for (String tName : allTables)
						{
							ITable t = server.getTable(tName);
							if (t == null) continue;

							Collection<Column> columns = t.getColumns();
							for (Column col : columns)
							{
								ColumnInfo colInfo = col.getColumnInfo();
								if (colInfo != null)
								{
									String fkTarget = colInfo.getForeignType();
									if (fkTarget != null && fkTarget.equals(targetSqlName))
									{
										fkCount++;
										resultBuilder.append("  ").append(fkCount).append(". Table: ").append(t.getSQLName());
										resultBuilder.append(" → Column: ").append(col.getName());
										resultBuilder.append(" (FK to this table)\n");
									}
								}
							}
						}

						if (fkCount == 0)
						{
							resultBuilder.append("  (none)\n");
						}
					}
				}

				else
				{
					// Analyze all tables and show comprehensive relationship analysis
					List<String> tables = server.getTableNames(false);

					resultBuilder.append("Database Server: ").append(serverName).append("\n");
					resultBuilder.append("Tables (").append(tables.size()).append("):\n");

					for (String tName : tables)
					{
						resultBuilder.append("  - ").append(tName).append("\n");
					}

					// Analyze ALL foreign key relationships (explicit FK metadata)
					resultBuilder.append("\n=== EXPLICIT FOREIGN KEY RELATIONSHIPS ===\n\n");
					int totalFKs = 0;

					for (String tName : tables)
					{
						ITable t = server.getTable(tName);
						if (t == null) continue;

						Collection<Column> columns = t.getColumns();
						for (Column col : columns)
						{
							ColumnInfo colInfo = col.getColumnInfo();
							if (colInfo != null)
							{
								String fkTarget = colInfo.getForeignType();
								if (fkTarget != null && !fkTarget.trim().isEmpty())
								{
									totalFKs++;
									resultBuilder.append(totalFKs).append(". ");
									resultBuilder.append(t.getSQLName()).append(".").append(col.getName());
									resultBuilder.append(" → ").append(fkTarget);
									resultBuilder.append("\n");
								}
							}
						}
					}

					if (totalFKs == 0)
					{
						resultBuilder.append("(No explicit FK metadata found)\n");
					}

					// Find potential relations based on PK matching
					resultBuilder.append("\n=== POTENTIAL RELATIONS (PK column name + type matching) ===\n\n");
					int potentialCount = 0;

					// For each table's PK, find matching columns in other tables
					for (String tName : tables)
					{
						ITable t = server.getTable(tName);
						if (t == null) continue;

						List<Column> pkCols = t.getRowIdentColumns();
						if (pkCols == null || pkCols.isEmpty()) continue;

						for (Column pkCol : pkCols)
						{
							String pkName = pkCol.getName();
							ColumnType pkType = pkCol.getColumnType();

							// Look for this PK column in OTHER tables
							for (String otherTableName : tables)
							{
								if (otherTableName.equalsIgnoreCase(tName)) continue; // Skip same table

								ITable otherTable = server.getTable(otherTableName);
								if (otherTable == null) continue;

								Collection<Column> otherColumns = otherTable.getColumns();
								for (Column otherCol : otherColumns)
								{
									// Check if column name and type match the PK
									if (otherCol.getName().equalsIgnoreCase(pkName) && pkType.equals(otherCol.getColumnType()))
									{
										// Check if this is already an explicit FK
										ColumnInfo colInfo = otherCol.getColumnInfo();
										boolean isExplicitFK = false;
										if (colInfo != null)
										{
											String fkTarget = colInfo.getForeignType();
											isExplicitFK = (fkTarget != null && !fkTarget.trim().isEmpty());
										}

										if (!isExplicitFK)
										{
											potentialCount++;
											resultBuilder.append(potentialCount).append(". ");
											resultBuilder.append(otherTable.getSQLName()).append(".").append(otherCol.getName());
											resultBuilder.append(" → ").append(t.getSQLName()).append(".").append(pkName);
											resultBuilder.append(" (PK match)\n");
										}
									}
								}
							}
						}
					}

					if (potentialCount == 0)
					{
						resultBuilder.append("(No potential relations found)\n");
					}
				}
			}
		}
		catch (RepositoryException e)
		{
			errorMessage = "Repository error: " + e.getMessage();
			ServoyLog.logError("[CommonToolHandler] " + errorMessage, e);
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[CommonToolHandler] Error in handleQueryDatabaseSchema: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: getTableColumns
	// =============================================

	private McpSchema.CallToolResult handleGetTableColumns(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
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

			IServerInternal server = null;
			try
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				server = (IServerInternal)serverManager.getServer(serverName, false, false);
			}
			catch (Exception e)
			{
				ServoyLog.logError("[CommonToolHandler] Error getting server: " + e.getMessage(), e);
			}

			if (server == null)
			{
				errorMessage = "Database server '" + serverName + "' not found.";
			}
			else
			{
				ITable table = server.getTable(tableName);

				if (table == null)
				{
					errorMessage = "Table '" + tableName + "' not found in server '" + serverName + "'.";
				}
				else
				{
					resultBuilder.append("Table: ").append(table.getSQLName()).append("\n");
					resultBuilder.append("DataSource: ").append(table.getDataSource()).append("\n\n");

					// Get all columns with detailed information
					resultBuilder.append("Columns:\n\n");
					Collection<Column> columns = table.getColumns();

					if (columns != null && !columns.isEmpty())
					{
						int colNum = 1;
						List<Column> pkColumns = table.getRowIdentColumns();
						Set<String> pkNames = new java.util.HashSet<>();
						if (pkColumns != null)
						{
							for (Column pkCol : pkColumns)
							{
								pkNames.add(pkCol.getName());
							}
						}

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
		catch (RepositoryException e)
		{
			errorMessage = "Repository error: " + e.getMessage();
			ServoyLog.logError("[CommonToolHandler] " + errorMessage, e);
		}
		catch (Exception e)
		{
			errorMessage = "Error: " + e.getMessage();
			ServoyLog.logError("[CommonToolHandler] Error in handleGetTableColumns: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
