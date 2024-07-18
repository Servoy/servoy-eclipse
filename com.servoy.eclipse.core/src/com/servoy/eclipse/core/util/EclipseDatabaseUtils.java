/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */
package com.servoy.eclipse.core.util;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.json.JSONException;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DatabaseUtils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Set of utility methods related to database operations.
 *
 * @author acostescu
 */
public final class EclipseDatabaseUtils
{
	public static final int UPDATE_NOW = 0;
	public static final int UPDATE_LATER = 1;
	public static final int NO_UPDATE = 2;

	private EclipseDatabaseUtils()
	{
		// not meant to be instantiated
	}

	public static String createNewTableFromColumnInfo(IServerInternal server, String tableName, String dbiFileContent, int updateContent)
	{
		return createNewTableFromColumnInfo(server, tableName, dbiFileContent, updateContent, true);
	}

	/**
	 * Creates a new table in the database based on the contents of a .dbi file.
	 *
	 * @param server the server to which the table will be added.
	 * @param tableName the name of the table.
	 * @param dbiFileContent the JSON column/table info content.
	 * @param updateContent Specify how the table's info will be written to it's dbi file.
	 * @return null if all is OK; if any problems are encountered, they will be added to this String, separated by "\n"
	 */
	public static String createNewTableFromColumnInfo(IServerInternal server, String tableName, String dbiFileContent, int updateContent,
		boolean fireTableCreated)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null) return "Cannot find database information manager. There may be problems with the resources project.";
		TableDef tableInfo;
		try
		{
			tableInfo = DatabaseUtils.deserializeTableInfo(dbiFileContent);
		}
		catch (JSONException e)
		{
			return "Corrupt .dbi file: " + e.getMessage();
		}
		if (!tableName.equals(tableInfo.name))
		{
			return "Table name does not match dbi file name for " + tableName;
		}
		if (tableInfo.columnInfoDefSet.size() == 0)
		{
			return "Table " + tableName + " does not have any columns";
		}
		final StringBuffer problems = new StringBuffer();

		// create table
		try
		{
			// use a validator that validates everything, but logs problems
			IValidateName validator = new IValidateName()
			{
				private final IValidateName normalValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();

				public void checkName(String nameToCheck, int skip_element_id, ValidatorSearchContext searchContext, boolean sqlRelated)
					throws RepositoryException
				{
					try
					{
						normalValidator.checkName(nameToCheck, skip_element_id, searchContext, sqlRelated);
					}
					catch (RepositoryException e)
					{
						ServoyLog.logWarning("Create new table from column info - " + e.getMessage(), null);
						problems.append("WARNING! " + e.getMessage() + "\n");
					}
				}
			};
			ITable table = server.createNewTable(validator, tableName, false, fireTableCreated);
			table.setMarkedAsMetaData(Boolean.TRUE.equals(tableInfo.isMetaData));
			server.setTableMarkedAsHiddenInDeveloper(table, tableInfo.hiddenInDeveloper, fireTableCreated);

			// Warn if the table types are different.
			if (table.getTableType() != tableInfo.tableType)
			{
				problems.append("WARNING! The table '" + table.getName() + " in server '" + table.getServerName() + "' has type " +
					Table.getTableTypeAsString(table.getTableType()) + " but in the import it has type " + Table.getTableTypeAsString(tableInfo.tableType) +
					". Servoy cannot keep the datamodels synchronized automatically if you use database views, please do so manually.\n");
			}

			// Iterate over all the columns of this table and add them to
			// the table if necessary.
			Collections.sort(tableInfo.columnInfoDefSet, new Comparator<ColumnInfoDef>()
			{
				public int compare(ColumnInfoDef o1, ColumnInfoDef o2)
				{
					if (o1.creationOrderIndex < o2.creationOrderIndex) return -1;
					else if (o1.creationOrderIndex > o2.creationOrderIndex) return 1;
					else if (o1.creationOrderIndex == o2.creationOrderIndex) return o1.name.compareTo(o2.name);
					else return 0;
				}
			});
			Iterator<ColumnInfoDef> columnInfoIt = tableInfo.columnInfoDefSet.iterator();
			while (columnInfoIt.hasNext())
			{
				ColumnInfoDef columnInfoInfo = columnInfoIt.next();

				// Add the column with the appropriate information.
				Column column = table.createNewColumn(validator, columnInfoInfo.name, columnInfoInfo.columnType.getSqlType(),
					columnInfoInfo.columnType.getLength(), columnInfoInfo.columnType.getScale());
				column.setDatabasePK((columnInfoInfo.flags & IBaseColumn.PK_COLUMN) != 0);
				column.setFlags(columnInfoInfo.flags);
				column.setAllowNull(columnInfoInfo.allowNull);

				// update the auto enter type
				// See if we must set the sequence type: must do that before creation of column.
				if (columnInfoInfo.autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER)
				{
					// Set the sequence type (new table, or override).
					try
					{
						int sequenceType = columnInfoInfo.autoEnterSubType;
						if (!server.supportsSequenceType(sequenceType, null))
						{
							// Database does not support the import sequence type, default to servoy sequence.
							problems.append("The import version of the column '" + columnInfoInfo.name + "' of table '" + tableInfo.name + "' in server '" +
								server.getName() + "' has '" + ColumnInfo.getSeqDisplayTypeString(sequenceType) +
								"' sequence type which is not supported by the database, using '" +
								ColumnInfo.getSeqDisplayTypeString(ColumnInfo.SERVOY_SEQUENCE) + "' sequence type instead.\n");
							sequenceType = ColumnInfo.SERVOY_SEQUENCE;
						}

						// Set the resulting sequence type on the column.
						column.setSequenceType(sequenceType);
					}
					catch (Exception e)
					{
						throw new RepositoryException(e);
					}
				}
			}

			// Sync database table with table object of server.
			// make sure the .dbi file is not written back to disk - this may cause problems if this method is
			// called from a resource change listeners that locks the resources tree...
			dmm.setWritesEnabled(false);
			try
			{
				server.syncTableObjWithDB(table, false, false);
			}
			catch (Exception e)
			{
				try
				{
					if (!table.getExistInDB()) server.removeTable(table);
				}
				catch (Exception fatal)
				{
					problems.append("Fatal error: " + fatal.getMessage() + "\n");
					ServoyLog.logError(fatal);
				}
				throw (e instanceof RepositoryException) ? (RepositoryException)e : new RepositoryException(e);
			}
			finally
			{
				dmm.setWritesEnabled(true);
			}

			// Iterate over all the columns of this table and update the
			// column info.
			columnInfoIt = tableInfo.columnInfoDefSet.iterator();
			while (columnInfoIt.hasNext())
			{
				ColumnInfoDef columnInfoDef = columnInfoIt.next();

				// Check to see if the column exists.
				Column column = table.getColumn(columnInfoDef.name);
				if (column == null)
				{
					throw new RepositoryException("Column '" + columnInfoDef.name + "' in table '" + tableInfo.name + "' for server '" + server.getName() +
						"' does not exist and could not be created.");
				}

				// Update the column info of this table.
				ColumnInfo columnInfo = column.getColumnInfo();
				boolean newColumnInfoObj = false;
				if (columnInfo == null)
				{
					int element_id = ApplicationServerRegistry.get().getDeveloperRepository().getNewElementID(null);
					columnInfo = new ColumnInfo(element_id, true);
					newColumnInfoObj = true;
				}

				columnInfo.setAutoEnterType(columnInfoDef.autoEnterType);
				columnInfo.setAutoEnterSubType(columnInfoDef.autoEnterSubType);
				columnInfo.setDefaultValue(columnInfoDef.defaultValue);
				columnInfo.setLookupValue(columnInfoDef.lookupValue);
				// validation id not implemented yet -- leave alone
				columnInfo.setTitleText(columnInfoDef.titleText);
				columnInfo.setDescription(columnInfoDef.description);
				columnInfo.setConverterProperties(columnInfoDef.converterProperties);
				columnInfo.setConverterName(columnInfoDef.converterName);
				columnInfo.setForeignType(columnInfoDef.foreignType);
				columnInfo.setValidatorProperties(columnInfoDef.validatorProperties);
				columnInfo.setValidatorName(columnInfoDef.validatorName);
				columnInfo.setDefaultFormat(columnInfoDef.defaultFormat);
				columnInfo.setElementTemplateProperties(columnInfoDef.elementTemplateProperties);
				columnInfo.setDatabaseSequenceName(columnInfoDef.databaseSequenceName);
				columnInfo.setPreSequenceChars(columnInfoDef.preSequenceChars);
				columnInfo.setPostSequenceChars(columnInfoDef.postSequenceChars);
				columnInfo.setSequenceStepSize(columnInfoDef.sequenceStepSize);
				columnInfo.setDataProviderID(columnInfoDef.dataProviderID);
				columnInfo.setContainsMetaData(columnInfoDef.containsMetaData);
				columnInfo.setConfiguredColumnType(columnInfoDef.columnType);
				columnInfo.setCompatibleColumnTypes(columnInfoDef.compatibleColumnTypes);
				columnInfo.setSortIgnorecase(columnInfoDef.sortIgnorecase);
				columnInfo.setSortingNullprecedence(columnInfoDef.sortingNullprecedence);

				if (newColumnInfoObj) column.setColumnInfo(columnInfo); // it was null before so set it in column now
				column.setFlags(columnInfoDef.flags);
				columnInfo.flagChanged();
			}
			try
			{
				table.updateDataproviderIDsIfNeeded(); // column info above could have set a dataproviderid/alias
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
			// createMissingDBSequences uses columninfo
			try
			{
				server.createMissingDBSequences(table);
			}
			catch (SQLException e)
			{
				throw new RepositoryException("Could not create db sequences for table " + table, e);
			}

			if (updateContent != EclipseDatabaseUtils.NO_UPDATE)
			{
				dmm.updateAllColumnInfo(table, updateContent != EclipseDatabaseUtils.UPDATE_LATER ? false : true);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
			problems.append(e.getMessage() + "\n");
		}


		return problems.length() > 0 ? problems.toString() : null;
	}

	public static String getRelationsString(Relation[] relations)
	{
		if (relations == null)
		{
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < relations.length; i++)
		{
			if (i > 0) sb.append('.');
			sb.append(relations[i].getName());
		}
		return sb.toString();
	}


	public static int getDataproviderType(IPersist persist, String format, String dataProviderID)
	{
		int type = IColumnTypes.TEXT;

		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form != null)
		{
			IDataProviderLookup dataproviderLookup = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getDataproviderLookup(
				null, form);
			ComponentFormat componentFormat = null;
			if (format != null)
			{
				componentFormat = ComponentFormat.getComponentFormat(format, dataProviderID, dataproviderLookup, Activator.getDefault().getDesignClient());
			}
			if (componentFormat != null)
			{
				type = componentFormat.dpType;
			}
			else
			{
				try
				{
					IDataProvider dataProvider = dataproviderLookup.getDataProvider(dataProviderID);
					if (dataProvider != null)
					{
						type = dataProvider.getDataProviderType();
					}
				}
				catch (RepositoryException re)
				{
					ServoyLog.logError(re);
				}
			}
		}
		return type;
	}

	public static String getPostgresServerUrl(ServerConfig origConfig, String name)
	{
		String dbname = null;
		String serverUrl = origConfig.getServerUrl();
		int startIndex = serverUrl.lastIndexOf("/");
		int endIndex = serverUrl.indexOf("?", startIndex);
		if (endIndex == -1) endIndex = serverUrl.length();
		dbname = serverUrl.substring(startIndex + 1, endIndex);
		if (dbname != null) serverUrl = serverUrl.replaceFirst("/" + dbname, "/" + name);
		if (serverUrl.equals(origConfig.getServerUrl()))
		{
			// hmm, no replace, fall back to default
			serverUrl = "jdbc:postgresql://localhost/" + name;
		}
		return serverUrl;
	}
}