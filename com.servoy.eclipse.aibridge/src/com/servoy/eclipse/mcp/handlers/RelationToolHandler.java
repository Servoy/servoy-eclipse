package com.servoy.eclipse.mcp.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.mcp.services.DatabaseSchemaService;
import com.servoy.eclipse.mcp.services.DatabaseSchemaService.ForeignKeyRelationship;
import com.servoy.eclipse.mcp.services.DatabaseSchemaService.PotentialRelationship;
import com.servoy.eclipse.mcp.services.RelationService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Relations handler - all RELATIONS intent tools
 * Tools: openRelation, getRelations, deleteRelations, discoverDbRelations
 */
public class RelationToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "RelationToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("openRelation", new ToolHandlerRegistry.ToolDefinition(
			"Opens an existing database relation or creates a new relation between two tables. " +
			"Required: name (string). " +
			"Required for creation: primaryDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), " +
			"foreignDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'). " +
			"Optional: primaryColumn (string), foreignColumn (string) for column mapping, " +
			"properties (object: map of relation properties - joinType: 'left outer'|'inner', " +
			"allowCreationRelatedRecords: boolean, allowParentDeleteWhenHavingRelatedRecords: boolean, " +
			"deleteRelatedRecords: boolean, initialSort: string, encapsulation: 'public'|'hide'|'module', " +
			"deprecated: string, comment: string).",
			this::handleOpenRelation));

		tools.put("getRelations", new ToolHandlerRegistry.ToolDefinition(
			"Lists all existing database relations in the active solution. No parameters required.",
			this::handleGetRelations));

		tools.put("deleteRelations", new ToolHandlerRegistry.ToolDefinition(
			"Deletes one or more existing database relations. Required: names (array of strings) - the names of the relations to delete.",
			this::handleDeleteRelations));

		tools.put("discoverDbRelations", new ToolHandlerRegistry.ToolDefinition(
			"Discovers potential database relations by analyzing foreign key relationships. " +
			"Required: serverName (string) - database server name. " +
			"Returns explicit FK constraints and potential relations based on PK name matching.",
			this::handleDiscoverDbRelations));

		return tools;
	}

	/**
	 * Register all relations tools with MCP server
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
	// HELPER METHODS - Parameter Extraction
	// =============================================
	
	private String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> extractMap(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof Map)
		{
			return (Map<String, Object>)value;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> extractStringArray(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof List)
		{
			List<String> result = new java.util.ArrayList<>();
			for (Object item : (List<?>)value)
			{
				if (item != null)
				{
					result.add(item.toString());
				}
			}
			return result;
		}
		return null;
	}

	// =============================================
	// TOOL: openRelation
	// =============================================

	private McpSchema.CallToolResult handleOpenRelation(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract parameters
			String name = extractString(args, "name", null);
			String primaryDataSource = extractString(args, "primaryDataSource", null);
			String foreignDataSource = extractString(args, "foreignDataSource", null);
			String primaryColumn = extractString(args, "primaryColumn", null);
			String foreignColumn = extractString(args, "foreignColumn", null);
			Map<String, Object> properties = extractMap(args, "properties");
			
			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = openOrCreateRelation(name, primaryDataSource, foreignDataSource, 
						primaryColumn, foreignColumn, properties);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error opening/creating relation: " + name, exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + exception[0].getMessage())))
					.isError(true)
					.build();
			}
			
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleOpenRelation", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	/**
	 * Opens an existing relation or creates a new one if it doesn't exist (with datasources provided).
	 * Supports updating properties on existing relations via properties map.
	 */
	private String openOrCreateRelation(String name, String primaryDataSource, String foreignDataSource,
		String primaryColumn, String foreignColumn, Map<String, Object> properties) throws RepositoryException
	{
		ServoyLog.logInfo("[RelationToolHandler] Processing relation: " + name);
		
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		
		if (servoyProject == null)
		{
			throw new RepositoryException("No active Servoy solution project found");
		}
		
		if (servoyProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get the Servoy Solution from the selected Servoy Project");
		}
		
		// Check if relation already exists
		Relation relation = servoyProject.getEditingSolution().getRelation(name);
		boolean isNewRelation = false;
		boolean propertiesModified = false;
		
		if (relation != null)
		{
			ServoyLog.logInfo("[RelationToolHandler] Relation exists: " + name);
			
			// Apply properties if provided (update existing relation)
			if (properties != null && !properties.isEmpty())
			{
				ServoyLog.logInfo("[RelationToolHandler] Updating relation properties");
				RelationService.updateRelationProperties(relation, properties);
				propertiesModified = true;
			}
		}
		else
		{
			// Relation doesn't exist - create it
			ServoyLog.logInfo("[RelationToolHandler] Relation doesn't exist, creating: " + name);
			
			// Validate datasources are provided for creation
			if (primaryDataSource == null || primaryDataSource.trim().isEmpty())
			{
				throw new RepositoryException("Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.");
			}
			if (foreignDataSource == null || foreignDataSource.trim().isEmpty())
			{
				throw new RepositoryException("Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.");
			}
			
			// Validate and correct datasource format
			primaryDataSource = RelationService.validateAndCorrectDataSource(primaryDataSource);
			foreignDataSource = RelationService.validateAndCorrectDataSource(foreignDataSource);
			
			// Create the relation using service
			relation = RelationService.createRelation(name, primaryDataSource, foreignDataSource, 
				primaryColumn, foreignColumn, properties);
			isNewRelation = true;
		}
		
		// Open editor on UI thread
		final Relation relationToOpen = relation;
		Display.getDefault().asyncExec(() -> {
			EditorUtil.openRelationEditor(relationToOpen, true);
		});
		
		// Build result message
		StringBuilder result = new StringBuilder();
		if (isNewRelation)
		{
			result.append("Relation '").append(name).append("' created successfully");
			result.append(" (from ").append(primaryDataSource).append(" to ").append(foreignDataSource).append(")");
			if (properties != null && properties.containsKey("joinType"))
			{
				result.append(" with join type: ").append(properties.get("joinType"));
			}
		}
		else
		{
			result.append("Relation '").append(name).append("' opened successfully");
			if (propertiesModified)
			{
				result.append(". Properties updated");
			}
		}
		
		return result.toString();
	}

	// =============================================
	// TOOL: deleteRelations
	// =============================================

	private McpSchema.CallToolResult handleDeleteRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract names array
			List<String> names = extractStringArray(args, "names");
			
			// Validate names is required
			if (names == null || names.isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'names' parameter is required (array of relation names)")))
					.isError(true)
					.build();
			}
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = deleteRelations(names);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error deleting relations", exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + exception[0].getMessage())))
					.isError(true)
					.build();
			}
			
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteRelations", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	/**
	 * Deletes one or more relations.
	 */
	private String deleteRelations(List<String> names) throws RepositoryException
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		
		if (servoyProject == null)
		{
			throw new RepositoryException("No active Servoy solution project found");
		}
		
		if (servoyProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get the Servoy Solution from the selected Servoy Project");
		}
		
		java.util.List<String> deletedRelations = new java.util.ArrayList<>();
		java.util.List<String> notFoundRelations = new java.util.ArrayList<>();
		
		for (String name : names)
		{
			if (name == null || name.trim().isEmpty())
			{
				continue;
			}
			
			Relation relation = servoyProject.getEditingSolution().getRelation(name);
			
			if (relation == null)
			{
				notFoundRelations.add(name);
			}
			else
			{
				servoyProject.getEditingSolution().removeChild(relation);
				deletedRelations.add(name);
				ServoyLog.logInfo("[RelationToolHandler] Deleted relation: " + name);
			}
		}
		
		// Build result message
		StringBuilder result = new StringBuilder();
		
		if (!deletedRelations.isEmpty())
		{
			result.append("Successfully deleted ").append(deletedRelations.size()).append(" relation(s): ");
			result.append(String.join(", ", deletedRelations));
		}
		
		if (!notFoundRelations.isEmpty())
		{
			if (result.length() > 0)
			{
				result.append("\n\n");
			}
			result.append("Relations not found (").append(notFoundRelations.size()).append("): ");
			result.append(String.join(", ", notFoundRelations));
		}
		
		if (deletedRelations.isEmpty() && notFoundRelations.isEmpty())
		{
			result.append("No relations specified for deletion");
		}
		
		return result.toString();
	}

	// =============================================
	// TOOL: getRelations
	// =============================================

	private McpSchema.CallToolResult handleGetRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Iterator<Relation> relations = servoyModel.getActiveProject().getEditingSolution().getRelations(true);

			int count = 0;
			resultBuilder.append("Relations in solution '").append(servoyModel.getActiveProject().getEditingSolution().getName()).append("':\n\n");

			while (relations.hasNext())
			{
				Relation relation = relations.next();
				count++;
				resultBuilder.append(count).append(". ").append(relation.getName());
				resultBuilder.append("\n   Primary: ").append(relation.getPrimaryDataSource());
				resultBuilder.append("\n   Foreign: ").append(relation.getForeignDataSource());
				resultBuilder.append("\n");
			}

			if (count == 0)
			{
				resultBuilder.append("No relations found.");
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[RelationToolHandler] Error in handleGetRelations: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: discoverDbRelations
	// =============================================

	private McpSchema.CallToolResult handleDiscoverDbRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
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

			// Validate serverName is required
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
				// Get table names for context
				List<String> tables = DatabaseSchemaService.getTableNames(server);

				resultBuilder.append("Database Server: ").append(serverName).append("\n");
				resultBuilder.append("Tables (").append(tables.size()).append("):\n");

				for (String tName : tables)
				{
					resultBuilder.append("  - ").append(tName).append("\n");
				}

				// Analyze explicit foreign key relationships using service
				resultBuilder.append("\n=== EXPLICIT FOREIGN KEY RELATIONSHIPS ===\n\n");
				List<ForeignKeyRelationship> explicitFKs = DatabaseSchemaService.getExplicitForeignKeys(server);

				if (explicitFKs.isEmpty())
				{
					resultBuilder.append("(No explicit FK metadata found)\n");
				}
				else
				{
					int fkNum = 1;
					for (ForeignKeyRelationship fk : explicitFKs)
					{
						resultBuilder.append(fkNum).append(". ");
						resultBuilder.append(fk.sourceTable).append(".").append(fk.sourceColumn);
						resultBuilder.append(" → ").append(fk.targetTable);
						resultBuilder.append("\n");
						fkNum++;
					}
				}

				// Find potential relations using service
				resultBuilder.append("\n=== POTENTIAL RELATIONS (PK column name + type matching) ===\n\n");
				List<PotentialRelationship> potentialRels = DatabaseSchemaService.getPotentialRelationships(server);

				if (potentialRels.isEmpty())
				{
					resultBuilder.append("(No potential relations found)\n");
				}
				else
				{
					int relNum = 1;
					for (PotentialRelationship rel : potentialRels)
					{
						resultBuilder.append(relNum).append(". ");
						resultBuilder.append(rel.sourceTable).append(".").append(rel.sourceColumn);
						resultBuilder.append(" → ").append(rel.targetTable).append(".").append(rel.targetColumn);
						resultBuilder.append(" (PK match)\n");
						relNum++;
					}
				}
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[RelationToolHandler] Error in handleDiscoverDbRelations: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
