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
package com.servoy.eclipse.core.quickfix.dbi;

import org.eclipse.jface.dialogs.MessageDialog;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;

/**
 * Quick fix for differences between column info in the dbi file and columns in the DB. It will change the DB column to match the column information.
 *
 * @author acostescu
 */
public class DBIQuickFixUpdateColumnFromInfo extends TableDifferenceQuickFix
{

	private static DBIQuickFixUpdateColumnFromInfo instance;

	private DBIQuickFixUpdateColumnFromInfo()
	{
	}

	public static DBIQuickFixUpdateColumnFromInfo getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixUpdateColumnFromInfo();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Replace DB column with a column created from the DB information file. THIS WILL DROP THE COLUMN AND CREATE A NEW ONE";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_CONFLICT &&
			(!difference.getDbiFileDefinition().columnType.equals(difference.getTableDefinition().columnType) ||
				(difference.getDbiFileDefinition().allowNull && (!difference.getTableDefinition().allowNull)));
	}

	@Override
	public void run(TableDifference difference)
	{
		ColumnInfoDef dbiFileDefinition = difference.getDbiFileDefinition();
		ColumnType columnType = dbiFileDefinition.columnType;
		try
		{
			IDeveloperServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
			DataModelManager dmm = sm.getDataModelManager();
			if (dmm != null)
			{
				dmm.setWritesEnabled(false);
				try
				{
					IServerInternal s = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(difference.getServerName());

					// delete column from memory obj.
					difference.getTable().removeColumn(difference.getColumnName());

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
								ServoyLog.logWarning("Fix update column from column info - " + e.getMessage(), null);
								storeException(e);
							}
						}
					};

					// create a new column with the same name, but using column information
					Column c = difference.getTable().createNewColumn(validator, difference.getColumnName(), columnType);
					c.setDatabasePK((dbiFileDefinition.flags & IBaseColumn.PK_COLUMN) != 0);
					c.setFlags(dbiFileDefinition.flags);
					c.setAllowNull(dbiFileDefinition.allowNull);
					if (dbiFileDefinition.autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER)
					{
						// Set the sequence type (new table, or override).
						try
						{
							int sequenceType = dbiFileDefinition.autoEnterSubType;
							if (!s.supportsSequenceType(sequenceType, null))
							{
								// Database does not support the import sequence type, default to servoy sequence.
								StringBuffer message = new StringBuffer("The DB information on column '");
								message.append(difference.getColumnString());
								message.append("' has '");
								message.append(ColumnInfo.getSeqDisplayTypeString(sequenceType));
								message.append("' sequence type which is not supported by the database; using '");
								message.append(ColumnInfo.getSeqDisplayTypeString(ColumnInfo.SERVOY_SEQUENCE));
								message.append("' sequence type instead.");
								MessageDialog.openWarning(UIUtils.getActiveShell(), "Unsupported sequence type", message.toString());
								sequenceType = ColumnInfo.SERVOY_SEQUENCE;
							}
							else if (sequenceType == ColumnInfo.DATABASE_IDENTITY)
							{
								StringBuffer message = new StringBuffer("The DB information on column '");
								message.append(difference.getColumnString());
								message.append(
									"' has type 'dbident'. As the table already exists, this column is only marked by Servoy as 'dbident' but it will not be created as such in the database.");
								MessageDialog.openWarning(UIUtils.getActiveShell(), "Creating 'dbident' column in existing table",
									message.toString());
							}

							// Set the resulting sequence type on the column.
							c.setSequenceType(sequenceType);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
							storeException(e);
						}
					}

					// apply the changes (delete column from database as well)
					s.syncTableObjWithDB(difference.getTable(), false, true);

					// reload the column information for this table just to make sure everything is in sync
					dmm.loadAllColumnInfo(difference.getTable());
				}
				finally
				{
					dmm.setWritesEnabled(true);
				}
			}
			else
			{
				ServoyLog.logError("Null dmm while trying to apply UpdateColumnFromInfo QuickFix", null);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
			storeException(e);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			storeException(e);
		}
	}

}
