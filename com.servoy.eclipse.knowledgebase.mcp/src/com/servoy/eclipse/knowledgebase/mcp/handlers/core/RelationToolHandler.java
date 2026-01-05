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
import com.servoy.eclipse.knowledgebase.mcp.services.DatabaseSchemaService;
import com.servoy.eclipse.knowledgebase.mcp.services.DatabaseSchemaService.ForeignKeyRelationship;
import com.servoy.eclipse.knowledgebase.mcp.services.DatabaseSchemaService.PotentialRelationship;
import com.servoy.eclipse.knowledgebase.mcp.services.RelationService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Relations handler - all RELATIONS intent tools
 * Tools: openRelation, getRelations, deleteRelations, discoverDbRelations
 */
public class RelationToolHandler extends AbstractPersistenceHandler
{
	@Override
	public String getHandlerName()
	{
		return "RelationToolHandler";
	}

	/**
	 * Define all tools for this handler with their descriptions and handlers
	 */
	@Override
	protected Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("openRelation", new ToolHandlerRegistry.ToolDefinition(
			"Opens an existing database relation or creates a new relation between two tables. " +
			"[CONTEXT-AWARE for CREATE] When creating a new relation, it will be created in the current context (active solution or module). " +
			"Use getContext to check where it will be created, setContext to change target location. " +
			"Required: name (string). " +
			"Required for creation: primaryDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'), " +
			"foreignDataSource (format: 'server_name/table_name' or 'db:/server_name/table_name'). " +
			"Optional: primaryColumn (string), foreignColumn (string) for column mapping, " +
			"properties (object: map of relation properties - joinType: 'left outer'|'inner', " +
			"allowCreationRelatedRecords: boolean, allowParentDeleteWhenHavingRelatedRecords: boolean, " +
			"deleteRelatedRecords: boolean, initialSort: string, encapsulation: 'public'|'hide'|'module', " +
			"deprecated: string, comment: string).",
			this::handleOpenRelation));

		tools.put("getRelations", new ToolHandlerRegistry.ToolDefinition(
			"Lists relations in the active solution and its modules. " +
			"Optional: scope (string: 'current' or 'all', default 'all'). " +
			"  - 'current': Returns relations from current context only (active solution or specific module based on current context). " +
			"  - 'all': Returns relations from active solution and all modules. " +
			"Returns: List of relation names with their primary and foreign datasources, including origin information (which solution/module each relation belongs to).",
			this::handleGetRelations));

		tools.put("deleteRelations", new ToolHandlerRegistry.ToolDefinition(
			"Deletes one or more existing database relations. Required: names (array of strings) - the names of the relations to delete.",
			this::handleDeleteRelations));

		tools.put("discoverDbRelations", new ToolHandlerRegistry.ToolDefinition(
			"Discovers potential database relations by analyzing foreign key relationships. " +
			"Required: serverName (string) - database server name. " +
			"Returns explicit FK constraints and potential relations based on PK name matching.",
			this::handleDiscoverDbRelations));

