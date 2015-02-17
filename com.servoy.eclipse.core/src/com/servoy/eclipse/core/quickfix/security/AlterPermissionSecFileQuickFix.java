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

import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.j2db.server.shared.SecurityInfo;

/**
 * Abstract class that helps derived classes to alter the table/form sec. files.
 *
 * @author acostescu
 */
public abstract class AlterPermissionSecFileQuickFix extends SecurityQuickFix
{

	@Override
	protected String parseAndAlterSecurityFile(String fileContent) throws JSONException
	{
		Map<String, List<SecurityInfo>> access = new HashMap<String, List<SecurityInfo>>();

		// read the file into memory
		WorkspaceUserManager.deserializeSecurityPermissionInfo(fileContent, access);

		// alter contents
		boolean altered = alterPermissionInfo(access);

		// write new contents
		if (altered) return WorkspaceUserManager.serializeSecurityPermissionInfo(access);
		return null;
	}

	protected abstract boolean alterPermissionInfo(Map<String, List<SecurityInfo>> access);

}
