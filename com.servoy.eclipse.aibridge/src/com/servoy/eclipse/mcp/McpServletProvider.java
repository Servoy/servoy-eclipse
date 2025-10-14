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

//		Map<String, Object> outputSchema = Map.of(
//			"type", "object",
//			"properties", Map.of(
//				"data", Map.of(
//					"type", "array",
//					"items", Map.of(
//						"type", "object",
//						"properties", Map.of(
//							"name", Map.of("type", "string"),
//							"datasource", Map.of("type", "string"))))));


		Tool tool = McpSchema.Tool.builder().inputSchema(new JsonSchema("object", null, null, null, null, null)).name("getForms") //.outputSchema(outputSchema)
			.description("Getting all the forms in the current solution").build();

		SyncToolSpecification syncToolSpecification = SyncToolSpecification.builder().tool(tool).callHandler((exchange, request) -> {
//			List<Map<String, String>> lst = List.of(Map.of("name", "forma", "datasource", "db:/servera/tablea"),
//				Map.of("name", "formb", "datasource", "db:/servera/tableb"));
//			return McpSchema.CallToolResult.builder().structuredContent(lst).build();
			return McpSchema.CallToolResult.builder().content(List.of(new TextContent("FormA"))).build();
		}).build();
		server.addTool(syncToolSpecification);

		// createValueList tool
		Tool createValueListTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("createValueList")
			.description("Creates a new value list with the specified name and optional array of values")
			.build();

		SyncToolSpecification createValueListSpec = SyncToolSpecification.builder()
			.tool(createValueListTool)
			.callHandler(this::handleCreateValueList)
			.build();
		server.addTool(createValueListSpec);

		// createRelation tool
		Tool createRelationTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("createRelation")
			.description(
				"Creates a new database relation between two tables. Required: name (string), primaryDataSource (format: db:/server_name/table_name'), foreignDataSource (format: 'db:/server_name/table_name'). Optional: primaryColumn (string), foreignColumn (string) for column mapping.")
			.build();

		SyncToolSpecification createRelationSpec = SyncToolSpecification.builder()
			.tool(createRelationTool)
			.callHandler(this::handleCreateRelation)
			.build();
		server.addTool(createRelationSpec);

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Handler for the createValueList tool
	 */
	private McpSchema.CallToolResult handleCreateValueList(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		List<String> valuesList = null;
		String errorMessage = null;

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
			if (name == null || name.trim().isEmpty())
			{
				throw new IllegalArgumentException("The 'name' argument is required.");
			}
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList myVL = servoyModel.getActiveProject().getEditingSolution().createNewValueList(servoyModel.getNameValidator(), name);
			Iterator<ValueList> it = servoyModel.getActiveProject().getSolution().getValueLists(false);
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


		String resultMessage = errorMessage != null ? errorMessage : "Value list '" + name + "' created successfully";
		McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
		return result;
	}

	/**
	 * Handler for the createRelation tool
	 */
	private McpSchema.CallToolResult handleCreateRelation(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		String primaryDataSource = null;
		String foreignDataSource = null;
		String primaryColumn = null;
		String foreignColumn = null;
		String errorMessage = null;

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

			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}
			if (primaryDataSource == null || primaryDataSource.trim().isEmpty())
			{
				errorMessage = "The 'primaryDataSource' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}
			if (foreignDataSource == null || foreignDataSource.trim().isEmpty())
			{
				errorMessage = "The 'foreignDataSource' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Auto-correct datasource format if needed
			// If format is just "table_name", assume it needs "db:/example_data/table_name"
			// If format is "server/table", convert to "db:/server/table"
			if (!primaryDataSource.startsWith("db:/"))
			{
				
				primaryDataSource = "db:/" + primaryDataSource;
			}
			if (!foreignDataSource.startsWith("db:/"))
			{
				foreignDataSource = "db:/" + foreignDataSource;
			}

			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Relation existingRelation = servoyModel.getActiveProject().getEditingSolution().getRelation(name);
			if (existingRelation != null)
			{
				errorMessage = "Relation '" + name + "' already exists. Please use a different name or delete the existing relation first.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			Relation relation = servoyModel.getActiveProject().getEditingSolution().createNewRelation(
				servoyModel.getNameValidator(),
				name,
				primaryDataSource,
				foreignDataSource,
				IQueryConstants.LEFT_OUTER_JOIN);

			relation.setAllowCreationRelatedRecords(true);
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
			: "Relation '" + name + "' created successfully (from " + primaryDataSource + " to " + foreignDataSource + ")";
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

}
