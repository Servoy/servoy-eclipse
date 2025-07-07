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

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.AbstractMemServerWrapper;
import com.servoy.eclipse.model.util.InMemServerWrapper;
import com.servoy.eclipse.model.util.MenuFoundsetServerWrapper;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.model.util.ViewFoundsetServerWrapper;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions.TableListType;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;

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
				serverNames = ApplicationServerRegistry.get().getServerManager().getServerNames(true, false, true, false);
			}
			else
			{
				serverNames = new String[] { options.serverName };
			}
			for (String serverName : serverNames)
			{
				if (options.serverName == null)
				{
					// top nodes servers
					lst.add(new TableWrapper(serverName, null)); // table == null -> server
				}
				else
				{
					// list tables directly
					lst.addAll(Arrays.asList(getTables(serverName, options)));
				}
			}
			if (options.serverName == null)
			{
				lst.add(new InMemServerWrapper());
				if (options.includeViewFS) lst.add(new ViewFoundsetServerWrapper());
				lst.add(new MenuFoundsetServerWrapper());
			}
			else if (options.serverName.equals(DataSourceUtils.INMEM_DATASOURCE))
			{
				lst.addAll(Arrays.asList(getChildren(new InMemServerWrapper())));
			}
			else if (options.serverName.equals(DataSourceUtils.VIEW_DATASOURCE))
			{
				lst.addAll(Arrays.asList(getChildren(new ViewFoundsetServerWrapper())));
			}
			else if (options.serverName.equals(DataSourceUtils.MENU_DATASOURCE))
			{
				lst.addAll(Arrays.asList(getChildren(new MenuFoundsetServerWrapper())));
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
		else if (element instanceof InMemServerWrapper && ((InMemServerWrapper)element).getTableName() != null)
		{
			return new InMemServerWrapper();
		}
		else if (element instanceof ViewFoundsetServerWrapper && ((ViewFoundsetServerWrapper)element).getTableName() != null)
		{
			return new ViewFoundsetServerWrapper();
		}
		else if (element instanceof MenuFoundsetServerWrapper && ((MenuFoundsetServerWrapper)element).getTableName() != null)
		{
			return new MenuFoundsetServerWrapper();
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
		else if (parentElement instanceof AbstractMemServerWrapper && ((AbstractMemServerWrapper)parentElement).getTableName() == null)
		{
			Collection<String> tableNames = ((AbstractMemServerWrapper)parentElement).getTableNames();
			AbstractMemServerWrapper[] wrappers = new AbstractMemServerWrapper[tableNames.size()];
			int i = 0;
			for (String tableName : tableNames)
			{
				wrappers[i++] = parentElement instanceof InMemServerWrapper ? new InMemServerWrapper(tableName)
					: (parentElement instanceof ViewFoundsetServerWrapper ? new ViewFoundsetServerWrapper(tableName)
						: new MenuFoundsetServerWrapper(tableName));
			}
			return wrappers;
		}

		return new Object[0];
	}

	public static Object[] getTables(String serverName, TableListOptions options)
	{
		List<TableWrapper> lst = new ArrayList<TableWrapper>();
		ServoyModelManager.getServoyModelManager().getServoyModel();
		IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, true, true);
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
					lst.add(new TableWrapper(serverName, tableName, ((IServer)server).getTableType(tableName) == ITable.VIEW));
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get tables for server " + server, e);
			}
			catch (RemoteException e)
			{
				ServoyLog.logError(e);
			}
		}
		return lst.toArray();
	}

	public boolean hasChildren(Object element)
	{
		return !TABLE_NONE.equals(element) && ((element instanceof TableWrapper && ((TableWrapper)element).getTableName() == null) ||
			(element instanceof AbstractMemServerWrapper && ((AbstractMemServerWrapper)element).getTableName() == null));
	}

	public static class TableListOptions
	{
		public static enum TableListType
		{
			ALL, I18N
		}

		public final TableListType type;
		public final boolean includeNone;
		public final boolean includeViewFS;
		public final String serverName;

		public TableListOptions(TableListType type, boolean includeNone)
		{
			this.type = type;
			this.includeNone = includeNone;
			this.serverName = null;
			this.includeViewFS = true;
		}

		public TableListOptions(TableListType type, boolean includeNone, boolean includeView)
		{
			this.type = type;
			this.includeNone = includeNone;
			this.serverName = null;
			this.includeViewFS = includeView;
		}

		public TableListOptions(TableListType type, boolean includeNone, String serverName)
		{
			this.type = type;
			this.includeNone = includeNone;
			this.serverName = serverName;
			this.includeViewFS = true;
		}
	}

}
