package com.servoy.eclipse.mcp;

import java.util.Set;

import org.apache.tomcat.starter.IServicesProvider;
import org.apache.tomcat.starter.ServletInstance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolManager;
import com.servoy.eclipse.model.util.ServoyLog;

import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.jackson.DefaultJsonSchemaValidator;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

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

		// Register all tools via handlers
		registerHandlers(server);

		return Set.of(new ServletInstance(transportProvider, "/mcp"));
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