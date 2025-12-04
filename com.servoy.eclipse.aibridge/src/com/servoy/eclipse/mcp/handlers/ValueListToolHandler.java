package com.servoy.eclipse.mcp.handlers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.mcp.IToolHandler;
import com.servoy.eclipse.mcp.ToolHandlerRegistry;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ValueList;

import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * Value Lists handler - all VALUE_LISTS intent tools
 * Tools: openValueList, deleteValueList, listValueLists
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
			"Opens an existing value list or creates a new value list. Required: name (string). Optional for CUSTOM type: customValues (array of strings). Optional for DATABASE type: dataSource (format: 'server_name/table_name'), displayColumn (string), returnColumn (string), separator (string), sortOptions (string like 'column asc').",
			this::handleOpenValueList));

		tools.put("deleteValueList", new ToolHandlerRegistry.ToolDefinition(
			"Deletes an existing value list. Required: name (string) - the name of the value list to delete.",
			this::handleDeleteValueList));

		tools.put("listValueLists", new ToolHandlerRegistry.ToolDefinition(
			"Retrieves a list of all existing value lists in the active solution. No parameters required.",
			this::handleListValueLists));

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
	// TOOL: openValueList
	// =============================================

	private McpSchema.CallToolResult handleOpenValueList(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		List<String> customValues = null;
		String dataSource = null;
		String displayColumn = null;
		String returnColumn = null;
		String separator = null;
		String sortOptions = null;
		String errorMessage = null;
		boolean isCreate = false;

		try
		{
			Map<String, Object> args = request.arguments();
			if (args != null)
			{
				// Extract name
				if (args.containsKey("name"))
				{
					Object nameObj = args.get("name");
					if (nameObj != null) name = nameObj.toString();
				}

				// Extract CUSTOM_VALUES parameters
				if (args.containsKey("customValues"))
				{
					Object valuesObj = args.get("customValues");
					if (valuesObj instanceof List< ? >)
					{
						customValues = ((List< ? >)valuesObj).stream()
							.map(Object::toString)
							.collect(java.util.stream.Collectors.toList());
					}
				}
				// Legacy support for "values" parameter
				else if (args.containsKey("values"))
				{
					Object valuesObj = args.get("values");
					if (valuesObj instanceof List< ? >)
					{
						customValues = ((List< ? >)valuesObj).stream()
							.map(Object::toString)
							.collect(java.util.stream.Collectors.toList());
					}
				}

				// Extract DATABASE_VALUES parameters
				if (args.containsKey("dataSource"))
				{
					Object dsObj = args.get("dataSource");
					if (dsObj != null) dataSource = dsObj.toString();
				}
				if (args.containsKey("displayColumn"))
				{
					Object dcObj = args.get("displayColumn");
					if (dcObj != null) displayColumn = dcObj.toString();
				}
				if (args.containsKey("returnColumn"))
				{
					Object rcObj = args.get("returnColumn");
					if (rcObj != null) returnColumn = rcObj.toString();
				}
				if (args.containsKey("separator"))
				{
					Object sepObj = args.get("separator");
					if (sepObj != null) separator = sepObj.toString();
				}
				if (args.containsKey("sortOptions"))
				{
					Object sortObj = args.get("sortOptions");
					if (sortObj != null) sortOptions = sortObj.toString();
				}
			}

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Check if value list already exists
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList myVL = servoyModel.getActiveProject().getEditingSolution().getValueList(name);

			if (myVL == null)
			{
				// Value list doesn't exist - create it
				ServoyLog.logInfo("[ValueListToolHandler] Creating value list: " + name);
				isCreate = true;
				myVL = servoyModel.getActiveProject().getEditingSolution().createNewValueList(servoyModel.getNameValidator(), name);

				// Determine type and configure
				boolean isDatabase = (dataSource != null && !dataSource.trim().isEmpty());
				boolean isCustom = (customValues != null && !customValues.isEmpty());

				if (isDatabase)
				{
					// DATABASE_VALUES type (value = 1)
					myVL.setValueListType(1); // DATABASE_VALUES

					// Auto-correct datasource format if needed
					if (!dataSource.startsWith("db:/"))
					{
						if (dataSource.contains("/"))
						{
							dataSource = "db:/" + dataSource;
						}
						else
						{
							errorMessage = "Invalid dataSource format: '" + dataSource +
								"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'";
							return McpSchema.CallToolResult.builder()
								.content(List.of(new TextContent(errorMessage)))
								.build();
						}
					}

					myVL.setDataSource(dataSource);

					// Determine display and return column configuration
					boolean hasDisplayColumn = (displayColumn != null && !displayColumn.trim().isEmpty());
					boolean hasReturnColumn = (returnColumn != null && !returnColumn.trim().isEmpty());

					if (hasDisplayColumn && hasReturnColumn && !displayColumn.equals(returnColumn))
					{
						// Different columns: display one, return another
						// dataProviderID1 = display column (what user sees)
						// dataProviderID2 = return column (what gets stored)
						myVL.setDataProviderID1(displayColumn);
						myVL.setDataProviderID2(returnColumn);
						myVL.setShowDataProviders(1); // Show first column (display)
						myVL.setReturnDataProviders(2); // Return second column (return)
					}
					else if (hasDisplayColumn)
					{
						// Only displayColumn: use it for both display and return
						myVL.setDataProviderID1(displayColumn);
						myVL.setShowDataProviders(1); // Show first column
						myVL.setReturnDataProviders(1); // Return first column
					}
					else if (hasReturnColumn)
					{
						// Only returnColumn: use it for both display and return (backward compatibility)
						myVL.setDataProviderID1(returnColumn);
						myVL.setShowDataProviders(1); // Show first column
						myVL.setReturnDataProviders(1); // Return first column
					}

					// Set separator if provided
					if (separator != null && !separator.trim().isEmpty())
					{
						myVL.setSeparator(separator);
					}

					// Set sort options if provided
					if (sortOptions != null && !sortOptions.trim().isEmpty())
					{
						myVL.setSortOptions(sortOptions);
					}
				}
				else if (isCustom)
				{
					// CUSTOM_VALUES type (value = 0, which is default)
					myVL.setValueListType(0); // CUSTOM_VALUES

					// Convert list to newline-separated string
					StringBuilder customValuesStr = new StringBuilder();
					for (String value : customValues)
					{
						if (value != null && !value.trim().isEmpty())
						{
							if (customValuesStr.length() > 0)
							{
								customValuesStr.append("\n");
							}
							customValuesStr.append(value.trim());
						}
					}
					myVL.setCustomValues(customValuesStr.toString());
				}
			}

			// Save the value list if it was created
			if (isCreate)
			{
				ServoyLog.logInfo("[ValueListToolHandler] Saving value list: " + name);
				ServoyProject servoyProject = servoyModel.getActiveProject();
				servoyProject.saveEditingSolutionNodes(new IPersist[] { myVL }, true);
			}

			// Open editor on UI thread
			final ValueList valueListToOpen = myVL;
			Display.getDefault().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					EditorUtil.openValueListEditor(valueListToOpen, true);
				}
			});

		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[ValueListToolHandler] Error in handleOpenValueList: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage
			: (isCreate ? "Value list '" + name + "' created successfully" : "Value list '" + name + "' opened in editor.");
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: deleteValueList
	// =============================================

	private McpSchema.CallToolResult handleDeleteValueList(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
	{
		String name = null;
		String errorMessage = null;

		try
		{
			Map<String, Object> args = request.arguments();

			if (args != null && args.containsKey("name"))
			{
				Object nameObj = args.get("name");
				if (nameObj != null) name = nameObj.toString();
			}

			// Validate name is required
			if (name == null || name.trim().isEmpty())
			{
				errorMessage = "The 'name' argument is required.";
				return McpSchema.CallToolResult.builder()
					.content(List.of(new TextContent(errorMessage)))
					.build();
			}

			// Find the value list
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ValueList valueList = servoyModel.getActiveProject().getEditingSolution().getValueList(name);

			if (valueList == null)
			{
				errorMessage = "Value list '" + name + "' not found.";
			}
			else
			{
				servoyModel.getActiveProject().getEditingSolution().removeChild(valueList);
				ServoyLog.logInfo("[ValueListToolHandler] Deleted value list: " + name);
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			ServoyLog.logError("[ValueListToolHandler] Error in handleDeleteValueList: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : "Value list '" + name + "' deleted successfully.";
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}

	// =============================================
	// TOOL: listValueLists
	// =============================================

	private McpSchema.CallToolResult handleListValueLists(McpSyncServerExchange exchange, McpSchema.CallToolRequest request)
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
			ServoyLog.logError("[ValueListToolHandler] Error in handleListValueLists: " + errorMessage, e);
		}

		String resultMessage = errorMessage != null ? errorMessage : resultBuilder.toString();
		return McpSchema.CallToolResult.builder()
			.content(List.of(new TextContent(resultMessage)))
			.build();
	}
}
