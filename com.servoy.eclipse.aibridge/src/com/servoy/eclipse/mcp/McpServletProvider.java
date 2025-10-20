package com.servoy.eclipse.mcp;

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
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;

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

		Tool tool = McpSchema.Tool.builder().inputSchema(new JsonSchema("object", null, null, null, null, null)).name("getForms") //.outputSchema(outputSchema)
			.description("Getting all the forms in the current solution").build();

		SyncToolSpecification syncToolSpecification = SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
			return McpSchema.CallToolResult.builder().content(List.of(new TextContent("FormA"))).build();
		}).build();
		server.addTool(syncToolSpecification);

		// openValueList tool
		Tool openValueListTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("openValueList")
			.description("Opens an existing value list or creates a new value list. Required: name (string). Optional: values (array of strings) - only needed when creating a new value list.")
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

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Unified handler for the valueList tool - opens existing value list or creates new one
	 */
	private McpSchema.CallToolResult handleOpenValueList(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		List<String> valuesList = null;
		String errorMessage = null;
		boolean isCreate = false;

		try
		{
			Map<String, Object> args = request.arguments();
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

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Check if value list already exists
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList myVL = servoyModel.getActiveProject().getEditingSolution().getValueList(name);

			if (myVL == null)
			{
				// Value list doesn't exist - create it
				isCreate = true;
				myVL = servoyModel.getActiveProject().getEditingSolution().createNewValueList(servoyModel.getNameValidator(), name);

				// Set custom values if provided
				if (valuesList != null && !valuesList.isEmpty())
				{
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

			// Open editor on UI thread
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
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Value list '" + name + "' created successfully" : "Value list '" + name + "' opened in editor.");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	/**
	 * Unified handler for the relation tool - opens existing relation or creates new one
	 */
	private McpSchema.CallToolResult handleOpenRelation(Object exchange, McpSchema.CallToolRequest request)
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
						errorMessage = "Invalid primaryDataSource format: '" + primaryDataSource + "'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
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
						errorMessage = "Invalid foreignDataSource format: '" + foreignDataSource + "'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
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
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Relation '" + name + "' created successfully (from " + primaryDataSource + " to " + foreignDataSource + ")" : "Relation '" + name + "' opened in editor.");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

}
