package com.servoy.eclipse.knowledgebase.services;

import java.util.List;
import java.util.Map;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValueList;

/**
 * Service for valuelist operations - create, update, and query valuelists.
 * Provides reusable business logic for ValueListToolHandler.
 * 
 * Supports all 4 valuelist types:
 * - CUSTOM_VALUES: Fixed list of display/real value pairs
 * - DATABASE_VALUES (TABLE_VALUES): Values from database table
 * - DATABASE_VALUES (RELATED_VALUES): Values from related table via relation
 * - GLOBAL_METHOD_VALUES: Dynamic values from global method
 */
public class ValueListService
{
	/**
	 * Creates a new valuelist with specified parameters and properties.
	 * 
	 * @param name ValueList name
	 * @param customValues Custom values list (for CUSTOM type)
	 * @param dataSource Database datasource (for DATABASE type with table)
	 * @param relationName Relation name (for DATABASE type with relation)
	 * @param globalMethod Global method name (for GLOBAL_METHOD type)
	 * @param displayColumn Display column name
	 * @param returnColumn Return column name
	 * @param properties Optional map of valuelist properties
	 * @return The created valuelist
	 * @throws RepositoryException If creation fails
	 */
	public static ValueList createValueList(String name, List<String> customValues, String dataSource,
		String relationName, String globalMethod, String displayColumn, String returnColumn,
		Map<String, Object> properties) throws RepositoryException
	{
		ServoyLog.logInfo("[ValueListService] Creating valuelist: " + name);
		
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
		
		// Create the valuelist
		ValueList valueList = servoyProject.getEditingSolution().createNewValueList(
			servoyModel.getNameValidator(), name);
		
		// Determine type and configure
		if (globalMethod != null && !globalMethod.trim().isEmpty())
		{
			// GLOBAL_METHOD_VALUES type
			configureGlobalMethodValueList(valueList, globalMethod);
		}
		else if (relationName != null && !relationName.trim().isEmpty())
		{
			// DATABASE_VALUES with RELATED_VALUES
			configureRelatedValueList(valueList, relationName, displayColumn, returnColumn);
		}
		else if (dataSource != null && !dataSource.trim().isEmpty())
		{
			// DATABASE_VALUES with TABLE_VALUES
			configureDatabaseValueList(valueList, dataSource, displayColumn, returnColumn);
		}
		else if (customValues != null && !customValues.isEmpty())
		{
			// CUSTOM_VALUES type
			configureCustomValueList(valueList, customValues);
		}
		
		// Apply additional properties
		applyValueListProperties(valueList, properties);
		
		// Save the valuelist
		servoyProject.saveEditingSolutionNodes(new IPersist[] { valueList }, true);
		ServoyLog.logInfo("[ValueListService] ValueList created and saved: " + name);
		
		return valueList;
	}
	
	/**
	 * Updates properties of an existing valuelist.
	 * 
	 * @param valueList The valuelist to update
	 * @param properties Map of properties to update
	 * @throws RepositoryException If update fails
	 */
	public static void updateValueListProperties(ValueList valueList, Map<String, Object> properties) throws RepositoryException
	{
		if (properties == null || properties.isEmpty())
		{
			return;
		}
		
		ServoyLog.logInfo("[ValueListService] Updating valuelist properties: " + valueList.getName());
		
		applyValueListProperties(valueList, properties);
		
		// Save the valuelist
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		servoyProject.saveEditingSolutionNodes(new IPersist[] { valueList }, true);
		
		ServoyLog.logInfo("[ValueListService] ValueList properties updated and saved: " + valueList.getName());
	}
	
	/**
	 * Configures a custom valuelist with fixed values.
	 */
	private static void configureCustomValueList(ValueList valueList, List<String> customValues)
	{
		valueList.setValueListType(0); // CUSTOM_VALUES
		
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
		valueList.setCustomValues(customValuesStr.toString());
		
		ServoyLog.logInfo("[ValueListService] Configured as CUSTOM_VALUES with " + customValues.size() + " values");
	}
	
