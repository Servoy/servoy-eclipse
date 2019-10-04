/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.chromium.Browser;
import org.eclipse.swt.chromium.BrowserFunction;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author jcompagner
 *
 */
public class ChromiumVisualFormEditorDesignPage extends RfbVisualFormEditorDesignPage
{
	private Browser browser;

	/**
	 * @param editorPart
	 */
	public ChromiumVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	protected void createBrowser(Composite parent)
	{
		try
		{
			browser = new Browser(parent, SWT.NONE);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
			return;
		}

		try
		{
			// install fake WebSocket in case browser does not support it
//			SwtWebsocket.installFakeWebSocket(browser, editorKey.getClientnr(), clientKey.getClientnr());
			// install console
			new BrowserFunction(browser, "consoleLog")
			{
				@Override
				public Object function(Object[] arguments)
				{
					if (arguments.length > 1)
					{
						if ("log".equals(arguments[0]))
						{
							ServoyLog.logInfo(arguments[1] != null ? arguments[1].toString() : null);
						}
						else if ("error".equals(arguments[0]))
						{
							ServoyLog.logError(arguments[1] != null ? arguments[1].toString() : null, null);
						}
						else if ("onerror".equals(arguments[0]))
						{
							ServoyLog.logError(Arrays.toString(arguments), null);
						}
					}
					return null;
				}
			};
		}
		catch (Exception e)
		{
			ServoyLog.logError("couldn't load the editor: ", e);
		}
	}

	@Override
	protected void showUrl(String url)
	{
		if (!browser.isDisposed()) browser.setUrl(url); // + "&replacewebsocket=true");
	}

	@Override
	public void setFocus()
	{
		browser.setFocus();
	}

	public void refresh()
	{
		super.refreshBrowserUrl(true);
	}
}
