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
package com.servoy.eclipse.core;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ServerConfig;

/**
 * Manage server configuration changes in developer, make sure that the servers are updated accordingly.
 * 
 * @see ServerConfigSyncer will be used in ApplicationServer
 * 
 * @author rgansevles
 * 
 */
public class DeveloperServerConfigSyncer implements IServerConfigListener
{
	private final IServerManagerInternal serverManager;

	public DeveloperServerConfigSyncer(IServerManagerInternal serverManager)
	{
		this.serverManager = serverManager;
	}

	public void serverConfigurationChanged(ServerConfig oldServerConfig, ServerConfig newServerConfig)
	{
		int oldState = -1;
		boolean valid = false;
		if (oldServerConfig != null)
		{
			// server was deleted or changed
			IServerInternal oldServer = (IServerInternal)serverManager.getServer(oldServerConfig.getServerName(), false, false);
			if (oldServer != null)
			{
				oldState = oldServer.getState();
				if (oldServerConfig.isEnabled() && oldServer.isValid()) valid = true;
			}

			serverManager.deleteServer(oldServerConfig);
			ModelUtils.updateActiveSolutionServerProxies(ServoyModel.getDeveloperRepository());
		}

		IServerInternal newServer = null;
		if (newServerConfig != null)
		{
			// server was changed or created
			newServer = (IServerInternal)serverManager.createServer(newServerConfig);
		}
		if (oldState != -1 && newServer != null && oldState != newServer.getState())
		{
			newServer.fireStateChanged(oldState, newServer.getState());
			if (valid && (!newServer.isValid() || !newServerConfig.isEnabled()))
			{
				try
				{
					for (String tableName : newServer.getTableNames(false))
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().flushDataProvidersForTable(newServer.getTable(tableName));
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
	}
}
