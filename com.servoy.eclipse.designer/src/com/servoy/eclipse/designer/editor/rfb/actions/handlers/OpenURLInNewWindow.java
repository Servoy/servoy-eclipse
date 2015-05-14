/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.j2db.util.Debug;

/**
 * @author gganea@servoy.com
 *
 */
public class OpenURLInNewWindow implements IServerService
{

	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final IWebBrowser browser = PlatformUI.getWorkbench().getBrowserSupport().createBrowser(SWT.EMBEDDED, "com.servoy.browser.preview.gif",
						"Preview", "");
					URL url = new URL(args.get("url").toString());
					browser.openURL(url);
				}
				catch (PartInitException | MalformedURLException | JSONException e)
				{
					Debug.log(e);
				}
			}
		});

		return null;
	}
}
