package com.servoy.eclipse.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.mcp.ai.PromptEnricher;
import com.servoy.eclipse.mcp.ai.ServoyEmbeddingService;
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

		// getContextFor tool - NEW: Action list based context retrieval
		Tool getContextForTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("getContextFor")
			.description(
				"Retrieves Servoy documentation and tools for specified action queries. " +
				"Required: queries (array of strings) - action phrases like 'create form', 'add buttons', 'create relation'. " +
				"Each query should be a simple 2-4 word phrase describing one action type.")
			.build();

		SyncToolSpecification getContextForSpec = SyncToolSpecification.builder()
			.tool(getContextForTool)
			.callHandler(this::handleGetContextFor)
			.build();
		server.addTool(getContextForSpec);

		registerHandlers(server);


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

	/**
	 * NEW: Get context for action list queries.
	 * Receives array of action phrases, does similarity search for each,
	 * returns aggregated Servoy context.
	 */
	private McpSchema.CallToolResult handleGetContextFor(Object exchange, McpSchema.CallToolRequest request)
	{
		// Extract queries array from request
		List<String> queries = new ArrayList<>();
		Map<String, Object> args = request.arguments();

		if (args != null && args.containsKey("queries"))
		{
			Object queriesObj = args.get("queries");
			if (queriesObj instanceof List< ? >)
			{
				List< ? > queriesList = (List< ? >)queriesObj;
				for (Object query : queriesList)
				{
					if (query != null)
					{
						queries.add(query.toString());
					}
				}
			}
			else if (queriesObj != null)
			{
				// Try to parse as single query
				queries.add(queriesObj.toString());
			}
		}

		if (queries.isEmpty())
		{
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Error: queries parameter is required (array of strings)")))
				.isError(true)
				.build();
		}

		try
		{
			// Get embedding service
			ServoyEmbeddingService embeddingService = ServoyEmbeddingService.getInstance();

			// Track matched categories and their contexts
			Map<String, CategoryMatch> categoryMatches = new LinkedHashMap<>();

			// For each query, do similarity search
			for (String query : queries)
			{
				// Search with top 3 results to allow multiple category matches
				List<ServoyEmbeddingService.SearchResult> results = embeddingService.search(query, 3);

				for (ServoyEmbeddingService.SearchResult result : results)
				{
					String intent = result.metadata.get("intent");
					if (intent != null && !intent.equals("PASS_THROUGH"))
					{
						// Track this category
						if (!categoryMatches.containsKey(intent))
						{
							categoryMatches.put(intent, new CategoryMatch(intent, query, result.score));
						}
						else
						{
							// Update if better score
							CategoryMatch existing = categoryMatches.get(intent);
							if (result.score > existing.bestScore)
							{
								existing.bestScore = result.score;
								existing.matchedQuery = query;
							}
						}
					}
				}
			}

			// Build response with matched categories
			StringBuilder response = new StringBuilder();
			response.append("=== SERVOY CONTEXT FOR YOUR ACTION LIST ===\n\n");
			response.append("Analyzed ").append(queries.size()).append(" action queries.\n");
			response.append("Found ").append(categoryMatches.size()).append(" relevant Servoy categories.\n\n");

			if (categoryMatches.isEmpty())
			{
				response.append("⚠️ NO MATCHING SERVOY CATEGORIES FOUND\n\n");
				response.append("Your queries:\n");
				for (String query : queries)
				{
					response.append("  - \"").append(query).append("\"\n");
				}
				response.append("\nThese don't match any known Servoy categories.\n");
				response.append("Either this is not a Servoy-related request, or you need to rephrase your action queries.\n");
			}
			else
			{
				response.append("📋 MATCHED CATEGORIES:\n");
				int categoryNum = 1;
				for (CategoryMatch match : categoryMatches.values())
				{
					response.append("\n").append(categoryNum++).append(". Category: ").append(match.category).append("\n");
					response.append("   Matched query: \"").append(match.matchedQuery).append("\"\n");
					response.append("   Confidence: ").append(String.format("%.1f%%", match.bestScore * 100)).append("\n");
				}

				response.append("\n\n🔧 AVAILABLE TOOLS & CONTEXT:\n\n");
				response.append("(Note: Full tool definitions, rules, and examples will be added in Phase 3)\n");
				response.append("For now, showing matched categories to verify action list approach works.\n\n");

				for (CategoryMatch match : categoryMatches.values())
				{
					response.append("Category: ").append(match.category).append("\n");
					response.append("  - Tools will be defined here\n");
					response.append("  - Rules and requirements\n");
					response.append("  - Parameter specifications\n");
					response.append("  - Usage examples\n\n");
				}
			}

			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(response.toString())))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[MCP] Error in getContextFor: " + e.getMessage(), e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Error processing queries: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}

	/**
	 * Helper class to track category matches
	 */
	private static class CategoryMatch
	{
		String category;
		String matchedQuery;
		double bestScore;

		CategoryMatch(String category, String matchedQuery, double bestScore)
		{
			this.category = category;
			this.matchedQuery = matchedQuery;
			this.bestScore = bestScore;
		}
	}

	/**
	 * Auto-register all handlers from the registry.
	 */
	private void registerHandlers(McpSyncServer server)
	{
		IToolHandler[] handlers = ToolHandlerRegistry.getHandlers();
		for (IToolHandler handler : handlers)
		{
			try
			{
				handler.registerTools(server);
				ServoyLog.logInfo("[MCP] Registered handler: " + handler.getHandlerName());
			}
			catch (Exception e)
			{
				ServoyLog.logError("[MCP] Failed to register handler: " + handler.getHandlerName(), e);
			}
		}
	}
}