package com.servoy.eclipse.knowledgebase.mcp.handlers.core;

import com.servoy.eclipse.knowledgebase.mcp.handlers.AbstractPersistenceHandler;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.knowledgebase.mcp.services.ContextService;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Forms handler - all FORMS intent tools
 * Tools: openForm, setMainForm, listForms
 */
public class FormToolHandler extends AbstractPersistenceHandler
{
	@Override
	public String getHandlerName()
	{
		return "FormToolHandler";
	}

	@Override
	public void registerTools(McpSyncServer server)
	{
		// Utility tool: getCurrentForm
		ToolHandlerRegistry.registerTool(
			server,
			"getCurrentForm",
			"Gets the name of the currently opened form in the active editor. " +
			"No parameters required. " +
			"Returns: The form name if a form is currently open in the editor, or an error if no form is open. " +
			"Use this when the user refers to 'current form', 'this form', 'active form', or doesn't specify a form name.",
			this::handleGetCurrentForm);
		
		// Main tool: openForm (handles both create and open)
		ToolHandlerRegistry.registerTool(
			server,
			"openForm",
			"Opens an existing form or creates a new form. " +
			"[CONTEXT-AWARE for CREATE] When create=true, new form will be created in current context (active solution or module). " +
			"Use getContext to check where it will be created, setContext to change target location. " +
			"Required: name (string). " +
			"Optional: create (boolean, default false - if true, creates form if it doesn't exist), " +
			"width (int, default 640), height (int, default 480), " +
			"style (string: 'css' or 'responsive', default 'css'), " +
			"dataSource (string, format: 'db:/server_name/table_name'), " +
			"extendsForm (string: parent form name for inheritance), " +
			"setAsMainForm (boolean, default false - sets as solution's first form), " +
			"properties (object: map of form properties to set, e.g., {\"minWidth\": 800, \"showInMenu\": true}).",
			this::handleOpenForm);
		
		// Helper tool: setMainForm
		ToolHandlerRegistry.registerTool(
			server,
			"setMainForm",
			"Sets the solution's main/first form (the form that loads when the solution starts). " +
			"Required: name (string) - the form name to set as main form.",
			this::handleSetMainForm);
		
		// Helper tool: listForms
		ToolHandlerRegistry.registerTool(
			server,
			"listForms",
			"Lists forms in the active solution and its modules. " +
			"Optional: scope (string: 'current' or 'all', default 'all'). " +
			"  - 'current': Returns forms from current context only (active solution or specific module based on current context). " +
			"  - 'all': Returns forms from active solution and all modules. " +
			"Returns: List of form names including origin information (which solution/module each form belongs to).",
			this::handleListForms);
		
		// Helper tool: getFormProperties
		ToolHandlerRegistry.registerTool(
			server,
			"getFormProperties",
			"Gets the properties of a form. " +
			"Required: name (string) - the form name. " +
			"Returns: Form properties including width, height, dataSource, style type, and other settings.",
			this::handleGetFormProperties);
	}

	// =============================================
	// TOOL: openForm
	// =============================================

