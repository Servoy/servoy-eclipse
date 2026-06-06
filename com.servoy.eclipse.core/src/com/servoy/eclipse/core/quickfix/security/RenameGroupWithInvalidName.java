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

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;

/**
 * Quick fix for renaming a group declared with an invalid name.
 * 
 * @author acostescu
 */
public class RenameGroupWithInvalidName extends AlterUserGroupSecFileQuickFix
{

	private static RenameGroupWithInvalidName instance;

	public static RenameGroupWithInvalidName getInstance()
	{
		if (instance == null)
		{
			instance = new RenameGroupWithInvalidName();
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
		return "Rename group.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
	{
		boolean altered = false;
		ReturnValueRunnable r = new GetNewGroupNameRunnable(groups, "Rename group with invalid name", (String)wrongValue);
		UIUtils.runInUI(r, true);
		String newGroupName = (String)r.getReturnValue();
		if (newGroupName != null)
		{
			groups.remove(wrongValue);
			groups.add(newGroupName);
			List<String> userUidsForGroup = usersForGroups.remove(wrongValue);
			if (userUidsForGroup != null) usersForGroups.put(newGroupName, userUidsForGroup);
			altered = true;
		}
		return altered;
	}

}
