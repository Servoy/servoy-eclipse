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

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Quick fix for correcting an user with invalid name or password.
 *
 * @author acostescu
 */
public class CorrectInvalidUser extends AlterUserGroupSecFileQuickFix
{

	private static CorrectInvalidUser instance;

	public static CorrectInvalidUser getInstance()
	{
		if (instance == null)
		{
			instance = new CorrectInvalidUser();
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
		return "Specify valid name/password for this user.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, final List<User> users, Map<String, List<String>> usersForGroups)
	{
		final User invalidUser;
		boolean altered = false;
		try
		{
			invalidUser = User.fromJSON(new ServoyJSONObject((String)wrongValue, false));
			if (users.contains(invalidUser))
			{
				final User newUser = new User(invalidUser.name, invalidUser.passwordHash, invalidUser.userUid);
				ReturnValueRunnable r;
				if (newUser.name == null || newUser.name.trim().length() == 0)
				{
					r = new GetNewUserNameRunnable(users, "Correct invalid user", newUser.name);
					UIUtils.runInUI(r, true);
					newUser.name = (String)r.getReturnValue();
				}
				if (newUser.name != null)
				{
					if (newUser.passwordHash == null || newUser.passwordHash.length() == 0)
					{
						r = new GetNewPasswordRunnable("Correct invalid user");
						UIUtils.runInUI(r, true);
						newUser.passwordHash = (String)r.getReturnValue();
					}
					if (newUser.passwordHash != null)
					{
						users.remove(invalidUser);
						users.add(newUser);
						altered = true;
					} // else user chose cancel
				} // else user chose cancel
			}
			else
			{
				reportProblem("Cannot find the user that is to be corrected.");
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}
		return altered;
	}
}