		return tools;
	}

	// =============================================
	// TOOL: openRelation
	// =============================================

	private McpSchema.CallToolResult handleOpenRelation(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		
		// Extract parameters
		String name = extractString(args, "name");
		String primaryDataSource = extractString(args, "primaryDataSource");
		String foreignDataSource = extractString(args, "foreignDataSource");
		String primaryColumn = extractString(args, "primaryColumn");
		String foreignColumn = extractString(args, "foreignColumn");
		Map<String, Object> properties = extractMap(args, "properties");
		
		// Validate name is required
		String error = validateRequired(name, "name");
		if (error != null)
		{
			return errorResult(error);
		}
		
		// Execute on UI thread
		return executeOnUIThread(() -> openOrCreateRelation(name, primaryDataSource, foreignDataSource, 
			primaryColumn, foreignColumn, properties), "Error opening/creating relation: " + name);
	}
	
	/**
	 * Opens an existing relation or creates a new one if it doesn't exist (with datasources provided).
	 * For opening (when datasources not provided): searches current context first, then falls back to active solution and modules.
	 * For creating: creates ONLY in current context.
	 * Supports updating properties on existing relations via properties map.
	 */
	private String openOrCreateRelation(String name, String primaryDataSource, String foreignDataSource,
		String primaryColumn, String foreignColumn, Map<String, Object> properties) throws RepositoryException
	{
		ServoyLog.logInfo("[RelationToolHandler] Processing relation: " + name);

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
		boolean isCreateOperation = (primaryDataSource != null && !primaryDataSource.trim().isEmpty()) ||
		                            (foreignDataSource != null && !foreignDataSource.trim().isEmpty());
		
		Relation relation = null; // Primary relation for backward compatibility
		java.util.List<Relation> allMatchingRelations = new java.util.ArrayList<>();
		java.util.List<String> relationLocations = new java.util.ArrayList<>();
		
		if (!isCreateOperation)
		{
			// READ operation: Search ALL contexts and collect ALL matches
			// Search in target context first
			Relation relationInTarget = targetProject.getEditingSolution().getRelation(name);
			if (relationInTarget != null)
			{
				allMatchingRelations.add(relationInTarget);
				relationLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
				relation = relationInTarget; // Set as primary for compatibility
			}
			
			// Search in active solution (if different from target)
			if (!targetProject.equals(servoyProject))
			{
				Relation relationInActive = servoyProject.getEditingSolution().getRelation(name);
				if (relationInActive != null && !allMatchingRelations.contains(relationInActive))
				{
					allMatchingRelations.add(relationInActive);
					relationLocations.add(servoyProject.getProject().getName() + " (active solution)");
					if (relation == null) relation = relationInActive;
				}
			}
			
			// Search in all modules
			ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
			for (ServoyProject module : modules)
			{
				if (module != null && module.getEditingSolution() != null && 
				    !module.equals(targetProject) && !module.equals(servoyProject))
				{
					Relation relationInModule = module.getEditingSolution().getRelation(name);
					if (relationInModule != null && !allMatchingRelations.contains(relationInModule))
					{
						allMatchingRelations.add(relationInModule);
						relationLocations.add(module.getProject().getName());
						if (relation == null) relation = relationInModule;
						ServoyLog.logInfo("[RelationToolHandler] Relation found in module: " + module.getProject().getName());
					}
				}
			}
		}
		else
		{
			// CREATE operation: Search current context only
			relation = targetProject.getEditingSolution().getRelation(name);
			if (relation != null)
			{
				allMatchingRelations.add(relation);
				relationLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
			}
		}
		
		boolean isNewRelation = false;
		boolean propertiesModified = false;

		if (!allMatchingRelations.isEmpty())
		{
			ServoyLog.logInfo("[RelationToolHandler] Found " + allMatchingRelations.size() + " matching relation(s): " + name);

			// If properties are provided (UPDATE operation), check if relation is in current context
			if (properties != null && !properties.isEmpty())
			{
				// Check if the relation is in current context
				Relation relationInCurrentContext = targetProject.getEditingSolution().getRelation(name);
				
				if (relationInCurrentContext == null)
				{
					// UPDATE operation but relation not in current context - need approval
					String foundLocation = null;
					
					// Find where it is
					if (!targetProject.equals(servoyProject) && servoyProject.getEditingSolution().getRelation(name) != null)
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
								if (module.getEditingSolution().getRelation(name) != null)
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
						approvalMsg.append("Relation '").append(name).append("' found in ").append(locationDisplay).append(".\n");
						approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
						approvalMsg.append("To update this relation's properties, I need to switch to ").append(locationDisplay).append(".\n");
						approvalMsg.append("Do you want to proceed?\n\n");
						approvalMsg.append("[If yes, I will: setContext({context: \"").append(foundLocation).append("\"}) then update properties]");
						
						return approvalMsg.toString();
					}
				}
				else
				{
					// Relation in current context - can update
					ServoyLog.logInfo("[RelationToolHandler] Updating relation properties");
					RelationService.updateRelationProperties(relation, properties);
					propertiesModified = true;
				}
			}
		}
		else if (isCreateOperation)
		{
			// Relation doesn't exist - create it in current context
			ServoyLog.logInfo("[RelationToolHandler] Relation doesn't exist, creating in " + targetContext + ": " + name);

			// Validate datasources are provided for creation
			if (primaryDataSource == null || primaryDataSource.trim().isEmpty())
			{
				throw new RepositoryException("Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.");
			}
			if (foreignDataSource == null || foreignDataSource.trim().isEmpty())
			{
				throw new RepositoryException("Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.");
			}

			// Validate and correct datasource format
			primaryDataSource = RelationService.validateAndCorrectDataSource(primaryDataSource);
			foreignDataSource = RelationService.validateAndCorrectDataSource(foreignDataSource);

			// Create the relation in target project using service
			relation = RelationService.createRelationInProject(targetProject, name, primaryDataSource, foreignDataSource, primaryColumn, foreignColumn,
				properties);
			allMatchingRelations.add(relation);
			relationLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
			isNewRelation = true;
		}
		else
		{
			// READ operation but relation not found anywhere
			throw new RepositoryException("Relation '" + name + "' not found. To create it, provide 'primaryDataSource' and 'foreignDataSource'.");
		}
		
		// Open relation(s) in editor on UI thread
		// For READ operations: Open ALL matching relations
		// For CREATE operations: Open the newly created relation
		if (isNewRelation)
		{
			final Relation relationToOpen = relation;
			Display.getDefault().asyncExec(() -> {
				EditorUtil.openRelationEditor(relationToOpen, true);
			});
		}
		else if (!allMatchingRelations.isEmpty())
		{
			// READ operation: Open ALL matching relations
			final java.util.List<Relation> relationsToOpen = new java.util.ArrayList<>(allMatchingRelations);
			Display.getDefault().asyncExec(() -> {
				for (Relation relToOpen : relationsToOpen)
				{
					EditorUtil.openRelationEditor(relToOpen, true);
				}
			});
		}
		
		// Build result message with context information
		StringBuilder result = new StringBuilder();
		// contextDisplay already defined at top of method

		if (isNewRelation)
		{
			result.append("Relation '").append(name).append("' created successfully in ").append(contextDisplay);
			result.append(" (from ").append(primaryDataSource).append(" to ").append(foreignDataSource).append(")");
			if (properties != null && properties.containsKey("joinType"))
			{
				result.append(" with join type: ").append(properties.get("joinType"));
			}
		}
		else
		{
			// READ operation result: Show ALL matches
			if (allMatchingRelations.size() == 1)
			{
				result.append("Relation '").append(name).append("' opened successfully");
				result.append(" (from ").append(relationLocations.get(0)).append(")");
				
				if (propertiesModified)
				{
					result.append(". Properties updated");
				}
			}
			else
			{
				result.append("Relation '").append(name).append("' found in ").append(allMatchingRelations.size()).append(" locations. Opened all:\n");
				for (int i = 0; i < allMatchingRelations.size(); i++)
				{
					result.append("  - ").append(relationLocations.get(i)).append("\n");
				}
			}
			result.append("\n[Context remains: ").append(contextDisplay).append("]");
		}

		return result.toString();
	}

	// =============================================
	// TOOL: deleteRelations
	// =============================================

	private McpSchema.CallToolResult handleDeleteRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		
		// Extract names array
		List<String> names = extractStringList(args, "names");
		
		// Validate names is required
		String error = (names == null || names.isEmpty()) ? "'names' parameter is required (array of relation names)" : null;
		if (error != null)
		{
			return errorResult(error);
		}
		
		// Execute on UI thread
		return executeOnUIThread(() -> deleteRelations(names), "Error deleting relations");
	}
	
	/**
	 * Deletes one or more relations.
	 * CUD operation: Requires approval if relation not in current context.
	 */
	private String deleteRelations(List<String> names) throws RepositoryException
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
		
		java.util.List<String> deletedRelations = new java.util.ArrayList<>();
		java.util.List<String> notFoundRelations = new java.util.ArrayList<>();
		java.util.List<String> needsApproval = new java.util.ArrayList<>();
		java.util.Map<String, String> approvalLocations = new java.util.HashMap<>();
		java.util.List<Relation> relationsToDelete = new java.util.ArrayList<>();
		
		// Step 1: Find relations and check if they're in current context
		for (String name : names)
		{
			if (name == null || name.trim().isEmpty())
			{
				continue;
			}
			
			// First check current context
			Relation relation = targetProject.getEditingSolution().getRelation(name);
			String foundInContext = null;
			
			if (relation != null)
			{
				// Found in current context - can delete without approval
				foundInContext = targetContext;
				relationsToDelete.add(relation);
			}
			else
			{
				// Not in current context - search other locations
				// Search in active solution (if different from target)
				if (!targetProject.equals(servoyProject))
				{
					relation = servoyProject.getEditingSolution().getRelation(name);
					if (relation != null)
					{
						foundInContext = "active";
					}
				}
				
				// If still not found, search in all modules
				if (relation == null)
				{
					ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : modules)
					{
						if (module != null && module.getEditingSolution() != null && !module.equals(targetProject))
						{
							relation = module.getEditingSolution().getRelation(name);
							if (relation != null)
							{
								foundInContext = module.getProject().getName();
								break;
							}
						}
					}
				}
				
				if (relation != null)
				{
					// Found in different context - needs approval
					needsApproval.add(name);
					approvalLocations.put(name, foundInContext);
				}
				else
				{
					// Not found anywhere
					notFoundRelations.add(name);
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
				String relationName = needsApproval.get(0);
				String location = approvalLocations.get(relationName);
				String locationDisplay = "active".equals(location) ? servoyProject.getProject().getName() + " (active solution)" : location;
				
				approvalMsg.append("Relation '").append(relationName).append("' found in ").append(locationDisplay).append(".\n");
				approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
				approvalMsg.append("To delete this relation, I need to switch to ").append(locationDisplay).append(".\n");
				approvalMsg.append("Do you want to proceed?\n\n");
				approvalMsg.append("[If yes, I will: setContext({context: \"").append(location).append("\"}) then delete]");
			}
			else
			{
				approvalMsg.append("Multiple relations found in different locations:\n");
				for (String relationName : needsApproval)
				{
					String location = approvalLocations.get(relationName);
					String locationDisplay = "active".equals(location) ? servoyProject.getProject().getName() + " (active solution)" : location;
					approvalMsg.append("  - ").append(relationName).append(" (in ").append(locationDisplay).append(")\n");
				}
				approvalMsg.append("\nCurrent context is ").append(contextDisplay).append(".\n");
				approvalMsg.append("Please specify which relation(s) to delete and from which location,\n");
				approvalMsg.append("or switch context explicitly using setContext({context: \"module_name\"})");
			}
			
			// If there are also items we can delete, mention that
			if (!relationsToDelete.isEmpty())
			{
				approvalMsg.append("\n\nNote: Can delete from current context without approval: ");
				approvalMsg.append(relationsToDelete.stream().map(r -> r.getName()).collect(java.util.stream.Collectors.joining(", ")));
			}
			
			return approvalMsg.toString();
		}
		
		// Step 2: Delete relations (all are in current context, no approval needed)
		if (!relationsToDelete.isEmpty())
		{
			// Get the repository
			com.servoy.eclipse.model.repository.EclipseRepository repository = 
				(com.servoy.eclipse.model.repository.EclipseRepository)servoyProject.getEditingSolution().getRepository();
			
			try
			{
				// Delete each relation using Servoy API
				for (Relation relation : relationsToDelete)
				{
					IPersist editingNode = servoyProject.getEditingPersist(relation.getUUID());
					
					if (editingNode == null)
					{
						editingNode = relation; // fallback to the relation itself
					}
					
					repository.deleteObject(editingNode);
					ServoyLog.logInfo("[RelationToolHandler] Called deleteObject for relation: " + relation.getName());
				}
				
				// Now save to persist the deletions to disk
				for (Relation relation : relationsToDelete)
				{
					IPersist editingNode = servoyProject.getEditingPersist(relation.getUUID());
					if (editingNode == null) editingNode = relation;
					
					servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
					deletedRelations.add(relation.getName());
					ServoyLog.logInfo("[RelationToolHandler] Successfully deleted relation: " + relation.getName());
				}
				
			}
			catch (Exception e)
			{
				// Deletion failed
				ServoyLog.logError("[RelationToolHandler] FAILED to delete relations", e);
				
				// Throw exception with clear message
				throw new RepositoryException("Failed to delete relations. Error: " + e.getMessage(), e);
			}
		}
		
		// Build result message
		StringBuilder result = new StringBuilder();
		
		if (!deletedRelations.isEmpty())
		{
			result.append("Successfully deleted ").append(deletedRelations.size()).append(" relation(s): ");
			result.append(String.join(", ", deletedRelations));
		}
		
		if (!notFoundRelations.isEmpty())
		{
			if (result.length() > 0)
			{
				result.append("\n\n");
			}
			result.append("Relations not found (").append(notFoundRelations.size()).append("): ");
			result.append(String.join(", ", notFoundRelations));
		}
		
		if (deletedRelations.isEmpty() && notFoundRelations.isEmpty())
		{
			result.append("No relations specified for deletion");
		}
		return result.toString();
	}

	// =============================================
	// TOOL: getRelations
	// =============================================

	private McpSchema.CallToolResult handleGetRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract scope parameter (default: "all")
			String scope = extractString(args, "scope", "all");
			
			// Validate scope parameter
			String error = validateEnum(scope, "scope", "current", "all");
			if (error != null)
			{
				return errorResult(error);
			}
			
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();
			
			if (activeProject == null || activeProject.getEditingSolution() == null)
			{
				return errorResult("No active Servoy solution project found");
			}
			
			String activeSolutionName = activeProject.getEditingSolution().getName();
			String contextName = null;
			StringBuilder resultBuilder = new StringBuilder();
			
			// Collect all relations based on scope
			java.util.List<Relation> allRelations = new java.util.ArrayList<>();
			
			if ("current".equals(scope))
			{
				// Get relations from current context only
				ServoyProject targetProject = resolveTargetProject(servoyModel);
				String context = ContextService.getInstance().getCurrentContext();
				contextName = "active".equals(context) ? activeSolutionName : context;
				
				Iterator<Relation> relations = targetProject.getEditingSolution().getRelations(false); // false = no modules
				while (relations.hasNext())
				{
					allRelations.add(relations.next());
				}
			}
			else
			{
				// Get relations from active solution and all modules
				// First, add active solution relations
				Iterator<Relation> activeRelations = activeProject.getEditingSolution().getRelations(false); // false = only active
				while (activeRelations.hasNext())
				{
					allRelations.add(activeRelations.next());
				}
				
				// Then, add relations from each module (excluding active project to avoid duplication)
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
						
						Iterator<Relation> moduleRelations = module.getEditingSolution().getRelations(false); // false = only this module
						int moduleCount = 0;
						while (moduleRelations.hasNext())
						{
							allRelations.add(moduleRelations.next());
							moduleCount++;
						}
					}
				}
			}

			int count = 0;
			
			// Build appropriate header based on scope
			if ("current".equals(scope))
			{
				resultBuilder.append("Relations in '").append(contextName).append("':\n\n");
			}
			else
			{
				resultBuilder.append("Relations in solution '").append(activeSolutionName).append("' and modules:\n\n");
			}

			for (Relation relation : allRelations)
			{
				count++;
				String solutionName = getSolutionName(relation);
				String originInfo = formatOrigin(solutionName, activeSolutionName);
				
				resultBuilder.append(count).append(". ").append(relation.getName()).append(originInfo);
				resultBuilder.append("\n   Primary: ").append(relation.getPrimaryDataSource());
				resultBuilder.append("\n   Foreign: ").append(relation.getForeignDataSource());
				resultBuilder.append("\n");
			}
			
			if (count == 0)
			{
				resultBuilder.append("No relations found.");
			}
			return successResult(resultBuilder.toString());
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RelationToolHandler] Error in handleGetRelations", e);
			return errorResult(e.getMessage());
		}
	}

	// =============================================
	// TOOL: discoverDbRelations
	// =============================================

	private McpSchema.CallToolResult handleDiscoverDbRelations(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		Map<String, Object> args = request.arguments();
		String serverName = extractString(args, "serverName");

		// Validate serverName is required
		String error = validateRequired(serverName, "serverName");
		if (error != null)
		{
			return errorResult(error);
		}

		try
		{
			IServerInternal server = DatabaseSchemaService.getServer(serverName);
			if (server == null)
			{
				return errorResult("Database server '" + serverName + "' not found");
			}

			// Get table names for context
			List<String> tables = DatabaseSchemaService.getTableNames(server);
			StringBuilder result = new StringBuilder();
			result.append("Database Server: ").append(serverName).append("\n");
			result.append("Tables (").append(tables.size()).append("):\n");

			for (String tName : tables)
			{
				result.append("  - ").append(tName).append("\n");
			}

			// Analyze explicit foreign key relationships
			result.append("\n=== EXPLICIT FOREIGN KEY RELATIONSHIPS ===\n\n");
			List<ForeignKeyRelationship> explicitFKs = DatabaseSchemaService.getExplicitForeignKeys(server);

			if (explicitFKs.isEmpty())
			{
				result.append("(No explicit FK metadata found)\n");
			}
			else
			{
				int fkNum = 1;
				for (ForeignKeyRelationship fk : explicitFKs)
				{
					result.append(fkNum).append(". ");
					result.append(fk.sourceTable).append(".").append(fk.sourceColumn);
					result.append(" → ").append(fk.targetTable);
					result.append("\n");
					fkNum++;
				}
			}

			// Find potential relations
			result.append("\n=== POTENTIAL RELATIONS (PK column name + type matching) ===\n\n");
			List<PotentialRelationship> potentialRels = DatabaseSchemaService.getPotentialRelationships(server);

			if (potentialRels.isEmpty())
			{
				result.append("(No potential relations found)\n");
			}
			else
			{
				int relNum = 1;
				for (PotentialRelationship rel : potentialRels)
				{
					result.append(relNum).append(". ");
					result.append(rel.sourceTable).append(".").append(rel.sourceColumn);
					result.append(" → ").append(rel.targetTable).append(".").append(rel.targetColumn);
					result.append(" (PK match)\n");
					relNum++;
				}
			}

			return successResult(result.toString());
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RelationToolHandler] Error in handleDiscoverDbRelations", e);
			return errorResult(e.getMessage());
		}
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
			if (rootObject instanceof Solution)
			{
				return ((Solution)rootObject).getName();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[RelationToolHandler] Error getting solution name", e);
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
