package com.servoy.eclipse.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;

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
			.description("Creates a new value list with the specified name")
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
		// Extract the name parameter from the request
		String name = "DefaultValueList";
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
		}
		catch (Exception e)
		{
			// Handle parsing errors
		}

		// TODO: Implement actual value list creation logic here
		String resultMessage = "Value list '" + name + "' created successfully";
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

}
