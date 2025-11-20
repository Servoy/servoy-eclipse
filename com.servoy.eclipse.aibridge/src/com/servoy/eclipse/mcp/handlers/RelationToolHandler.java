package com.servoy.eclipse.mcp.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.base.query.IQueryConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

/**
 * Relations handler - all RELATIONS intent tools
 * Tools: openRelation, deleteRelation, listRelations
 */
public class RelationToolHandler
{
	/**
	 * Register all relation tools with MCP server
	 */
	public static void registerTools(McpSyncServer server)
	{
		registerOpenRelation(server);
		registerDeleteRelation(server);
		registerListRelations(server);
	}

	// =============================================
	// TOOL: openRelation
	// =============================================

	private static void registerOpenRelation(McpSyncServer server)
	{
		Tool tool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("openRelation")
			.description(
				"Opens an existing database relation or creates a new relation between two tables. Required: name (string). Optional (for creation): primaryDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), foreignDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), primaryColumn (string), foreignColumn (string) for column mapping.")
			.build();

		SyncToolSpecification spec = SyncToolSpecification.builder()
			.tool(tool)
			.callHandler(RelationToolHandler::handleOpenRelation)
			.build();

		server.addTool(spec);
	}

	private static McpSchema.CallToolResult handleOpenRelation(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		String primaryDataSource = null;
		String foreignDataSource = null;
		String primaryColumn = null;
		String foreignColumn = null;
		String errorMessage = null;
		boolean isCreate = false;

		try
		{
			Map<String, Object> args = request.arguments();
			if (args != null)
			{
				// Extract name parameter
				if (args.containsKey("name"))
				{
					Object nameObj = args.get("name");
					if (nameObj != null)
					{
						name = nameObj.toString();
					}
				}

				// Extract optional datasource parameters
				if (args.containsKey("primaryDataSource"))
				{
					Object primaryObj = args.get("primaryDataSource");
					if (primaryObj != null)
					{
						primaryDataSource = primaryObj.toString();
					}
				}

				if (args.containsKey("foreignDataSource"))
				{
					Object foreignObj = args.get("foreignDataSource");
					if (foreignObj != null)
					{
						foreignDataSource = foreignObj.toString();
					}
				}

				if (args.containsKey("primaryColumn"))
				{
					Object primaryColObj = args.get("primaryColumn");
					if (primaryColObj != null)
					{
						primaryColumn = primaryColObj.toString();
					}
				}

				if (args.containsKey("foreignColumn"))
				{
					Object foreignColObj = args.get("foreignColumn");
					if (foreignColObj != null)
					{
						foreignColumn = foreignColObj.toString();
					}
				}
			}

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Check if relation already exists
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);

			if (relation == null)
			{
				// Relation doesn't exist - try to create it
				ServoyLog.logInfo("[RelationToolHandler] Creating relation: " + name);
				isCreate = true;

				// Validate datasources are provided for creation
				if (primaryDataSource == null || primaryDataSource.trim().isEmpty())
				{
					errorMessage = "Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.";
					return McpSchema.CallToolResult.builder()
						.content(List.of(new TextContent(errorMessage)))
						.build();
				}
				if (foreignDataSource == null || foreignDataSource.trim().isEmpty())
				{
					errorMessage = "Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.";
					return McpSchema.CallToolResult.builder()
						.content(List.of(new TextContent(errorMessage)))
						.build();
				}

				// Auto-correct datasource format if needed
				if (!primaryDataSource.startsWith("db:/"))
				{
					if (primaryDataSource.contains("/"))
					{
						primaryDataSource = "db:/" + primaryDataSource;
					}
					else
					{
						errorMessage = "Invalid primaryDataSource format: '" + primaryDataSource +
							"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
						return McpSchema.CallToolResult.builder()
							.content(List.of(new TextContent(errorMessage)))
							.build();
					}
				}
				if (!foreignDataSource.startsWith("db:/"))
				{
					if (foreignDataSource.contains("/"))
					{
						foreignDataSource = "db:/" + foreignDataSource;
					}
					else
					{
						errorMessage = "Invalid foreignDataSource format: '" + foreignDataSource +
							"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
						return McpSchema.CallToolResult.builder()
							.content(List.of(new TextContent(errorMessage)))
							.build();
					}
				}

				// Create the relation
				relation = servoyModel.getActiveProject().getEditingSolution().createNewRelation(
					servoyModel.getNameValidator(),
					name,
					primaryDataSource,
					foreignDataSource,
					IQueryConstants.LEFT_OUTER_JOIN);

				relation.setAllowCreationRelatedRecords(true);

				// Add relation item (column mapping) if both columns are provided
				if (primaryColumn != null && !primaryColumn.trim().isEmpty() &&
					foreignColumn != null && !foreignColumn.trim().isEmpty())
				{
					try
					{
						ITable primaryTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(primaryDataSource);
						ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(foreignDataSource);
						Column primaryCol = primaryTable.getColumn(primaryColumn);
						Column foreignCol = foreignTable.getColumn(foreignColumn);

						relation.createNewRelationItems(
							new IDataProvider[] { primaryCol },
							new int[] { com.servoy.base.query.IBaseSQLCondition.EQUALS_OPERATOR },
							new Column[] { foreignCol });
					}
					catch (Exception e)
					{
						ServoyLog.logError("[RelationToolHandler] Could not add column mapping: " + e.getMessage());
						// Silently ignore - editor will open and user can add columns manually
					}
				}
			}

			// Open editor on UI thread
			final Relation relationToOpen = relation;
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					EditorUtil.openRelationEditor(relationToOpen, true);
				}
			});

		}
		catch (RepositoryException e)
		{
			errorMessage = "Repository error: " + e.getMessage();
			ServoyLog.logError("[RelationToolHandler] " + errorMessage, e);
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[RelationToolHandler] Error in handleOpenRelation: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Relation '" + name + "' created successfully (from " + primaryDataSource + " to " + foreignDataSource + ")"
				: "Relation '" + name + "' opened in editor.");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: deleteRelation
	// =============================================

	private static void registerDeleteRelation(McpSyncServer server)
	{
		Tool tool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("deleteRelation")
			.description(
				"Deletes an existing database relation. Required: name (string) - the name of the relation to delete.")
			.build();

		SyncToolSpecification spec = SyncToolSpecification.builder()
			.tool(tool)
			.callHandler(RelationToolHandler::handleDeleteRelation)
			.build();

		server.addTool(spec);
	}

	private static McpSchema.CallToolResult handleDeleteRelation(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		String errorMessage = null;

		try
		{
			Map<String, Object> args = request.arguments();

			if (args != null && args.containsKey("name"))
			{
				Object nameObj = args.get("name");
				if (nameObj != null)
				{
					name = nameObj.toString();
				}
			}

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Find the relation
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);

			if (relation == null)
			{
				errorMessage = "Relation '" + name + "' not found.";
			}
			else
			{
				servoyModel.getActiveProject().getEditingSolution().removeChild(relation);
				ServoyLog.logInfo("[RelationToolHandler] Deleted relation: " + name);
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[RelationToolHandler] Error in handleDeleteRelation: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : "Relation '" + name + "' deleted successfully.";
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: listRelations
	// =============================================

	private static void registerListRelations(McpSyncServer server)
	{
		Tool tool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("listRelations")
			.description(
				"Retrieves a list of all existing database relations in the active solution. No parameters required.")
			.build();

		SyncToolSpecification spec = SyncToolSpecification.builder()
			.tool(tool)
			.callHandler(RelationToolHandler::handleListRelations)
			.build();

		server.addTool(spec);
	}

	private static McpSchema.CallToolResult handleListRelations(Object exchange, McpSchema.CallToolRequest request)
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
			ServoyLog.logError("[RelationToolHandler] Error in handleListRelations: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
