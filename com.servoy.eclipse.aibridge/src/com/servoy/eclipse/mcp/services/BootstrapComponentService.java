package com.servoy.eclipse.mcp.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.UUID;

/**
 * Service for manipulating bootstrap component JSON in Servoy form (.frm) files.
 * Handles adding components to forms with proper JSON structure.
 * 
 * CRITICAL: This service is the ONLY way to manipulate form files for bootstrap components.
 * Direct file editing by AI models is FORBIDDEN due to corruption risks.
 */
public class BootstrapComponentService
{
	/**
	 * Adds a bootstrap component to a form file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param componentName Name for the component (REQUIRED)
	 * @param typeName Component type name (e.g., "bootstrapcomponents-button")
	 * @param cssPosition CSS position string (format: "top,right,bottom,left,width,height") (REQUIRED)
	 * @param properties Map of component-specific properties to add to json section
	 * @return null if successful, error message string if failed
	 */
	public static String addComponentToForm(String projectPath, String formName, String componentName, 
		String typeName, String cssPosition, Map<String, Object> properties)
	{
		try
		{
			System.out.println("[BootstrapComponentService] addComponentToForm called: form='" + formName + 
				"', component='" + componentName + "', type='" + typeName + "'");
			
			// Build path to form file
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				ServoyLog.logError("[BootstrapComponentService] Form file not found: " + formPath);
				// Get available forms to help user
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			System.out.println("[BootstrapComponentService] Loading form file: " + formPath);
			
			// Read and parse existing form JSON (will throw JSONException if corrupted)
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			
			// Validate form structure - only check for items array
			if (!formJson.has("items"))
			{
				ServoyLog.logError("[BootstrapComponentService] Invalid form structure - missing 'items' array: " + formPath);
				return "Invalid form structure - form '" + formName + "' is missing 'items' array";
			}
			
			// Parse and validate CSS position
			String[] positionParts = cssPosition.split(",");
			if (positionParts.length != 6)
			{
				ServoyLog.logError("[BootstrapComponentService] Invalid CSS position format. Expected: top,right,bottom,left,width,height");
				return "Invalid CSS position format. Expected 'top,right,bottom,left,width,height' but got: '" + cssPosition + "'";
			}
			
			// Build cssPosition object for json section
			JSONObject cssPositionObj = new JSONObject();
			cssPositionObj.put("top", positionParts[0]);
			cssPositionObj.put("right", positionParts[1]);
			cssPositionObj.put("bottom", positionParts[2]);
			cssPositionObj.put("left", positionParts[3]);
			cssPositionObj.put("width", positionParts[4]);
			cssPositionObj.put("height", positionParts[5]);
			
			// Create component JSON structure
			JSONObject component = new JSONObject();
			
			// Root level properties
			component.put("cssPosition", cssPosition);  // String format at root
			component.put("name", componentName);
			component.put("typeName", typeName);
			component.put("typeid", 47);  // Always 47 for bootstrap components
			component.put("uuid", UUID.randomUUID().toString());
			
			// Build json section
			JSONObject jsonSection = new JSONObject();
			jsonSection.put("cssPosition", cssPositionObj);  // Object format in json
			
			// Add component-specific properties
			if (properties != null && !properties.isEmpty())
			{
				for (Map.Entry<String, Object> entry : properties.entrySet())
				{
					String key = entry.getKey();
					Object value = entry.getValue();
					
					// Handle styleClass - goes in BOTH root and json
					if ("styleClass".equals(key))
					{
						component.put("styleClass", value);
						jsonSection.put("styleClass", value);
					}
					else
					{
						jsonSection.put(key, value);
					}
				}
			}
			
			component.put("json", jsonSection);
			
			// Add component to items array
			JSONArray items = formJson.getJSONArray("items");
			items.put(component);
			
			// Create backup before writing
			Path backupPath = Paths.get(formPath.toString() + ".backup");
			Files.copy(formPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Write updated form with pretty printing
			String updatedContent = formJson.toString(4);
			Files.write(formPath, updatedContent.getBytes());
			
			ServoyLog.logInfo("[BootstrapComponentService] Successfully added component '" + componentName + 
				"' (type: " + typeName + ") to form '" + formName + "'");
			System.out.println("[BootstrapComponentService] Component '" + componentName + "' added successfully");
			
			return null;  // Success
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error adding component to form: " + e.getMessage(), e);
			e.printStackTrace();
			return "Error adding component: " + e.getMessage();
		}
	}
	
	/**
	 * Lists all available forms in the project.
	 * Used to help users when they mistype form names.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @return Comma-separated list of form names
	 */
	public static String listAvailableForms(String projectPath)
	{
		try
		{
			Path formsDir = Paths.get(projectPath, "forms");
			
			if (!Files.exists(formsDir) || !Files.isDirectory(formsDir))
			{
				return "No forms directory found";
			}
			
			StringBuilder formsList = new StringBuilder();
			Files.list(formsDir)
				.filter(path -> path.toString().endsWith(".frm"))
				.forEach(path -> {
					String fileName = path.getFileName().toString();
					String formName = fileName.substring(0, fileName.length() - 4);
					if (formsList.length() > 0) formsList.append(", ");
					formsList.append(formName);
				});
			
			return formsList.length() > 0 ? formsList.toString() : "No forms found";
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error listing forms: " + e.getMessage(), e);
			return "Error listing forms";
		}
	}
	
	/**
	 * Lists all components in a form with their details.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @return JSON string with component details, or error message
	 */
	public static String listComponents(String projectPath, String formName)
	{
		try
		{
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			JSONArray items = formJson.getJSONArray("items");
			
			JSONArray components = new JSONArray();
			
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				
				// Skip parts (they don't have typeName)
				if (!item.has("typeName"))
				{
					continue;
				}
				
				// Extract key information
				JSONObject compInfo = new JSONObject();
				compInfo.put("name", item.optString("name", ""));
				compInfo.put("typeName", item.optString("typeName", ""));
				compInfo.put("cssPosition", item.optString("cssPosition", ""));
				
				// Extract text if available (from json section)
				if (item.has("json"))
				{
					JSONObject json = item.getJSONObject("json");
					if (json.has("text")) compInfo.put("text", json.getString("text"));
					if (json.has("styleClass")) compInfo.put("styleClass", json.getString("styleClass"));
				}
				
				components.put(compInfo);
			}
			
			return components.toString(2);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error listing components: " + e.getMessage(), e);
			return "Error listing components: " + e.getMessage();
		}
	}
	
