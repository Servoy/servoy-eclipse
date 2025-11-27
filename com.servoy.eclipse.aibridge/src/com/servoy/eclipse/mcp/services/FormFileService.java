package com.servoy.eclipse.mcp.services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.UUID;

/**
 * Service for safe manipulation of Servoy form (.frm) files.
 * Handles JSON parsing, component addition, validation, and file writing.
 * 
 * CRITICAL: This service is the ONLY way to manipulate form files.
 * Direct file editing by AI models is FORBIDDEN due to corruption risks.
 */
public class FormFileService
{
	/**
	 * Adds a button component to a form file.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @param buttonName Name for the button component
	 * @param buttonText Text to display on the button
	 * @param cssPosition CSS position string (format: "top,right,bottom,left,width,height")
	 * @return true if successful, false otherwise
	 */
	public static boolean addButtonToForm(String projectPath, String formName, String buttonName, String buttonText, String cssPosition)
	{
		try
		{
			// Build path to form file
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				ServoyLog.logError("[FormFileService] Form file not found: " + formPath);
				return false;
			}
			
			// Read and parse existing form JSON
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			
			// Validate form structure
			if (!formJson.has("items") || !formJson.has("uuid"))
			{
				ServoyLog.logError("[FormFileService] Invalid form structure in: " + formPath);
				return false;
			}
			
			// Check if form uses CSS positioning
			boolean usesCssPosition = false;
			if (formJson.has("customProperties"))
			{
				JSONObject customProps = formJson.getJSONObject("customProperties");
				usesCssPosition = customProps.optBoolean("useCssPosition", false);
			}
			
			if (!usesCssPosition)
			{
				ServoyLog.logError("[FormFileService] Form does not use CSS positioning: " + formName);
				return false;
			}
			
			// Parse CSS position
			String[] positionParts = cssPosition.split(",");
			if (positionParts.length != 6)
			{
				ServoyLog.logError("[FormFileService] Invalid CSS position format. Expected: top,right,bottom,left,width,height");
				return false;
			}
			
			// Create button component JSON
			JSONObject buttonComponent = new JSONObject();
			buttonComponent.put("cssPosition", cssPosition);
			
			// Create button JSON properties
			JSONObject buttonJson = new JSONObject();
			JSONObject cssPositionObj = new JSONObject();
			cssPositionObj.put("top", positionParts[0]);
			cssPositionObj.put("right", positionParts[1]);
			cssPositionObj.put("bottom", positionParts[2]);
			cssPositionObj.put("left", positionParts[3]);
			cssPositionObj.put("width", positionParts[4]);
			cssPositionObj.put("height", positionParts[5]);
			buttonJson.put("cssPosition", cssPositionObj);
			buttonJson.put("text", buttonText);
			
			buttonComponent.put("json", buttonJson);
			buttonComponent.put("name", buttonName);
			buttonComponent.put("typeName", "bootstrapcomponents-button");
			buttonComponent.put("typeid", 47);
			buttonComponent.put("uuid", UUID.randomUUID().toString());
			
			// Add button to items array
			JSONArray items = formJson.getJSONArray("items");
			items.put(buttonComponent);
			
			// Write back to file with pretty printing
			String updatedContent = formJson.toString(4); // 4-space indentation
			
			// Create backup before writing
			Path backupPath = Paths.get(formPath.toString() + ".backup");
			Files.copy(formPath, backupPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			
			// Write updated form
			try (FileWriter writer = new FileWriter(formPath.toFile()))
			{
				writer.write(updatedContent);
			}
			
			ServoyLog.logInfo("[FormFileService] Successfully added button '" + buttonName + "' to form '" + formName + "'");
			return true;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[FormFileService] Error adding button to form: " + e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Validates a form file structure.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @return true if valid, false otherwise
	 */
	public static boolean validateFormFile(String projectPath, String formName)
	{
		try
		{
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				return false;
			}
			
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			
			// Check required fields
			if (!formJson.has("name") || !formJson.has("uuid") || !formJson.has("items") || !formJson.has("typeid"))
			{
				return false;
			}
			
			// Validate items array
			JSONArray items = formJson.getJSONArray("items");
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				if (!item.has("uuid") || !item.has("typeid"))
				{
					return false;
				}
			}
			
			return true;
		}
		catch (Exception e)
		{
			ServoyLog.logError("[FormFileService] Error validating form: " + e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * Gets form information.
	 * 
	 * @param projectPath Path to Servoy project root
	 * @param formName Name of the form (without .frm extension)
	 * @return JSON string with form info, or null if error
	 */
	public static String getFormInfo(String projectPath, String formName)
	{
		try
		{
			Path formPath = Paths.get(projectPath, "forms", formName + ".frm");
			
			if (!Files.exists(formPath))
			{
				return null;
			}
			
			String formContent = new String(Files.readAllBytes(formPath));
			JSONObject formJson = new JSONObject(formContent);
			
			JSONObject info = new JSONObject();
			info.put("name", formJson.getString("name"));
			info.put("uuid", formJson.getString("uuid"));
			info.put("size", formJson.optString("size", "unknown"));
			
			// Check CSS positioning
			boolean usesCssPosition = false;
			if (formJson.has("customProperties"))
			{
				JSONObject customProps = formJson.getJSONObject("customProperties");
				usesCssPosition = customProps.optBoolean("useCssPosition", false);
			}
			info.put("useCssPosition", usesCssPosition);
			
			// Count components
			JSONArray items = formJson.getJSONArray("items");
			int componentCount = 0;
			for (int i = 0; i < items.length(); i++)
			{
				JSONObject item = items.getJSONObject(i);
				// Parts have partType, components have name
				if (item.has("name"))
				{
					componentCount++;
				}
			}
			info.put("componentCount", componentCount);
			
			return info.toString(4);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[FormFileService] Error getting form info: " + e.getMessage(), e);
			return null;
		}
	}
}
