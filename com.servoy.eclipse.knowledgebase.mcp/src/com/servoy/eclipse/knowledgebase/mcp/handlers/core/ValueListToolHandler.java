package com.servoy.eclipse.knowledgebase.mcp.handlers.core;

import com.servoy.eclipse.knowledgebase.mcp.handlers.AbstractPersistenceHandler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.ContextService;
import com.servoy.eclipse.knowledgebase.mcp.services.ValueListService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Value Lists handler - all VALUE_LISTS intent tools
 * Tools: openValueList, getValueLists, deleteValueLists
 */
public class ValueListToolHandler extends AbstractPersistenceHandler
{
	@Override
	public String getHandlerName()
	{
		return "ValueListToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	@Override
	protected Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("openValueList", new ToolHandlerRegistry.ToolDefinition(
			"Opens an existing valuelist or creates a new valuelist. Supports 4 types: CUSTOM, DATABASE (table), DATABASE (related), GLOBAL_METHOD. " +
			"[CONTEXT-AWARE for CREATE] When creating a new valuelist, it will be created in the current context (active solution or module). " +
			"Use getContext to check where it will be created, setContext to change target location. " +
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
			"Lists valuelists in the active solution and its modules. " +
			"Optional: scope (string: 'current' or 'all', default 'all'). " +
			"  - 'current': Returns valuelists from current context only (active solution or specific module based on current context). " +
			"  - 'all': Returns valuelists from active solution and all modules. " +
			"Returns: List of valuelist names with their types and configuration, including origin information (which solution/module each valuelist belongs to).",
			this::handleGetValueLists));

		tools.put("deleteValueLists", new ToolHandlerRegistry.ToolDefinition(
			"Deletes one or more existing valuelists. Required: names (array of strings) - the names of the valuelists to delete.",
			this::handleDeleteValueLists));

		return tools;
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
	 * For opening (when creation params not provided): searches current context first, then falls back to active solution and modules.
	 * For creating: creates ONLY in current context.
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
			throw new RepositoryException("Cannot get editing solution from active project");
		}

		// Resolve target project based on current context
		ServoyProject targetProject = resolveTargetProject(servoyModel);
		String targetContext = ContextService.getInstance().getCurrentContext();
		String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;

		if (targetProject == null)
		{
			throw new RepositoryException("No target solution/module found for context: " + targetContext);
		}

		if (targetProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get editing solution from target: " + targetContext);
		}

		// Determine if this is a READ operation (opening existing) or CREATE operation
		boolean hasCustom = (customValues != null && !customValues.isEmpty());
		boolean hasDatabase = (dataSource != null && !dataSource.trim().isEmpty());
		boolean hasRelated = (relationName != null && !relationName.trim().isEmpty());
		boolean hasGlobalMethod = (globalMethod != null && !globalMethod.trim().isEmpty());
		boolean isCreateOperation = hasCustom || hasDatabase || hasRelated || hasGlobalMethod;
		
		ValueList valueList = null; // Primary valuelist for backward compatibility
		java.util.List<ValueList> allMatchingValueLists = new java.util.ArrayList<>();
		java.util.List<String> valueListLocations = new java.util.ArrayList<>();
		
