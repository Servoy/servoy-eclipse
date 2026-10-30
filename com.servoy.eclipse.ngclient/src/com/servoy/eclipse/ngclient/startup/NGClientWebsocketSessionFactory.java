/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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
import java.util.function.Supplier;

import org.sablo.eventthread.Event;
import org.sablo.eventthread.IEventDispatcher;
import org.sablo.websocket.BaseWebsocketSession;
import org.sablo.websocket.IWebsocketSession;
import org.sablo.websocket.IWebsocketSessionFactory;
import org.sablo.websocket.WebsocketSessionKey;

import com.servoy.j2db.IDebugClientHandler;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.eventthread.NGEventDispatcher;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;

/**
 * The single {@link IWebsocketSessionFactory} registered for the NG client endpoint by {@link Activator}. In Servoy
 * Developer the suppliers return the real designer callback and developer NG client; in the headless Cypress runner
 * those are never set, so both are null. All request-parameter branches (formpreview, nodebug, svy_developer, debug
 * client) live here.
 * <p>
 * The suppliers are evaluated lazily at session creation / init time, because the Developer values are set
 * after the bundle activator has started.
 *
 * @since 2026.6
 */
public class NGClientWebsocketSessionFactory implements IWebsocketSessionFactory
{
	public enum ClientKind
	{
		FORM_PREVIEW, NO_DEBUG, DEVELOPER, DEBUG, PLAIN
	}

	private final Supplier<IDesignerCallback> designerCallback;
	private final Supplier<NGClient> developerNGClient;

	public NGClientWebsocketSessionFactory(Supplier<IDesignerCallback> designerCallback, Supplier<NGClient> developerNGClient)
	{
		this.designerCallback = designerCallback;
		this.developerNGClient = developerNGClient;
	}

	/**
	 * Factory for headless use: no designer callback and no developer NG client.
	 */
	public static NGClientWebsocketSessionFactory forHeadless()
	{
		return new NGClientWebsocketSessionFactory(() -> null, () -> null);
	}

	@Override
	public IWebsocketSession createSession(WebsocketSessionKey sessionKey) throws Exception
	{
		return new FactorySession(sessionKey, designerCallback.get(), developerNGClient);
	}

	public static ClientKind selectClientKind(Map<String, List<String>> params, boolean hasDeveloperNGClient, boolean hasDebugHandler)
	{
		if (params.containsKey("formpreview")) return ClientKind.FORM_PREVIEW;
		if (params.containsKey("nodebug")) return ClientKind.NO_DEBUG;
		if (params.containsKey("svy_developer") && hasDeveloperNGClient) return ClientKind.DEVELOPER;
		if (params.containsKey("svy_developer")) return ClientKind.PLAIN;
		return hasDebugHandler ? ClientKind.DEBUG : ClientKind.PLAIN;
	}

	public static boolean shouldReuseFormPreviewClient(boolean exists, boolean shutDown, boolean sameSessionKey)
	{
		return exists && !shutDown && sameSessionKey;
	}

	static class FactorySession extends NGClientWebsocketSession
	{
		private final IDesignerCallback designerCallback;
		private final Supplier<NGClient> developerNGClient;

		FactorySession(WebsocketSessionKey sessionKey, IDesignerCallback designerCallback, Supplier<NGClient> developerNGClient)
		{
			super(sessionKey, designerCallback);
			this.designerCallback = designerCallback;
			this.developerNGClient = developerNGClient;
		}

		@Override
		public void init(Map<String, List<String>> requestParams) throws Exception
		{
			if (getClient() != null) return;

			NGClient developerClient = developerNGClient.get();
			IDebugClientHandler debugHandler = null;
			if (ApplicationServerRegistry.getServiceRegistry() != null)
			{
				debugHandler = ApplicationServerRegistry.getServiceRegistry().getService(IDebugClientHandler.class);
			}

			switch (selectClientKind(requestParams, developerClient != null, debugHandler != null))
			{
				case FORM_PREVIEW :
					initFormPreview(requestParams.get("formpreview").get(0));
					break;
				case NO_DEBUG :
					setClient(new NGClient(this, designerCallback));
					break;
				case DEVELOPER :
					setClient(developerClient);
					break;
				case DEBUG :
					if (getWindowTimeout() == Long.valueOf(BaseWebsocketSession.DEFAULT_WINDOW_TIMEOUT))
					{
						// in developer increase the timeout to 15 minutes, else will expire during debugging
						setSessionWindowTimeout(Long.valueOf(60 * 15));
					}
					NGClient debugNGClient = (NGClient)debugHandler.getDebugNGClient();
					if (debugNGClient != null && !debugNGClient.isShutDown() &&
						debugNGClient.getWebsocketSession().getSessionKey().equals(getSessionKey())) setClient(debugNGClient);
					else setClient((NGClient)debugHandler.createDebugNGClient(this));
					break;
				default :
					if (requestParams.containsKey("svy_developer"))
					{
						Debug.warn("svy_developer requested but no developer NG client is available; using a plain NGClient");
					}
					setClient(new NGClient(this, designerCallback));
					break;
			}
		}

		/**
		 * Cypress/browser form tests open ?formpreview=&lt;formName&gt;: a real NG client showing that form with data,
		 * authentication bypassed by FormPreviewNGClient.showDefaultLogin(). Per-spec isolation (headless) relies on
		 * FormPreviewNGClient.shutdownExisting() having cleared the instance, so the reuse path only triggers when the
		 * same websocket session re-sends init while its client is alive.
		 */
		private void initFormPreview(String formName) throws Exception
		{
			FormPreviewNGClient existing = FormPreviewNGClient.getInstance();
			boolean reuse = existing != null &&
				shouldReuseFormPreviewClient(true, existing.isShutDown(), existing.getWebsocketSession().getSessionKey().equals(getSessionKey()));
			if (reuse)
			{
				// retarget the existing preview client on the same session instead of recreating it
				existing.retarget(formName);
				setClient(existing);
			}
			else
			{
				FormPreviewNGClient.setPendingTargetFormName(formName);
				setClient(new FormPreviewNGClient(this, designerCallback, formName));
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
	}
}
