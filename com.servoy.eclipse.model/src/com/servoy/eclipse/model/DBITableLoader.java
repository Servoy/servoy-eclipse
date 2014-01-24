/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import com.servoy.eclipse.model.util.ServoyExporterUtils;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IColumnInfoManager;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITableLoader;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.IMetadataDefManager;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsManager;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * @author obuligan
 *
 */
public class DBITableLoader implements ITableLoader
{
	private Pair<ITableDefinitionsManager, IMetadataDefManager> defManagers = null;

	@Override
	public boolean loadTables(LinkedHashMap<String, Table> loading_tables, IServerInternal server)
	{
		try
		{
			if (defManagers == null) defManagers = ServoyExporterUtils.getInstance().prepareDbiFilesBasedExportData(null, true, true, true, true);

			for (Entry<String, List<TableDef>> entry : defManagers.getLeft().getServerTableDefs().entrySet())
			{
				if (server.getConfig().getServerName().equals(entry.getKey()))
				{
					List<TableDef> tableDefList = entry.getValue();

					for (TableDef tableDef : tableDefList)
					{
						if (!tableDef.name.toUpperCase().startsWith(IServer.SERVOY_UPPERCASE_PREFIX))
						{
							Table table = new Table(server.getConfig().getServerName(), tableDef.name, true, tableDef.tableType, null, null);
							for (ColumnInfoDef colInfo : tableDef.columnInfoDefSet)
							{
								Column c = new Column(table, colInfo.name, colInfo.columnType.getSqlType(), colInfo.columnType.getLength(),
									colInfo.columnType.getScale(), true);

								c.setAllowNull(colInfo.allowNull);
								//c.setColumnInfo(colInfo);
								if ((Column.PK_COLUMN & colInfo.flags) > 0) c.setDatabasePK(true);
								c.setFlags(colInfo.flags);
								table.addColumn(c);
								//table.a
							}
							//tableDef.primaryKey
							//Column c = new Column(table, name, columnMetaInfo.type, columnMetaInfo.length, columnMetaInfo.scale, true);
							//table.addColumn(c);
							table.setInitialized(true);

							loading_tables.put(table.getName(), table);
						}
					}
				}
			}
			return true;
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return false;
	}

	@Override
	public void loadAllColumnInfo(Collection<Table> tables, IServerInternal server) throws RepositoryException
	{
		IServerManagerInternal sm = ApplicationServerSingleton.get().getServerManager();
		for (Table table : tables)
		{
			IColumnInfoManager[] colInfoManagers = sm.getColumnInfoManagers(table.getName());
			if (colInfoManagers != null && colInfoManagers.length > 0)
			{
				colInfoManagers[0].loadAllColumnInfo(table);
			}
		}
	}
}
