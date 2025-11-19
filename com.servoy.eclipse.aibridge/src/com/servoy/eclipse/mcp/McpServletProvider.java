package com.servoy.eclipse.mcp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;
import org.eclipse.swt.widgets.Display;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.base.query.IQueryConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.ai.PromptEnricher;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.JsonSchema;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

public class McpServletProvider implements IServicesProvider
{
	final PromptEnricher enricher = new PromptEnricher();

	@Override
	public Set<ServletInstance> getServletInstances(String context)
	{
		JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
		HttpServletStreamableServerTransportProvider transportProvider = HttpServletStreamableServerTransportProvider.builder()
			.jsonMapper(jsonMapper).mcpEndpoint("/mcp").build();
		McpSyncServer server = McpServer.sync(transportProvider).jsonMapper(jsonMapper).jsonSchemaValidator(new DefaultJsonSchemaValidator())
			.capabilities(ServerCapabilities.builder()
				.resources(Boolean.FALSE, Boolean.TRUE) // Enable resource support
				.tools(Boolean.TRUE) // Enable tool support
				.prompts(Boolean.TRUE) // Enable prompt support
				.logging() // Enable logging support
				.completions() // Enable completions support
				.build())
			.build();

		// processPrompt tool - AI-powered intent detection and enrichment
		Tool processPromptTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("processPrompt")
			.description(
				"Process user prompts for Servoy operations. This tool analyzes the user's request and either handles it or returns PASS_THROUGH for non-Servoy tasks. Required: prompt (string) - the complete user message.")
			.build();

		SyncToolSpecification processPromptSpec = SyncToolSpecification.builder()
			.tool(processPromptTool)
			.callHandler(this::handleProcessPrompt)
			.build();
		server.addTool(processPromptSpec);

		// openValueList tool
		Tool openValueListTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("openValueList")
			.description(
				"Opens an existing value list or creates a new value list. Required: name (string). Optional: values (array of strings) - only needed when creating a new value list.")
			.build();

		SyncToolSpecification openValueListSpec = SyncToolSpecification.builder()
			.tool(openValueListTool)
			.callHandler(this::handleOpenValueList)
			.build();
		server.addTool(openValueListSpec);

		// openRelation tool
		Tool openRelationTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("openRelation")
			.description(
				"Opens an existing database relation or creates a new relation between two tables. Required: name (string). Optional (for creation): primaryDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), foreignDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), primaryColumn (string), foreignColumn (string) for column mapping.")
			.build();

		SyncToolSpecification openRelationSpec = SyncToolSpecification.builder()
			.tool(openRelationTool)
			.callHandler(this::handleOpenRelation)
			.build();
		server.addTool(openRelationSpec);

		// deleteRelation tool
		Tool deleteRelationTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("deleteRelation")
			.description(
				"Deletes an existing database relation. Required: name (string) - the name of the relation to delete.")
			.build();

		SyncToolSpecification deleteRelationSpec = SyncToolSpecification.builder()
			.tool(deleteRelationTool)
			.callHandler(this::handleDeleteRelation)
			.build();
		server.addTool(deleteRelationSpec);

		// listRelations tool
		Tool listRelationsTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("listRelations")
			.description(
				"Retrieves a list of all existing database relations in the active solution. No parameters required.")
			.build();

		SyncToolSpecification listRelationsSpec = SyncToolSpecification.builder()
			.tool(listRelationsTool)
			.callHandler(this::handleListRelations)
			.build();
		server.addTool(listRelationsSpec);

		// queryDatabaseSchema tool (skeleton)
		Tool queryDatabaseSchemaTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("queryDatabaseSchema")
			.description(
				"Queries the database schema for available servers, tables, and columns. Optional: serverName (string) - filter by specific database server, tableName (string) - filter by specific table.")
			.build();

		SyncToolSpecification queryDatabaseSchemaSpec = SyncToolSpecification.builder()
			.tool(queryDatabaseSchemaTool)
			.callHandler(this::handleQueryDatabaseSchema)
			.build();
		server.addTool(queryDatabaseSchemaSpec);

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Process the prompt using AI-powered intent detection and enrichment:
	 * 1. Use embedding-based intent detection (ONNX local model)
	 * 2. Enrich prompt with rules and examples based on detected intent
	 * 3. Return enriched prompt to Copilot for processing
	 */
	private McpSchema.CallToolResult handleProcessPrompt(Object exchange, McpSchema.CallToolRequest request)
	{
		String prompt = null;

		// Extract prompt from request
		Map<String, Object> args = request.arguments();
		if (args != null && args.containsKey("prompt"))
		{
			Object promptObj = args.get("prompt");
			if (promptObj != null)
			{
				prompt = promptObj.toString();
			}
		}

		if (prompt == null || prompt.trim().isEmpty())
		{
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Error: prompt parameter is required")))
				.build();
		}


		ServoyLog.logInfo("[MCP] Prompt: \"" + prompt + "\"");
		try
		{
			String result = enricher.processPrompt(prompt);

			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result)))
				.build();
		}
		catch (Exception e)
		{
			System.err.println("[MCP] Error in AI processing: " + e.getMessage());
			e.printStackTrace();
			System.out.println("[MCP] ========================================\n");
			// Fallback to pass-through on error
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("PASS_THROUGH")))
				.build();
		}
	}

	/**
	 * Unified handler for the valueList tool - opens existing value list or creates new one
	 */
	private McpSchema.CallToolResult handleOpenValueList(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] openValueList tool called");
		System.out.println("[MCP] ========================================");

		String name = null;
		List<String> valuesList = null;
		String errorMessage = null;
		boolean isCreate = false;

		try
		{
			Map<String, Object> args = request.arguments();
			System.out.println("[MCP] Arguments received: " + args);
			if (args != null)
			{
				if (args.containsKey("name"))
				{
					Object nameObj = args.get("name");
					if (nameObj != null)
					{
						name = nameObj.toString();
					}
				}
				if (args.containsKey("values"))
				{
					Object valuesObj = args.get("values");
					if (valuesObj instanceof List< ? >)
					{
						valuesList = ((List< ? >)valuesObj).stream()
							.map(Object::toString)
							.collect(java.util.stream.Collectors.toList());
					}
				}
			}

			System.out.println("[MCP] Extracted parameters:");
			System.out.println("[MCP]   name: " + name);
			System.out.println("[MCP]   values: " + valuesList);

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Check if value list already exists
			System.out.println("[MCP] Checking if value list '" + name + "' exists...");
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList myVL = servoyModel.getActiveProject().getEditingSolution().getValueList(name);

			if (myVL == null)
			{
				// Value list doesn't exist - create it
				System.out.println("[MCP] Value list not found, creating new one...");
				isCreate = true;
				myVL = servoyModel.getActiveProject().getEditingSolution().createNewValueList(servoyModel.getNameValidator(), name);
				System.out.println("[MCP] Value list created successfully");

				// Set custom values if provided
				if (valuesList != null && !valuesList.isEmpty())
				{
					System.out.println("[MCP] Setting custom values: " + valuesList);
					StringBuilder customValues = new StringBuilder();
					for (String value : valuesList)
					{
						if (value != null && !value.trim().isEmpty())
						{
							if (customValues.length() > 0)
							{
								customValues.append("\n");
							}
							customValues.append(value.trim());
						}
					}
					myVL.setCustomValues(customValues.toString());
				}
			}
			else
			{
				System.out.println("[MCP] Value list found, opening existing one");
			}

			// Open editor on UI thread
			System.out.println("[MCP] Opening value list editor...");
			final ValueList valueListToOpen = myVL;
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					EditorUtil.openValueListEditor(valueListToOpen, true);
				}
			});

		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			System.err.println("[MCP] !!!!! ERROR in handleOpenValueList !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Value list '" + name + "' created successfully" : "Value list '" + name + "' opened in editor.");
		System.out.println("[MCP] Result: " + resultMessage);
		System.out.println("[MCP] ========================================\n");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	/**
	 * Unified handler for the relation tool - opens existing relation or creates new one
	 */
	private McpSchema.CallToolResult handleOpenRelation(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] openRelation tool called");
		System.out.println("[MCP] ========================================");

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
			System.out.println("[MCP] Arguments received: " + args);
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

			System.out.println("[MCP] Extracted parameters:");
			System.out.println("[MCP]   name: " + name);
			System.out.println("[MCP]   primaryDataSource: " + primaryDataSource);
			System.out.println("[MCP]   foreignDataSource: " + foreignDataSource);
			System.out.println("[MCP]   primaryColumn: " + primaryColumn);
			System.out.println("[MCP]   foreignColumn: " + foreignColumn);

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				System.err.println("[MCP] ERROR: name parameter is required");
				System.out.println("[MCP] ========================================\n");
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Check if relation already exists
			System.out.println("[MCP] Checking if relation '" + name + "' exists...");
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);

			if (relation == null)
			{
				// Relation doesn't exist - try to create it
				System.out.println("[MCP] Relation not found, attempting to create...");
				isCreate = true;

				// Validate datasources are provided for creation
				if (primaryDataSource == null || primaryDataSource.trim().isEmpty())
				{
					errorMessage = "Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.";
					System.err.println("[MCP] ERROR: " + errorMessage);
					System.out.println("[MCP] ========================================\n");
					return McpSchema.CallToolResult.builder()
						.content(List.of(new TextContent(errorMessage)))
						.build();
				}
				if (foreignDataSource == null || foreignDataSource.trim().isEmpty())
				{
					errorMessage = "Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.";
					System.err.println("[MCP] ERROR: " + errorMessage);
					System.out.println("[MCP] ========================================\n");
					return McpSchema.CallToolResult.builder()
						.content(List.of(new TextContent(errorMessage)))
						.build();
				}

				// Auto-correct datasource format if needed
				System.out.println("[MCP] Validating datasource formats...");
				if (!primaryDataSource.startsWith("db:/"))
				{
					if (primaryDataSource.contains("/"))
					{
						primaryDataSource = "db:/" + primaryDataSource;
						System.out.println("[MCP] Auto-corrected primaryDataSource to: " + primaryDataSource);
					}
					else
					{
						errorMessage = "Invalid primaryDataSource format: '" + primaryDataSource +
							"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
						System.err.println("[MCP] ERROR: " + errorMessage);
						System.out.println("[MCP] ========================================\n");
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
						System.out.println("[MCP] Auto-corrected foreignDataSource to: " + foreignDataSource);
					}
					else
					{
						errorMessage = "Invalid foreignDataSource format: '" + foreignDataSource +
							"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
						System.err.println("[MCP] ERROR: " + errorMessage);
						System.out.println("[MCP] ========================================\n");
						return McpSchema.CallToolResult.builder()
							.content(List.of(new TextContent(errorMessage)))
							.build();
					}
				}

				// Create the relation
				System.out.println("[MCP] Creating relation with:");
				System.out.println("[MCP]   name: " + name);
				System.out.println("[MCP]   primaryDataSource: " + primaryDataSource);
				System.out.println("[MCP]   foreignDataSource: " + foreignDataSource);
				relation = servoyModel.getActiveProject().getEditingSolution().createNewRelation(
					servoyModel.getNameValidator(),
					name,
					primaryDataSource,
					foreignDataSource,
					IQueryConstants.LEFT_OUTER_JOIN);

				relation.setAllowCreationRelatedRecords(true);
				System.out.println("[MCP] Relation created successfully");

				// Add relation item (column mapping) if both columns are provided
				if (primaryColumn != null && !primaryColumn.trim().isEmpty() &&
					foreignColumn != null && !foreignColumn.trim().isEmpty())
				{
					try
					{
						System.out.println("[MCP] Adding column mapping: " + primaryColumn + " -> " + foreignColumn);
						ITable primaryTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(primaryDataSource);
						ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(foreignDataSource);
						Column primaryCol = primaryTable.getColumn(primaryColumn);
						Column foreignCol = foreignTable.getColumn(foreignColumn);

						relation.createNewRelationItems(
							new IDataProvider[] { primaryCol },
							new int[] { com.servoy.base.query.IBaseSQLCondition.EQUALS_OPERATOR },
							new Column[] { foreignCol });
						System.out.println("[MCP] Column mapping added successfully");
					}
					catch (Exception e)
					{
						System.err.println("[MCP] Warning: Could not add column mapping - " + e.getMessage());
						// Silently ignore - editor will open and user can add columns manually
					}
				}
			}
			else
			{
				System.out.println("[MCP] Relation found, opening existing one");
			}

			// Open editor on UI thread
			System.out.println("[MCP] Opening relation editor...");
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
			System.err.println("[MCP] !!!!! REPOSITORY ERROR !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			System.err.println("[MCP] !!!!! ERROR in handleOpenRelation !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Relation '" + name + "' created successfully (from " + primaryDataSource + " to " + foreignDataSource + ")"
				: "Relation '" + name + "' opened in editor.");
		System.out.println("[MCP] Result: " + resultMessage);
		System.out.println("[MCP] ========================================\n");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	/**
	 * Handler for deleteRelation tool - deletes an existing relation
	 */
	private McpSchema.CallToolResult handleDeleteRelation(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] deleteRelation tool called");
		System.out.println("[MCP] ========================================");

		String name = null;
		String errorMessage = null;

		try
		{
			Map<String, Object> args = request.arguments();
			System.out.println("[MCP] Arguments received: " + args);

			if (args != null && args.containsKey("name"))
			{
				Object nameObj = args.get("name");
				if (nameObj != null)
				{
					name = nameObj.toString();
				}
			}

			System.out.println("[MCP] Extracted parameters:");
			System.out.println("[MCP]   name: " + name);

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				System.out.println("[MCP] Error: " + errorMessage);
				System.out.println("[MCP] ========================================\n");
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Find the relation
			System.out.println("[MCP] Looking for relation '" + name + "'...");
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Relation relation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);

			if (relation == null)
			{
				errorMessage = "Relation '" + name + "' not found.";
				System.out.println("[MCP] Error: " + errorMessage);
			}
			else
			{
				System.out.println("[MCP] Relation found, deleting...");
				servoyModel.getActiveProject().getEditingSolution().removeChild(relation);
				System.out.println("[MCP] Relation deleted successfully");
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			System.err.println("[MCP] !!!!! ERROR in handleDeleteRelation !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}

		String resultMessage = errorMessage != null ? errorMessage : "Relation '" + name + "' deleted successfully.";
		System.out.println("[MCP] Result: " + resultMessage);
		System.out.println("[MCP] ========================================\n");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	/**
	 * Handler for listRelations tool - retrieves all relations in the active solution
	 */
	private McpSchema.CallToolResult handleListRelations(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] listRelations tool called");
		System.out.println("[MCP] ========================================");

		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			System.out.println("[MCP] Retrieving all relations from active solution...");
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
			System.out.println("[MCP] Found " + count + " relation(s)");
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			System.err.println("[MCP] !!!!! ERROR in handleListRelations !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		System.out.println("[MCP] Result: " + (errorMessage != null ? errorMessage : "List retrieved successfully"));
		System.out.println("[MCP] ========================================\n");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	/**
	 * Handler for queryDatabaseSchema tool - queries database schema for servers, tables, columns
	 * SKELETON IMPLEMENTATION - TO BE COMPLETED
	 */
	private McpSchema.CallToolResult handleQueryDatabaseSchema(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] queryDatabaseSchema tool called");
		System.out.println("[MCP] ========================================");

		String serverName = null;
		String tableName = null;
		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			Map<String, Object> args = request.arguments();
			System.out.println("[MCP] Arguments received: " + args);

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

			System.out.println("[MCP] Extracted parameters:");
			System.out.println("[MCP]   serverName: " + serverName);
			System.out.println("[MCP]   tableName: " + tableName);

			// Validate serverName is required
			if (serverName == null || serverName.trim().isEmpty())
			{
				errorMessage = "The 'serverName' argument is required. Cannot enumerate database servers - please specify the server name.";
				System.out.println("[MCP] Error: " + errorMessage);
				System.out.println("[MCP] ========================================\n");
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			System.out.println("[MCP] Querying database schema via ApplicationServerRegistry...");

			IServerInternal server = null;
			try
			{
				System.out.println("[MCP] Getting server manager from ApplicationServerRegistry...");
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();

				System.out.println("[MCP] Getting server: " + serverName);
				server = (IServerInternal)serverManager.getServer(serverName, false, false);

				if (server != null)
				{
					System.out.println("[MCP] SUCCESS: Server found: " + serverName);
				}
				else
				{
					System.out.println("[MCP] Server is null from ApplicationServerRegistry");
				}
			}
			catch (Exception e)
			{
				System.err.println("[MCP] ApplicationServerRegistry approach failed: " + e.getMessage());
				e.printStackTrace();
			}

			if (server == null)
			{
				errorMessage = "Database server '" + serverName + "' not found. Check that the server is configured and valid.";
				System.out.println("[MCP] Error: " + errorMessage);
			}
			else
			{
				System.out.println("[MCP] Server found via ApplicationServerRegistry: " + serverName);

				if (tableName != null && !tableName.trim().isEmpty())
				{
					// Query specific table
					System.out.println("[MCP] Querying table: " + tableName);
					ITable table = server.getTable(tableName);

					if (table == null)
					{
						errorMessage = "Table '" + tableName + "' not found in server '" + serverName + "'.";
						System.out.println("[MCP] Error: " + errorMessage);
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

						System.out.println("[MCP] Found " + pkColumns.size() + " PK column(s) and " + fkCount + " incoming FK(s)");
					}
				}

				else
				{
					// Analyze all tables and show comprehensive relationship analysis
					System.out.println("[MCP] Analyzing all tables and relationships in server: " + serverName);
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

					System.out
						.println("[MCP] Found " + tables.size() + " table(s), " + totalFKs + " explicit FK(s), " + potentialCount + " potential relation(s)");
				}
			}
		}
		catch (RepositoryException e)
		{
			errorMessage = "Repository error: " + e.getMessage();
			System.err.println("[MCP] !!!!! REPOSITORY ERROR !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			System.err.println("[MCP] !!!!! ERROR in handleQueryDatabaseSchema !!!!!");
			System.err.println("[MCP] Error: " + errorMessage);
			e.printStackTrace();
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		System.out.println("[MCP] Result: " + (errorMessage != null ? errorMessage : "Schema query successful"));
		System.out.println("[MCP] ========================================\n");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

}
