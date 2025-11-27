package com.servoy.eclipse.mcp.handlers;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
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
 * Tools: createForm
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
		System.out.println("[FormToolHandler] registerTools() called");
		ToolHandlerRegistry.registerTool(
			server,
			"createForm",
			"Creates a new form in the active solution. Required: name (string). Optional: width (int, default 640), height (int, default 480), style (string: 'css' or 'responsive', default 'css'), dataSource (string, format: 'db:/server_name/table_name').",
			this::handleCreateForm);
		System.out.println("[FormToolHandler] createForm tool registered");
	}

	// =============================================
	// TOOL: createForm
	// =============================================

	private McpSchema.CallToolResult handleCreateForm(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		System.out.println("========================================");
		System.out.println("[FormToolHandler] handleCreateForm CALLED");
		String name = null;
		int width = 640; // default width
		int height = 480; // default height
		String style = "css"; // default style: 'css' or 'responsive'
		String dataSource = null;
		String errorMessage = null;

		try
		{
			Map<String, Object> args = request.arguments();
			System.out.println("[FormToolHandler] Request arguments: " + args);
			if (args != null)
			{
				// Extract name parameter (required)
				if (args.containsKey("name"))
				{
					Object nameObj = args.get("name");
					if (nameObj != null)
					{
						name = nameObj.toString();
					}
				}

				// Extract width parameter (optional)
				if (args.containsKey("width"))
				{
					Object widthObj = args.get("width");
					if (widthObj != null)
					{
						try
						{
							width = Integer.parseInt(widthObj.toString());
						}
						catch (NumberFormatException e)
						{
							errorMessage = "Invalid width value: " + widthObj;
						}
					}
				}

				// Extract height parameter (optional)
				if (args.containsKey("height"))
				{
					Object heightObj = args.get("height");
					if (heightObj != null)
					{
						try
						{
							height = Integer.parseInt(heightObj.toString());
						}
						catch (NumberFormatException e)
						{
							errorMessage = "Invalid height value: " + heightObj;
						}
					}
				}

				// Extract style parameter (optional)
				if (args.containsKey("style"))
				{
					Object styleObj = args.get("style");
					if (styleObj != null)
					{
						style = styleObj.toString().toLowerCase();
						if (!style.equals("css") && !style.equals("responsive"))
						{
							errorMessage = "Invalid style value: " + styleObj + ". Must be 'css' or 'responsive'.";
						}
					}
				}

				// Extract dataSource parameter (optional)
				if (args.containsKey("dataSource"))
				{
					Object dataSourceObj = args.get("dataSource");
					if (dataSourceObj != null)
					{
						dataSource = dataSourceObj.toString();
					}
				}
			}

			// Validate required parameters
			System.out.println("[FormToolHandler] Extracted parameters - name: " + name + ", width: " + width + ", height: " + height + ", style: " + style + ", dataSource: " + dataSource);
			
			if (name == null || name.trim().isEmpty())
			{
				System.out.println("[FormToolHandler] ERROR: name parameter is required");
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: 'name' parameter is required")))
					.isError(true)
					.build();
			}

			if (errorMessage != null)
			{
				System.out.println("[FormToolHandler] ERROR: " + errorMessage);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error: " + errorMessage)))
					.isError(true)
					.build();
			}

			// Execute form creation on UI thread
			System.out.println("[FormToolHandler] Preparing to create form on UI thread...");
			final String finalName = name;
			final int finalWidth = width;
			final int finalHeight = height;
			final String finalStyle = style;
			final String finalDataSource = dataSource;
			final String[] result = new String[1];
			final Exception[] exception = new Exception[1];

			Display.getDefault().syncExec(() -> {
				try
				{
					System.out.println("[FormToolHandler] Executing createForm() on UI thread...");
					result[0] = createForm(finalName, finalWidth, finalHeight, finalStyle, finalDataSource);
					System.out.println("[FormToolHandler] createForm() completed successfully");
				}
				catch (Exception e)
				{
					System.out.println("[FormToolHandler] createForm() threw exception: " + e.getMessage());
					e.printStackTrace();
					exception[0] = e;
				}
			});

			if (exception[0] != null)
			{
				System.out.println("[FormToolHandler] Form creation failed");
				ServoyLog.logError("Error creating form: " + finalName, exception[0]);
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent("Error creating form '" + finalName + "': " + exception[0].getMessage())))
					.isError(true)
					.build();
			}

			System.out.println("[FormToolHandler] Form creation succeeded: " + result[0]);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent(result[0])))
				.build();
		}
		catch (Exception e)
		{
			ServoyLog.logError("Unexpected error in handleCreateForm", e);
			return McpSchema.CallToolResult.builder()
				.content(List.of(new TextContent("Unexpected error: " + e.getMessage())))
				.isError(true)
				.build();
		}
	}

	/**
	 * Create a new form with the specified parameters.
	 * Based on NewFormWizard.performFinish() logic.
	 */
	private String createForm(String formName, int width, int height, String style, String dataSource) throws RepositoryException
	{
		System.out.println("[FormToolHandler.createForm] Starting form creation: " + formName);
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();

		if (servoyProject == null)
		{
			System.out.println("[FormToolHandler.createForm] ERROR: No active Servoy project");
			throw new RepositoryException("No active Servoy solution project found");
		}

		if (servoyProject.getEditingSolution() == null)
		{
			System.out.println("[FormToolHandler.createForm] ERROR: Cannot get editing solution");
			throw new RepositoryException("Cannot get the Servoy Solution from the selected Servoy Project");
		}

		System.out.println("[FormToolHandler.createForm] Active project: " + servoyProject.getProject().getName());
		
		// Create validator
		IValidateName validator = new ScriptNameValidator(servoyProject.getEditingFlattenedSolution());

		// Create the form with dimensions
		Dimension size = new Dimension(width, height);
		System.out.println("[FormToolHandler.createForm] Creating form with size: " + width + "x" + height);
		Form form = servoyProject.getEditingSolution().createNewForm(validator, null, formName, dataSource, true, size);
		System.out.println("[FormToolHandler.createForm] Form created, configuring style: " + style);

		boolean isResponsive = "responsive".equalsIgnoreCase(style);

		if (!isResponsive)
		{
			// Create default CSS-positioned form
			// Create body part (480 height as in the wizard)
			form.createNewPart(Part.BODY, 480);

			// Enable CSS positioning by default
			form.setUseCssPosition(Boolean.TRUE);
		}
		else
		{
			// Create responsive layout form
			form.setResponsiveLayout(true);
		}

		// Save the form
		servoyProject.saveEditingSolutionNodes(new IPersist[] { form }, true);

		// Open the form in designer
		Display.getDefault().asyncExec(() -> {
			EditorUtil.openFormDesignEditor(form, true, true);
		});

		String formType = isResponsive ? "responsive" : "CSS-positioned";
		return "Form '" + formName + "' created successfully as " + formType + " form (" + width + "x" + height + " pixels)" +
			(dataSource != null ? " with datasource: " + dataSource : "");
	}
}
