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

package com.servoy.eclipse.ui.views;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.browser.BrowserViewer;
import org.eclipse.ui.internal.browser.WebBrowserView;

/**
 * Shows a page from the Servoy web site. The intention is to provide the Servoy developer with latest info related to Servoy.
 * 
 * @author gerzse
 */
public class ServoyWebBrowserView extends WebBrowserView
{
	public static final String ID = "com.servoy.eclipse.ui.browser.view"; //$NON-NLS-1$
	private static final String SERVOY_URL = "http://www.servoy.com/i";

	@Override
	public void init(IViewSite site) throws PartInitException
	{
		super.init(site);
		getSite().getWorkbenchWindow().getPartService().addPartListener(new IPartListener()
		{
			public void partActivated(IWorkbenchPart part)
			{
			}

			public void partBroughtToTop(IWorkbenchPart part)
			{
			}

			public void partClosed(IWorkbenchPart part)
			{
			}

			public void partDeactivated(IWorkbenchPart part)
			{
			}

			public void partOpened(IWorkbenchPart part)
			{
				setURL(SERVOY_URL);
			}
		});
	}

	// Not a nice solution, but we need to get rid of the navigation bar
	// and this is a handy way to clear the style. Another way would be 
	// through secondary-id, but then the view needs to allow multiple instances.
	@Override
	public void createPartControl(Composite parent)
	{
		viewer = new BrowserViewer(parent, 0);
		viewer.setContainer(this);
		initDragAndDrop();
	}

}