	/**
	 * Configures a database valuelist with table values.
	 */
	private static void configureDatabaseValueList(ValueList valueList, String dataSource,
		String displayColumn, String returnColumn) throws RepositoryException
	{
		valueList.setValueListType(1); // DATABASE_VALUES
		
		// Auto-correct datasource format if needed
		dataSource = validateAndCorrectDataSource(dataSource);
		valueList.setDataSource(dataSource);
		
		// Configure display and return columns
		boolean hasDisplayColumn = (displayColumn != null && !displayColumn.trim().isEmpty());
		boolean hasReturnColumn = (returnColumn != null && !returnColumn.trim().isEmpty());
		
		if (hasDisplayColumn && hasReturnColumn && !displayColumn.equals(returnColumn))
		{
			// Different columns: display one, return another
			valueList.setDataProviderID1(displayColumn);
			valueList.setDataProviderID2(returnColumn);
			valueList.setShowDataProviders(1); // Show first column (display)
			valueList.setReturnDataProviders(2); // Return second column (return)
		}
		else if (hasDisplayColumn)
		{
			// Only displayColumn: use it for both display and return
			valueList.setDataProviderID1(displayColumn);
			valueList.setShowDataProviders(1);
			valueList.setReturnDataProviders(1);
		}
		else if (hasReturnColumn)
		{
			// Only returnColumn: use it for both display and return
			valueList.setDataProviderID1(returnColumn);
			valueList.setShowDataProviders(1);
			valueList.setReturnDataProviders(1);
		}
		
		ServoyLog.logInfo("[ValueListService] Configured as DATABASE_VALUES (table) with dataSource: " + dataSource);
	}
	
	/**
	 * Configures a database valuelist with related values.
	 */
	private static void configureRelatedValueList(ValueList valueList, String relationName,
		String displayColumn, String returnColumn)
	{
		valueList.setValueListType(1); // DATABASE_VALUES
		valueList.setRelationName(relationName);
		
		// Configure display and return columns
		boolean hasDisplayColumn = (displayColumn != null && !displayColumn.trim().isEmpty());
		boolean hasReturnColumn = (returnColumn != null && !returnColumn.trim().isEmpty());
		
		if (hasDisplayColumn && hasReturnColumn && !displayColumn.equals(returnColumn))
		{
			valueList.setDataProviderID1(displayColumn);
			valueList.setDataProviderID2(returnColumn);
			valueList.setShowDataProviders(1);
			valueList.setReturnDataProviders(2);
		}
		else if (hasDisplayColumn)
		{
			valueList.setDataProviderID1(displayColumn);
			valueList.setShowDataProviders(1);
			valueList.setReturnDataProviders(1);
		}
		else if (hasReturnColumn)
		{
			valueList.setDataProviderID1(returnColumn);
			valueList.setShowDataProviders(1);
			valueList.setReturnDataProviders(1);
		}
		
		ServoyLog.logInfo("[ValueListService] Configured as DATABASE_VALUES (related) with relation: " + relationName);
	}
	
	/**
	 * Configures a global method valuelist.
	 */
	private static void configureGlobalMethodValueList(ValueList valueList, String globalMethod)
	{
		valueList.setValueListType(4); // GLOBAL_METHOD_VALUES
		valueList.setCustomValues(globalMethod); // Global method name stored in customValues
		
		ServoyLog.logInfo("[ValueListService] Configured as GLOBAL_METHOD_VALUES with method: " + globalMethod);
	}
	
