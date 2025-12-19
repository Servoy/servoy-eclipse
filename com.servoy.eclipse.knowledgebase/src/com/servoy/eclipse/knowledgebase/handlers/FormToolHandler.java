package com.servoy.eclipse.knowledgebase.handlers;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.knowledgebase.IToolHandler;
import com.servoy.eclipse.knowledgebase.ToolHandlerRegistry;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Forms handler - all FORMS intent tools
 * Tools: openForm, setMainForm, listForms
 */
public class FormToolHandler implements IToolHandler
{
	@Override
	public String getHandlerName()
	{
		return "FormToolHandler";
	}

	@Override
	public void registerTools(McpSyncServer server)
	{
		System.err.println("[FormToolHandler] registerTools() called");
		
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
			"Opens an existing form or creates a new form in the active solution. " +
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
			"Lists all forms in the active solution. No parameters required.",
			this::handleListForms);
		
		// Helper tool: getFormProperties
		ToolHandlerRegistry.registerTool(
			server,
			"getFormProperties",
			"Gets the properties of a form. " +
			"Required: name (string) - the form name. " +
			"Returns: Form properties including width, height, dataSource, style type, and other settings.",
			this::handleGetFormProperties);
		
		System.err.println("[FormToolHandler] Tools registered: getCurrentForm, openForm, setMainForm, listForms, getFormProperties");
	}

	// =============================================
	// TOOL: openForm
	// =============================================

	private McpSchema.CallToolResult handleOpenForm(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.err.println("========================================");
		System.err.println("[FormToolHandler] handleOpenForm CALLED");
		
		try
		{
			Map<String, Object> args = request.arguments();
			System.err.println("[FormToolHandler] Request arguments: " + args);
			
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
				System.err.println("[FormToolHandler] ERROR: name parameter is required");
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
			
			System.err.println("[FormToolHandler] Extracted parameters - name: " + name + ", create: " + create + 
				", width: " + width + ", height: " + height + ", style: " + style + 
				", dataSource: " + dataSource + ", extendsForm: " + extendsForm + ", setAsMainForm: " + setAsMainForm);
			
			// Execute on UI thread
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					System.err.println("[FormToolHandler] Executing openForm() on UI thread...");
					result[0] = openOrCreateForm(name, create, width, height, style, dataSource, extendsForm, setAsMainForm, properties, args);
					System.err.println("[FormToolHandler] openForm() completed successfully");
				}
				catch (Exception e)
				{
					System.err.println("[FormToolHandler] openForm() threw exception: " + e.getMessage());
					e.printStackTrace();
					exception[0] = e;
				}
			});
			
			if (exception[0] != null)
			{
				System.err.println("[FormToolHandler] Form operation failed");
				ServoyLog.logError("Error opening/creating form: " + name, exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + exception[0].getMessage())))
					.isError(true)
					.build();
			}
			
			System.err.println("[FormToolHandler] Form operation succeeded: " + result[0]);
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
		System.err.println("========================================");
		System.err.println("[FormToolHandler] handleSetMainForm CALLED");
		
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
		System.err.println("========================================");
		System.err.println("[FormToolHandler] handleListForms CALLED");
		
		try
		{
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];
			
			Display.getDefault().syncExec(() -> {
				try
				{
					result[0] = listForms();
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
		System.err.println("========================================");
		System.err.println("[FormToolHandler] handleGetFormProperties CALLED");
		
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
	 */
	private String openOrCreateForm(String name, boolean create, int width, int height, String style, 
		String dataSource, String extendsForm, boolean setAsMainForm, Map<String, Object> properties, Map<String, Object> args) throws RepositoryException
	{
		System.err.println("[FormToolHandler.openOrCreateForm] Processing form: " + name);
		
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
		
		// Check if form already exists
		Form form = servoyProject.getEditingSolution().getForm(name);
		boolean isNewForm = false;
		boolean isFirstForm = false;
		
		if (form != null)
		{
			System.err.println("[FormToolHandler.openOrCreateForm] Form exists, opening: " + name);
			// Form exists - open it
		}
		else if (create)
		{
			System.err.println("[FormToolHandler.openOrCreateForm] Form doesn't exist, creating: " + name);
			
			// Check if this will be the first form in the solution
			java.util.Iterator<Form> existingForms = servoyProject.getEditingSolution().getForms(null, true);
			isFirstForm = !existingForms.hasNext();
			
			if (isFirstForm)
			{
				System.err.println("[FormToolHandler.openOrCreateForm] This is the first form in the solution - will set as main form");
			}
			
			// Form doesn't exist and create=true - create it
			form = createNewForm(servoyProject, name, width, height, style, dataSource);
			isNewForm = true;
		}
		else
		{
			// Form doesn't exist and create=false - error
			throw new RepositoryException("Form '" + name + "' does not exist. Use create=true to create it.");
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
				System.err.println("[FormToolHandler.openOrCreateForm] Adding explicitly provided width=" + width + " to properties for existing form");
				properties.put("width", width);
			}
			
			// Check if height was explicitly provided in the original arguments
			if (args.containsKey("height") && !properties.containsKey("height"))
			{
				System.err.println("[FormToolHandler.openOrCreateForm] Adding explicitly provided height=" + height + " to properties for existing form");
				properties.put("height", height);
			}
		}
		
		// Apply additional properties if provided
		boolean propertiesModified = false;
		if (properties != null && !properties.isEmpty())
		{
			System.err.println("[FormToolHandler.openOrCreateForm] Applying properties: " + properties);
			applyFormProperties(form, properties, servoyProject);
			propertiesModified = true;
		}
		
		// Set form parent (inheritance) if specified
		if (extendsForm != null && !extendsForm.trim().isEmpty())
		{
			System.err.println("[FormToolHandler.openOrCreateForm] Setting parent form: " + extendsForm);
			setFormParent(form, extendsForm, servoyProject);
			propertiesModified = true;
		}
		
		// Set as main form if requested OR if this is the first form in the solution
		if (setAsMainForm || isFirstForm)
		{
			System.err.println("[FormToolHandler.openOrCreateForm] Setting as main form" + 
				(isFirstForm ? " (first form in solution)" : ""));
			servoyProject.getEditingSolution().setFirstFormID(form.getUUID().toString());
			propertiesModified = true;
		}
		
		// Save if modifications were made
		if (propertiesModified || isNewForm)
		{
			servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
		}
		
		// Open the form in designer
		final Form finalForm = form;
		final boolean finalIsNewForm = isNewForm;
		Display.getDefault().asyncExec(() -> {
			EditorUtil.openFormDesignEditor(finalForm, finalIsNewForm, true);
		});
		
		// Build result message
		StringBuilder result = new StringBuilder();
		if (isNewForm)
		{
			String formType = "responsive".equals(style) ? "responsive" : "CSS-positioned";
			result.append("Form '").append(name).append("' created successfully as ").append(formType)
				.append(" form (").append(width).append("x").append(height).append(" pixels)");
			if (dataSource != null)
			{
				result.append(" with datasource: ").append(dataSource);
			}
		}
		else
		{
			result.append("Form '").append(name).append("' opened successfully");
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
		System.err.println("[FormToolHandler.createNewForm] Creating form: " + formName);
		
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
		for (Map.Entry<String, Object> entry : properties.entrySet())
		{
			String propName = entry.getKey();
			Object propValue = entry.getValue();
			
			System.err.println("[FormToolHandler.applyFormProperties] Setting property: " + propName + " = " + propValue);
			
			try
			{
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
						System.err.println("[FormToolHandler.applyFormProperties] WARNING: Unknown property: " + propName);
						break;
				}
			}
			catch (Exception e)
			{
				System.err.println("[FormToolHandler.applyFormProperties] ERROR setting property " + propName + ": " + e.getMessage());
				throw new RepositoryException("Error setting property '" + propName + "': " + e.getMessage());
			}
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
		System.err.println("[FormToolHandler.setFormParent] Set parent form to: " + parentFormName);
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
	private String listForms() throws RepositoryException
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
		
		java.util.Iterator<Form> formsIterator = servoyProject.getEditingSolution().getForms(null, true);
		java.util.List<String> formNames = new java.util.ArrayList<>();
		
		while (formsIterator.hasNext())
		{
			Form form = formsIterator.next();
			formNames.add(form.getName());
		}
		
		if (formNames.isEmpty())
		{
			return "No forms found in the active solution";
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
		result.append("Forms in solution (").append(formNames.size()).append(" total):\n");
		
		for (String formName : formNames)
		{
			result.append("- ").append(formName);
			if (formName.equals(mainFormName))
			{
				result.append(" [MAIN FORM]");
			}
			result.append("\n");
		}
		
		return result.toString();
	}
	
	/**
	 * Gets properties of a form.
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
		
		Form form = servoyProject.getEditingSolution().getForm(formName);
		
		if (form == null)
		{
			throw new RepositoryException("Form '" + formName + "' not found");
		}
		
		StringBuilder result = new StringBuilder();
		result.append("Form Properties for '").append(formName).append("':\n\n");
		
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
		System.err.println("========================================");
		System.err.println("[FormToolHandler] handleGetCurrentForm CALLED");
		
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
							System.err.println("[FormToolHandler] Active editor: '" + editorTitle + "' (type: " + editorClass + ")");
							
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
								System.err.println("[FormToolHandler] Exception during form lookup: " + e.getMessage());
								ServoyLog.logError("Error looking up form by editor title", e);
							}
							
							if (form != null)
							{
								result[0] = form.getName();
								System.err.println("[FormToolHandler] SUCCESS - Form detected: " + result[0]);
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
								System.err.println("[FormToolHandler] FAIL - " + errorDetail[0]);
							}
						}
						else
						{
							errorDetail[0] = "No active editor found - no file is currently open or focused";
							System.err.println("[FormToolHandler] FAIL - " + errorDetail[0]);
						}
					}
					else
					{
						errorDetail[0] = "No active workbench page found";
						System.err.println("[FormToolHandler] FAIL - " + errorDetail[0]);
					}
				}
				catch (Exception e)
				{
					errorDetail[0] = "Exception: " + e.getClass().getSimpleName() + " - " + e.getMessage();
					System.err.println("[FormToolHandler] EXCEPTION - " + errorDetail[0]);
					e.printStackTrace();
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
			System.err.println("[FormToolHandler] EXCEPTION: " + e.getMessage());
			e.printStackTrace();
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
	
	private String extractString(Map<String, Object> args, String key, String defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		return value != null ? value.toString() : defaultValue;
	}
	
	private int extractInt(Map<String, Object> args, String key, int defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		if (value instanceof Number)
		{
			return ((Number)value).intValue();
		}
		try
		{
			return Integer.parseInt(value.toString());
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	private boolean extractBoolean(Map<String, Object> args, String key, boolean defaultValue)
	{
		if (args == null || !args.containsKey(key))
		{
			return defaultValue;
		}
		Object value = args.get(key);
		if (value instanceof Boolean)
		{
			return (Boolean)value;
		}
		return Boolean.parseBoolean(value.toString());
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
}
