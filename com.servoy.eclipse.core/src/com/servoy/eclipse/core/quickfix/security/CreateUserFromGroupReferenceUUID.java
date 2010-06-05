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

import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.core.repository.EclipseUserManager.User;
import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;

/**
 * Quick fix for creating an user for an UUID specified in the user to group mappings.
 * 
 * @author Andrei Costescu
 */
public class CreateUserFromGroupReferenceUUID extends AlterUserGroupSecFileQuickFix
{

	private static CreateUserFromGroupReferenceUUID instance;

	public static CreateUserFromGroupReferenceUUID getInstance()
	{
		if (instance == null)
		{
			instance = new CreateUserFromGroupReferenceUUID();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == EclipseUserManager.SecurityReadException.MISSING_USER_REFERENCED_IN_GROUP;
	}

	public String getLabel()
	{
		return "Create an user for that UID.";
	}

	@Override
	protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
	{
		boolean altered = false;
		EclipseUserManager.User newUser = new EclipseUserManager.User(null, null, (String)wrongValue);
		ReturnValueRunnable r = new GetNewUserNameRunnable(users, "Create user for UID", null);
		UIUtils.runInUI(r, true);
		newUser.name = (String)r.getReturnValue();
		if (newUser.name != null)
		{
			r = new GetNewPasswordRunnable("Create user for UID");
			UIUtils.runInUI(r, true);
			newUser.passwordHash = (String)r.getReturnValue();
			if (newUser.passwordHash != null)
			{
				users.add(newUser);
				altered = true;
			} // else user canceled dialog
		} // else user canceled dialog
		return altered;
	}

}
