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

package com.servoy.eclipse.core;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.part.EditorPart;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.util.Settings;

/**
 * Editor used to show the Servoy Main Concepts.
 *
 * @author pbakker
 */
public class MainConceptsPageBrowserEditor extends EditorPart
{
	public static final String MAINCONCEPTSPAGE_BROWSER_EDITOR_ID = "com.servoy.eclipse.core.MainConceptsPageBrowserEditor";
	public static final String MAINCONCEPTSPAGE_URL = "https://servoy.github.io/servoy_documentation/202003/mainconcepts.html";

	public static final MainConceptsPageBrowserEditorInput INPUT = new MainConceptsPageBrowserEditorInput();

	private Browser browser;

	/*
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor)
	{
		// ignore
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs()
	{
		// ignore
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		setSite(site);
		setInput(input);
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	@Override
	public boolean isDirty()
	{
		return false;
	}

	/*
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed()
	{
		return false;
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		browser = new Browser(parent, SWT.NONE);
		String url = MAINCONCEPTSPAGE_URL + "?dl=true";
		String showOnStartup = Settings.getInstance().getProperty("servoy.developer.showStartPage");
		if (showOnStartup == null || showOnStartup.equals("true"))
		{
			url += "&show=true";
		}
		else
		{
			url += "&show=false";
		}
		browser.setUrl(url, null, new String[] { "Cache-Control: no-cache" });
		browser.addLocationListener(new LocationAdapter()
		{
			@Override
			public void changing(LocationEvent event)
			{
				String url = event.location;
				if (url == null) return;

				IntroURLParser parser = new IntroURLParser(url);
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

						if (actionObject instanceof IMainConceptsPageAction)
						{
							Display display = Display.getCurrent();
							BusyIndicator.showWhile(display, new Runnable()
							{
								public void run()
								{
									((IMainConceptsPageAction)actionObject).runAction(introURL);
								}
							});
							return;
						}
					}
					introURL.execute();
				}
				else if (!url.startsWith(MAINCONCEPTSPAGE_URL) && url.toLowerCase().contains("servoy"))
				{
					event.doit = false;
					try
					{
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(url));
					}
					catch (PartInitException | MalformedURLException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		});
	}

	/*
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus()
	{
		if (browser != null) browser.setFocus();
	}
}
