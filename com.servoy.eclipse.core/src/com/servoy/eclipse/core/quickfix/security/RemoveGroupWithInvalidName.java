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
package com.servoy.eclipse.core.quickfix.security;

import java.util.List;
import java.util.Map;

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;


/**
 * Quick fix for removing an invalid group.
 * 
 * @author acostescu
 */
public class RemoveGroupWithInvalidName extends AlterUserGroupSecFileQuickFix
{

	private static RemoveGroupWithInvalidName instance;

	public static RemoveGroupWithInvalidName getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveGroupWithInvalidName();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.INVALID_GROUP_NAME;
	}

	public String getLabel()
	{
		return "Remove group.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
	{
		boolean altered = false;
		if (groups.remove(wrongValue))
		{
			altered = true;
		}

		if (usersForGroups.remove(wrongValue) != null)
		{
			altered = true;
		}
		return altered;
	}

}
