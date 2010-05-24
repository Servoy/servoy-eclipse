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
package com.servoy.eclipse.ui.property;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.TableWrapper;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.Table;

public class TableValueEditor implements IValueEditor<Object>
{
	public static final TableValueEditor INSTANCE = new TableValueEditor();

	public void openEditor(Object value)
	{
		EditorUtil.openTableEditor(((TableWrapper)value).getServerName(), ((TableWrapper)value).getTableName());
	}

	public boolean canEdit(Object value)
	{
		if (value instanceof TableWrapper)
		{
			TableWrapper tw = (TableWrapper)value;

			ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			IServer server = servoyModel.getServerManager().getServer(tw.getServerName(), true, true);
			if (server != null)
			{
				try
				{
					if ((Table)server.getTable(tw.getTableName()) != null)
					{
						return true;
					}
				}
				catch (Exception e)
				{
					String msg = "Could not find table " + tw.getServerName() + '/' + tw.getTableName();
					ServoyLog.logError(msg, e);
					throw new RuntimeException(msg + ": " + e.getMessage());
				}
			}
		}

		return false;
	}
}
