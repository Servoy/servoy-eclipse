package com.servoy.eclipse.knowledgebase.mcp.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Service for accessing database schema metadata.
 * Provides reusable methods for querying tables, columns, primary keys, and foreign key relationships.
 * Used by multiple tool handlers (DatabaseToolHandler, RelationToolHandler, ValueListToolHandler).
 */
public class DatabaseSchemaService
{
	/**
	 * Get a database server by name.
	 *
	 * @param serverName the database server name
	 * @return IServerInternal instance or null if not found
	 */
	public static IServerInternal getServer(String serverName)
	{
		if (serverName == null || serverName.trim().isEmpty())
		{
			return null;
		}

		try
		{
			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
			return (IServerInternal)serverManager.getServer(serverName, false, false);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get server '" + serverName + "': " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get list of all table names in a server.
	 *
	 * @param server the database server
	 * @return list of table names (empty list if none found)
	 */
	public static List<String> getTableNames(IServerInternal server)
	{
		if (server == null)
		{
			return new ArrayList<>();
		}

		try
		{
			return server.getTableNames(false);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get table names: " + e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get a specific table from a server.
	 *
	 * @param server the database server
	 * @param tableName the table name
	 * @return ITable instance or null if not found
	 */
	public static ITable getTable(IServerInternal server, String tableName)
	{
		if (server == null || tableName == null || tableName.trim().isEmpty())
		{
			return null;
		}

		try
		{
			return server.getTable(tableName);
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get table '" + tableName + "': " + e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Get all columns for a table.
	 *
	 * @param table the table
	 * @return collection of columns (empty if none found)
	 */
	public static Collection<Column> getColumns(ITable table)
	{
		if (table == null)
		{
			return new ArrayList<>();
		}

		try
		{
			Collection<Column> cols = table.getColumns();
			return cols != null ? cols : new ArrayList<>();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get columns: " + e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get primary key columns for a table.
	 *
	 * @param table the table
	 * @return list of primary key columns (empty if none found)
	 */
	public static List<Column> getPrimaryKeyColumns(ITable table)
	{
		if (table == null)
		{
			return new ArrayList<>();
		}

		try
		{
			List<Column> pkCols = table.getRowIdentColumns();
			return pkCols != null ? pkCols : new ArrayList<>();
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get primary keys: " + e.getMessage(), e);
			return new ArrayList<>();
		}
	}

	/**
	 * Get set of primary key column names for a table.
	 *
	 * @param table the table
	 * @return set of PK column names (empty if none found)
	 */
	public static Set<String> getPrimaryKeyNames(ITable table)
	{
		Set<String> pkNames = new HashSet<>();
		List<Column> pkColumns = getPrimaryKeyColumns(table);

		for (Column pkCol : pkColumns)
		{
			pkNames.add(pkCol.getName());
		}

		return pkNames;
	}

	/**
	 * Represents an explicit foreign key relationship found in database metadata.
	 */
	public static class ForeignKeyRelationship
	{
		public final String sourceTable;
		public final String sourceColumn;
		public final String targetTable;

		public ForeignKeyRelationship(String sourceTable, String sourceColumn, String targetTable)
		{
			this.sourceTable = sourceTable;
			this.sourceColumn = sourceColumn;
			this.targetTable = targetTable;
		}
	}

	/**
	 * Analyze all explicit foreign key relationships in a database server.
	 * These are actual FK constraints defined in the database metadata (ColumnInfo).
	 *
	 * @param server the database server
	 * @return list of foreign key relationships (empty if none found)
	 */
	public static List<ForeignKeyRelationship> getExplicitForeignKeys(IServerInternal server)
	{
		List<ForeignKeyRelationship> fkRelationships = new ArrayList<>();

		if (server == null)
		{
			return fkRelationships;
		}

		try
		{
			List<String> tables = getTableNames(server);

			for (String tableName : tables)
			{
				ITable table = getTable(server, tableName);
				if (table == null) continue;

				Collection<Column> columns = getColumns(table);
				for (Column col : columns)
				{
					ColumnInfo colInfo = col.getColumnInfo();
					if (colInfo != null)
					{
						String fkTarget = colInfo.getForeignType();
						if (fkTarget != null && !fkTarget.trim().isEmpty())
						{
							fkRelationships.add(new ForeignKeyRelationship(
								table.getSQLName(),
								col.getName(),
								fkTarget));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to analyze foreign keys: " + e.getMessage(), e);
		}

		return fkRelationships;
	}

	/**
	 * Represents a potential foreign key relationship based on PK name and type matching.
	 */
	public static class PotentialRelationship
	{
		public final String sourceTable;
		public final String sourceColumn;
		public final String targetTable;
		public final String targetColumn;

		public PotentialRelationship(String sourceTable, String sourceColumn, String targetTable, String targetColumn)
		{
			this.sourceTable = sourceTable;
			this.sourceColumn = sourceColumn;
			this.targetTable = targetTable;
			this.targetColumn = targetColumn;
		}
	}

	/**
	 * Analyze potential foreign key relationships based on PK name and type matching.
	 * Finds columns in other tables that match a table's PK column name and type.
	 * Excludes relationships that are already explicit FKs.
	 *
	 * @param server the database server
	 * @return list of potential relationships (empty if none found)
	 */
	public static List<PotentialRelationship> getPotentialRelationships(IServerInternal server)
	{
		List<PotentialRelationship> potentialRelationships = new ArrayList<>();

		if (server == null)
		{
			return potentialRelationships;
		}

		try
		{
			List<String> tables = getTableNames(server);

			// For each table's PK, find matching columns in other tables
			for (String tableName : tables)
			{
				ITable table = getTable(server, tableName);
				if (table == null) continue;

				List<Column> pkColumns = getPrimaryKeyColumns(table);
				if (pkColumns.isEmpty()) continue;

				for (Column pkCol : pkColumns)
				{
					String pkName = pkCol.getName();
					ColumnType pkType = pkCol.getColumnType();

					// Look for this PK column in OTHER tables
					for (String otherTableName : tables)
					{
						if (otherTableName.equalsIgnoreCase(tableName)) continue; // Skip same table

						ITable otherTable = getTable(server, otherTableName);
						if (otherTable == null) continue;

						Collection<Column> otherColumns = getColumns(otherTable);
						for (Column otherCol : otherColumns)
						{
							// Check if column name and type match the PK
							if (otherCol.getName().equalsIgnoreCase(pkName) && pkType.equals(otherCol.getColumnType()))
							{
								// Check if this is already an explicit FK
								ColumnInfo colInfo = otherCol.getColumnInfo();
								boolean isExplicitFK = false;
								if (colInfo != null)
								{
									String fkTarget = colInfo.getForeignType();
									isExplicitFK = (fkTarget != null && !fkTarget.trim().isEmpty());
								}

								if (!isExplicitFK)
								{
									potentialRelationships.add(new PotentialRelationship(
										otherTable.getSQLName(),
										otherCol.getName(),
										table.getSQLName(),
										pkName));
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to analyze potential relationships: " + e.getMessage(), e);
		}

		return potentialRelationships;
	}

	/**
	 * Represents an incoming foreign key reference to a table.
	 */
	public static class IncomingForeignKey
	{
		public final String sourceTable;
		public final String sourceColumn;

		public IncomingForeignKey(String sourceTable, String sourceColumn)
		{
			this.sourceTable = sourceTable;
			this.sourceColumn = sourceColumn;
		}
	}

	/**
	 * Find all incoming foreign keys for a specific table.
	 * These are FK columns in other tables that reference this table.
	 *
	 * @param server the database server
	 * @param targetTable the table to find incoming FKs for
	 * @return list of incoming foreign keys (empty if none found)
	 */
	public static List<IncomingForeignKey> getIncomingForeignKeys(IServerInternal server, ITable targetTable)
	{
		List<IncomingForeignKey> incomingFKs = new ArrayList<>();

		if (server == null || targetTable == null)
		{
			return incomingFKs;
		}

		try
		{
			String targetSqlName = targetTable.getSQLName();
			List<String> allTables = getTableNames(server);

			for (String tableName : allTables)
			{
				ITable table = getTable(server, tableName);
				if (table == null) continue;

				Collection<Column> columns = getColumns(table);
				for (Column col : columns)
				{
					ColumnInfo colInfo = col.getColumnInfo();
					if (colInfo != null)
					{
						String fkTarget = colInfo.getForeignType();
						if (fkTarget != null && fkTarget.equals(targetSqlName))
						{
							incomingFKs.add(new IncomingForeignKey(table.getSQLName(), col.getName()));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("[DatabaseSchemaService] Failed to get incoming foreign keys: " + e.getMessage(), e);
		}

		return incomingFKs;
	}
}