	/**
	 * Gets detailed information about a specific component.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param componentName Name of the component
	 * @return JSON string with component details, or error message
	 */
	public static String getComponentInfo(String projectPath, String formName, String componentName)
	{
		try
		{
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			JSONArray items = formJson.getJSONArray("items");
			
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				
				if (item.has("name") && componentName.equals(item.getString("name")))
				{
					return item.toString(2);
				}
			}
			
			return "Component '" + componentName + "' not found in form '" + formName + "'";
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error getting component info: " + e.getMessage(), e);
			return "Error getting component info: " + e.getMessage();
		}
	}
	
	/**
	 * Updates an existing component with new property values.
	 * Only specified properties are updated, others remain unchanged.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param componentName Name of the component to update
	 * @param propertiesToUpdate Map of properties to update
	 * @return null if successful, error message string if failed
	 */
	public static String updateComponent(String projectPath, String formName, String componentName, Map<String, Object> propertiesToUpdate)
	{
		try
		{
			System.out.println("[BootstrapComponentService] updateComponent called: form='" + formName + 
				"', component='" + componentName + "'");
			
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			// Read and parse form JSON
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			JSONArray items = formJson.getJSONArray("items");
			
			// Find the component
			boolean found = false;
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				
				if (item.has("name") && componentName.equals(item.getString("name")))
				{
					found = true;
					
					// Get or create json section
					JSONObject jsonSection = item.has("json") ? item.getJSONObject("json") : new JSONObject();
					
					// Update properties
					for (Map.Entry<String, Object> entry : propertiesToUpdate.entrySet())
					{
						String key = entry.getKey();
						Object value = entry.getValue();
						
						// Special handling for cssPosition
						if ("cssPosition".equals(key) && value instanceof String)
						{
							String cssPos = (String)value;
							String[] parts = cssPos.split(",");
							if (parts.length != 6)
							{
								return "Invalid CSS position format. Expected 'top,right,bottom,left,width,height'";
							}
							
							// Update root level string
							item.put("cssPosition", cssPos);
							
							// Update json section object
							JSONObject cssPositionObj = new JSONObject();
							cssPositionObj.put("top", parts[0]);
							cssPositionObj.put("right", parts[1]);
							cssPositionObj.put("bottom", parts[2]);
							cssPositionObj.put("left", parts[3]);
							cssPositionObj.put("width", parts[4]);
							cssPositionObj.put("height", parts[5]);
							jsonSection.put("cssPosition", cssPositionObj);
						}
						// Special handling for styleClass - goes in both root and json
						else if ("styleClass".equals(key))
						{
							item.put("styleClass", value);
							jsonSection.put("styleClass", value);
						}
						// All other properties go in json section
						else
						{
							jsonSection.put(key, value);
						}
					}
					
					// Update json section in item
					item.put("json", jsonSection);
					
					break;
				}
			}
			
			if (!found)
			{
				return "Component '" + componentName + "' not found in form '" + formName + "'";
			}
			
			// Create backup
			Path backupPath = Paths.get(formPath.toString() + ".backup");
			Files.copy(formPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Write updated form
			String updatedContent = formJson.toString(4);
			Files.write(formPath, updatedContent.getBytes());
			
			ServoyLog.logInfo("[BootstrapComponentService] Successfully updated component '" + componentName + 
				"' in form '" + formName + "'");
			
			return null;  // Success
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error updating component: " + e.getMessage(), e);
			return "Error updating component: " + e.getMessage();
		}
	}

	/**
	 * Deletes a component from a form.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param componentName Name of the component to delete
	 * @return null if successful, error message string if failed
	 */
	public static String deleteComponent(String projectPath, String formName, String componentName)
	{
		try
		{
			System.out.println("[BootstrapComponentService] deleteComponent called: form='" + formName + 
				"', component='" + componentName + "'");
			
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			// Read and parse form JSON
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			JSONArray items = formJson.getJSONArray("items");
			
			// Find and remove the component
			boolean found = false;
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				
				if (item.has("name") && componentName.equals(item.getString("name")))
				{
					items.remove(i);
					found = true;
					break;
				}
			}
			
			if (!found)
			{
				return "Component '" + componentName + "' not found in form '" + formName + "'";
			}
			
			// Create backup
			Path backupPath = Paths.get(formPath.toString() + ".backup");
			Files.copy(formPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			
			// Write updated form
			String updatedContent = formJson.toString(4);
			Files.write(formPath, updatedContent.getBytes());
			
			ServoyLog.logInfo("[BootstrapComponentService] Successfully deleted component '" + componentName + 
				"' from form '" + formName + "'");
			
			return null;  // Success
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error deleting component: " + e.getMessage(), e);
			return "Error deleting component: " + e.getMessage();
		}
	}

	/**
	 * Lists components of a specific type in a form.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param typeName Component type to filter (e.g., "bootstrapcomponents-label")
	 * @return JSON string with component details, or error message
	 */
	public static String listComponentsByType(String projectPath, String formName, String typeName)
	{
		try
		{
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				String availableForms = listAvailableForms(projectPath);
				return "Form '" + formName + "' not found. Available forms: " + availableForms;
			}
			
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			JSONArray items = formJson.getJSONArray("items");
			
			JSONArray components = new JSONArray();
			
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				
				// Filter by typeName
				if (item.has("typeName") && typeName.equals(item.getString("typeName")))
				{
					// Extract key information
					JSONObject compInfo = new JSONObject();
					compInfo.put("name", item.optString("name", ""));
					compInfo.put("typeName", item.optString("typeName", ""));
					compInfo.put("cssPosition", item.optString("cssPosition", ""));
					
					// Extract text if available (from json section)
					if (item.has("json"))
					{
						JSONObject json = item.getJSONObject("json");
						if (json.has("text")) compInfo.put("text", json.getString("text"));
						if (json.has("styleClass")) compInfo.put("styleClass", json.getString("styleClass"));
					}
					
					components.put(compInfo);
				}
			}
			
			return components.toString(2);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[BootstrapComponentService] Error listing components by type: " + e.getMessage(), e);
			return "Error listing components: " + e.getMessage();
		}
	}
}