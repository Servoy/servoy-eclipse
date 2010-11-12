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

import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.widgets.Composite;

/**
 * Shows the Servoy Marketplace
 * 
 * @author gboros
 */
public class ServoyMarketplaceView extends ServoyWebBrowserView
{
	public static final String MARKETPLACE_VIEW_ID = "com.servoy.eclipse.ui.views.ServoyMarketplaceView"; //$NON-NLS-1$
	private static final String MARKETPLACE_URL = "http://www.servoy.com/marketplace"; //$NON-NLS-1$

	@Override
	public void createPartControl(Composite parent)
	{
		super.createPartControl(parent);
		viewer.getBrowser().addLocationListener(new LocationListener()
		{
			public void changing(LocationEvent event)
			{
				// TODO implement install link click
				if (false) // if install link
				{
					// get the link and start install 
					event.doit = false;
				}
			}

			public void changed(LocationEvent event)
			{
			}

		});
//		viewer.getBrowser().addOpenWindowListener(new OpenWindowListener()
//		{
//
//			public void open(WindowEvent event)
//			{
//			}
//
//		});
	}

	@Override
	protected String getViewID()
	{
		return MARKETPLACE_VIEW_ID;
	}

	@Override
	protected String getViewURL()
	{
		return MARKETPLACE_URL;
	}
}
