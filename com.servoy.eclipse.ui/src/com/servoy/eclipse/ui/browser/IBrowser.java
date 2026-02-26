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

import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.GridData;

/**
 * @author jcompagner
 * @since 2021.06
 */
public interface IBrowser
{

	/**
	 * @param width
	 * @param height
	 */
	void setSize(int width, int height);

	/**
	 * @param url
	 */
	boolean setUrl(String url);

	/**
	 * @param url
	 * @param object
	 * @param strings
	 */
	boolean setUrl(String url, String postData, String[] headers);

	/**
	 * @param locationListener
	 */
	void addLocationListener(LocationListener locationListener);

	/**
	 * @param gridData
	 */
	void setLayoutData(GridData gridData);

	/**
	 * @return
	 */
	int getStyle();

	/**
	 *
	 */
	void setFocus();

	boolean isChromium();

	/**
	 * Gets the underlying browser instance.
	 * This is needed for creating BrowserFunction instances.
	 *
	 * @return the underlying browser object
	 */
	public Object getBrowserInstance();

	/**
	 * Sets the HTML content of the browser.
	 * For Chromium on Linux, if the content is large, it uses a temporary file approach
	 * instead of setText to avoid size limitations.
	 *
	 * @param html the HTML content to display
	 * @return true if the operation was successful, false otherwise
	 */
	public boolean setText(String html);

	/**
	 * @return true if the browser is disposed, false otherwise
	 */
	boolean isDisposed();

	/**
	 * @param string
	 */
	void execute(String string);

	/**
	 *
	 */
	void dispose();
}
