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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.json.JSONException;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.DataModelManager;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * Set of utility methods related to database operations.
 * 
 * @author acostescu
 */
public final class DatabaseUtils
{

	private DatabaseUtils()
	{
		// not meant to be instantiated
	}

	/**
	 * Creates a new table in the database based on the contents of a .dbi file.
	 * 
	 * @param server the server to which the table will be added.
	 * @param tableName the name of the table.
	 * @param dbiFileContent the JSON column/table info content.
	 * @param writeBackLater if true, the new table's info will be written to it's dbi file later. Otherwise the write will occur during this call.
	 * @return null if all is OK; if any problems are encountered, they will be added to this String, separated by "\n"
	 */
	public static String createNewTableFromColumnInfo(IServerInternal server, String tableName, String dbiFileContent, boolean writeBackLater)
	{
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm == null) return "Cannot find database information manager. There may be problems with the resources project.";
		TableDef tableInfo;
		try
		{
			tableInfo = dmm.deserializeTableInfo(dbiFileContent);
		}
		catch (JSONException e)
		{
			return "Corrupt .dbi file: " + e.getMessage();
		}
		if (!tableName.equals(tableInfo.name))
		{
			return "Table name does not match dbi file name for " + tableName;
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
						problems.append("WARNING! " + e.getMessage() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ 
					}
				}
			};
			Table table = server.createNewTable(validator, tableName, false);
			// Warn if the table types are different.
			if (table.getTableType() != tableInfo.tableType)
			{
				problems.append("WARNING! The table '" + table.getName() + " in server '" + table.getServerName() + "' has type " + Table.getTableTypeAsString(table.getTableType()) + " but in the import it has type " + Table.getTableTypeAsString(tableInfo.tableType) + ". Servoy cannot keep the datamodels synchronized automatically if you use database views, please do so manually.\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}

			// Iterate over all the columns of this table and add them to
			// the table if necessary.
			Collections.sort(tableInfo.columnInfoDefSet, new Comparator<ColumnInfoDef>()
			{
				public int compare(ColumnInfoDef o1, ColumnInfoDef o2)
				{
					if (o1.creationOrderIndex < o2.creationOrderIndex) return -1;
					else if (o1.creationOrderIndex > o2.creationOrderIndex) return 1;
					else return 0;
				}
			});
			Iterator<ColumnInfoDef> columnInfoIt = tableInfo.columnInfoDefSet.iterator();
			while (columnInfoIt.hasNext())
			{
				ColumnInfoDef columnInfoInfo = columnInfoIt.next();

				// Add the column with the appropriate information.
				Column column = table.createNewColumn(validator, columnInfoInfo.name, columnInfoInfo.datatype, columnInfoInfo.length);
				column.setDatabasePK((columnInfoInfo.flags & Column.PK_COLUMN) != 0);
				column.setAllowNull(columnInfoInfo.allowNull);

				// update the auto enter type
				// See if we must set the sequence type: must do that before creation of column.
				if (columnInfoInfo.autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER)
				{
					// Set the sequence type (new table, or override).
					try
					{
						int sequenceType = columnInfoInfo.systemValue;
						if (!server.supportsSequenceType(sequenceType, null))
						{
							// Database does not support the import sequence type, default to servoy sequence.
							problems.append("The import version of the column '" + columnInfoInfo.name + "' of table '" + tableInfo.name + "' in server '" + server.getName() + "' has '" + ColumnInfo.getSeqDisplayTypeString(sequenceType) + "' sequence type which is not supported by the database, using '" + ColumnInfo.getSeqDisplayTypeString(ColumnInfo.SERVOY_SEQUENCE) + "' sequence type instead.\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
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
				server.syncTableObjWithDB(table, false, true, null);
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
					throw new RepositoryException(
						"Column '" + columnInfoDef.name + "' in table '" + tableInfo.name + "' for server '" + server.getName() + "' does not exist and could not be created."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}

				// Update the column info of this table.
				ColumnInfo columnInfo = column.getColumnInfo();
				if (columnInfo != null)
				{
					columnInfo.setAutoEnterType(columnInfoDef.autoEnterType);
					columnInfo.setAutoEnterSubType(columnInfoDef.systemValue);
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
					column.setFlags(columnInfoDef.flags);

					columnInfo.flagChanged();
				}
			}
			dmm.updateAllColumnInfo(table, writeBackLater);
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

}