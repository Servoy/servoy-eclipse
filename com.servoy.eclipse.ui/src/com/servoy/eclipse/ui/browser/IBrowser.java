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
	 * Registers a callback that can be invoked from JavaScript running in this browser.
	 * The registration is backed by the browser-specific {@code BrowserFunction}
	 * implementation, so callers do not need to know which browser backend is used.
	 *
	 * @param name the name of the JavaScript function to create
	 * @param function the callback invoked when the JavaScript function is called
	 */
	public void addBrowserFunction(String name, IBrowserFunction function);

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
	 * Runs the given JavaScript and returns its result, unlike {@link #execute(String)}
	 * which is fire-and-forget. The script's returned value is converted to a Java
	 * value the same way the underlying SWT/Chromium browser converts it: a JavaScript
	 * string/number/boolean/null becomes {@link String}/{@link Double}/{@link Boolean}/
	 * {@code null}, and a JavaScript array becomes an {@code Object[]} of those.
	 * <p>
	 * Must be called on the SWT display thread. A script that fails may throw the
	 * browser backend's {@code SWTException}; callers wanting a value back should have
	 * the script {@code return} it.
	 * </p>
	 *
	 * @param script the JavaScript to evaluate
	 * @return the script result converted to a Java value, or {@code null}
	 */
	Object evaluate(String script);

	/**
	 * Captures a screenshot of the current rendered page and returns it as PNG image bytes.
	 * <p>
	 * This works on the Chromium backend only; other backends (e.g. the plain SWT browser)
	 * return {@code null}. Because CEF renders offscreen, the capture succeeds even when the
	 * hosting shell is hidden or positioned offscreen.
	 * </p>
	 * <p>
	 * <b>Threading contract:</b> this method MUST be called <em>off</em> the SWT display thread.
	 * This is the opposite of {@link #evaluate(String)}, which must be called <em>on</em> the
	 * display thread. The underlying Chromium capture is asynchronous and this method blocks
	 * until it completes, so calling it on the display thread would deadlock; when that is
	 * detected the call is refused and {@code null} is returned.
	 * </p>
	 *
	 * @return the PNG image bytes of the current rendered page, or {@code null} when the backend
	 *         cannot capture (unsupported backend, wrong thread, timeout, or capture failure)
	 */
	byte[] captureScreenshot();

	/**
	 *
	 */
	void dispose();
}
