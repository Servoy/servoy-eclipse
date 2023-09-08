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
package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMarkerResolution;
import org.json.JSONObject;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.util.UUID;

public class DuplicateSiblingUuidQuickFix implements IMarkerResolution
{
	private final String relativePath;

	public DuplicateSiblingUuidQuickFix(String relativePath)
	{
		super();
		this.relativePath = relativePath;
	}

	public String getLabel()
	{
		return "Generate new uuid for persist: '" + relativePath + "'.";
	}

	public void run(IMarker marker)
	{
		try
		{
			IFileAccess workspaceFileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			JSONObject json_obj = SolutionDeserializer.getJSONObject(workspaceFileAccess.getUTF8Contents(relativePath));
			json_obj.putOpt(SolutionSerializer.PROP_UUID, UUID.randomUUID());
			workspaceFileAccess.setUTF8Contents(relativePath, json_obj.toString());
			MessageDialog.openInformation(UIUtils.getActiveShell(), "UUID generated sucessfully",
				"Restart application in order for the change to be effective.");
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
}