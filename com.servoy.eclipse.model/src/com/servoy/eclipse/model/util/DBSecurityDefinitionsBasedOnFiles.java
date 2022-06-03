/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.model.util;

import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.xmlxport.ColumnInfoDef;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsAndSecurityBasedOnWorkspaceFiles;
import com.servoy.j2db.util.xmlxport.ServerDef;
import com.servoy.j2db.util.xmlxport.TableDef;

/**
 * This class is meant to be able to build up database security information based directly on available .sec files from the resources project - for all the tables from a
 * .dbi based ITableDefinitionsAndSecurityBasedOnWorkspaceFiles.<br/><br/>
 *
 * Basically this is needed to be able to export database security information properly for .dbi only export - so when the DB might be offline and
 * the user manager didn't load the security info for DBs.
 *
 * @author acostescu
 */
class DBSecurityDefinitionsBasedOnFiles
{

	private final Map<String, List<SecurityInfo>> databaseSecurityInfoByGroup = new HashMap<>();

	DBSecurityDefinitionsBasedOnFiles(ITableDefinitionsAndSecurityBasedOnWorkspaceFiles tableDefinitionsBasedOnDBIFiles)
	{
		DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager(); // this should always be non-null when exporting; we always export the active solution so static access shouldn't hurt

		Set<String> availableGroupNames = new HashSet<>();
		try
		{
			ApplicationServerRegistry.get().getUserManager().getGroups(null).getRows()
				.forEach(row -> availableGroupNames.add((String)row[1])); // user manager is refreshed to the active solution all the time so using this static access here should be ok
		}
		catch (RemoteException | ServoyException e)
		{
			ServoyLog.logError(e); // should never happen as it's always an eclipse user manager that doesn't throw these
		}

		Map<ServerDef, List<TableDef>> allTableDefsFromAllServersBasedOnDBI = tableDefinitionsBasedOnDBIFiles.getServerTableDefs();
		for (Entry<ServerDef, List<TableDef>> serverToTableList : allTableDefsFromAllServersBasedOnDBI.entrySet())
		{
			String serverName = serverToTableList.getKey().name;
			for (TableDef tableDef : serverToTableList.getValue())
			{
				IFile file = dataModelManager.getSecurityFile(serverName, tableDef.name);
				if (file.exists())
				{
					try
					{
						String fileContent = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8"));
						if (fileContent != null)
						{
							// check to see if this table also has security info next to it (.sec file); if so, load it into memory
							Map<String, List<SecurityInfo>> tableAccess = WorkspaceUserManager.deserializeSecurityPermissionInfo(fileContent);

							tableAccess.forEach((groupName, tableRightsFromFile) -> {
								if (availableGroupNames.contains(groupName))
								{
									SecurityInfo permissionsForThisTableAndThisGroup = tableRightsFromFile.get(0); // it's might be - for some reason an array of columns but all columns have the same access; even the .sec file stores it as one thing so we just get the first entry (which I think is in this case the only one as it is read from a .sec file)
									List<SecurityInfo> globalGroupRights = databaseSecurityInfoByGroup.get(groupName);
									if (globalGroupRights == null)
									{
										globalGroupRights = new ArrayList<>();
										databaseSecurityInfoByGroup.put(groupName, globalGroupRights);
									}

									for (ColumnInfoDef columnDef : tableDef.columnInfoDefSet)
									{
										globalGroupRights.add(new SecurityInfo(Utils.getDotQualitfied(serverName, tableDef.name, columnDef.name),
											permissionsForThisTableAndThisGroup.access));
									}
								}
							});
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}

	List<SecurityInfo> getDatabaseSecurityInfoByGroup(String groupName)
	{
		return databaseSecurityInfoByGroup.get(groupName);
	}

}
