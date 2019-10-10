/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author dtimut
 *
 */
public class InvalidTableBecauseNoPrimaryKeyQuickFix implements IMarkerResolution
{

	private final String serverName;
	private final String tableName;

	/**
	 * @param tableNode
	 * @param project
	 */
	public InvalidTableBecauseNoPrimaryKeyQuickFix(final String serverName, final String tableName)
	{
		this.serverName = serverName;
		this.tableName = tableName;
	}

	@Override
	public String getLabel()
	{
		return "The quick fix will hide the table: " + tableName;
	}

	@Override
	public void run(final IMarker marker)
	{
		final DataModelManager dataModelManager = ServoyModelFinder.getServoyModel().getDataModelManager();
		final IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, true, true);
		try
		{
			final Table table = (Table)server.getTable(tableName);
			server.setTableMarkedAsHiddenInDeveloper(tableName, true);
			dataModelManager.updateHiddenInDeveloperState(table);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

	}

}