		if (!isCreateOperation)
		{
			// READ operation: Search ALL contexts and collect ALL matches
			// Search in target context first
			ValueList valueListInTarget = targetProject.getEditingSolution().getValueList(name);
			if (valueListInTarget != null)
			{
				allMatchingValueLists.add(valueListInTarget);
				valueListLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
				valueList = valueListInTarget; // Set as primary for compatibility
			}
			
			// Search in active solution (if different from target)
			if (!targetProject.equals(servoyProject))
			{
				ValueList valueListInActive = servoyProject.getEditingSolution().getValueList(name);
				if (valueListInActive != null && !allMatchingValueLists.contains(valueListInActive))
				{
					allMatchingValueLists.add(valueListInActive);
					valueListLocations.add(servoyProject.getProject().getName() + " (active solution)");
					if (valueList == null) valueList = valueListInActive;
				}
			}
			
			// Search in all modules
			ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
			for (ServoyProject module : modules)
			{
				if (module != null && module.getEditingSolution() != null && 
				    !module.equals(targetProject) && !module.equals(servoyProject))
				{
					ValueList valueListInModule = module.getEditingSolution().getValueList(name);
					if (valueListInModule != null && !allMatchingValueLists.contains(valueListInModule))
					{
						allMatchingValueLists.add(valueListInModule);
						valueListLocations.add(module.getProject().getName());
						if (valueList == null) valueList = valueListInModule;
						ServoyLog.logInfo("[ValueListToolHandler] ValueList found in module: " + module.getProject().getName());
					}
				}
			}
		}
		else
		{
			// CREATE operation: Search current context only
			valueList = targetProject.getEditingSolution().getValueList(name);
			if (valueList != null)
			{
				allMatchingValueLists.add(valueList);
				valueListLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
			}
		}
		
		
		boolean isNewValueList = false;
		boolean propertiesModified = false;

		if (!allMatchingValueLists.isEmpty())
		{
			ServoyLog.logInfo("[ValueListToolHandler] Found " + allMatchingValueLists.size() + " matching valuelist(s): " + name);

			// If properties are provided (UPDATE operation), check if valuelist is in current context
			if (properties != null && !properties.isEmpty())
			{
				// Check if the valuelist is in current context
				ValueList valueListInCurrentContext = targetProject.getEditingSolution().getValueList(name);
				
				if (valueListInCurrentContext == null)
				{
					// UPDATE operation but valuelist not in current context - need approval
					String foundLocation = null;
					
					// Find where it is
					if (!targetProject.equals(servoyProject) && servoyProject.getEditingSolution().getValueList(name) != null)
					{
						foundLocation = "active";
					}
					else
					{
						ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
						for (ServoyProject module : modules)
						{
							if (module != null && module.getEditingSolution() != null && !module.equals(targetProject))
							{
								if (module.getEditingSolution().getValueList(name) != null)
								{
									foundLocation = module.getProject().getName();
									break;
								}
							}
						}
					}
					
					if (foundLocation != null)
					{
						String locationDisplay = "active".equals(foundLocation) ? servoyProject.getProject().getName() + " (active solution)" : foundLocation;
						StringBuilder approvalMsg = new StringBuilder();
						approvalMsg.append("Current context: ").append(contextDisplay).append("\n\n");
						approvalMsg.append("ValueList '").append(name).append("' found in ").append(locationDisplay).append(".\n");
						approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
						approvalMsg.append("To update this valuelist's properties, I need to switch to ").append(locationDisplay).append(".\n");
						approvalMsg.append("Do you want to proceed?\n\n");
						approvalMsg.append("[If yes, I will: setContext({context: \"").append(foundLocation).append("\"}) then update properties]");
						
						return approvalMsg.toString();
					}
				}
				else
				{
					// ValueList in current context - can update
					ServoyLog.logInfo("[ValueListToolHandler] Updating valuelist properties");
					ValueListService.updateValueListProperties(valueList, properties);
					propertiesModified = true;
				}
			}
		}
		else if (isCreateOperation)
		{
			// ValueList doesn't exist - create it in current context
			ServoyLog.logInfo("[ValueListToolHandler] ValueList doesn't exist, creating in " + targetContext + ": " + name);

			// Validate that at least one type parameter is provided (variables already checked at method start)
			if (!hasCustom && !hasDatabase && !hasRelated && !hasGlobalMethod)
			{
				throw new RepositoryException("ValueList '" + name + "' not found. To create it, provide one of: " +
					"customValues (array), dataSource (string), relationName (string), or globalMethod (string).");
			}

			// Create the valuelist in target project using service
			valueList = ValueListService.createValueListInProject(targetProject, name, customValues, dataSource, relationName, globalMethod, displayColumn,
				returnColumn, properties);
			allMatchingValueLists.add(valueList);
			valueListLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
			isNewValueList = true;
		}
		else
		{
			// READ operation but valuelist not found anywhere
			throw new RepositoryException("ValueList '" + name + "' not found. To create it, provide one of: " +
				"customValues (array), dataSource (string), relationName (string), or globalMethod (string).");
		}
		
