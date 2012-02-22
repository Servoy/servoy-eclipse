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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.preferences.DbiFilePreferences;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.util.ServoyLog;

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
		return "Accept column difference, no longer report this difference (is stored in active project preferences).";
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
		new DbiFilePreferences(ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject()).addAcceptedColumnDifference(
			difference.getDbiFileDefinition().columnType, difference.getTableDefinition().columnType);

		// trigger dbi file change
		DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
		if (dmm != null)
		{
			IFile file = dmm.getDBIFile(difference.getServerName(), difference.getTableName());
			if (file.exists())
			{
				try
				{
					file.touch(null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}
}
