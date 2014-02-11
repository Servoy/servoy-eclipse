/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.actions;

import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class OpenAdminPage implements IWorkbenchWindowActionDelegate
{

	public void run(IAction action)
	{
		try
		{
			PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(
				new URL("http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/servoy-admin"));
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
	}

}
