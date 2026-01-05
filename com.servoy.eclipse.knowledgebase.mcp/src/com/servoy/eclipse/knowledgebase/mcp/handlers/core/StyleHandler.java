package com.servoy.eclipse.knowledgebase.mcp.handlers.core;

import com.servoy.eclipse.knowledgebase.mcp.handlers.AbstractPersistenceHandler;

import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.ContextService;
import com.servoy.eclipse.knowledgebase.mcp.services.FormService;
import com.servoy.eclipse.knowledgebase.mcp.services.StyleService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.RepositoryException;

import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Style handler - CRUD operations for CSS/LESS styles
 * Tools: addStyle, getStyle, listStyles, deleteStyle
 */
public class StyleHandler extends AbstractPersistenceHandler
{
	@Override
	public String getHandlerName()
	{
		return "StyleHandler";
	}

	@Override
	protected Map<String, ToolHandlerRegistry.ToolDefinition> getToolDefinitions()
	{
		Map<String, ToolHandlerRegistry.ToolDefinition> tools = new java.util.LinkedHashMap<>();

		tools.put("addStyle", new ToolHandlerRegistry.ToolDefinition(
			"Adds or updates a CSS class in a LESS file. " +
				"[CONTEXT-AWARE] Style will be added to the LESS file in the current context (active solution or module). " +
				"Use getContext to check where it will be created, setContext to change target location. " +
				"Required: className (string - without dot), cssContent (string - CSS rules). " +
				"Optional: lessFileName (string - file to add style to, defaults to <solution-name>.less). " +
				"If lessFileName is provided and different from main solution file, import is automatically added.",
			this::handleAddStyle));

		tools.put("getStyle", new ToolHandlerRegistry.ToolDefinition(
			"Gets the CSS content of a class from a LESS file. " +
				"Required: className (string - without dot). " +
				"Optional: lessFileName (string - file to search in, defaults to <solution-name>.less).",
			this::handleGetStyle));

		tools.put("listStyles", new ToolHandlerRegistry.ToolDefinition(
			"Lists all CSS class names in a LESS file. " +
				"Optional: lessFileName (string - file to list from, defaults to <solution-name>.less). " +
				"Optional: scope (string: 'current' or 'all', default 'current'). " +
				"  - 'current': Lists styles from current context only. " +
				"  - 'all': Lists styles from active solution and all modules with origin information. " +
				"Returns: Comma-separated list of class names (or detailed list with origins if scope='all').",
			this::handleListStyles));

		tools.put("deleteStyle", new ToolHandlerRegistry.ToolDefinition(
			"Deletes a CSS class from a LESS file in the CURRENT CONTEXT only. " +
				"To delete from a different module, use setContext first to switch to that module. " +
				"Required: className (string - without dot). " +
				"Optional: lessFileName (string - file to delete from, defaults to <solution-name>.less).",
			this::handleDeleteStyle));

		return tools;
	}

	// =============================================
	// TOOL: addStyle
	// =============================================

	private McpSchema.CallToolResult handleAddStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String cssContent = extractString(args, "cssContent", null);
			String lessFileName = extractString(args, "lessFileName", null);

