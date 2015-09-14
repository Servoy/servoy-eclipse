/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.smart;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.test.SolutionJSUnitSuiteCodeBuilder;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;
import com.servoy.j2db.dataprocessing.IUserClient;
import com.servoy.j2db.debug.DebugClientHandler;
import com.servoy.j2db.debug.DebugJ2DBClient;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IUserManager;
import com.servoy.j2db.util.Debug;

/**
 * Debug smart client specialised for unit test runs.
 *
 * @author acostescu
 */
public class DebugJ2DBTestClient extends DebugJ2DBClient
{
	private final List<Runnable> events = new ArrayList<Runnable>();
	private final IUserManager userManager;

	public DebugJ2DBTestClient(DebugClientHandler debugClientHandler)
	{
		super(false, debugClientHandler);
		userManager = new JSUnitUserManager(ServoyModelManager.getServoyModelManager().getServoyModel().getUserManager());
		setUnitTestMode(true); // TODO move all unit test code from superclass here
	}

	@Override
	public void updateUI(int time)
	{
		runEvents();
		super.updateUI(time);
	}

	@Override
	protected void dispatchEventsToHideDialog()
	{
		// do nothing so that tests are run after client is loaded
	}

	@Override
	protected boolean registerClient(IUserClient uc) throws Exception
	{
		boolean register = super.registerClient(uc);

		// access the server directly to mark the client as local
		// * commented out to work the same as automated import test client - it will need authentication in
		// order to provide access to remote services when enhanced security is enabled - (more like smart client); if this is too restrictive, in the future we
		// can uncomment this and do the same for import test client; see case SVY-6360
		// ApplicationServerRegistry.get().setServerProcess(getClientID());

		return register;
	}


	@Override
	protected IUserManager createUserManager()
	{
		try
		{
			userManager.createGroup(ApplicationServerRegistry.get().getClientId(), IRepository.ADMIN_GROUP);
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		return userManager;
	}

	private void runEvents()
	{
		if (events.size() == 0/* || isShutDown() */) return;
		Runnable[] runnables = events.toArray(new Runnable[events.size()]);
		events.clear();
		for (Runnable runnable : runnables)
		{
//			if (isShutDown()) return;
			runnable.run();
		}
		runEvents();
	}

	@Override
	protected void doInvokeLater(Runnable r)
	{
		events.add(r);
		final IServiceProvider client = this;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				IServiceProvider prevServiceProvider = J2DBGlobals.setSingletonServiceProvider(client);
				try
				{
					runEvents();
				}
				finally
				{
					J2DBGlobals.setSingletonServiceProvider(prevServiceProvider);
				}
			}
		});
	}

	// do not show info/error dialogs in test client
	@Override
	public void reportError(Component parentComponent, String message, Object detail)
	{
		DebugUtils.errorToDebugger(getScriptEngine(), message, detail);
		logError(message, detail);

		// tests should fail when this happens;
		// remember this exception
		SolutionJSUnitSuiteCodeBuilder.failAfterCurrentTestWithError(getScriptEngine().getSolutionScope(), message, detail);
	}

	@Override
	public void reportInfo(Component parentComponent, String message, String title)
	{
		DebugUtils.infoToDebugger(getScriptEngine(), message);
		Debug.trace(message);
	}

}