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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURL;
import org.eclipse.ui.internal.intro.impl.model.url.IntroURLParser;
import org.eclipse.ui.part.EditorPart;

/**
 * Editor used to show the Servoy Marketplace. 
 *  
 * @author pbakker
 */
public class StartPageBrowserEditor extends EditorPart
{
	public static final String STARTPAGE_BROWSER_EDITOR_ID = "com.servoy.eclipse.core.StartPageBrowserEditor"; //$NON-NLS-1$
	public static final String STARTPAGE_URL = "http://servoy.com/i"; //$NON-NLS-1$
	public static final StartPageBrowserEditorInput INPUT = new StartPageBrowserEditorInput();

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
		browser.setUrl(STARTPAGE_URL);
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
					IntroURL introURL = parser.getIntroURL();
					introURL.execute();
					return;
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