		// Open valuelist(s) in editor on UI thread
		// For READ operations: Open ALL matching valuelists
		// For CREATE operations: Open the newly created valuelist
		if (isNewValueList)
		{
			final ValueList valueListToOpen = valueList;
			Display.getDefault().asyncExec(() -> {
				EditorUtil.openValueListEditor(valueListToOpen, true);
			});
		}
		else if (!allMatchingValueLists.isEmpty())
		{
			// READ operation: Open ALL matching valuelists
			final java.util.List<ValueList> valueListsToOpen = new java.util.ArrayList<>(allMatchingValueLists);
			Display.getDefault().asyncExec(() -> {
				for (ValueList vlToOpen : valueListsToOpen)
				{
					EditorUtil.openValueListEditor(vlToOpen, true);
				}
			});
		}

		// Build result message with context information
		StringBuilder result = new StringBuilder();
		// contextDisplay already defined at top of method

		if (isNewValueList)
		{
			result.append("ValueList '").append(name).append("' created successfully in ").append(contextDisplay);
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
			// READ operation result: Show ALL matches
			if (allMatchingValueLists.size() == 1)
			{
				result.append("ValueList '").append(name).append("' opened successfully");
				result.append(" (from ").append(valueListLocations.get(0)).append(")");
				
				if (propertiesModified)
				{
					result.append(". Properties updated");
				}
			}
			else
			{
				result.append("ValueList '").append(name).append("' found in ").append(allMatchingValueLists.size()).append(" locations. Opened all:\n");
				for (int i = 0; i < allMatchingValueLists.size(); i++)
				{
					result.append("  - ").append(valueListLocations.get(i)).append("\n");
				}
			}
			result.append("\n[Context remains: ").append(contextDisplay).append("]");
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
	 * CUD operation: Requires approval if valuelist not in current context.
	 */
	private String deleteValueLists(List<String> names) throws RepositoryException
	{
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
		
		// Get current context
		ServoyProject targetProject = resolveTargetProject(servoyModel);
		String targetContext = ContextService.getInstance().getCurrentContext();
		String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
		
		java.util.List<String> deletedValueLists = new java.util.ArrayList<>();
		java.util.List<String> notFoundValueLists = new java.util.ArrayList<>();
		java.util.List<String> needsApproval = new java.util.ArrayList<>();
		java.util.Map<String, String> approvalLocations = new java.util.HashMap<>();
		java.util.List<ValueList> valueListsToDelete = new java.util.ArrayList<>();
		
		// Step 1: Find valuelists and check if they're in current context
		for (String name : names)
		{
			if (name == null || name.trim().isEmpty())
			{
				continue;
			}
			
			// First check current context
			ValueList valueList = targetProject.getEditingSolution().getValueList(name);
			String foundInContext = null;
			
			if (valueList != null)
			{
				// Found in current context - can delete without approval
				foundInContext = targetContext;
				valueListsToDelete.add(valueList);
			}
			else
			{
				// Not in current context - search other locations
				// Search in active solution (if different from target)
				if (!targetProject.equals(servoyProject))
				{
					valueList = servoyProject.getEditingSolution().getValueList(name);
					if (valueList != null)
					{
						foundInContext = "active";
					}
				}
				
				// If still not found, search in all modules
				if (valueList == null)
				{
					ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : modules)
					{
						if (module != null && module.getEditingSolution() != null && !module.equals(targetProject))
						{
							valueList = module.getEditingSolution().getValueList(name);
							if (valueList != null)
							{
								foundInContext = module.getProject().getName();
								break;
							}
						}
					}
				}
				
				if (valueList != null)
				{
					// Found in different context - needs approval
					needsApproval.add(name);
					approvalLocations.put(name, foundInContext);
				}
				else
				{
					// Not found anywhere
					notFoundValueLists.add(name);
				}
			}
		}
		
		// If any items need approval, return approval request message
		if (!needsApproval.isEmpty())
		{
			StringBuilder approvalMsg = new StringBuilder();
			approvalMsg.append("Current context: ").append(contextDisplay).append("\n\n");
			
			if (needsApproval.size() == 1)
			{
				String valueListName = needsApproval.get(0);
				String location = approvalLocations.get(valueListName);
				String locationDisplay = "active".equals(location) ? servoyProject.getProject().getName() + " (active solution)" : location;
				
				approvalMsg.append("ValueList '").append(valueListName).append("' found in ").append(locationDisplay).append(".\n");
				approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
				approvalMsg.append("To delete this valuelist, I need to switch to ").append(locationDisplay).append(".\n");
				approvalMsg.append("Do you want to proceed?\n\n");
				approvalMsg.append("[If yes, I will: setContext({context: \"").append(location).append("\"}) then delete]");
			}
			else
			{
				approvalMsg.append("Multiple valuelists found in different locations:\n");
				for (String valueListName : needsApproval)
				{
					String location = approvalLocations.get(valueListName);
					String locationDisplay = "active".equals(location) ? servoyProject.getProject().getName() + " (active solution)" : location;
					approvalMsg.append("  - ").append(valueListName).append(" (in ").append(locationDisplay).append(")\n");
				}
				approvalMsg.append("\nCurrent context is ").append(contextDisplay).append(".\n");
				approvalMsg.append("Please specify which valuelist(s) to delete and from which location,\n");
				approvalMsg.append("or switch context explicitly using setContext({context: \"module_name\"})");
			}
			
			// If there are also items we can delete, mention that
			if (!valueListsToDelete.isEmpty())
			{
				approvalMsg.append("\n\nNote: Can delete from current context without approval: ");
				approvalMsg.append(valueListsToDelete.stream().map(vl -> vl.getName()).collect(java.util.stream.Collectors.joining(", ")));
			}
			
			return approvalMsg.toString();
		}
		
		// Step 2: Delete valuelists (all are in current context, no approval needed)
		if (!valueListsToDelete.isEmpty())
		{
			// Get the repository
			com.servoy.eclipse.model.repository.EclipseRepository repository = 
				(com.servoy.eclipse.model.repository.EclipseRepository)servoyProject.getEditingSolution().getRepository();
			
			try
			{
				// Delete each valuelist using Servoy API
				for (ValueList valueList : valueListsToDelete)
				{
					IPersist editingNode = servoyProject.getEditingPersist(valueList.getUUID());
					
					if (editingNode == null)
					{
						editingNode = valueList; // fallback to the valuelist itself
					}
					
					repository.deleteObject(editingNode);
					ServoyLog.logInfo("[ValueListToolHandler] Called deleteObject for valuelist: " + valueList.getName());
				}
				
				// Now save to persist the deletions to disk
				for (ValueList valueList : valueListsToDelete)
				{
					IPersist editingNode = servoyProject.getEditingPersist(valueList.getUUID());
					if (editingNode == null) editingNode = valueList;
					servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
					deletedValueLists.add(valueList.getName());
					ServoyLog.logInfo("[ValueListToolHandler] Successfully deleted valuelist: " + valueList.getName());
				}
			}
			catch (Exception e)
			{
				// Deletion failed
				ServoyLog.logError("[ValueListToolHandler] FAILED to delete valuelists", e);
				
				// Throw exception with clear message
				throw new RepositoryException("Failed to delete valuelists. Error: " + e.getMessage(), e);
			}
		}
		
		// Build result message
		StringBuilder result = new StringBuilder();
		
		if (!deletedValueLists.isEmpty())
		{
			result.append("Successfully deleted ").append(deletedValueLists.size()).append(" valuelist(s): ");
			result.append(String.join(", ", deletedValueLists));
		}
		
		if (!notFoundValueLists.isEmpty())
		{
			if (result.length() > 0)
			{
				result.append("\n\n");
			}
			result.append("ValueLists not found (").append(notFoundValueLists.size()).append("): ");
			result.append(String.join(", ", notFoundValueLists));
		}
		
		if (deletedValueLists.isEmpty() && notFoundValueLists.isEmpty())
		{
			result.append("No valuelists specified for deletion");
		}
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
			Map<String, Object> args = request.arguments();
			
			// Extract scope parameter (default: "all")
			String scope = extractString(args, "scope", "all");
			
			// Validate scope parameter
			if (!scope.equals("current") && !scope.equals("all"))
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: Invalid scope value '" + scope + "'. Must be 'current' or 'all'.")))
					.isError(true)
					.build();
			}
			
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();
			String activeSolutionName = activeProject.getEditingSolution().getName();
			String contextName = null;
			
