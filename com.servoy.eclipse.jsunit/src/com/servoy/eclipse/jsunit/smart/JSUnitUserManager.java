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

package com.servoy.eclipse.jsunit.smart;

import java.util.Arrays;
import java.util.List;

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.j2db.dataprocessing.IDataServerInternal;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IClientInternal;
import com.servoy.j2db.server.shared.IClientManagerInternal;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Settings;

/**
 * A special workspace user manager for jsunit testing, this one does copy over all the existing users once,
 * But then will not touch the workspace data (update/create or delete users)
 * @author jcompagner
 * @since 6.1
 */
public final class JSUnitUserManager extends WorkspaceUserManager
{

	private final WorkspaceUserManager workspaceUM;

	public JSUnitUserManager(WorkspaceUserManager workspaceUM)
	{
		this.workspaceUM = workspaceUM;
	}

	public void reloadFromWorkspace()
	{
		copyDataFrom(workspaceUM);
	}

	/** Check if user is administrator.
	 * <p> Some operations can only be done by admin user or own user (like change own password)
	 * @param clientId
	 * @param ownerUserId allowed when non-null
	 * @throws RepositoryException
	 */
	@Override
	public void checkForAdminUser(String clientId, String ownerUserId) throws RepositoryException
	{
		if (ApplicationServerRegistry.get().getClientId().equals(clientId))
		{
			// internal: ok
			return;
		}

		// check if user is in admin group
		IClientManagerInternal clientManager = ((IDataServerInternal)ApplicationServerRegistry.get().getDataServer()).getClientManager();
		IClientInternal client = clientManager.getClient(clientId);
		if (client == null || client.getClientInfo().getUserGroups() == null ||
			!Arrays.asList(client.getClientInfo().getUserGroups()).contains(IRepository.ADMIN_GROUP))
		{
			// non-admin user, check for own user
			if (ownerUserId == null || !ownerUserId.equals(client.getClientInfo().getUserUid()))
			{
				Debug.error("Access to repository server denied to client code, see admin property " + Settings.ALLOW_CLIENT_REPOSITORY_ACCESS_SETTING,
					new IllegalAccessException());
				throw new RepositoryException(ServoyException.NO_ACCESS, new Object[] { IRepository.ADMIN_GROUP });
			}
		}

		// ok
	}

	@Override
	protected void writeUserAndGroupInfo(boolean later) throws RepositoryException
	{
	}

	@Override
	public boolean isOperational()
	{
		return true;
	}

	@Override
	protected void deleteGroupReferences(List<String> groupNames)
	{
	}

	@Override
	public void writeSecurityInfo(String serverName, String tableName, boolean later) throws RepositoryException
	{
	}

	@Override
	protected void writeAllTableInfo() throws RepositoryException
	{
	}

}