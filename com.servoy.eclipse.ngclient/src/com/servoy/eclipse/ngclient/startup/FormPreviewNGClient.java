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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.sablo.eventthread.IEventDispatcher;

import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.IDesignerCallback;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.INGClientWebsocketSession;
import com.servoy.j2db.server.ngclient.NGClient;
import com.servoy.j2db.server.ngclient.NGFormManager;
import com.servoy.j2db.util.Debug;

/**
 * A lightweight NG client for form preview/testing purposes.
 * <p>
 * This client:
 * <ul>
 *   <li>Skips authentication (bypasses mustAuthenticate/login form)</li>
 *   <li>Shows a specific form directly instead of the solution's first form</li>
 *   <li>Does not attach the debugger</li>
 * </ul>
 * <p>
 * Started via the URL parameter {@code ?formpreview=formName}.
 * Authentication is also bypassed at the HTTP filter level in {@code IndexPageFilter}.
 *
 * @since 2026.6
 */
public class FormPreviewNGClient extends NGClient
{
	private static final long SHUTDOWN_TIMEOUT_SECONDS = 10;
	private static volatile String pendingTargetFormName;
	private static volatile FormPreviewNGClient instance;
	private volatile String targetFormName;

	public FormPreviewNGClient(INGClientWebsocketSession wsSession, IDesignerCallback designerCallback, String targetFormName) throws Exception
	{
		super(wsSession, designerCallback);
		this.targetFormName = targetFormName;
		synchronized (FormPreviewNGClient.class)
		{
			FormPreviewNGClient previous = instance;
			if (previous != null && !previous.isShutDown())
			{
				Debug.warn("Shutting down existing FormPreviewNGClient before creating a new one");
				shutdownOnEventThread(previous);
			}
			instance = this;
		}
	}

	/**
	 * Shuts down the currently active FormPreviewNGClient instance, if any.
	 * Follows the same pattern as DebugClientHandler.createDebugNGClient().
	 */
	public static synchronized void shutdownExisting()
	{
		FormPreviewNGClient previous = instance;
		if (previous != null && !previous.isShutDown())
		{
			shutdownOnEventThread(previous);
		}
		instance = null;
	}

	/**
	 * Returns the currently active FormPreviewNGClient singleton, or null if none is set.
	 * <p>
	 * Used by the session factory (Activator) to decide - mirroring the debug NG client recycle pattern - whether an
	 * existing preview client can be reused (same websocket session key) and retargeted rather than shut down and
	 * recreated.
	 */
	public static FormPreviewNGClient getInstance()
	{
		synchronized (FormPreviewNGClient.class)
		{
			return instance;
		}
	}

	/**
	 * Retargets this (reused) preview client to show a different form.
	 * <p>
	 * On a second preview request that arrives on the same websocket session, the existing client is reused instead of
	 * being shut down and recreated (mirroring the debug NG client). Retargeting updates the tracked target form name
	 * and shows the newly requested form on this client's main panel. The {@code showFormInMainPanel} call is performed
	 * on this client's own event dispatch thread (posted via its dispatcher when not already on it) so no thread
	 * affinity is violated.
	 *
	 * @param formName the name of the form to show now
	 */
	public void retarget(final String formName)
	{
		this.targetFormName = formName;
		runOnEventThread(() -> ((NGFormManager)getFormManager()).showFormInMainPanel(formName));
	}

	/**
	 * Runs the given action on this client's own event dispatch thread.
	 * <p>
	 * If this client has a live event dispatcher and the current thread is <b>not</b> its event dispatch thread, the
	 * action is posted onto the dispatcher via {@link IEventDispatcher#addEvent(Runnable)}. Otherwise (no live
	 * dispatcher, or already on its event thread) the action is invoked directly on the calling thread.
	 */
	private void runOnEventThread(Runnable action)
	{
		IEventDispatcher dispatcher = null;
		INGClientWebsocketSession wss = getWebsocketSession();
		if (wss != null)
		{
			dispatcher = wss.getEventDispatcher(false);
		}
		runRouting(dispatcher, action);
	}

	/**
	 * Routes a fire-and-forget action either onto the given event dispatcher or invokes it directly, following the same
	 * thread-affinity contract as {@link #runOnEventThread(Runnable)}.
	 * <p>
	 * If {@code dispatcher} is non-null and the current thread is <b>not</b> its event dispatch thread, the
	 * {@code action} is posted onto the dispatcher via {@link IEventDispatcher#addEvent(Runnable)} (no wait - unlike the
	 * shutdown path there is no completion latch). If {@code dispatcher} is null (already gone) or we are already on its
	 * event dispatch thread, {@code action} is invoked directly on the calling thread.
	 * <p>
	 * Package-private and taking its collaborators as parameters, following the same shape as
	 * {@link #shutdownRouting(IEventDispatcher, Runnable, BooleanSupplier)}. External behaviour is unchanged.
	 *
	 * @param dispatcher this client's event dispatcher, or null if none is live
	 * @param action the action to run (typically the retarget {@code showFormInMainPanel} call)
	 */
	static void runRouting(IEventDispatcher dispatcher, Runnable action)
	{
		if (dispatcher != null && !dispatcher.isEventDispatchThread())
		{
			dispatcher.addEvent(action);
		}
		else
		{
			action.run();
		}
	}