			// Collect all valuelists based on scope
			java.util.List<ValueList> allValueLists = new java.util.ArrayList<>();
			
			if ("current".equals(scope))
			{
				// Get valuelists from current context only
				ServoyProject targetProject = resolveTargetProject(servoyModel);
				String context = ContextService.getInstance().getCurrentContext();
				contextName = "active".equals(context) ? activeSolutionName : context;
				
				Iterator<ValueList> valueLists = targetProject.getEditingSolution().getValueLists(false); // false = no modules
				while (valueLists.hasNext())
				{
					allValueLists.add(valueLists.next());
				}
			}
			else
			{
				// Get valuelists from active solution and all modules
				// First, add active solution valuelists
				Iterator<ValueList> activeValueLists = activeProject.getEditingSolution().getValueLists(false); // false = only active
				while (activeValueLists.hasNext())
				{
					allValueLists.add(activeValueLists.next());
				}
				
				// Then, add valuelists from each module (excluding active project to avoid duplication)
				ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
				for (ServoyProject module : modules)
				{
					if (module != null && module.getEditingSolution() != null)
					{
						// Skip if this module is actually the active project itself (prevent duplication)
						if (module.equals(activeProject))
						{
							continue;
						}
						
						Iterator<ValueList> moduleValueLists = module.getEditingSolution().getValueLists(false); // false = only this module
						while (moduleValueLists.hasNext())
						{
							allValueLists.add(moduleValueLists.next());
						}
					}
				}
			}

