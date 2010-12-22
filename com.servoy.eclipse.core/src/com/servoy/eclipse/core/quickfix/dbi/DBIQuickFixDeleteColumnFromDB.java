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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Quick fix for missing column info in the dbi file (although they are present in the DB). It will delete the DB column.
 * 
 * @author acostescu
 */
public class DBIQuickFixDeleteColumnFromDB extends TableDifferenceQuickFix
{

	private static DBIQuickFixDeleteColumnFromDB instance;

	private DBIQuickFixDeleteColumnFromDB()
	{
	}

	public static DBIQuickFixDeleteColumnFromDB getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixDeleteColumnFromDB();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Delete column from DB table because it has no column information associated.";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_MISSING_FROM_DBI_FILE;
	}

	@Override
	public void run(TableDifference difference)
	{
		try
		{
			ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();
			DataModelManager dmm = sm.getDataModelManager();
			if (dmm != null)
			{
				dmm.setWritesEnabled(false);
				try
				{
					IServerInternal s = (IServerInternal)ServoyModel.getServerManager().getServer(difference.getServerName());

					// delete column from memory obj.
					difference.getTable().removeColumn(difference.getColumnName());

					// apply the changes (delete column from database as well)
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
				ServoyLog.logError("Null dmm while trying to apply DeleteColumnFromDB QuickFix", null);
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
