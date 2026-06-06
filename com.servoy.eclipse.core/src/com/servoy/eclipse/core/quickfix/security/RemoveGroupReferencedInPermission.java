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
import com.servoy.j2db.server.shared.SecurityInfo;

/**
 * Quick fix for removing permissions/access mask in a form/table sec file for a group that is not declared.
 *
 * @author acostescu
 */
public class RemoveGroupReferencedInPermission extends AlterPermissionSecFileQuickFix
{

	private static RemoveGroupReferencedInPermission instance;

	public static RemoveGroupReferencedInPermission getInstance()
	{
		if (instance == null)
		{
			instance = new RemoveGroupReferencedInPermission();
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
		return "Remove access settings for this invalid group.";
	}

	@Override
	protected boolean alterPermissionInfo(Map<String, List<SecurityInfo>> access)
	{
		return (access.remove(wrongValue) != null);
	}

}
