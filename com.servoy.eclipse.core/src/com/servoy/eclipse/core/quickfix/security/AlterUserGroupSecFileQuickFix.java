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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.core.repository.EclipseUserManager.User;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;

/**
 * Abstract class that helps derived classes to alter the user/groups sec file easier.
 * 
 * @author Andrei Costescu
 */
public abstract class AlterUserGroupSecFileQuickFix extends SecurityQuickFix
{

	@Override
	protected String parseAndAlterSecurityFile(String fileContent) throws JSONException
	{
		List<String> groups = new SortedList<String>(StringComparator.INSTANCE);
		List<User> users = new SortedList<User>();
		Map<String, List<String>> usersForGroups = new HashMap<String, List<String>>();

		// read the file into memory
		EclipseUserManager.deserializeUserAndGroupInfo(fileContent, groups, users, usersForGroups);

		// alter contents
		boolean altered = alterUserAndGroupInfo(groups, users, usersForGroups);

		// write new contents
		if (altered) return EclipseUserManager.serializeUserAndGroupInfo(groups, users, usersForGroups);
		return null;
	}

	/**
	 * Alters the contents given as parameters if needed.
	 * 
	 * @param groups the group list.
	 * @param users the user list.
	 * @param usersForGroups users-to-groups associations.
	 * @return true if any content was changed, false otherwise.
	 */
	protected abstract boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups);

}
