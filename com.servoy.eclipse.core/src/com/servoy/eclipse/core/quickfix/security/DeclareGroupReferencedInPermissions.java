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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.repository.WorkspaceUserManager.User;
import com.servoy.j2db.server.shared.SecurityInfo;

/**
 * Quick fix for declaring a missing group that has associated access mask/permissions in a form/table sec. file.
 *
 * @author acostescu
 */
public class DeclareGroupReferencedInPermissions extends AlterPermissionSecFileQuickFix
{

	private static DeclareGroupReferencedInPermissions instance;
	protected boolean permissionsFileAltered = false;

	public static DeclareGroupReferencedInPermissions getInstance()
	{
		if (instance == null)
		{
			instance = new DeclareGroupReferencedInPermissions();
		}
		return instance;
	}

	@Override
	protected boolean canHandleType(int type)
	{
		return type == WorkspaceUserManager.SecurityReadException.GROUP_NOT_DECLARED;
	}

	public String getLabel()
	{
		return "Create missing group.";
	}

	@Override
	protected boolean alterPermissionInfo(final Map<String, List<SecurityInfo>> access)
	{
		permissionsFileAltered = false;
		final String groupName = (String)wrongValue;

		// use an AlterUserGroupSecFileQuickFix instance to modify users/groups file easier
		AlterUserGroupSecFileQuickFix alterUsersAndGroups = new AlterUserGroupSecFileQuickFix()
		{

			@Override
			protected boolean alterUserAndGroupInfo(List<String> groups, List<User> users, Map<String, List<String>> usersForGroups)
			{
				boolean altered = false;
				String newGroupName = groupName;
				if (groupName == null || groupName.trim().length() == 0)
				{
					// group name is not only not declared, but also invalid - ask for new group name
					ReturnValueRunnable r = new GetNewGroupNameRunnable(groups, "Declare new group (needs valid group name)", groupName);
					UIUtils.runInUI(r, true);
					newGroupName = (String)r.getReturnValue();
					if (newGroupName != null)
					{
						// replace the invalid name with new one in form/table sec file
						List<SecurityInfo> permissions = access.remove(groupName);
						access.put(newGroupName, permissions);
						permissionsFileAltered = true;
					}
				}
				if (newGroupName != null)
				{
					groups.add(newGroupName);
					altered = true;
				}
				return altered;
			}

			// methods below are not needed
			@Override
			protected boolean canHandleType(int type)
			{
				return true;
			}

			public String getLabel()
			{
				return "";
			}
		};
		IPath path = new Path(WorkspaceUserManager.SECURITY_FILE_RELATIVE_TO_PROJECT);
		IFile usersGroupsFile = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject().getFile(path);
		alterUsersAndGroups.run(WorkspaceUserManager.SecurityReadException.UNKNOWN, null, usersGroupsFile); // type and wrong value are not relevant

		return permissionsFileAltered;
	}
}
