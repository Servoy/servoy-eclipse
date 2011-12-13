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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions.TableListType;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * Content provider class for tables.
 * 
 * @author rgansevles
 * 
 */

public class TableContentProvider extends ArrayContentProvider implements ITreeContentProvider
{
	private TableListOptions options;

	public static final TableWrapper TABLE_NONE = new TableWrapper(null, null);

	// do not use a static instance, this content provider keeps state
	public TableContentProvider()
	{
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof TableListOptions)
		{
			options = (TableListOptions)inputElement;
			List<Object> lst = new ArrayList<Object>();
			if (options.includeNone) lst.add(TABLE_NONE);

			String[] serverNames;
			if (options.serverName == null)
			{
				serverNames = ServoyModel.getServerManager().getServerNames(true, false, true, false);
			}
			else
			{
				serverNames = new String[] { options.serverName };
			}
			for (String serverName : serverNames)
			{
				Object[] tables = null;
				if (options.type != TableListType.ALL || options.serverName != null)
				{
					tables = getTables(serverName, options);
				}
				if (options.serverName == null)
				{
					// top nodes servers
					if (options.type == TableListType.ALL || tables.length > 0)
					{
						lst.add(new TableWrapper(serverName, null)); // table == null -> server
					}
				}
				else
				{
					// list tables directly
					lst.addAll(Arrays.asList(tables));
				}
			}
			return lst.toArray();
		}

		return super.getElements(inputElement);
	}

	public Object getParent(Object element)
	{
		if (element instanceof TableWrapper)
		{
			TableWrapper tw = (TableWrapper)element;
			if (tw.getTableName() != null)
			{
				return new TableWrapper(tw.getServerName(), null);
			}
		}
		return null;
	}

	public Object[] getChildren(Object parentElement)
	{
		if (parentElement instanceof TableWrapper)
		{
			TableWrapper tw = (TableWrapper)parentElement;
			if (tw.getTableName() == null)
			{
				return getTables(tw.getServerName(), options);
			}
		}

		return new Object[0];
	}

	public static Object[] getTables(String serverName, TableListOptions options)
	{
		List<TableWrapper> lst = new ArrayList<TableWrapper>();
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IServerInternal server = (IServerInternal)ServoyModel.getServerManager().getServer(serverName, true, true);
		if (server != null)
		{
			try
			{
				for (String tableName : server.getTableAndViewNames(true, true))
				{
					if (options.type == TableListType.I18N)
					{
						ITable table = server.getTable(tableName);
						List<String> columnNames = Arrays.asList(table.getColumnNames());
						if (!columnNames.contains("message_key") || !columnNames.contains("message_value") || !columnNames.contains("message_language"))
						{
							continue;
						}
					}
					lst.add(new TableWrapper(serverName, tableName));
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get tables for server " + server, e);
			}
		}
		return lst.toArray();
	}

	public boolean hasChildren(Object element)
	{
		return !TABLE_NONE.equals(element) && (element instanceof TableWrapper && ((TableWrapper)element).getTableName() == null);
	}

	public static class TableListOptions
	{
		public static enum TableListType
		{
			ALL, I18N
		}

		public final TableListType type;
		public final boolean includeNone;
		public final String serverName;

		public TableListOptions(TableListType type, boolean includeNone)
		{
			this.type = type;
			this.includeNone = includeNone;
			this.serverName = null;
		}

		public TableListOptions(TableListType type, boolean includeNone, String serverName)
		{
			this.type = type;
			this.includeNone = includeNone;
			this.serverName = serverName;
		}
	}

}
