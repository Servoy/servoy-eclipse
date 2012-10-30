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
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.ServerConfig;

public class MissingServerQuickFix implements IMarkerResolution
{
	private final String serverName;

	public MissingServerQuickFix(String serverName)
	{
		this.serverName = serverName;
	}

	public String getLabel()
	{
		return "Add/edit server '" + serverName + "'.";
	}

	public void run(IMarker marker)
	{
		try
		{
			ServerConfig serverConfig = ServoyModel.getServerManager().getServerConfig(serverName);
			if (serverConfig == null)
			{
				serverConfig = ServerConfig.TEMPLATES.get(ServerConfig.POSTGRESQL_TEMPLATE_NAME).getNamedCopy(serverName);
			}
			ServerEditorInput serverConfigEditorInput = new ServerEditorInput(serverConfig);
			serverConfigEditorInput.setIsNew(true);

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				serverConfigEditorInput,
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(ServerEditorInput.SERVER_RESOURCE_ID)).getId());
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}
}