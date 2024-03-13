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
package com.servoy.eclipse.ui.actions;

import java.net.URL;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;

public class OpenUrlAction implements IWorkbenchWindowActionDelegate
{
	public static String openForum = "com.servoy.eclipse.ui.OpenForumAction";
	public static String openWiki = "com.servoy.eclipse.ui.OpenWikiAction";

	public static String openTutorial = "com.servoy.eclipse.ui.OpenTutorialAction";

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
	}

	public void run(IAction action)
	{
		try
		{
			if (openWiki.equals(action.getActionDefinitionId()))
			{
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://wiki.servoy.com"));
			}
			else if (openTutorial.equals(action.getActionDefinitionId()))
			{
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://docs.servoy.com/guides/get-started"));
			}
			else
			{
				PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL("https://forum.servoy.com"));
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}
}
