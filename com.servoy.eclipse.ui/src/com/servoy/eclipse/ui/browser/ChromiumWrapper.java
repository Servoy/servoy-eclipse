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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.equo.chromium.ChromiumBrowser;
import com.equo.chromium.swt.Browser;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author jcompagner
 * @since 2021.06
 */
public class ChromiumWrapper implements IBrowser
{
	private final Browser browser;
	private Path tempHtmlFile;

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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.browser.IBrowser#setText(java.lang.String)
	 */
	@Override
	public boolean setText(String html)
	{
		// Chromium has size limitations with setText for large content
		// If content is larger than 500KB, use a file-based approach
		if (html.length() > 500_000)
		{
			return setTextViaFile(html);
		}
		else
		{
			return browser.setText(html);
		}
	}

	/**
	 * Sets HTML content via a temporary file for large content on Chromium.
	 * This avoids the size limitations of setText().
	 *
	 * @param html the HTML content to display
	 * @return true if successful, false otherwise
	 */
	private boolean setTextViaFile(String html)
	{
		try
		{
			// Clean up previous temp file if it exists
			if (tempHtmlFile != null && Files.exists(tempHtmlFile))
			{
				try
				{
					Files.delete(tempHtmlFile);
				}
				catch (IOException e)
				{
					// Ignore cleanup errors
				}
			}

			// Create a new temporary file
			tempHtmlFile = Files.createTempFile("servoy-ai-chat-", ".html");
			Files.write(tempHtmlFile, html.getBytes(StandardCharsets.UTF_8));

			// Load the file via URL
			String fileUrl = tempHtmlFile.toUri().toString();
			return setUrl(fileUrl);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void setSize(int width, int height)
	{
		this.browser.setSize(width, height);
	}

	@Override
	public boolean setUrl(String url)
	{
		return this.browser.setUrl(url);
	}

	@Override
	public boolean setUrl(String url, String postData, String[] headers)
	{
		return this.browser.setUrl(url, postData, headers);
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

	@Override
	public Object getBrowserInstance()
	{
		return this.browser;
	}

	@Override
	public void addBrowserFunction(String name, IBrowserFunction function)
	{
		new com.equo.chromium.swt.BrowserFunction(this.browser, name)
		{
			@Override
			public Object function(Object[] arguments)
			{
				return function.function(arguments);
			}
		};
	}

	@Override
	public boolean isDisposed()
	{
		return this.browser.isDisposed();
	}

	@Override
	public void dispose()
	{
		this.browser.dispose();
	}

	@Override
	public void execute(String string)
	{
		this.browser.execute(string);
	}

	@Override
	public Object evaluate(String script)
	{
		return this.browser.evaluate(script);
	}

	@Override
	public byte[] captureScreenshot()
	{
		// The Equo Chromium capture is asynchronous and this method blocks on the result, so it must NOT
		// run on the SWT display thread (that would deadlock, and Equo explicitly forbids the main thread).
		if (Display.getCurrent() != null)
		{
			ServoyLog.logWarning("IBrowser.captureScreenshot() was called on the SWT display thread; " +
				"it must be called off the UI thread. Returning null.", null);
			return null;
		}
		if (this.browser.isDisposed())
		{
			return null;
		}
		try
		{
			ChromiumBrowser chromiumBrowser = (ChromiumBrowser)this.browser.getWebBrowser();
			if (chromiumBrowser == null)
			{
				return null;
			}
			CompletableFuture<byte[]> future = chromiumBrowser.captureScreenshot();
			return decodeScreenshotResult(future.get(30, TimeUnit.SECONDS));
		}
		catch (TimeoutException e)
		{
			ServoyLog.logWarning("Timed out waiting for the Chromium screenshot capture.", e);
			return null;
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
			return null;
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not capture a screenshot from the Chromium browser.", e);
			return null;
		}
	}

	/**
	 * Normalizes the raw result of {@link ChromiumBrowser#captureScreenshot()} into raw PNG bytes.
	 * <p>
	 * Equo returns the PNG image as Base64-encoded text (the DevTools {@code Page.captureScreenshot}
	 * "data" string) exposed as the UTF-8 bytes of that Base64 text, so those bytes are decoded back to
	 * the raw PNG bytes. A {@code null} or empty result becomes {@code null}. If the bytes are not valid
	 * Base64 (e.g. a future Equo change that already yields raw PNG bytes) they are returned unchanged as
	 * a defensive fallback.
	 * </p>
	 * <p>
	 * Package-private and static so the pure decode logic can be unit-tested without a live browser.
	 * </p>
	 *
	 * @param result the raw bytes produced by the Equo capture future, may be {@code null}
	 * @return the raw PNG bytes, or {@code null} when there is nothing to decode
	 */
	static byte[] decodeScreenshotResult(byte[] result)
	{
		if (result == null || result.length == 0)
		{
			return null;
		}
		try
		{
			return Base64.getDecoder().decode(result);
		}
		catch (IllegalArgumentException notBase64)
		{
			// Should not happen with the current Equo contract, but if the bytes are already raw PNG
			// (or otherwise not valid Base64) fall back to returning them unchanged.
			return result;
		}
	}
}
