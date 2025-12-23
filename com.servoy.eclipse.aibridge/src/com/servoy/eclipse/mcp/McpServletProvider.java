package com.servoy.eclipse.mcp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolManager;
import com.servoy.eclipse.knowledgebase.service.RulesCache;
import com.servoy.eclipse.knowledgebase.service.ServoyEmbeddingService;
import com.servoy.eclipse.model.nature.ServoyProject;
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

		// getContext tool - Action list based context retrieval
		Tool getContextTool = McpSchema.Tool.builder()
			.inputSchema(new JsonSchema("object", null, null, null, null, null))
			.name("getContext")
			.description(
				"Retrieves Servoy documentation and tools for specified action queries. " +
					"Required: queries (array of strings) - action phrases like 'create form', 'add buttons', 'create relation'. " +
					"Each query should be a simple 2-4 word phrase describing one action type.")
			.build();

		SyncToolSpecification getContextSpec = SyncToolSpecification.builder()
			.tool(getContextTool)
			.callHandler(this::handleGetContext)
			.build();
		server.addTool(getContextSpec);

		registerHandlers(server);


		return Set.of(new ServletInstance(transportProvider, "/mcp"));
	}

	/**
	 * Get context for action list queries.
	 * Receives array of action phrases, does similarity search for each,
	 * returns aggregated Servoy context.
	 */
	private McpSchema.CallToolResult handleGetContext(Object exchange, McpSchema.CallToolRequest request)
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
				response.append("[!!! NO MATCHING SERVOY CATEGORIES FOUND !!!]\n\n");
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
				response.append("=============================================================================\n");
				response.append("=== AVAILABLE TOOLS & CONTEXT ===\n");
				response.append("=============================================================================\n\n");

				// Get active project name for variable substitution
				String projectName = null;
				try
				{
					ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
					if (activeProject != null)
					{
						projectName = activeProject.getProject().getName();
					}
				}
			catch (Exception e)
			{
				// Ignore - will use null project name
			}

				int categoryNum = 1;
				for (CategoryMatch match : categoryMatches.values())
				{
					response.append("--- Category ").append(categoryNum++).append(": ").append(match.category).append(" ---\n");
					response.append("Matched query: \"").append(match.matchedQuery).append("\"\n");
					response.append("Confidence: ").append(String.format("%.1f%%", match.bestScore * 100)).append("\n\n");

					// Load actual rules content from RulesCache with project name substitution
					String rules = RulesCache.getRules(match.category, projectName);
					if (rules != null && !rules.isEmpty())
					{
						response.append(rules).append("\n\n");
					}
					else
					{
						response.append("[NOT YET IMPLEMENTED]\n\n");
						response.append("This category was matched by similarity search, but tools for ")
							.append(match.category).append(" are not yet available.\n");
						response.append("This feature is planned for future implementation.\n\n");
						response.append("For now, inform the user that this functionality is coming soon.\n\n");
					}

					response.append("=============================================================================\n\n");
				}
			}

			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(response.toString())))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[MCP] Error in getContext: " + e.getMessage(), e);
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
	 * Auto-register all handlers from the MCP plugin.
	 */
	private void registerHandlers(McpSyncServer server)
	{
		IToolHandler[] handlers = ToolManager.getHandlers();

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