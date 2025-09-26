package com.servoy.eclipse.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
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

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Handler for the createValueList tool
	 */
	private McpSchema.CallToolResult handleCreateValueList(Object exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("=== DEBUG: handleCreateValueList called ===");

		// Extract parameters from the request
		String name = "DefaultValueList";
		List<String> valuesList = null; // Optional array of values

		try
		{
			Map<String, Object> args = request.arguments();
			System.out.println("DEBUG: Raw arguments: " + args);

			if (args != null)
			{
				System.out.println("DEBUG: Arguments map is not null, size: " + args.size());
				System.out.println("DEBUG: Available keys: " + args.keySet());

				// Extract required 'name' parameter
				if (args.containsKey("name"))
				{
					Object nameObj = args.get("name");
					System.out.println("DEBUG: Found 'name' parameter, raw value: " + nameObj + " (type: " +
						(nameObj != null ? nameObj.getClass().getSimpleName() : "null") + ")");
					if (nameObj != null)
					{
						name = nameObj.toString();
						System.out.println("DEBUG: Extracted name: '" + name + "'");
					}
				}
				else
				{
					System.out.println("DEBUG: No 'name' parameter found, using default: " + name);
				}

				// Extract optional 'values' parameter (JSON array)
				if (args.containsKey("values"))
				{
					Object valuesObj = args.get("values");
					System.out.println("DEBUG: Found 'values' parameter, raw value: " + valuesObj + " (type: " +
						(valuesObj != null ? valuesObj.getClass().getSimpleName() : "null") + ")");

					if (valuesObj instanceof List< ? >)
					{
						System.out.println("DEBUG: Values object is a List, processing...");
						valuesList = ((List< ? >)valuesObj).stream()
							.map(Object::toString)
							.collect(java.util.stream.Collectors.toList());
						System.out.println("DEBUG: Processed values list: " + valuesList + " (size: " + valuesList.size() + ")");
					}
					else
					{
						System.out.println("DEBUG: Values object is NOT a List, it's: " + (valuesObj != null ? valuesObj.getClass().getSimpleName() : "null"));
					}
				}
				else
				{
					System.out.println("DEBUG: No 'values' parameter found");
				}
			}
			else
			{
				System.out.println("DEBUG: Arguments map is null!");
			}
		}
		catch (Exception e)
		{
			System.out.println("DEBUG: Exception during parameter extraction: " + e.getMessage());
			e.printStackTrace();
		}

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

		try
		{
			System.out.println("DEBUG: Creating ValueList with name: '" + name + "'");
			ValueList myVL = servoyModel.getActiveProject().getSolution().createNewValueList(servoyModel.getNameValidator(), name);
			System.out.println("DEBUG: ValueList created: " + (myVL != null ? "SUCCESS" : "NULL"));

			// If values were provided, add them to the value list
			if (valuesList != null && !valuesList.isEmpty())
			{
				System.out.println("DEBUG: Processing " + valuesList.size() + " values...");
				// Process the array of values
				StringBuilder customValues = new StringBuilder();
				for (String value : valuesList)
				{
					System.out.println("DEBUG: Processing value: '" + value + "'");
					if (value != null && !value.trim().isEmpty())
					{
						if (customValues.length() > 0)
						{
							customValues.append("\n");
						}
						customValues.append(value.trim());
						System.out.println("DEBUG: Added value to customValues");
					}
					else
					{
						System.out.println("DEBUG: Skipped empty/null value");
					}
				}
				System.out.println("DEBUG: Final customValues string: '" + customValues.toString() + "'");

				// Set the custom values on the value list
				System.out.println("DEBUG: About to set custom values on ValueList...");
				myVL.setCustomValues(customValues.toString());
				System.out.println("DEBUG: Custom values set on value list");
				
				// Verify the values were actually set
				String retrievedValues = myVL.getCustomValues();
				System.out.println("DEBUG: Retrieved custom values from ValueList: '" + retrievedValues + "'");
				
				// Check if they match what we set
				boolean valuesMatch = customValues.toString().equals(retrievedValues);
				System.out.println("DEBUG: Values match check: " + valuesMatch);
				
				if (!valuesMatch)
				{
					System.out.println("DEBUG: WARNING - Set values don't match retrieved values!");
					System.out.println("DEBUG: Expected: '" + customValues.toString() + "'");
					System.out.println("DEBUG: Actual: '" + retrievedValues + "'");
				}
				else
				{
					System.out.println("DEBUG: SUCCESS - Values were set correctly on ValueList!");
				}
			}
			else
			{
				System.out.println("DEBUG: No values to process (valuesList is null or empty)");
				// Check if ValueList has any default values
				String defaultValues = myVL.getCustomValues();
				System.out.println("DEBUG: ValueList default custom values: '" + defaultValues + "'");
			}

		}
		catch (RepositoryException e)
		{
			e.printStackTrace();
		}
		System.out.println("DEBUG: Preparing result message...");
		String resultMessage = "Value list '" + name + "' created successfully";
		if (valuesList != null && !valuesList.isEmpty())
		{
			resultMessage += " with values: " + String.join(", ", valuesList);
		}
		System.out.println("DEBUG: Final result message: '" + resultMessage + "'");

		System.out.println("DEBUG: Building MCP result...");
		McpSchema.CallToolResult result = McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
		System.out.println("DEBUG: MCP result built successfully, returning...");
		System.out.println("=== DEBUG: handleCreateValueList completed ===");

		return result;
	}

}
