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
package com.servoy.eclipse.jsunit.smart;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IWorkbenchWindow;

import com.servoy.eclipse.debug.handlers.StartDebugHandler;
import com.servoy.eclipse.jsunit.Activator;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;
import com.servoy.j2db.util.Debug;

/**
 * @author jcompagner
 *
 */
public class StartJsUnitClientActionDelegate extends StartDebugHandler implements IRunnableWithProgress
{
	private boolean started = false;
	private IWorkbenchWindow window;

	public void init(IWorkbenchWindow w)
	{
		this.window = w;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		started = false;

		makeSureNeededPluginsAreStarted();

		try
		{
			// run in the progress service thread in stead of the main swt thread.
			// needed to make sure the main swt thread is not busy when the debug smart client
			// is created, see ApplicationServer.DebugClientHandler.
			window.getWorkbench().getProgressService().run(true, false, this);
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	public void run(IProgressMonitor monitor)
	{
		if (testAndStartDebugger())
		{
			IDebugHeadlessClient jsunitClient = Activator.getDefault().getJSUnitClient();
			try
			{
				jsunitClient.loadDebugSolution();
				started = true;
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
	}

	/**
	 * Returns whether or not the client was successfully started on the last run(). Will be false if the user canceled the run or if some problem ocurred.
	 *
	 * @return whether or not the client was successfully started on the last run().
	 */
	public boolean clientStartSucceeded()
	{
		return started;
	}


}
