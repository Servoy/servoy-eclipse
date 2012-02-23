/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Debug;

/**
 * Quick fix for differences between column info in the dbi file and columns in the DB. It save in the prefs to no longer complain about this difference. 
 * 
 * @author rgansevles
 * 
 * @since 6.1
 */
public class DBIQuickFixIgnoreColumnDifference extends TableDifferenceQuickFix
{

	private static DBIQuickFixIgnoreColumnDifference instance;

	private DBIQuickFixIgnoreColumnDifference()
	{
	}

	public static DBIQuickFixIgnoreColumnDifference getInstance()
	{
		if (instance == null)
		{
			instance = new DBIQuickFixIgnoreColumnDifference();
		}
		return instance;
	}

	public String getLabel()
	{
		return "Accept column difference, no longer report this difference (is stored in the column info as a compatible column type).";
	}

	@Override
	public boolean canHandleDifference(TableDifference difference)
	{
		return difference != null && difference.getType() == TableDifference.COLUMN_CONFLICT &&
			!difference.getDbiFileDefinition().columnType.equals(difference.getTableDefinition().columnType);
	}

	@Override
	public void run(TableDifference difference)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		DataModelManager dmm = servoyModel.getDataModelManager();
		if (dmm != null)
		{
			Column column = difference.getTable().getColumn(difference.getColumnName());
			column.getColumnInfo().addCompatibleColumnType(difference.getTableDefinition().columnType);
			try
			{
				dmm.updateAllColumnInfo(difference.getTable(), false, false);
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
	}
}
