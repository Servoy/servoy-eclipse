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
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;

/**
 * Quick fix for missing columns in DB (although they are present in the dbi files). It will create a column.
 * 
 * @author acostescu
 */
public class DBIQuickFixCreateColumnInDB extends TableDifferenceQuickFix
{

	private static DBIQuickFixCreateColumnInDB instance;

	private DBIQuickFixCreateColumnInDB()
	{
	}

	public static DBIQuickFixCreateColumnInDB getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixCreateColumnInDB();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Create missing column in the DB table using the information from the DB information file.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_MISSING_FROM_DB;
	}

	@Override
	public void run(TableDifference difference)
	{
		int type = difference.getDbiFileDefinition().datatype;
		int lenght = difference.getDbiFileDefinition().length;
		Column c;
		try
		{
			ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
			DataModelManager dmm = sm.getDataModelManager();
			if (dmm != null)
			{
				// because creating a new column will also create some default column information,
				// we must disable writes so that the dbi file is not overwritten (we want to use the information
				// from the dbi file)
				dmm.setWritesEnabled(false);
				try
				{
					IServerInternal s = (IServerInternal)ServoyModel.getServerManager().getServer(difference.getServerName());

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
								ServoyLog.logWarning("Fix create column from column info - " + e.getMessage(), null);
							}
						}
					};
					// create the new column in memory
					c = difference.getTable().createNewColumn(validator, difference.getColumnName(), type, lenght);
					c.setDatabasePK((difference.getDbiFileDefinition().flags & Column.PK_COLUMN) != 0);
					c.setAllowNull(difference.getDbiFileDefinition().allowNull);
					if (difference.getDbiFileDefinition().autoEnterType == ColumnInfo.SEQUENCE_AUTO_ENTER)
					{
						// Set the sequence type (new table, or override).
						try
						{
							int sequenceType = difference.getDbiFileDefinition().autoEnterSubType;
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
								MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Unsupported sequence type", message.toString());
								sequenceType = ColumnInfo.SERVOY_SEQUENCE;
							}
							else if (sequenceType == ColumnInfo.DATABASE_IDENTITY)
							{
								StringBuffer message = new StringBuffer("The DB information on column '");
								message.append(difference.getColumnString());
								message.append("' has type 'dbident'. As the table already exists, this column is only marked by Servoy as 'dbident' but it will not be created as such in the database.");
								MessageDialog.openWarning(Display.getCurrent().getActiveShell(), "Creating 'dbident' column in existing table",
									message.toString());
							}

							// Set the resulting sequence type on the column.
							c.setSequenceType(sequenceType);
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}

					// apply the changes (create column in database as well)
					s.syncTableObjWithDB(difference.getTable(), false, true, null);

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
				ServoyLog.logError("Null dmm while trying to apply CreateColumnInDB QuickFix", null);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
}