			int count = 0;
			
			// Build appropriate header based on scope
			if ("current".equals(scope))
			{
				resultBuilder.append("Value lists in '").append(contextName).append("':\n\n");
			}
			else
			{
				resultBuilder.append("Value lists in solution '").append(activeSolutionName).append("' and modules:\n\n");
			}

			for (ValueList vl : allValueLists)
			{
				count++;
				String solutionName = getSolutionName(vl);
				String originInfo = formatOrigin(solutionName, activeSolutionName);
				
				resultBuilder.append(count).append(". ").append(vl.getName()).append(originInfo);

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

	// =============================================
	// UTILITY METHODS
	// =============================================

	/**
	 * Get the solution name that owns a persist object.
	 * Returns the solution name or "unknown" if cannot determine.
	 */
	private String getSolutionName(IPersist persist)
	{
		try
		{
			IRootObject rootObject = persist.getRootObject();
			if (rootObject instanceof Solution solution)
			{
				return solution.getName();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[ValueListToolHandler] Error getting solution name", e);
		}
		return "unknown";
	}

	/**
	 * Format origin information for display.
	 * Returns " (in: solutionName)" or " (in: active solution)" based on context.
	 */
	private String formatOrigin(String solutionName, String activeSolutionName)
	{
		if (solutionName.equals(activeSolutionName))
		{
			return " (in: active solution)";
		}
		else
		{
			return " (in: " + solutionName + ")";
		}
	}
}