	private McpSchema.CallToolResult handleOpenForm(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			
			// Extract parameters
			String name = extractString(args, "name", null);
			boolean create = extractBoolean(args, "create", false);
			int width = extractInt(args, "width", 640);
			int height = extractInt(args, "height", 480);
			String style = extractString(args, "style", "css");
			String dataSource = extractString(args, "dataSource", null);
			String extendsForm = extractString(args, "extendsForm", null);
			boolean setAsMainForm = extractBoolean(args, "setAsMainForm", false);
			Map<String, Object> properties = extractMap(args, "properties");
			
			// Validate required parameters
			if (name == null || name.trim().isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}
			
			// Validate style
			if (!style.equals("css") && !style.equals("responsive"))
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: Invalid style value: " + style + ". Must be 'css' or 'responsive'.")))
					.isError(true)
					.build();
			}
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = openOrCreateForm(name, create, width, height, style, dataSource, extendsForm, setAsMainForm, properties, args);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error opening/creating form: " + name, exception[0]);
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
			ServoyLog.logError("Unexpected error in handleOpenForm", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	// =============================================
	// TOOL: setMainForm
	// =============================================
	
	private McpSchema.CallToolResult handleSetMainForm(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			String name = extractString(args, "name", null);
			
			if (name == null || name.trim().isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}
			
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = setMainForm(name);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error setting main form: " + name, exception[0]);
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
			ServoyLog.logError("Unexpected error in handleSetMainForm", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	// =============================================
	// TOOL: listForms
	// =============================================
	
	private McpSchema.CallToolResult handleListForms(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
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
			
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			final String finalScope = scope;
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = listForms(finalScope);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error listing forms", exception[0]);
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
			ServoyLog.logError("Unexpected error in handleListForms", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}

	// =============================================
	// TOOL: getFormProperties
	// =============================================
	
	private McpSchema.CallToolResult handleGetFormProperties(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			Map<String, Object> args = request.arguments();
			String name = extractString(args, "name", null);
			
			if (name == null || name.trim().isEmpty())
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}
			
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = getFormProperties(name);
				}
				catch (Exception e)
				{
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				ServoyLog.logError("Error getting form properties: " + name, exception[0]);
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
			ServoyLog.logError("Unexpected error in handleGetFormProperties", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}

	// =============================================
	// CORE LOGIC METHODS
	// =============================================
	
	/**
	 * Opens an existing form or creates a new one if it doesn't exist (and create=true).
	 * For opening (create=false): searches current context first, then falls back to active solution and modules.
	 * For creating (create=true): creates ONLY in current context.
	 */
	private String openOrCreateForm(String name, boolean create, int width, int height, String style,
		String dataSource, String extendsForm, boolean setAsMainForm, Map<String, Object> properties, Map<String, Object> args) throws RepositoryException
	{
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

		if (targetProject == null)
		{
			throw new RepositoryException("No target solution/module found for context: " + targetContext);
		}

		if (targetProject.getEditingSolution() == null)
		{
			throw new RepositoryException("Cannot get editing solution from target: " + targetContext);
		}

		// For READ operations (opening existing form): Search ALL contexts and collect ALL matches
		// Context NEVER changes for READ operations
		java.util.List<Form> allMatchingForms = new java.util.ArrayList<>();
		java.util.List<String> formLocations = new java.util.ArrayList<>();
		
		// Search in target context first
		Form formInTarget = targetProject.getEditingSolution().getForm(name);
		if (formInTarget != null)
		{
			allMatchingForms.add(formInTarget);
			formLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
		}
		
		// Search in active solution (if different from target)
		if (!targetProject.equals(servoyProject))
		{
			Form formInActive = servoyProject.getEditingSolution().getForm(name);
			if (formInActive != null && !allMatchingForms.contains(formInActive))
			{
				allMatchingForms.add(formInActive);
				formLocations.add(servoyProject.getProject().getName() + " (active solution)");
			}
		}
		
		// Search in ALL modules
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		for (ServoyProject module : modules)
		{
			if (module != null && module.getEditingSolution() != null && 
			    !module.equals(targetProject) && !module.equals(servoyProject))
			{
				Form formInModule = module.getEditingSolution().getForm(name);
				if (formInModule != null && !allMatchingForms.contains(formInModule))
				{
					allMatchingForms.add(formInModule);
					formLocations.add(module.getProject().getName());
				}
			}
		}
		
		boolean isNewForm = false;
		boolean isFirstForm = false;
		Form form = null; // Primary form reference for backward compatibility

		if (!allMatchingForms.isEmpty())
		{
			form = allMatchingForms.get(0); // Use first match as primary for compatibility
			// Will open ALL matches below
		}
		else if (create)
		{
			// Check if this will be the first form in the target solution
			java.util.Iterator<Form> existingForms = targetProject.getEditingSolution().getForms(null, true);
			isFirstForm = !existingForms.hasNext();

			if (isFirstForm)
			{
			}

			// Form doesn't exist and create=true - create it in target project (current context)
			form = createNewForm(targetProject, name, width, height, style, dataSource);
			isNewForm = true;
		}
		else
		{
			// Form doesn't exist and create=false - error
			throw new RepositoryException("Form '" + name + "' does not exist in current context, active solution, or any modules. Use create=true to create it.");
		}
		
		// Merge width/height into properties if explicitly provided for existing forms
		// This allows: openForm(name="MyForm", width=800, height=600) to work for updates
		if (!isNewForm && args != null)
		{
			// Initialize properties map if null
			if (properties == null)
			{
				properties = new java.util.HashMap<>();
			}
			
			// Check if width was explicitly provided in the original arguments
			if (args.containsKey("width") && !properties.containsKey("width"))
			{
				properties.put("width", width);
			}
			
			// Check if height was explicitly provided in the original arguments
			if (args.containsKey("height") && !properties.containsKey("height"))
			{
				properties.put("height", height);
			}
		}
		
		// Check if UPDATE operation is requested (properties, extendsForm, or setAsMainForm)
		boolean isUpdateOperation = (properties != null && !properties.isEmpty()) || 
		                            (extendsForm != null && !extendsForm.trim().isEmpty()) ||
		                            setAsMainForm;
		
		// If UPDATE operation and form not in current context, request approval
		if (!isNewForm && isUpdateOperation)
		{
			// Check if form is in current context
			Form formInCurrentContext = targetProject.getEditingSolution().getForm(name);
			
			if (formInCurrentContext == null)
			{
				// UPDATE operation but form not in current context - need approval
				String foundLocation = null;
				
				// Find where it is
				if (!targetProject.equals(servoyProject) && servoyProject.getEditingSolution().getForm(name) != null)
				{
					foundLocation = "active";
				}
				else
				{
					ServoyProject[] projectModules = servoyModel.getModulesOfActiveProject();
					for (ServoyProject module : projectModules)
					{
						if (module != null && module.getEditingSolution() != null && !module.equals(targetProject))
						{
							if (module.getEditingSolution().getForm(name) != null)
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
					String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
					StringBuilder approvalMsg = new StringBuilder();
					approvalMsg.append("Current context: ").append(contextDisplay).append("\n\n");
					approvalMsg.append("Form '").append(name).append("' found in ").append(locationDisplay).append(".\n");
					approvalMsg.append("Current context is ").append(contextDisplay).append(".\n\n");
					approvalMsg.append("To update this form's properties, I need to switch to ").append(locationDisplay).append(".\n");
					approvalMsg.append("Do you want to proceed?\n\n");
					approvalMsg.append("[If yes, I will: setContext({context: \"").append(foundLocation).append("\"}) then update properties]");
					
					return approvalMsg.toString();
				}
			}
		}
		
		// Apply additional properties if provided (only if in current context or no approval needed)
		boolean propertiesModified = false;
		if (properties != null && !properties.isEmpty())
		{
			applyFormProperties(form, properties, targetProject);
			propertiesModified = true;
		}

		// Set form parent (inheritance) if specified
		if (extendsForm != null && !extendsForm.trim().isEmpty())
		{
			setFormParent(form, extendsForm, targetProject);
			propertiesModified = true;
		}

		// Set as main form if requested OR if this is the first form in the solution
		if (setAsMainForm || isFirstForm)
		{
			targetProject.getEditingSolution().setFirstFormID(form.getUUID().toString());
			propertiesModified = true;
		}

		// Save if modifications were made
		if (propertiesModified || isNewForm)
		{
			targetProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
		}

		// Open form(s) in designer
		// For READ operations (existing forms): Open ALL matching forms
		// For CREATE operations: Open the newly created form
		if (isNewForm)
		{
			final Form finalForm = form;
			Display.getDefault().asyncExec(() -> {
				EditorUtil.openFormDesignEditor(finalForm, true, true);
			});
		}
		else if (!allMatchingForms.isEmpty())
		{
			// READ operation: Open ALL matching forms in separate editors
			// Context NEVER changes
			final java.util.List<Form> formsToOpen = new java.util.ArrayList<>(allMatchingForms);
			Display.getDefault().asyncExec(() -> {
				for (Form formToOpen : formsToOpen)
				{
					EditorUtil.openFormDesignEditor(formToOpen, false, true);
				}
			});
		}

		// Build result message with context information
		StringBuilder result = new StringBuilder();
		String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;

		if (isNewForm)
		{
			String formType = "responsive".equals(style) ? "responsive" : "CSS-positioned";
			result.append("Form '").append(name).append("' created successfully in ").append(contextDisplay).append(" as ").append(formType).append(" form (")
				.append(width).append("x").append(height).append(" pixels)");
			if (dataSource != null)
			{
				result.append(" with datasource: ").append(dataSource);
			}
		}
		else
		{
			// READ operation result: Show ALL matches
			if (allMatchingForms.size() == 1)
			{
				result.append("Form '").append(name).append("' opened successfully");
				result.append(" (from ").append(formLocations.get(0)).append(")");
			}
			else
			{
				result.append("Form '").append(name).append("' found in ").append(allMatchingForms.size()).append(" locations. Opened all:\n");
				for (int i = 0; i < allMatchingForms.size(); i++)
				{
					result.append("  - ").append(formLocations.get(i)).append("\n");
				}
			}
			result.append("\n[Context remains: ").append(contextDisplay).append("]");
		}

		if (propertiesModified && !isNewForm)
		{
			result.append(". Properties updated");
		}

		if (setAsMainForm)
		{
			result.append(". Set as solution's main form");
		}
		else if (isFirstForm)
		{
			result.append(". Automatically set as main form (first form in solution)");
		}

		return result.toString();
	}
	
	/**
	 * Creates a new form with the specified parameters.
	 */
	private Form createNewForm(ServoyProject servoyProject, String formName, int width, int height, 
		String style, String dataSource) throws RepositoryException
	{
		// Create validator
		IValidateName validator = new ScriptNameValidator(servoyProject.getEditingFlattenedSolution());

		// Create the form with dimensions
		Dimension size = new Dimension(width, height);
		Form form = servoyProject.getEditingSolution().createNewForm(validator, null, formName, dataSource, true, size);

		boolean isResponsive = "responsive".equalsIgnoreCase(style);

		if (!isResponsive)
		{
			// Create default CSS-positioned form
			form.createNewPart(Part.BODY, height);
			form.setUseCssPosition(Boolean.TRUE);
		}
		else
		{
			// Create responsive layout form
			form.setResponsiveLayout(true);
		}
		
		return form;
	}
	
	/**
	 * Applies a map of properties to a form.
	 */
	private void applyFormProperties(Form form, Map<String, Object> properties, ServoyProject servoyProject) throws RepositoryException
	{
		Object propValue = null;
		String propName = null;
		try
		{
			for (Map.Entry<String, Object> entry : properties.entrySet())
			{
				propName = entry.getKey();
				propValue = entry.getValue();
			
				switch (propName)
				{
					case "width":
						if (propValue instanceof Number)
						{
							form.setWidth(((Number)propValue).intValue());
						}
						break;
						
					case "height":
						if (propValue instanceof Number)
						{
							form.setHeight(((Number)propValue).intValue());
						}
						break;
						
					case "minWidth":
					case "useMinWidth":
						// Min width is controlled by useMinWidth boolean property
						if (propValue instanceof Boolean)
						{
							form.setUseMinWidth((Boolean)propValue);
						}
						break;
						
					case "minHeight":
					case "useMinHeight":
						// Min height is controlled by useMinHeight boolean property
						if (propValue instanceof Boolean)
						{
							form.setUseMinHeight((Boolean)propValue);
						}
						break;
						
					case "dataSource":
						if (propValue != null)
						{
							form.setDataSource(propValue.toString());
						}
						break;
						
					case "showInMenu":
						if (propValue instanceof Boolean)
						{
							form.setShowInMenu((Boolean)propValue);
						}
						break;
						
					case "styleName":
						if (propValue != null)
						{
							form.setStyleName(propValue.toString());
						}
						break;
						
					case "navigatorID":
					case "navigator":
						if (propValue != null)
						{
							form.setNavigatorID(propValue.toString());
						}
						break;
						
					case "initialSort":
						if (propValue != null)
						{
							form.setInitialSort(propValue.toString());
						}
						break;
						
					default:
						break;
				}
			}
		}
		catch (Exception e)
		{
			throw new RepositoryException("Error setting property '" + propName + "': " + e.getMessage());
		}
	}
	
	/**
	 * Sets the parent form (inheritance).
	 */
	private void setFormParent(Form form, String parentFormName, ServoyProject servoyProject) throws RepositoryException
	{
		Form parentForm = servoyProject.getEditingSolution().getForm(parentFormName);
		
		if (parentForm == null)
		{
			throw new RepositoryException("Parent form '" + parentFormName + "' not found");
		}
		
		form.setExtendsID(parentForm.getUUID().toString());
	}
	
	/**
	 * Sets the solution's main/first form.
	 */
	private String setMainForm(String formName) throws RepositoryException
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
		
		Form form = servoyProject.getEditingSolution().getForm(formName);
		
		if (form == null)
		{
			throw new RepositoryException("Form '" + formName + "' not found");
		}
		
		servoyProject.getEditingSolution().setFirstFormID(form.getUUID().toString());
		servoyProject.saveEditingSolutionNodes(new IPersist[] { servoyProject.getEditingSolution() }, true);
		
		return "Form '" + formName + "' set as solution's main form";
	}
	
	/**
	 * Lists all forms in the active solution.
	 */
	private String listForms(String scope) throws RepositoryException
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
		
		String activeSolutionName = servoyProject.getEditingSolution().getName();
		String contextName = null;
		
		// Collect all forms based on scope
		java.util.List<Form> forms = new java.util.ArrayList<>();
		
		if ("current".equals(scope))
		{
			// Get forms from current context only
			ServoyProject targetProject = resolveTargetProject(servoyModel);
			String context = ContextService.getInstance().getCurrentContext();
			contextName = "active".equals(context) ? activeSolutionName : context;
			
			java.util.Iterator<Form> formsIterator = targetProject.getEditingSolution().getForms(null, false); // false = no modules
			while (formsIterator.hasNext())
			{
				forms.add(formsIterator.next());
			}
		}
		else
		{
			// Get forms from active solution and all modules
			// First, add active solution forms
			java.util.Iterator<Form> activeForms = servoyProject.getEditingSolution().getForms(null, false); // false = only active
			while (activeForms.hasNext())
			{
				forms.add(activeForms.next());
			}
			
			// Then, add forms from each module (excluding active project to avoid duplication)
			ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
			for (ServoyProject module : modules)
			{
				if (module != null && module.getEditingSolution() != null)
				{
					// Skip if this module is actually the active project itself (prevent duplication)
					if (module.equals(servoyProject))
					{
						continue;
					}
					
					java.util.Iterator<Form> moduleForms = module.getEditingSolution().getForms(null, false); // false = only this module
					while (moduleForms.hasNext())
					{
						forms.add(moduleForms.next());
					}
				}
			}
		}
		
		if (forms.isEmpty())
		{
			return "No forms found" + ("current".equals(scope) ? " in '" + contextName + "'" : " in the active solution");
		}
		
		// Get main form if set
		String mainFormUUID = servoyProject.getEditingSolution().getFirstFormID();
		String mainFormName = null;
		if (mainFormUUID != null)
		{
			Form mainForm = servoyProject.getEditingSolution().getForm(mainFormUUID);
			if (mainForm != null)
			{
				mainFormName = mainForm.getName();
			}
		}
		
		StringBuilder result = new StringBuilder();
		
		// Build appropriate header based on scope
		if ("current".equals(scope))
		{
			result.append("Forms in '").append(contextName).append("' (").append(forms.size()).append(" total):\n");
		}
		else
		{
			result.append("Forms in solution '").append(activeSolutionName).append("' and modules (").append(forms.size()).append(" total):\n");
		}
		
		for (Form form : forms)
		{
			String formName = form.getName();
			String solutionName = getSolutionName(form);
			String originInfo = formatOrigin(solutionName, activeSolutionName);
			
			result.append("- ").append(formName).append(originInfo);
			if (formName.equals(mainFormName))
			{
				result.append(" [MAIN FORM]");
			}
			result.append("\n");
		}
		
		return result.toString();
	}
	
	/**
	 * Gets properties of form(s). READ operation - displays ALL matches if found in multiple locations.
	 * Searches in current context first, then falls back to active solution and all modules.
	 */
	private String getFormProperties(String formName) throws RepositoryException
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
		
		// Resolve current context
		ServoyProject targetProject = resolveTargetProject(servoyModel);
		String targetContext = ContextService.getInstance().getCurrentContext();
		
		// READ operation: Search ALL contexts and collect ALL matches
		java.util.List<Form> allMatchingForms = new java.util.ArrayList<>();
		java.util.List<String> formLocations = new java.util.ArrayList<>();
		
		// Search in target context
		if (targetProject != null && targetProject.getEditingSolution() != null)
		{
			Form formInTarget = targetProject.getEditingSolution().getForm(formName);
			if (formInTarget != null)
			{
				allMatchingForms.add(formInTarget);
				formLocations.add(targetContext.equals("active") ? targetProject.getProject().getName() + " (active solution)" : targetContext);
			}
		}
		
		// Search in active solution (if different from target)
		if (!targetProject.equals(servoyProject))
		{
			Form formInActive = servoyProject.getEditingSolution().getForm(formName);
			if (formInActive != null && !allMatchingForms.contains(formInActive))
			{
				allMatchingForms.add(formInActive);
				formLocations.add(servoyProject.getProject().getName() + " (active solution)");
			}
		}
		
		// Search in all modules
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		for (ServoyProject module : modules)
		{
			if (module != null && module.getEditingSolution() != null && 
			    !module.equals(targetProject) && !module.equals(servoyProject))
			{
				Form formInModule = module.getEditingSolution().getForm(formName);
				if (formInModule != null && !allMatchingForms.contains(formInModule))
				{
					allMatchingForms.add(formInModule);
					formLocations.add(module.getProject().getName());
				}
			}
		}
		
		if (allMatchingForms.isEmpty())
		{
			throw new RepositoryException("Form '" + formName + "' not found in current context, active solution, or any modules");
		}
		
		// Build result showing ALL matching forms
		StringBuilder result = new StringBuilder();
		String contextDisplay = "active".equals(targetContext) ? targetProject.getProject().getName() + " (active solution)" : targetContext;
		
		if (allMatchingForms.size() == 1)
		{
			// Single match - standard output
			Form form = allMatchingForms.get(0);
			result.append("Form Properties for '").append(formName).append("':\n");
			result.append("Location: ").append(formLocations.get(0)).append("\n\n");
			result.append(getFormPropertiesDetails(form, servoyProject));
		}
		else
		{
			// Multiple matches - show all
			result.append("Form '").append(formName).append("' found in ").append(allMatchingForms.size()).append(" locations:\n\n");
			for (int i = 0; i < allMatchingForms.size(); i++)
			{
				result.append("=== Form from: ").append(formLocations.get(i)).append(" ===\n");
				result.append(getFormPropertiesDetails(allMatchingForms.get(i), servoyProject));
				if (i < allMatchingForms.size() - 1)
				{
					result.append("\n");
				}
			}
		}
		
		result.append("\n[Context remains: ").append(contextDisplay).append("]");
		return result.toString();
	}
	
	/**
	 * Helper to extract detailed form properties.
	 */
	private String getFormPropertiesDetails(Form form, ServoyProject servoyProject)
	{
		StringBuilder result = new StringBuilder();
		
		// Basic properties
		result.append("Dimensions:\n");
		result.append("  width: ").append(form.getWidth()).append(" px\n");
		result.append("  height: ").append(form.getHeight()).append(" px\n");
		result.append("  useMinWidth: ").append(form.getUseMinWidth()).append("\n");
		result.append("  useMinHeight: ").append(form.getUseMinHeight()).append("\n\n");
		
		// Form type
		result.append("Form Type:\n");
		if (form.isResponsiveLayout())
		{
			result.append("  type: responsive\n");
		}
		else if (form.getUseCssPosition() != null && form.getUseCssPosition())
		{
			result.append("  type: css-positioned\n");
		}
		else
		{
			result.append("  type: absolute-positioned\n");
		}
		result.append("\n");
		
		// Data source
		result.append("Data:\n");
		String dataSource = form.getDataSource();
		result.append("  dataSource: ").append(dataSource != null ? dataSource : "none").append("\n\n");
		
		// Form settings
		result.append("Settings:\n");
		result.append("  showInMenu: ").append(form.getShowInMenu()).append("\n");
		
		String styleName = form.getStyleName();
		result.append("  styleName: ").append(styleName != null && !styleName.isEmpty() ? styleName : "default").append("\n");
		
		String navigatorID = form.getNavigatorID();
		result.append("  navigatorID: ").append(navigatorID != null && !navigatorID.isEmpty() ? navigatorID : "default").append("\n");
		
		String initialSort = form.getInitialSort();
		result.append("  initialSort: ").append(initialSort != null && !initialSort.isEmpty() ? initialSort : "none").append("\n\n");
		
		// Inheritance
		String extendsID = form.getExtendsID();
		if (extendsID != null && !extendsID.isEmpty())
		{
			Form parentForm = servoyProject.getEditingSolution().getForm(extendsID);
			result.append("Inheritance:\n");
			result.append("  extendsForm: ").append(parentForm != null ? parentForm.getName() : extendsID).append("\n\n");
		}
		
		// Check if main form
		String mainFormUUID = servoyProject.getEditingSolution().getFirstFormID();
		boolean isMainForm = form.getUUID().toString().equals(mainFormUUID);
		if (isMainForm)
		{
			result.append("Special Status:\n");
			result.append("  [MAIN FORM] - This form is set as the solution's startup form\n");
		}
		
		return result.toString();
	}
	
	// =============================================
	// TOOL: getCurrentForm
	// =============================================
	
	private McpSchema.CallToolResult handleGetCurrentForm(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		try
		{
			final String[] result = new String[1];
			final String[] errorDetail = new String[1];
			
			Display display = Display.getDefault();
			if (display == null)
			{
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: Cannot access Eclipse Display. UI context not available.")))
					.isError(true)
					.build();
			}
			
			display.syncExec(() -> {
				try
				{
					IWorkbenchPage activePage = EditorUtil.getActivePage();
					
					if (activePage != null)
					{
						IEditorPart activeEditor = activePage.getActiveEditor();
						
						if (activeEditor != null)
						{
							String editorTitle = activeEditor.getTitle();
							String editorClass = activeEditor.getClass().getSimpleName();
							
							// Detect form via ServoyModel lookup by editor title
							// This works because form editors have the form name as their title
							Form form = null;
							try
							{
								IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
								if (servoyModel != null && servoyModel.getActiveProject() != null)
								{
									ServoyProject servoyProject = servoyModel.getActiveProject();
									if (servoyProject.getEditingSolution() != null)
									{
										form = servoyProject.getEditingSolution().getForm(editorTitle);
									}
								}
							}
							catch (Exception e)
							{
								ServoyLog.logError("Error looking up form by editor title", e);
							}
							
							if (form != null)
							{
								result[0] = form.getName();
							}
							else
							{
								// Determine error message based on editor type
								if ("VisualFormEditor".equals(editorClass))
								{
									errorDetail[0] = "A form editor is open ('" + editorTitle + "') but the form could not be found in the solution.";
								}
								else
								{
									errorDetail[0] = "The active editor '" + editorTitle + "' is not a form editor (type: " + editorClass + "). Please open a form in the editor.";
								}
							}
						}
						else
						{
							errorDetail[0] = "No active editor found - no file is currently open or focused";
						}
					}
					else
					{
						errorDetail[0] = "No active workbench page found";
					}
				}
				catch (Exception e)
				{
					errorDetail[0] = "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage();
					ServoyLog.logError("Error detecting current form", e);
				}
			});
			
			if (result[0] == null)
			{
				String detailedError = errorDetail[0] != null ? errorDetail[0] : "No form is currently open in the editor";
				
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + detailedError + ". Please specify the formName parameter explicitly.")))
					.isError(true)
					.build();
			}
			
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Current form: " + result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleGetCurrentForm", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Error: Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}
	
	// =============================================
	// HELPER METHODS - Parameter Extraction
	// =============================================
	
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
			ServoyLog.logError("[FormToolHandler] Error getting solution name", e);
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
