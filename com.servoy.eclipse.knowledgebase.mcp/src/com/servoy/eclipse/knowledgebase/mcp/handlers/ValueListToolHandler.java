package com.servoy.eclipse.knowledgebase.mcp.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.IToolHandler;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.ValueListService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Value Lists handler - all VALUE_LISTS intent tools
 * Tools: openValueList, getValueLists, deleteValueLists
 */
public class ValueListToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "ValueListToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	private Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("openValueList", new ToolHandlerRegistry.ToolDefinition(
			"Opens an existing valuelist or creates a new valuelist. Supports 4 types: CUSTOM, DATABASE (table), DATABASE (related), GLOBAL_METHOD. " +
			"Required: name (string). " +
			"For CUSTOM: customValues (array of strings). " +
			"For DATABASE (table): dataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), displayColumn (string), returnColumn (string). " +
			"For DATABASE (related): relationName (string), displayColumn (string), returnColumn (string). " +
			"For GLOBAL_METHOD: globalMethod (string - method prefixed name like 'scopes.globals.getCountries'). " +
			"Optional properties (object): lazyLoading: boolean, displayValueType: int, realValueType: int, " +
			"separator: string, sortOptions: string, useTableFilter: boolean, addEmptyValue: boolean|'always'|'never', " +
			"fallbackValueListID: int, deprecated: string, encapsulation: 'public'|'hide'|'module', comment: string.",
			this::handleOpenValueList));

		tools.put("getValueLists", new ToolHandlerRegistry.ToolDefinition(
			"Lists all existing valuelists in the active solution. No parameters required.",
			this::handleGetValueLists));

		tools.put("deleteValueLists", new ToolHandlerRegistry.ToolDefinition(
			"Deletes one or more existing valuelists. Required: names (array of strings) - the names of the valuelists to delete.",
			this::handleDeleteValueLists));

		return tools;
	}

	/**
	 * Register all value list tools with MCP server
	 */
	@Override
	public void registerTools(McpSyncServer server)
	{
		// Map-based registration with ToolDefinition
		for (Map.Entry<String, ToolHandlerRegistry.ToolDefinition> entry : getToolDefinitions().entrySet())
		{
			ToolHandlerRegistry.registerTool(
				server,
				entry.getKey(),
				entry.getValue().description,
				entry.getValue().handler);
		}
	}

	// =============================================
	// HELPER METHODS - Parameter Extraction
	// =============================================
	
	private String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}
	
	@SuppressWarnings("unchecked")
	private List<String> extractStringList(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof List)
		{
			List<String> result = new java.util.ArrayList<>();
			for (Object item : (List<?>)value)
			{
				if (item != null)
				{
					result.add(item.toString());
				}
			}
			return result;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> extractMap(Map<String, Object> args, String key)
	{
		if (args == null || !args.containsKey(key))
		{
			return null;
		}
		Object value = args.get(key);
		if (value instanceof Map)
		{
			return (Map<String, Object>)value;
		}
		return null;
	}

	// =============================================
	// TOOL: openValueList
	// =============================================

	private McpSchema.CallToolResult handleOpenValueList(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract parameters
			String name = extractString(args, "name", null);
			List<String> tempCustomValues = extractStringList(args, "customValues");
			final List<String> customValues = (tempCustomValues != null) ? tempCustomValues : extractStringList(args, "values"); // Legacy support
			String dataSource = extractString(args, "dataSource", null);
			String relationName = extractString(args, "relationName", null);
			String globalMethod = extractString(args, "globalMethod", null);
			String displayColumn = extractString(args, "displayColumn", null);
			String returnColumn = extractString(args, "returnColumn", null);
			Map<String, Object> properties = extractMap(args, "properties");
			
			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = openOrCreateValueList(name, customValues, dataSource, relationName, 
						globalMethod, displayColumn, returnColumn, properties);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error opening/creating valuelist: " + name, exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + exception[0].getMessage())))
					.isError(true)
					.build();
			}
			
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleOpenValueList", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	/**
	 * Opens an existing valuelist or creates a new one if it doesn't exist.
	 * Supports updating properties on existing valuelists via properties map.
	 */
	private String openOrCreateValueList(String name, List<String> customValues, String dataSource,
		String relationName, String globalMethod, String displayColumn, String returnColumn,
		Map<String, Object> properties) throws RepositoryException
	{
		ServoyLog.logInfo("[ValueListToolHandler] Processing valuelist: " + name);
		
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		
		if (servoyProject == null)
		{
			throw new RepositoryException("No active Servoy solution project found");
		}
		
		if (servoyProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get the Servoy Solution from the selected Servoy Project");
		}
		
		// Check if valuelist already exists
		ValueList valueList = servoyProject.getEditingSolution().getValueList(name);
		boolean isNewValueList = false;
		boolean propertiesModified = false;
		
		if (valueList != null)
		{
			ServoyLog.logInfo("[ValueListToolHandler] ValueList exists: " + name);
			
			// Apply properties if provided (update existing valuelist)
			if (properties != null && !properties.isEmpty())
			{
				ServoyLog.logInfo("[ValueListToolHandler] Updating valuelist properties");
				ValueListService.updateValueListProperties(valueList, properties);
				propertiesModified = true;
			}
		}
		else
		{
			// ValueList doesn't exist - create it
			ServoyLog.logInfo("[ValueListToolHandler] ValueList doesn't exist, creating: " + name);
			
			// Validate that at least one type parameter is provided
			boolean hasCustom = (customValues != null && !customValues.isEmpty());
			boolean hasDatabase = (dataSource != null && !dataSource.trim().isEmpty());
			boolean hasRelated = (relationName != null && !relationName.trim().isEmpty());
			boolean hasGlobalMethod = (globalMethod != null && !globalMethod.trim().isEmpty());
			
			if (!hasCustom && !hasDatabase && !hasRelated && !hasGlobalMethod)
			{
				throw new RepositoryException("ValueList '" + name + "' not found. To create it, provide one of: " +
					"customValues (array), dataSource (string), relationName (string), or globalMethod (string).");
			}
			
			// Create the valuelist using service (this saves it)
			valueList = ValueListService.createValueList(name, customValues, dataSource, relationName,
				globalMethod, displayColumn, returnColumn, properties);
			isNewValueList = true;
			
			// Re-fetch from solution after save to ensure we have the saved version
			ValueList savedValueList = servoyProject.getEditingSolution().getValueList(name);
			if (savedValueList != null)
			{
				valueList = savedValueList;
				ServoyLog.logInfo("[ValueListToolHandler] Re-fetched saved valuelist from solution");
			}
		}
		
		// Open editor on UI thread
		final ValueList valueListToOpen = valueList;
		final boolean finalIsNewValueList = isNewValueList;
		Display.getDefault().asyncExec(() -> {
			EditorUtil.openValueListEditor(valueListToOpen, !finalIsNewValueList);
		});
		
		// Build result message
		StringBuilder result = new StringBuilder();
		if (isNewValueList)
		{
			result.append("ValueList '").append(name).append("' created successfully");
			if (customValues != null && !customValues.isEmpty())
			{
				result.append(" (CUSTOM with ").append(customValues.size()).append(" values)");
			}
			else if (globalMethod != null)
			{
				result.append(" (GLOBAL_METHOD: ").append(globalMethod).append(")");
			}
			else if (relationName != null)
			{
				result.append(" (RELATED: ").append(relationName).append(")");
			}
			else if (dataSource != null)
			{
				result.append(" (DATABASE: ").append(dataSource).append(")");
			}
		}
		else
		{
			result.append("ValueList '").append(name).append("' opened successfully");
			if (propertiesModified)
			{
				result.append(". Properties updated");
			}
		}
		
		return result.toString();
	}

	// =============================================
	// TOOL: deleteValueLists
	// =============================================

	private McpSchema.CallToolResult handleDeleteValueLists(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract names array
			List<String> names = extractStringList(args, "names");
			
			// Validate names is required
			if (names == null || names.isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'names' parameter is required (array of valuelist names)")))
					.isError(true)
					.build();
			}
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = deleteValueLists(names);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error deleting valuelists", exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + exception[0].getMessage())))
					.isError(true)
					.build();
			}
			
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteValueLists", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	/**
	 * Deletes one or more valuelists.
	 * Ensures memory state always reflects disk reality - only removes from memory after successful disk save.
	 */
	private String deleteValueLists(List<String> names) throws RepositoryException
	{
		System.err.println("[DEBUG deleteValueLists] ========== START DELETE VALUELISTS ==========");
		System.err.println("[DEBUG deleteValueLists] Requested to delete: " + names);
		
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		
		System.err.println("[DEBUG deleteValueLists] Active project: " + (servoyProject != null ? servoyProject.getProject().getName() : "NULL"));
		
		if (servoyProject == null)
		{
			throw new RepositoryException("No active Servoy solution project found");
		}
		
		if (servoyProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get the Servoy Solution from the selected Servoy Project");
		}
		
		System.err.println("[DEBUG deleteValueLists] Editing solution: " + servoyProject.getEditingSolution().getName());
		
		java.util.List<String> deletedValueLists = new java.util.ArrayList<>();
		java.util.List<String> notFoundValueLists = new java.util.ArrayList<>();
		java.util.List<ValueList> valueListsToDelete = new java.util.ArrayList<>();
		
		// Step 1: Find valuelists to delete (don't remove from memory yet)
		System.err.println("[DEBUG deleteValueLists] PHASE 1: Finding valuelists...");
		for (String name : names)
		{
			if (name == null || name.trim().isEmpty())
			{
				System.err.println("[DEBUG deleteValueLists] Skipping empty name");
				continue;
			}
			
			System.err.println("[DEBUG deleteValueLists] Looking for valuelist: " + name);
			ValueList valueList = servoyProject.getEditingSolution().getValueList(name);
			
			if (valueList == null)
			{
				System.err.println("[DEBUG deleteValueLists] NOT FOUND: " + name);
				notFoundValueLists.add(name);
			}
			else
			{
				System.err.println("[DEBUG deleteValueLists] FOUND: " + name + " (will be deleted)");
				valueListsToDelete.add(valueList);
			}
		}
		
		System.err.println("[DEBUG deleteValueLists] Found " + valueListsToDelete.size() + " valuelists to delete");
		System.err.println("[DEBUG deleteValueLists] Not found: " + notFoundValueLists.size() + " valuelists");
		
		// Step 2: Delete using proper Servoy API (repository.deleteObject + saveEditingSolutionNodes)
		if (!valueListsToDelete.isEmpty())
		{
			System.err.println("[DEBUG deleteValueLists] PHASE 2: Deleting via repository.deleteObject()...");
			
			// Get the repository
			com.servoy.eclipse.model.repository.EclipseRepository repository = 
				(com.servoy.eclipse.model.repository.EclipseRepository)servoyProject.getEditingSolution().getRepository();
			
			System.err.println("[DEBUG deleteValueLists] Got repository: " + repository);
			
			try
			{
				// Delete each valuelist using Servoy API
				for (ValueList valueList : valueListsToDelete)
				{
					System.err.println("[DEBUG deleteValueLists] Getting editing persist for: " + valueList.getName());
					IPersist editingNode = servoyProject.getEditingPersist(valueList.getUUID());
					
					if (editingNode == null)
					{
						System.err.println("[DEBUG deleteValueLists] WARNING: editingNode is null for: " + valueList.getName());
						editingNode = valueList; // fallback to the valuelist itself
					}
					
					System.err.println("[DEBUG deleteValueLists] Calling repository.deleteObject() for: " + valueList.getName());
					repository.deleteObject(editingNode);
					System.err.println("[DEBUG deleteValueLists] deleteObject succeeded for: " + valueList.getName());
					ServoyLog.logInfo("[ValueListToolHandler] Called deleteObject for valuelist: " + valueList.getName());
				}
				
				// Now save to persist the deletions to disk
				System.err.println("[DEBUG deleteValueLists] PHASE 3: Calling saveEditingSolutionNodes to persist deletions...");
				for (ValueList valueList : valueListsToDelete)
				{
					IPersist editingNode = servoyProject.getEditingPersist(valueList.getUUID());
					if (editingNode == null) editingNode = valueList;
					
					System.err.println("[DEBUG deleteValueLists] Saving deletion for: " + valueList.getName());
					servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
					System.err.println("[DEBUG deleteValueLists] Save succeeded for: " + valueList.getName());
					
					deletedValueLists.add(valueList.getName());
					ServoyLog.logInfo("[ValueListToolHandler] Successfully deleted valuelist: " + valueList.getName());
				}
				
				System.err.println("[DEBUG deleteValueLists] All deletions completed successfully");
			}
			catch (Exception e)
			{
				// Deletion failed
				System.err.println("[DEBUG deleteValueLists] Deletion FAILED: " + e.getMessage());
				System.err.println("[DEBUG deleteValueLists] Exception type: " + e.getClass().getName());
				e.printStackTrace(System.out);
				ServoyLog.logError("[ValueListToolHandler] FAILED to delete valuelists", e);
				
				// Throw exception with clear message
				throw new RepositoryException("Failed to delete valuelists. Error: " + e.getMessage(), e);
			}
		}
		else
		{
			System.err.println("[DEBUG deleteValueLists] No valuelists to delete (all were not found or empty)");
		}
		
		// Build result message
		System.err.println("[DEBUG deleteValueLists] Building result message...");
		StringBuilder result = new StringBuilder();
		
		if (!deletedValueLists.isEmpty())
		{
			result.append("Successfully deleted ").append(deletedValueLists.size()).append(" valuelist(s): ");
			result.append(String.join(", ", deletedValueLists));
			System.err.println("[DEBUG deleteValueLists] Result includes " + deletedValueLists.size() + " deleted valuelists");
		}
		
		if (!notFoundValueLists.isEmpty())
		{
			if (result.length() > 0)
			{
				result.append("\n\n");
			}
			result.append("ValueLists not found (").append(notFoundValueLists.size()).append("): ");
			result.append(String.join(", ", notFoundValueLists));
			System.err.println("[DEBUG deleteValueLists] Result includes " + notFoundValueLists.size() + " not found valuelists");
		}
		
		if (deletedValueLists.isEmpty() && notFoundValueLists.isEmpty())
		{
			result.append("No valuelists specified for deletion");
			System.err.println("[DEBUG deleteValueLists] No valuelists were specified");
		}
		
		System.err.println("[DEBUG deleteValueLists] Final result: " + result.toString());
		System.err.println("[DEBUG deleteValueLists] ========== END DELETE VALUELISTS ==========");
		return result.toString();
	}

	// =============================================
	// TOOL: getValueLists
	// =============================================

	private McpSchema.CallToolResult handleGetValueLists(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String errorMessage = null;
		StringBuilder resultBuilder = new StringBuilder();

		try
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			Iterator<ValueList> valueLists = servoyModel.getActiveProject().getEditingSolution().getValueLists(true);

			int count = 0;
			resultBuilder.append("Value lists in solution '").append(servoyModel.getActiveProject().getEditingSolution().getName()).append("':\n\n");

			while (valueLists.hasNext())
			{
				ValueList vl = valueLists.next();
				count++;
				resultBuilder.append(count).append(". ").append(vl.getName());

				// Show type
				int type = vl.getValueListType();
				if (type == 0)
				{
					resultBuilder.append(" (CUSTOM_VALUES)");
					String customVals = vl.getCustomValues();
					if (customVals != null && !customVals.trim().isEmpty())
					{
						// Show first few values
						String[] values = customVals.split("\n");
						resultBuilder.append("\n   Values: ");
						int showCount = Math.min(values.length, 5);
						for (int i = 0; i < showCount; i++)
						{
							if (i > 0) resultBuilder.append(", ");
							resultBuilder.append(values[i]);
						}
						if (values.length > 5)
						{
							resultBuilder.append(" (and ").append(values.length - 5).append(" more...)");
						}
					}
				}
				else if (type == 1)
				{
					resultBuilder.append(" (DATABASE_VALUES)");
					String ds = vl.getDataSource();
					if (ds != null)
					{
						resultBuilder.append("\n   DataSource: ").append(ds);
					}
					String dp1 = vl.getDataProviderID1();
					if (dp1 != null)
					{
						resultBuilder.append("\n   Column: ").append(dp1);
					}
				}
				else if (type == 4)
				{
					resultBuilder.append(" (GLOBAL_METHOD)");
				}
				else
				{
					resultBuilder.append(" (type=").append(type).append(")");
				}

				resultBuilder.append("\n");
			}

			if (count == 0)
			{
				resultBuilder.append("No value lists found.");
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[ValueListToolHandler] Error in handleGetValueLists: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
