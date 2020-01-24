/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import java.awt.Dimension;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;

import com.servoy.eclipse.core.IStartPageAction;
import com.servoy.eclipse.ui.preferences.StartupPreferences;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 * @since 2020.03
 *
 */
public class BrowserDialog extends Dialog
{

	private String url;
	private Browser browser;
	private Shell shell;
	private boolean showSkipNextTime;

	/**
	 * @param parentShell
	 */
	public BrowserDialog(Shell parentShell, String url, boolean modal, boolean showSkipNextTime)
	{
		super(parentShell, modal ? SWT.APPLICATION_MODAL : SWT.MODELESS);
		this.url = url;
		this.showSkipNextTime = showSkipNextTime;
	}


	public Object open()
	{
		Rectangle size = getParent().getBounds();
		Dimension newSize = new Dimension((int)(size.width / 1.5), (int)(size.height / 1.5));

		int locationX, locationY;
		locationX = (size.width - (int)(size.width / 1.5)) / 2 + size.x;
		locationY = (size.height - (int)(size.height / 1.5)) / 2 + size.y;

		return this.open(new Point(locationX, locationY), newSize);
	}

	public Object open(Point location, Dimension size)
	{
		Shell parent = getParent();
		shell = new Shell(parent, SWT.DIALOG_TRIM | getStyle());

		if (!ApplicationServerRegistry.get().hasDeveloperLicense())
		{
			this.showSkipNextTime = false;
		}

		if (showSkipNextTime)
		{
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			shell.setLayout(gridLayout);
		}
		else
		{
			shell.setLayout(new FillLayout());
		}
		//load html file in textReader
		Browser browser = new Browser(shell, SWT.NONE);
		browser.addLocationListener(new LocationListener()
		{
			@Override
			public void changing(LocationEvent event)
			{
				String loc = event.location;
				if (loc == null) return;
				if (loc.equals(url)) return;

				IntroURLParser parser = new IntroURLParser(loc);
				if (parser.hasIntroUrl())
				{
					// stop URL first.
					event.doit = false;
					// execute the action embedded in the IntroURL
					final IntroURL introURL = parser.getIntroURL();
					if (IntroURL.RUN_ACTION.equals(introURL.getAction()))
					{
						String pluginId = introURL.getParameter(IntroURL.KEY_PLUGIN_ID);
						String className = introURL.getParameter(IntroURL.KEY_CLASS);

						final Object actionObject = ModelLoaderUtil.createClassInstance(pluginId, className);

						if (actionObject instanceof IStartPageAction)
						{
							Display display = Display.getCurrent();
							BusyIndicator.showWhile(display, new Runnable()
							{
								public void run()
								{
									((IStartPageAction)actionObject).runAction(introURL);
								}
							});
							return;
						}
					}
					introURL.execute();
				}
			}

			@Override
			public void changed(LocationEvent event)
			{
			}
		});
		browser.setUrl(url);
		browser.setSize(size.width, size.height);

		if (showSkipNextTime)
		{
			Button showNextTime = new Button(shell, SWT.CHECK);
			showNextTime.setText("Do not show this dialog anymore");
			showNextTime.setSelection(!Utils.getAsBoolean(Settings.getInstance().getProperty(StartupPreferences.STARTUP_SHOW_START_PAGE, "true")));
			showNextTime.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					Settings.getInstance().setProperty(StartupPreferences.STARTUP_SHOW_START_PAGE, new Boolean(!showNextTime.getSelection()).toString());
				}
			});
		}
		shell.setLocation(location);
		shell.pack();
		shell.open();
		if (getStyle() == SWT.APPLICATION_MODAL)
		{
			Display display = parent.getDisplay();
			while (!shell.isDisposed())
			{
				if (!display.readAndDispatch()) display.sleep();
			}
		}
		return null;
	}

	public boolean isDisposed()
	{
		return shell == null || shell.isDisposed();
	}


	/**
	 * @param optString
	 */
	public void setUrl(String url)
	{
		this.url = url;
		browser.setUrl(url);
	}
}
