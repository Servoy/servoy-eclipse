package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.service.RulesCache;
import com.servoy.eclipse.knowledgebase.service.ServoyEmbeddingService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Knowledge handler - retrieves Servoy documentation and tool instructions.
 * Tools: getKnowledge
 */
public class KnowledgeToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "KnowledgeToolHandler";
	}

	@Override
	public void registerTools(McpSyncServer server)
	{
		ToolHandlerRegistry.registerTool(
			server,
			"getKnowledge",
			"Retrieves Servoy documentation and tools for specified action queries. " +
				"Required: queries (array of strings) - action phrases like 'create form', 'add buttons', 'create relation'. " +
				"Each query should be a simple 2-4 word phrase describing one action type.",
			this::handleGetKnowledge);
	}

	/**
	 * Get knowledge for action list queries.
	 * Receives array of action phrases, does similarity search for each,
	 * returns aggregated Servoy context.
	 */
	private McpSchema.CallToolResult handleGetKnowledge(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
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
			response.append("=== SERVOY KNOWLEDGE FOR YOUR ACTION LIST ===\n\n");
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
				response.append("=== AVAILABLE TOOLS & KNOWLEDGE ===\n");
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
						response.append("This category was matched by similarity search, but tools for ").append(match.category)
							.append(" are not yet available.\n");
						response.append("This feature is planned for future implementation.\n\n");
						response.append("For now, inform the user that this functionality is coming soon.\n\n");
					}

					response.append("=============================================================================\n\n");
				}
			}

			return McpSchema.CallToolResult.builder().content(List.of(new TextContent(response.toString()))).build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[KnowledgeToolHandler] Error in getKnowledge: " + e.getMessage(), e);
			return McpSchema.CallToolResult.builder().content(List.of(new TextContent("Error processing queries: " + e.getMessage()))).isError(true)
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
}