	/**
	 * Applies properties to a valuelist.
	 * Supports all valuelist properties from API discovery:
	 * - lazyLoading: boolean
	 * - displayValueType: int
	 * - realValueType: int
	 * - separator: string
	 * - sortOptions: string
	 * - useTableFilter: boolean
	 * - addEmptyValue: int (ALWAYS/NEVER)
	 * - fallbackValueListID: int
	 * - deprecated: string
	 * - encapsulation: string
	 * - comment: string
	 * 
	 * @param valueList The valuelist to update
	 * @param properties Map of properties
	 */
	private static void applyValueListProperties(ValueList valueList, Map<String, Object> properties)
	{
		if (properties == null || properties.isEmpty())
		{
			return;
		}
		
		for (Map.Entry<String, Object> entry : properties.entrySet())
		{
			String propName = entry.getKey();
			Object propValue = entry.getValue();
			
			ServoyLog.logInfo("[ValueListService] Applying property: " + propName + " = " + propValue);
			
			try
			{
				switch (propName)
				{
					case "lazyLoading":
						if (propValue instanceof Boolean)
						{
							valueList.setLazyLoading((Boolean)propValue);
						}
						else
						{
							valueList.setLazyLoading(Boolean.parseBoolean(propValue.toString()));
						}
						break;
						
					case "displayValueType":
						if (propValue instanceof Number)
						{
							valueList.setDisplayValueType(((Number)propValue).intValue());
						}
						else
						{
							valueList.setDisplayValueType(Integer.parseInt(propValue.toString()));
						}
						break;
						
					case "realValueType":
						if (propValue instanceof Number)
						{
							valueList.setRealValueType(((Number)propValue).intValue());
						}
						else
						{
							valueList.setRealValueType(Integer.parseInt(propValue.toString()));
						}
						break;
						
					case "separator":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							valueList.setSeparator(propValue.toString());
						}
						break;
						
					case "sortOptions":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							valueList.setSortOptions(propValue.toString());
						}
						break;
						
					case "useTableFilter":
						if (propValue instanceof Boolean)
						{
							valueList.setUseTableFilter((Boolean)propValue);
						}
						else
						{
							valueList.setUseTableFilter(Boolean.parseBoolean(propValue.toString()));
						}
						break;
						
					case "addEmptyValue":
						if (propValue instanceof Number)
						{
							valueList.setAddEmptyValue(((Number)propValue).intValue());
						}
						else if (propValue instanceof Boolean)
						{
							// Boolean convenience: true = ALWAYS (1), false = NEVER (2)
							valueList.setAddEmptyValue((Boolean)propValue ? 1 : 2);
						}
						else
						{
							String strVal = propValue.toString().toLowerCase();
							if ("always".equals(strVal) || "true".equals(strVal))
							{
								valueList.setAddEmptyValue(1); // EMPTY_VALUE_ALWAYS
							}
							else
							{
								valueList.setAddEmptyValue(2); // EMPTY_VALUE_NEVER
							}
						}
						break;
						
					case "fallbackValueListID":
					case "fallbackValueList":
						// Note: Despite the name, this actually takes the valuelist NAME as a string
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							valueList.setFallbackValueListID(propValue.toString());
						}
						break;
						
					case "deprecated":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							valueList.setDeprecated(propValue.toString());
						}
						break;
						
					case "encapsulation":
						if (propValue != null)
						{
							int encapsulation = parseEncapsulation(propValue.toString());
							valueList.setEncapsulation(encapsulation);
						}
						break;
						
					case "comment":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							valueList.setComment(propValue.toString());
						}
						break;
						
					default:
						ServoyLog.logInfo("[ValueListService] Unknown property: " + propName);
						break;
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("[ValueListService] Error setting property " + propName + ": " + e.getMessage(), e);
			}
		}
	}
	
	/**
	 * Parses encapsulation string to int value.
	 * 
	 * @param encapsulationStr String value: "public", "hide", or "module"
	 * @return Encapsulation int constant
	 */
	private static int parseEncapsulation(String encapsulationStr)
	{
		if (encapsulationStr == null)
		{
			return com.servoy.j2db.persistence.PersistEncapsulation.DEFAULT;
		}
		
		String normalized = encapsulationStr.toLowerCase().trim();
		switch (normalized)
		{
			case "hide":
			case "hide in scripting":
			case "hide_in_scripting":
				return com.servoy.j2db.persistence.PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE;
				
			case "module":
			case "module scope":
			case "module_scope":
				return com.servoy.j2db.persistence.PersistEncapsulation.MODULE_SCOPE;
				
			case "public":
			default:
				return com.servoy.j2db.persistence.PersistEncapsulation.DEFAULT;
		}
	}
	
	/**
	 * Validates datasource format and auto-corrects if needed.
	 * 
	 * @param dataSource Raw datasource string
	 * @return Corrected datasource with db:/ prefix
	 * @throws RepositoryException If format is invalid
	 */
	public static String validateAndCorrectDataSource(String dataSource) throws RepositoryException
	{
		if (dataSource == null || dataSource.trim().isEmpty())
		{
			throw new RepositoryException("Datasource cannot be empty");
		}
		
		// Auto-correct format if needed
		if (!dataSource.startsWith("db:/"))
		{
			if (dataSource.contains("/"))
			{
				return "db:/" + dataSource;
			}
			else
			{
				throw new RepositoryException("Invalid datasource format: '" + dataSource +
					"'. Please provide format 'db:/server_name/table_name' or 'server_name/table_name'");
			}
		}
		
		return dataSource;
	}
}