	/**
	 * Shuts the given (previous) FormPreviewNGClient instance down on its own event dispatch thread.
	 * <p>
	 * Tearing the previous preview client down synchronously from a Tomcat request thread (the thread on which a new
	 * FormPreviewNGClient is constructed) trips {@code DataAdapterList.checkThatThisIsTheEventThread()} during DAL
	 * teardown, logging a spurious ERROR. To avoid this we post the {@code shutDown(true)} call onto the old instance's
	 * event dispatch thread and wait (up to {@link #SHUTDOWN_TIMEOUT_SECONDS} seconds) for it to complete, so DAL
	 * teardown runs where the assertion expects it. If the wait times out or is interrupted we proceed anyway rather
	 * than blocking the caller indefinitely (the caller holds the class monitor while this runs).
	 * <p>
	 * If the old instance has no live dispatcher (already gone) or we are already on its event dispatch thread, the
	 * shutdown is invoked directly - which cannot trip the assertion (no dispatcher to check against, or already on the
	 * right thread).
	 */
	private static void shutdownOnEventThread(final FormPreviewNGClient old)
	{
		IEventDispatcher dispatcher = null;
		INGClientWebsocketSession wss = old.getWebsocketSession();
		if (wss != null)
		{
			dispatcher = wss.getEventDispatcher(false);
		}
		shutdownRouting(dispatcher, () -> old.shutDown(true), old::isShutDown);
	}

	/**
	 * Routes a shutdown action either onto the given event dispatcher (off-thread teardown) or invokes it directly,
	 * following the thread-affinity contract described on {@link #shutdownOnEventThread(FormPreviewNGClient)}.
	 * <p>
	 * If {@code dispatcher} is non-null and the current thread is <b>not</b> its event dispatch thread, the
	 * {@code shutdownAction} is posted onto the dispatcher via {@link IEventDispatcher#addEvent(Runnable)} and this
	 * method waits (up to {@link #SHUTDOWN_TIMEOUT_SECONDS} seconds) for it to complete; on timeout or interrupt it
	 * proceeds anyway rather than blocking the caller indefinitely, and warns if {@code isShutDown} still reports the
	 * old client as not fully shut down. If {@code dispatcher} is null (already gone) or we are already on its event
	 * dispatch thread, {@code shutdownAction} is invoked directly on the calling thread.
	 * <p>
	 * Package-private and taking its collaborators as parameters so the routing decision is isolated and readable.
	 * External behaviour is unchanged.
	 *
	 * @param dispatcher the old client's event dispatcher, or null if none is live
	 * @param shutdownAction the shutdown to run (typically {@code () -> old.shutDown(true)})
	 * @param isShutDown reports whether the old client is fully shut down (checked after off-thread teardown)
	 */
	static void shutdownRouting(IEventDispatcher dispatcher, final Runnable shutdownAction, final BooleanSupplier isShutDown)
	{
		if (dispatcher != null && !dispatcher.isEventDispatchThread())
		{
			final CountDownLatch done = new CountDownLatch(1);
			dispatcher.addEvent(() -> {
				try
				{
					shutdownAction.run();
				}
				finally
				{
					done.countDown();
				}
			});
			try
			{
				if (!done.await(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS))
				{
					Debug.warn("Previous FormPreviewNGClient did not shut down within " + SHUTDOWN_TIMEOUT_SECONDS +
						"s; proceeding anyway - the old client may not be fully shut down");
				}
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				Debug.warn("Interrupted while waiting for the previous FormPreviewNGClient to shut down; the old client may not be fully shut down");
			}
			if (!isShutDown.getAsBoolean())
			{
				Debug.warn("Previous FormPreviewNGClient is not fully shut down after off-thread teardown");
			}
		}
		else
		{
			shutdownAction.run();
		}
	}

	public static void setPendingTargetFormName(String formName)
	{
		pendingTargetFormName = formName;
	}

	public String getTargetFormName()
	{
		return targetFormName;
	}

	@Override
	protected IBasicFormManager createFormManager()
	{
		final String formToShow = pendingTargetFormName;

		return new NGFormManager(this)
		{
			@Override
			public void makeSolutionSettings(Solution s)
			{

				java.util.Iterator<com.servoy.j2db.persistence.Form> e = application.getFlattenedSolution().getForms(true);
				while (e.hasNext())
				{
					com.servoy.j2db.persistence.Form form = e.next();
					if (application.getFlattenedSolution().formCanBeInstantiated(form))
					{
						addForm(form, form.getName().equals(formToShow));
					}
					else
					{
						addForm(form, false);
					}
				}

				application.getModeManager().setMode(com.servoy.j2db.IModeManager.EDIT_MODE);

				if (getCurrentForm() == null)
				{
					showFormInMainPanel(formToShow);
				}
			}
		};
	}

	@Override
	protected void showInfoPanel()
	{
		// skip info panel for preview client
	}

	@Override
	public void showDefaultLogin() throws com.servoy.j2db.util.ServoyException
	{
		// skip authentication for form preview - set a fake user so solution loading continues

		getClientInfo().setUserUid("formpreview_user");
		getClientInfo().setUserName("FormPreview");
	}
}
