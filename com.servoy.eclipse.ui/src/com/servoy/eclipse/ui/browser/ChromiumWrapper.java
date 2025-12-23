/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.ui.browser;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * @author jcompagner
 * @since 2021.06
 */
public class ChromiumWrapper implements IBrowser
{
	private final Browser browser;

	/**
	 * @param parent
	 */
	public ChromiumWrapper(Composite parent)
	{
		this.browser = new Browser(parent, SWT.NONE);
//		this.browser.setData("AUTOSCALE_DISABLED", Boolean.TRUE); // HACK because chromium does not handle DPI zoom well
	}

	@Override
	public boolean isChromium()
	{
		return true;
	}

	@Override
	public void setSize(int width, int height)
	{
		this.browser.setSize(width, height);
	}

	@Override
	public void setUrl(String url)
	{
		this.browser.setUrl(url);
	}

	@Override
	public void setUrl(String url, String postData, String[] headers)
	{
		this.browser.setUrl(url, postData, headers);
	}

	@Override
	public void setFocus()
	{
		this.browser.setFocus();
	}

	@Override
	public void addLocationListener(LocationListener locationListener)
	{
		this.browser.addLocationListener(locationListener);
	}

	@Override
	public void setLayoutData(GridData gridData)
	{
		this.browser.setLayoutData(gridData);
	}

	@Override
	public int getStyle()
	{
		return this.browser.getStyle();
	}
}
