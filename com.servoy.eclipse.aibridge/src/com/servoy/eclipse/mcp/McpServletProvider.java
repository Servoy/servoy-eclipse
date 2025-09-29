package com.servoy.eclipse.mcp;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
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

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Handler for the createValueList tool
	 */
	private McpSchema.CallToolResult handleCreateValueList(Object exchange, McpSchema.CallToolRequest request)
	{
		String name = "DefaultValueList";
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
			servoyModel.getActiveProject().saveEditingSolutionNodes(new com.servoy.j2db.persistence.IPersist[] { myVL }, true);
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

}
