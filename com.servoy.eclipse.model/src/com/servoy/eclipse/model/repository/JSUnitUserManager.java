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

package com.servoy.eclipse.model.repository;

import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * A special workspaceuser manager for jsunit testing, this one does copy over all the existing users once,
 * But then will not touch the workspace data (update/create or delete users)
 * @author jcompagner
 * @since 6.1
 */
public final class JSUnitUserManager extends WorkspaceUserManager
{
	public JSUnitUserManager()
	{
		WorkspaceUserManager userManager = (WorkspaceUserManager)ApplicationServerSingleton.get().getUserManager();
		this.allDefinedUsers.addAll(userManager.allDefinedUsers);
		this.groupInfos.addAll(userManager.groupInfos);
		this.userGroups.putAll(userManager.userGroups);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.model.repository.WorkspaceUserManager#writeUserAndGroupInfo(boolean)
	 */
	@Override
	protected void writeUserAndGroupInfo(boolean later) throws RepositoryException
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.model.repository.WorkspaceUserManager#isOperational()
	 */
	@Override
	public boolean isOperational()
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.model.repository.WorkspaceUserManager#deleteGroupReferences(java.lang.String)
	 */
	@Override
	protected void deleteGroupReferences(String groupName)
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.model.repository.WorkspaceUserManager#writeSecurityInfo(java.lang.String, java.lang.String, boolean)
	 */
	@Override
	protected void writeSecurityInfo(String serverName, String tableName, boolean later) throws RepositoryException
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.model.repository.WorkspaceUserManager#writeAllTableInfo()
	 */
	@Override
	protected void writeAllTableInfo() throws RepositoryException
	{
	}
}