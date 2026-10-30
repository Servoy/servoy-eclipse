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

package com.servoy.eclipse.ngclient.startup;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author jcompagner
 */
public class Activator implements BundleActivator
{
	private static final String NGCLIENT_DEVELOPER_ID = "com.servoy.eclipse.ngclient.developer";

	private static final Bundle ngclientBundle = Platform.getBundle("servoy_ngclient");
	private static final Bundle saboBundle = Platform.getBundle("sablo");
	static IDesignerCallback designerCallback;
	static NGClient developerNGClient;

	public static Bundle getNClientBundle()
	{
		return ngclientBundle;
	}

	public static Bundle getSaboBundle()
	{
		return saboBundle;
	}

	public static void setDesignerCallback(IDesignerCallback designerCallback)
	{
		Activator.designerCallback = designerCallback;
	}

	public static void setDeveloperNGClient(NGClient developerNGClient)
	{
		Activator.developerNGClient = developerNGClient;
	}


	@Override
	public void start(BundleContext ctx) throws Exception
	{
		if (ApplicationServerRegistry.getServiceRegistry() != null)
		{
			// the single factory for Servoy Developer and for the headless Cypress runner (there the suppliers return null)
			WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT,
				new NGClientWebsocketSessionFactory(() -> designerCallback, () -> developerNGClient));
		}
	}

	//	public NGClient getNGClient(INGClientWebsocketSession session, Map<String, List<String>> requestParams)
	//	{
	//
	//		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(NGCLIENT_DEVELOPER_ID);
	//		try
	//		{
	//			for (IConfigurationElement e : config)
	//			{
	//				System.out.println("Evaluating extension");
	//				final Object o = e.createExecutableExtension("class");
	//				if (o instanceof IDeveloperClientHandler)
	//				{
	//					NGClient ngClient = ((IDeveloperClientHandler)o).getNGClient(session, requestParams);
	//					if (ngClient != null) return ngClient;
	//
	//				}
	//			}
	//		}
	//		catch (CoreException ex)
	//		{
	//			Debug.error(ex);
	//		}
	//		return null;
	//	}

	@Override
	public void stop(BundleContext ctx) throws Exception
	{
	}
}
