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

import org.json.JSONException;

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Quick fix for removing an invalid user.
 *
 * @author acostescu
 */
public class RemoveInvalidUser extends AlterUserGroupSecFileQuickFix
{

	private static RemoveInvalidUser instance;

	public static RemoveInvalidUser getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveInvalidUser();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.INVALID_USER_NAME_OR_PASSWORD;
	}

	public String getLabel()
	{
		return "Remove invalid user.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
	{
		WorkspaceUserManager.User invalidUser;
		boolean removed = false;
		try
		{
			invalidUser = WorkspaceUserManager.User.fromJSON(new ServoyJSONObject((String)wrongValue, false));
			removed = users.remove(invalidUser);
			if (removed)
			{
				for (List<String> uidList : usersForGroups.values())
				{
					uidList.remove(invalidUser.userUid);
				}
			}
			else
			{
				reportProblem("Cannot find the user that should be removed.");
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}
		return removed;
	}

}
