package com.servoy.eclipse.mcp;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.mcp.ai.PromptEnricher;

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

		System.out.println("\n[MCP] ========================================");
		System.out.println("[MCP] Received prompt: \"" + prompt + "\"");
		System.out.println("[MCP] ========================================");

		try
		{
			// Use AI-powered prompt enricher (with ONNX embeddings)
			PromptEnricher enricher = new PromptEnricher();
			String result = enricher.processPrompt(prompt);

			if (result.equals("PASS_THROUGH"))
			{
				System.out.println("[MCP] Result: PASS_THROUGH (not Servoy-related)");
			}
			else
			{
				System.out.println("[MCP] Result: ENRICHED (" + result.length() + " chars)");
			}
			System.out.println("[MCP] ========================================\n");
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

}
