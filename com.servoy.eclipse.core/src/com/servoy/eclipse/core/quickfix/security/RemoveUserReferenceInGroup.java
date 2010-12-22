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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;

/**
 * Quick fix for removing an UUID reference from the user-group mapping table to an user that is not declared.
 * 
 * @author acostescu
 */
public class RemoveUserReferenceInGroup extends AlterUserGroupSecFileQuickFix
{

	private static RemoveUserReferenceInGroup instance;

	public static RemoveUserReferenceInGroup getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveUserReferenceInGroup();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.MISSING_USER_REFERENCED_IN_GROUP;
	}

	public String getLabel()
	{
		return "Remove the missing user UID from all group user lists.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
	{
		boolean altered = false;
		for (List<String> userUIDs : usersForGroups.values())
		{
			altered = altered || userUIDs.remove(wrongValue);
		}
		Iterator<String> it = usersForGroups.keySet().iterator();
		while (it.hasNext())
		{
			List<String> tmp = usersForGroups.get(it.next());
			if (tmp == null || tmp.size() == 0)
			{
				it.remove();
			}
		}
		return altered;
	}

}
