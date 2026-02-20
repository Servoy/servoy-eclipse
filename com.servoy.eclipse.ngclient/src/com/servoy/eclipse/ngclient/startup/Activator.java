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

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.sablo.eventthread.Event;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionKey;
import org.sablo.websocket.WebsocketSessionManager;

import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.WebsocketSessionFactory;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
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
			WebsocketSessionManager.setWebsocketSessionFactory(WebsocketSessionFactory.CLIENT_ENDPOINT, new IWebsocketSessionFactory()
			{
				@Override
				public IWebsocketSession createSession(WebsocketSessionKey sessionKey) throws Exception
				{
					NGClientWebsocketSession wsSession = new NGClientWebsocketSession(sessionKey, designerCallback)
					{
						@Override
						public void init(Map<String, List<String>> requestParams) throws Exception
						{
							if (getClient() == null)
							{
//								NGClient ngClient = getNGClient(this, requestParams);
//								if (ngClient != null)
//								{
//									setClient(ngClient);
//								}
//								else 
								if (requestParams.containsKey("nodebug"))
								{
									setClient(new NGClient(this, designerCallback));
								}
								else if (requestParams.containsKey("svy_developer"))
								{
									setClient(developerNGClient);
								}
								else
								{
									if (getWindowTimeout() == Long.valueOf(BaseWebsocketSession.DEFAULT_WINDOW_TIMEOUT))
									{
										// in developer increase the timeout to 15 minutes, else will expire during debugging
										setSessionWindowTimeout(new Long(60 * 15));
									}
									final IDebugClientHandler service = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
									if (service != null)
									{
										NGClient debugNGClient = (NGClient)service.getDebugNGClient();
										if (debugNGClient != null && !debugNGClient.isShutDown() &&
											debugNGClient.getWebsocketSession().getSessionKey().equals(getSessionKey())) setClient(debugNGClient);
										else setClient((NGClient)service.createDebugNGClient(this));
									}
									else
									{
										setClient(new NGClient(this, designerCallback));
									}
								}
							}
						}

						@Override
						protected IEventDispatcher createEventDispatcher()
						{
							// make sure that the command console thread is seen as the dispatch thread
							// so it can executed command, that are api calls to the browser
							return new NGEventDispatcher(getClient())
							{
								@Override
								public boolean isEventDispatchThread()
								{
									return super.isEventDispatchThread() || Thread.currentThread().getName().equals("Debug command reader"); //$NON-NLS-1$
								}

								@Override
								protected Event getCurrentEventIfOnEventThread()
								{
									if (Thread.currentThread().getName().equals("Debug command reader")) return null;
									return super.getCurrentEventIfOnEventThread();
								}
							};
						}
					};
					return wsSession;
				}
			});
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
