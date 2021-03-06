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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;

import com.servoy.eclipse.core.IMainConceptsPageAction;
import com.servoy.eclipse.marketplace.MarketplaceBrowserEditor;
import com.servoy.eclipse.model.util.ServoyLog;

public class OpenMarketplace implements IWorkbenchWindowActionDelegate, IMainConceptsPageAction
{
	protected String deepLink = null;

	public OpenMarketplace()
	{

	}

	public void dispose()
	{
	}

	public void init(IWorkbenchWindow window)
	{
	}

	public void run(IAction action)
	{
		runAction(null);
	}

	public void runAction(IntroURL introUrl)
	{
		try
		{
			IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			MarketplaceBrowserEditor p = (MarketplaceBrowserEditor)activePage.findEditor(MarketplaceBrowserEditor.INPUT);
			if (p == null)
			{ //MarketPlaceEditor not already showing in the Active window, so opening with deeplink in launch URL
				p = (MarketplaceBrowserEditor)activePage.openEditor(MarketplaceBrowserEditor.INPUT, MarketplaceBrowserEditor.MARKETPLACE_BROWSER_EDITOR_ID);
				if (introUrl != null && introUrl.getParameter("a") != null)
				{
					p.deepLink("/filter/" + introUrl.getParameter("a"));
				}
			}
			else if (introUrl != null)
			//MarketPlace already opened, so calling the deeplink through a exposed callback method in the Web Client solution, after which the MarketPlace editor is activated
			{
				p.executeDeepLink(introUrl.getParameter("a"));
				activePage.openEditor(MarketplaceBrowserEditor.INPUT, MarketplaceBrowserEditor.MARKETPLACE_BROWSER_EDITOR_ID);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Failed to open Marketplace browser.", e);
		}
	}

	public void selectionChanged(IAction action, ISelection selection)
	{
	}
}