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

package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IServerConfigListener;
import com.servoy.j2db.persistence.ServerConfig;

/**
 * Listener responsible to make updates when changes occur in the server editor
 *
 * @author alorincz
 *
 */
public class SolutionExplorerServerConfigSync implements IServerConfigListener
{

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.j2db.persistence.IServerConfigListener#serverConfigurationChanged(com.servoy.j2db.persistence.ServerConfig,
	 * com.servoy.j2db.persistence.ServerConfig)
	 */
	public void serverConfigurationChanged(ServerConfig oldServerConfig, ServerConfig newServerConfig)
	{
		try
		{
			IViewReference solexRef = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findViewReference(SolutionExplorerView.PART_ID);
			SolutionExplorerView solexView = null;
			if (solexRef != null)
			{
				solexView = (SolutionExplorerView)solexRef.getView(false);
				solexView.enablePostgresDBCreation();
				solexView.enableSybaseDBCreation();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logInfo("Server configuration is changes but the solex couldn't be updated: " + e.getMessage());
		}
	}
}