			// Validate required parameters
			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			if (cssContent == null || cssContent.trim().isEmpty())
			{
				return errorResult("'cssContent' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				
				if (servoyProject == null)
				{
					return errorResult("No active Servoy solution project found");
				}
				
				// Resolve current context
				ServoyProject targetProject = resolveTargetProject(servoyModel);
				String targetContext = ContextService.getInstance().getCurrentContext();
				String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
				
				// Search for existing style across all contexts (UPDATE vs CREATE detection)
				String foundInContext = null;
				String foundProjectPath = null;
				String foundSolutionName = null;
				
				// Check current context first
				String projectPath = targetProject.getProject().getLocation().toOSString();
				String solutionName = targetProject.getSolution().getName();
				String result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
				
				if (!result.startsWith("Class '") || !result.contains("not found"))
				{
					foundInContext = targetContext;
					foundProjectPath = projectPath;
					foundSolutionName = solutionName;
				}
				
				// If not found in current context, search active solution
				if (foundInContext == null && !targetProject.equals(servoyProject))
				{
					projectPath = servoyProject.getProject().getLocation().toOSString();
					solutionName = servoyProject.getSolution().getName();
					result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
					
					if (!result.startsWith("Class '") || !result.contains("not found"))
					{
						foundInContext = "active";
						foundProjectPath = projectPath;
						foundSolutionName = solutionName;
					}
				}
				
				// If still not found, search all modules
				if (foundInContext == null)
				{
					ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : modules)
					{
						if (module != null && module.getSolution() != null && 
						    !module.equals(targetProject) && !module.equals(servoyProject))
						{
							projectPath = module.getProject().getLocation().toOSString();
							solutionName = module.getSolution().getName();
							result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
							
							if (!result.startsWith("Class '") || !result.contains("not found"))
							{
								foundInContext = module.getProject().getName();
								foundProjectPath = projectPath;
								foundSolutionName = solutionName;
								break;
							}
						}
					}
				}
				
				// If style exists in different context, request approval (UPDATE operation)
				if (foundInContext != null && !foundInContext.equals(targetContext))
				{
					String locationDisplay = "active".equals(foundInContext) ? 
						servoyProject.getProject().getName() + " (active solution)" : foundInContext;
					
					StringBuilder approvalMsg = new StringBuilder();
					approvalMsg.append("Current context: ").append(contextDisplay).append("\n\n");
					approvalMsg.append("Style '.").append(className).append("' found in ").append(locationDisplay).append(".\n");
					approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
					approvalMsg.append("To update this style, I need to switch to ").append(locationDisplay).append(".\n");
					approvalMsg.append("Do you want to proceed?\n\n");
					approvalMsg.append("[If yes, I will: setContext({context: \"").append(foundInContext).append("\"}) then update]");
					
					return successResult(approvalMsg.toString());
				}
				
				// Style is in current context OR doesn't exist - proceed with add/update
				// Use current context project path and solution name
				projectPath = targetProject.getProject().getLocation().toOSString();
				solutionName = targetProject.getSolution().getName();
				
				String error = StyleService.addOrUpdateStyle(projectPath, solutionName, lessFileName, className, cssContent);

				if (error != null)
				{
					return errorResult(error);
				}

				String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
					? lessFileName 
					: solutionName + ".less";
				
				if (!targetFile.endsWith(".less"))
				{
					targetFile += ".less";
				}
				
				// Refresh any open form to reflect style changes
				refreshCurrentForm(targetFile);
				
				return successResult("Successfully added/updated style '." + className + "' in " + targetFile);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error adding style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleAddStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: getStyle
	// =============================================

	private McpSchema.CallToolResult handleGetStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String lessFileName = extractString(args, "lessFileName", null);

			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				
				if (servoyProject == null)
				{
					return errorResult("No active Servoy solution project found");
				}
				
				// READ operation: Search ALL contexts and collect ALL matches
				java.util.List<String> allMatchingStyles = new java.util.ArrayList<>();
				java.util.List<String> styleLocations = new java.util.ArrayList<>();
				
				// Resolve current context
				ServoyProject targetProject = resolveTargetProject(servoyModel);
				String targetContext = ContextService.getInstance().getCurrentContext();
				String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
				
				// Search in target context
				String projectPath = targetProject.getProject().getLocation().toOSString();
				String solutionName = targetProject.getSolution().getName();
				String result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
				
				if (!result.startsWith("Class '") || !result.contains("not found"))
				{
					allMatchingStyles.add(result);
					styleLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
				}
				
				// Search in active solution (if different from target)
				if (!targetProject.equals(servoyProject))
				{
					projectPath = servoyProject.getProject().getLocation().toOSString();
					solutionName = servoyProject.getSolution().getName();
					result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
					
					if (!result.startsWith("Class '") || !result.contains("not found"))
					{
						allMatchingStyles.add(result);
						styleLocations.add(servoyProject.getProject().getName() + " (active solution)");
					}
				}
				
				// Search in all modules
				ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
				for (ServoyProject module : modules)
				{
					if (module != null && module.getSolution() != null && 
					    !module.equals(targetProject) && !module.equals(servoyProject))
					{
						projectPath = module.getProject().getLocation().toOSString();
						solutionName = module.getSolution().getName();
						String moduleResult = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
						
						if (!moduleResult.startsWith("Class '") || !moduleResult.contains("not found"))
						{
							allMatchingStyles.add(moduleResult);
							styleLocations.add(module.getProject().getName());
						}
					}
				}
				
				// Build result showing ALL matching styles
				if (allMatchingStyles.isEmpty())
				{
					return successResult("Class '." + className + "' not found in any context");
				}
				else if (allMatchingStyles.size() == 1)
				{
					String finalResult = "Style from " + styleLocations.get(0) + ":\n" + allMatchingStyles.get(0) +
					                     "\n\n[Context remains: " + contextDisplay + "]";
					return successResult(finalResult);
				}
				else
				{
					StringBuilder sb = new StringBuilder();
					sb.append("Style '.").append(className).append("' found in ").append(allMatchingStyles.size()).append(" locations:\n\n");
					for (int i = 0; i < allMatchingStyles.size(); i++)
					{
						sb.append("=== From ").append(styleLocations.get(i)).append(" ===\n");
						sb.append(allMatchingStyles.get(i));
						if (i < allMatchingStyles.size() - 1)
						{
							sb.append("\n\n");
						}
					}
					sb.append("\n\n[Context remains: ").append(contextDisplay).append("]");
					return successResult(sb.toString());
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error getting style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleGetStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: listStyles
	// =============================================

	private McpSchema.CallToolResult handleListStyles(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();

			String lessFileName = extractString(args, "lessFileName", null);
			String scope = extractString(args, "scope", "current");

			// Validate scope parameter
			if (!scope.equals("current") && !scope.equals("all"))
			{
				return errorResult("Invalid scope value '" + scope + "'. Must be 'current' or 'all'.");
			}

			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				
				if (servoyProject == null)
				{
					return errorResult("No active Servoy solution project found");
				}
				
				if ("current".equals(scope))
				{
					// List from current context only
					String projectPath = getProjectPath();
					String solutionName = getSolutionName();
					String result = StyleService.listStyles(projectPath, solutionName, lessFileName);
					return successResult(result);
				}
				else
				{
					// List from all modules with origin information
					StringBuilder allStyles = new StringBuilder();
					ServoyProject targetProject = resolveTargetProject(servoyModel);
					String activeSolutionName = servoyProject.getSolution().getName();
					
					// Add current context styles
					String contextName = targetProject.getProject().getName();
					String projectPath = targetProject.getProject().getLocation().toOSString();
					String solutionName = targetProject.getSolution().getName();
					String result = StyleService.listStyles(projectPath, solutionName, lessFileName);
					
					if (!result.startsWith("No CSS classes") && !result.startsWith("LESS file not found"))
					{
						String origin = solutionName.equals(activeSolutionName) ? " (in: active solution)" : " (in: " + contextName + ")";
						allStyles.append("Styles from ").append(solutionName).append(".less").append(origin).append(":\n");
						allStyles.append("  ").append(result).append("\n\n");
					}
					
					// Add active solution styles (if different from current context)
					if (!targetProject.equals(servoyProject))
					{
						projectPath = servoyProject.getProject().getLocation().toOSString();
						solutionName = servoyProject.getSolution().getName();
						result = StyleService.listStyles(projectPath, solutionName, lessFileName);
						
						if (!result.startsWith("No CSS classes") && !result.startsWith("LESS file not found"))
						{
							allStyles.append("Styles from ").append(solutionName).append(".less (in: active solution):\n");
							allStyles.append("  ").append(result).append("\n\n");
						}
					}
					
					// Add module styles
					ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : modules)
					{
						if (module != null && module.getSolution() != null && !module.equals(targetProject) && !module.equals(servoyProject))
						{
							projectPath = module.getProject().getLocation().toOSString();
							solutionName = module.getSolution().getName();
							result = StyleService.listStyles(projectPath, solutionName, lessFileName);
							
							if (!result.startsWith("No CSS classes") && !result.startsWith("LESS file not found"))
							{
								allStyles.append("Styles from ").append(solutionName).append(".less (in: ").append(module.getProject().getName()).append("):\n");
								allStyles.append("  ").append(result).append("\n\n");
							}
						}
					}
					
					if (allStyles.length() == 0)
					{
						return successResult("No CSS classes found in any module");
					}
					
					return successResult(allStyles.toString().trim());
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error listing styles", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleListStyles", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// TOOL: deleteStyle
	// =============================================

	private McpSchema.CallToolResult handleDeleteStyle(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();

			String className = extractString(args, "className", null);
			String lessFileName = extractString(args, "lessFileName", null);

			if (className == null || className.trim().isEmpty())
			{
				return errorResult("'className' parameter is required");
			}

			// Remove leading dot if provided
			if (className.startsWith("."))
			{
				className = className.substring(1);
			}

			try
			{
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
				ServoyProject servoyProject = servoyModel.getActiveProject();
				
				if (servoyProject == null)
				{
					return errorResult("No active Servoy solution project found");
				}
				
				// Resolve current context
				ServoyProject targetProject = resolveTargetProject(servoyModel);
				String targetContext = ContextService.getInstance().getCurrentContext();
				String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
				
				// Search for style across all contexts
				String foundInContext = null;
				String foundProjectPath = null;
				String foundSolutionName = null;
				
				// Check current context first
				String projectPath = targetProject.getProject().getLocation().toOSString();
				String solutionName = targetProject.getSolution().getName();
				String result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
				
				if (!result.startsWith("Class '") || !result.contains("not found"))
				{
					foundInContext = targetContext;
					foundProjectPath = projectPath;
					foundSolutionName = solutionName;
				}
				
				// If not found in current context, search active solution
				if (foundInContext == null && !targetProject.equals(servoyProject))
				{
					projectPath = servoyProject.getProject().getLocation().toOSString();
					solutionName = servoyProject.getSolution().getName();
					result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
					
					if (!result.startsWith("Class '") || !result.contains("not found"))
					{
						foundInContext = "active";
						foundProjectPath = projectPath;
						foundSolutionName = solutionName;
					}
				}
				
				// If still not found, search all modules
				if (foundInContext == null)
				{
					ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : modules)
					{
						if (module != null && module.getSolution() != null && 
						    !module.equals(targetProject) && !module.equals(servoyProject))
						{
							projectPath = module.getProject().getLocation().toOSString();
							solutionName = module.getSolution().getName();
							result = StyleService.getStyle(projectPath, solutionName, lessFileName, className);
							
							if (!result.startsWith("Class '") || !result.contains("not found"))
							{
								foundInContext = module.getProject().getName();
								foundProjectPath = projectPath;
								foundSolutionName = solutionName;
								break;
							}
						}
					}
				}
				
				// If not found anywhere, return error
				if (foundInContext == null)
				{
					return errorResult("Style '." + className + "' not found in any context");
				}
				
				// If found in different context, request approval
				if (!foundInContext.equals(targetContext))
				{
					String locationDisplay = "active".equals(foundInContext) ? 
						servoyProject.getProject().getName() + " (active solution)" : foundInContext;
					
					StringBuilder approvalMsg = new StringBuilder();
					approvalMsg.append("Current context: ").append(contextDisplay).append("\n\n");
					approvalMsg.append("Style '.").append(className).append("' found in ").append(locationDisplay).append(".\n");
					approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
					approvalMsg.append("To delete this style, I need to switch to ").append(locationDisplay).append(".\n");
					approvalMsg.append("Do you want to proceed?\n\n");
					approvalMsg.append("[If yes, I will: setContext({context: \"").append(foundInContext).append("\"}) then delete]");
					
					return successResult(approvalMsg.toString());
				}
				
				// Style is in current context - proceed with delete
				String error = StyleService.deleteStyle(foundProjectPath, foundSolutionName, lessFileName, className);

				if (error != null)
				{
					return errorResult(error);
				}

				String targetFile = (lessFileName != null && !lessFileName.trim().isEmpty()) 
					? lessFileName 
					: foundSolutionName + ".less";
				
				if (!targetFile.endsWith(".less"))
				{
					targetFile += ".less";
				}
				
				// Refresh any open form to reflect style changes
				refreshCurrentForm(targetFile);
				
				return successResult("Successfully deleted style '." + className + "' from " + targetFile);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error deleting style", e);
				return errorResult(e.getMessage());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleDeleteStyle", e);
			return errorResult("Unexpected error: " + e.getMessage());
		}
	}

	// =============================================
	// HELPER METHODS
	// =============================================

	private String getProjectPath() throws Exception
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		
		// Resolve target project based on current context
		ServoyProject targetProject = resolveTargetProject(servoyModel);
		
		if (targetProject == null)
		{
			throw new Exception("No target solution/module found for current context");
		}
		return targetProject.getProject().getLocation().toOSString();
	}

	private String getSolutionName() throws Exception
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		
		// Resolve target project based on current context
		ServoyProject targetProject = resolveTargetProject(servoyModel);
		
		if (targetProject == null)
		{
			throw new Exception("No target solution/module found for current context");
		}
		if (targetProject.getSolution() == null)
		{
			throw new Exception("No solution found in target project");
		}
		return targetProject.getSolution().getName();
	}

	/**
	 * Refreshes the currently open form (if any) to reflect style changes.
	 * This triggers a resource change notification without closing/reopening the editor.
	 * 
	 * @param lessFileName Name of the LESS file that was modified
	 */
	private void refreshCurrentForm(String lessFileName)
	{
		try
		{
			String projectPath = getProjectPath();
			
			// Refresh on UI thread
			Display.getDefault().asyncExec(() -> {
				try
				{
					FormService.refreshFormAfterStyleChange(projectPath, lessFileName);
				}
				catch (Exception e)
				{
					ServoyLog.logError("[StyleHandler] Error triggering style refresh: " + e.getMessage(), e);
				}
			});
		}
		catch (Exception e)
		{
			ServoyLog.logError("[StyleHandler] Error in refreshCurrentForm: " + e.getMessage(), e);
		}
	}
}
