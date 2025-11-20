package com.servoy.eclipse.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.mcp.ai.PromptEnricher;
import com.servoy.eclipse.mcp.handlers.CommonToolHandler;
import com.servoy.eclipse.mcp.handlers.RelationToolHandler;
import com.servoy.eclipse.mcp.handlers.ValueListToolHandler;
import com.servoy.eclipse.model.util.ServoyLog;

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

		// Register value list tools
		ValueListToolHandler.registerTools(server);

		// Register relation tools
		RelationToolHandler.registerTools(server);

		// Register common tools
		CommonToolHandler.registerTools(server);

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
			ServoyLog.logError("[MCP] Error in AI processing: " + e.getMessage());
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("PASS_THROUGH")))
				.build();
		}
	}

}
