package com.servoy.eclipse.knowledgebase.mcp.services;

import java.util.Map;

import com.servoy.base.query.IQueryConstants;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Service for relation operations - create, update, and query relations.
 * Provides reusable business logic for RelationToolHandler.
 */
public class RelationService
{
	/**
	 * Creates a new relation with specified parameters and properties.
	 * 
	 * @param name Relation name
	 * @param primaryDataSource Primary datasource (format: db:/server/table)
	 * @param foreignDataSource Foreign datasource (format: db:/server/table)
	 * @param primaryColumn Primary column name (optional)
	 * @param foreignColumn Foreign column name (optional)
	 * @param properties Optional map of relation properties
	 * @return The created relation
	 * @throws RepositoryException If creation fails
	 */
	public static Relation createRelation(String name, String primaryDataSource, String foreignDataSource,
		String primaryColumn, String foreignColumn, Map<String, Object> properties) throws RepositoryException
	{
		ServoyLog.logInfo("[RelationService] Creating relation: " + name);
		
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
		
		// Extract joinType from properties or use default
		int joinType = IQueryConstants.LEFT_OUTER_JOIN; // default
		if (properties != null && properties.containsKey("joinType"))
		{
			String joinTypeStr = properties.get("joinType").toString().toLowerCase();
			if ("inner".equals(joinTypeStr) || "inner join".equals(joinTypeStr))
			{
				joinType = IQueryConstants.INNER_JOIN;
			}
		}
		
		// Create the relation
		Relation relation = servoyProject.getEditingSolution().createNewRelation(
			servoyModel.getNameValidator(),
			name,
			primaryDataSource,
			foreignDataSource,
			joinType);
		
		// Apply properties
		applyRelationProperties(relation, properties);
		
		// Add relation item (column mapping) if both columns are provided
		if (primaryColumn != null && !primaryColumn.trim().isEmpty() &&
			foreignColumn != null && !foreignColumn.trim().isEmpty())
		{
			try
			{
				ITable primaryTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(primaryDataSource);
				ITable foreignTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(foreignDataSource);
				Column primaryCol = primaryTable.getColumn(primaryColumn);
				Column foreignCol = foreignTable.getColumn(foreignColumn);
				
				relation.createNewRelationItems(
					new IDataProvider[] { primaryCol },
					new int[] { com.servoy.base.query.IBaseSQLCondition.EQUALS_OPERATOR },
					new Column[] { foreignCol });
			}
			catch (Exception e)
			{
				ServoyLog.logError("[RelationService] Could not add column mapping: " + e.getMessage());
				// Continue - relation is created, columns can be added manually in editor
			}
		}
		
		// Save the relation
		servoyProject.saveEditingSolutionNodes(new IPersist[] { relation }, true);
		ServoyLog.logInfo("[RelationService] Relation created and saved: " + name);
		
		return relation;
	}
	
	/**
	 * Updates properties of an existing relation.
	 * 
	 * @param relation The relation to update
	 * @param properties Map of properties to update
	 * @throws RepositoryException If update fails
	 */
	public static void updateRelationProperties(Relation relation, Map<String, Object> properties) throws RepositoryException
	{
		if (properties == null || properties.isEmpty())
		{
			return;
		}
		
		ServoyLog.logInfo("[RelationService] Updating relation properties: " + relation.getName());
		
		applyRelationProperties(relation, properties);
		
		// Save the relation
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject servoyProject = servoyModel.getActiveProject();
		servoyProject.saveEditingSolutionNodes(new IPersist[] { relation }, true);
		
		ServoyLog.logInfo("[RelationService] Relation properties updated and saved: " + relation.getName());
	}
	
	/**
	 * Applies properties to a relation.
	 * Supports all 8 relation properties:
	 * - joinType: "left outer" | "inner"
	 * - allowCreationRelatedRecords: boolean
	 * - allowParentDeleteWhenHavingRelatedRecords: boolean
	 * - deleteRelatedRecords: boolean
	 * - initialSort: string
	 * - encapsulation: "public" | "hide" | "module"
	 * - deprecated: string
	 * - comment: string
	 * 
	 * @param relation The relation to update
	 * @param properties Map of properties
	 */
	private static void applyRelationProperties(Relation relation, Map<String, Object> properties)
	{
		if (properties == null || properties.isEmpty())
		{
			// Apply defaults for creation
			relation.setAllowCreationRelatedRecords(true);
			return;
		}
		
		for (Map.Entry<String, Object> entry : properties.entrySet())
		{
			String propName = entry.getKey();
			Object propValue = entry.getValue();
			
			ServoyLog.logInfo("[RelationService] Applying property: " + propName + " = " + propValue);
			
			try
			{
				switch (propName)
				{
					case "joinType":
						if (propValue != null)
						{
							String joinTypeStr = propValue.toString().toLowerCase();
							if ("inner".equals(joinTypeStr) || "inner join".equals(joinTypeStr))
							{
								relation.setJoinType(IQueryConstants.INNER_JOIN);
							}
							else if ("left outer".equals(joinTypeStr) || "left outer join".equals(joinTypeStr))
							{
								relation.setJoinType(IQueryConstants.LEFT_OUTER_JOIN);
							}
						}
						break;
						
					case "allowCreationRelatedRecords":
						if (propValue instanceof Boolean)
						{
							relation.setAllowCreationRelatedRecords((Boolean)propValue);
						}
						else
						{
							relation.setAllowCreationRelatedRecords(Boolean.parseBoolean(propValue.toString()));
						}
						break;
						
					case "allowParentDeleteWhenHavingRelatedRecords":
						if (propValue instanceof Boolean)
						{
							relation.setAllowParentDeleteWhenHavingRelatedRecords((Boolean)propValue);
						}
						else
						{
							relation.setAllowParentDeleteWhenHavingRelatedRecords(Boolean.parseBoolean(propValue.toString()));
						}
						break;
						
					case "deleteRelatedRecords":
						if (propValue instanceof Boolean)
						{
							relation.setDeleteRelatedRecords((Boolean)propValue);
						}
						else
						{
							relation.setDeleteRelatedRecords(Boolean.parseBoolean(propValue.toString()));
						}
						break;
						
					case "initialSort":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							relation.setInitialSort(propValue.toString());
						}
						break;
						
					case "encapsulation":
						if (propValue != null)
						{
							int encapsulation = parseEncapsulation(propValue.toString());
							relation.setEncapsulation(encapsulation);
						}
						break;
						
					case "deprecated":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							relation.setDeprecated(propValue.toString());
						}
						break;
						
					case "comment":
						if (propValue != null && !propValue.toString().trim().isEmpty())
						{
							relation.setComment(propValue.toString());
						}
						break;
						
					default:
						ServoyLog.logInfo("[RelationService] Unknown property: " + propName);
						break;
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("[RelationService] Error setting property " + propName + ": " + e.getMessage(), e);
			}
		}
		
		// Apply default for allowCreationRelatedRecords if not explicitly set
		if (!properties.containsKey("allowCreationRelatedRecords"))
		{
			relation.setAllowCreationRelatedRecords(true);
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
