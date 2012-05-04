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

package com.servoy.eclipse.marketplace;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.json.JSONArray;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.Utils;

/**
 * Editor used to show the Servoy Marketplace. 
 *  
 * @author gboros
 */
public class MarketplaceBrowserEditor extends EditorPart
{
	public static final String MARKETPLACE_BROWSER_EDITOR_ID = "com.servoy.eclipse.marketplace.MarketplaceBrowserEditor"; //$NON-NLS-1$
	public static final String MARKETPLACE_URL = "http://localhost:8080/servoy-webclient/ss/s/marketplace/m/onOpenFromDeveloper/a/dev"; //$NON-NLS-1$
	public static final String MARKETPLACE_WS = "http://localhost:8080/servoy-service/rest_ws/marketplace/ws_extensions/"; //$NON-NLS-1$
	private static final String PARAM_SERVOY_VERSION = "servoyVersion"; //$NON-NLS-1$ 
	private static final String PARAM_PLATFORM = "platform"; //$NON-NLS-1$
	public static final MarketplaceBrowserEditorInput INPUT = new MarketplaceBrowserEditorInput();

	private static final String WS_ACTION_EXP = "exp"; //$NON-NLS-1$
	private static final String WS_ACTION_PACKAGE_XML = "xml"; //$NON-NLS-1$
	private static final String WS_ACTION_VERSIONS = "versions"; //$NON-NLS-1$

	private Browser browser;
	private String url;

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
		url = new StringBuffer(MARKETPLACE_URL).append("/").append(PARAM_SERVOY_VERSION).append("/").append(ClientVersion.getBundleVersion()).append("/").append(PARAM_PLATFORM).append("/").append(Utils.getPlatformAsString()).toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ 
		browser.setUrl(url, null, new String[] { "Cache-Control: no-cache" }); //$NON-NLS-1$

		browser.addLocationListener(new LocationAdapter()
		{
			@Override
			public void changing(LocationEvent event)
			{
				// if install link
				if (event.location.startsWith(MARKETPLACE_WS))
				{
					event.doit = false;
//					String extensionId = event.location.substring(MARKETPLACE_WS.length());
//					ArrayList<String> versions = ws_getVersions(extensionId);
//					for (String v : versions)
//					{
//						String xml = ws_getPackageXML(extensionId, v);
//						System.out.println(xml);
//
//						BufferedInputStream bis = null;
//						FileOutputStream fos = null;
//						try
//						{
//							fos = new FileOutputStream("/home/gabi/" + extensionId + "_" + v + ".exp");
//							bis = new BufferedInputStream(ws_getEXP(extensionId, v));
//							byte[] buffer = new byte[1024];
//							int len;
//							while ((len = bis.read(buffer)) != -1)
//								fos.write(buffer, 0, len);
//						}
//						catch (Exception ex)
//						{
//							ServoyLog.logError(ex);
//						}
//						finally
//						{
//							Utils.closeInputStream(bis);
//							Utils.closeOutputStream(fos);
//						}
//					}
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

	public void deepLink(String deeplinkParam)
	{
		browser.setUrl(url.concat(deeplinkParam).toString());
	}

	public void executeDeepLink(String deeplinkParam)
	{
		browser.execute("executeMPDeepLink(\"" + deeplinkParam + "\");"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private InputStream ws_getEXP(String extensionId, String version) throws Exception
	{
		return ws_getStream(WS_ACTION_EXP, "application/binary", extensionId, version); //$NON-NLS-1$
	}

	private String ws_getPackageXML(String extensionId, String version)
	{
		String packageXML = null;

		ByteArrayOutputStream bos = null;
		BufferedInputStream bis = null;
		try
		{
			bos = new ByteArrayOutputStream();
			bis = new BufferedInputStream(ws_getStream(WS_ACTION_PACKAGE_XML, "application/binary", extensionId, version)); //$NON-NLS-1$
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bis.read(buffer)) != -1)
				bos.write(buffer, 0, len);

			packageXML = new String(bos.toByteArray(), "UTF-8"); //$NON-NLS-1$
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(bos);
		}


		return packageXML;
	}

	private ArrayList<String> ws_getVersions(String extensionId)
	{
		ArrayList<String> versions = new ArrayList<String>();

		ByteArrayOutputStream bos = null;
		BufferedInputStream bis = null;
		try
		{
			bos = new ByteArrayOutputStream();
			bis = new BufferedInputStream(ws_getStream(WS_ACTION_VERSIONS, "text/json", extensionId, null)); //$NON-NLS-1$
			byte[] buffer = new byte[1024];
			int len;
			while ((len = bis.read(buffer)) != -1)
				bos.write(buffer, 0, len);

			JSONArray jsonVersions = new JSONArray(new String(bos.toByteArray()));
			for (int i = 0; i < jsonVersions.length(); i++)
				versions.add(jsonVersions.getString(i));

		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		finally
		{
			Utils.closeInputStream(bis);
			Utils.closeOutputStream(bos);
		}

		return versions;
	}

	private InputStream ws_getStream(String action, String acceptContentType, String extensionId, String version) throws Exception
	{
		URL mpURL = new URL(MARKETPLACE_WS + action + "/" + extensionId + (version != null ? "/" + version : "")); //$NON-NLS-1$ //$NON-NLS-2$
		URLConnection urlConnection = mpURL.openConnection();

		urlConnection.addRequestProperty("accept", acceptContentType); //$NON-NLS-1$
		return urlConnection.getInputStream();
	}
}
