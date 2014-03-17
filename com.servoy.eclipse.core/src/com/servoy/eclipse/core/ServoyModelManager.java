/*
\ This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.core;

import java.util.concurrent.Semaphore;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * Is similar to JavaModelManager, single entry point for anything about servoy in eclipse
 * 
 * @author jblok
 */
public class ServoyModelManager
{
	/**
	 * The singleton manager
	 */
	private static ServoyModelManager MANAGER = new ServoyModelManager();
	private static boolean initializingModel = false;

	private volatile ServoyModel servoyModel = null;
	private final Semaphore asyncWaiter = new Semaphore(0);

	/**
	 * Constructs a new manager
	 */
	private ServoyModelManager()
	{
	}

	/**
	 * Returns the singleton
	 */
	public final static ServoyModelManager getServoyModelManager()
	{
		return MANAGER;
	}

	public ServoyModel getServoyModel()
	{
		if (servoyModel == null)
		{
			while (true)
			{
				boolean wait = false;
				try
				{
					// first just check if the workbench is running
					if (!PlatformUI.isWorkbenchRunning())
					{
						wait = true;
					}
					else
					{
						// even if it is running it could be that that parts
						// are not initialised yet (the WorkbenchPlugin.e4Context)
						// this will bomb out with a null point exception if not fully initialised
						PlatformUI.getWorkbench().getWorkingSetManager();
					}
				}
				catch (Exception e)
				{
					wait = true;
				}
				if (wait)
				{
					try
					{
						// just wait until it is ready 
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
					}
				}
				else
				{
					break;
				}
			}
			// create servoy model in the display thread.
			final boolean async[] = { false };
			Runnable run = new Runnable()
			{
				public void run()
				{
					synchronized (ServoyModelManager.this)
					{
						try
						{
							if (servoyModel == null)
							{
								if (initializingModel) throw new RuntimeException("Error: recursive attempt to create ServoyModel detected!"); //$NON-NLS-1$
								initializingModel = true; // to avoid multiple creations of ServoyModel (reentrant calls) and fail fast (for example ServoyModel() -> Activator.getDefault() -> ServoyModel())
								servoyModel = new ServoyModel();
								initializingModel = false;
								servoyModel.initialize();
							}
						}
						finally
						{
							initializingModel = false;
							if (async[0]) asyncWaiter.release();
						}
					}
				}
			};

			if (Display.getCurrent() != null)
			{
				run.run();
			}
			else
			{
				async[0] = true;
				Display.getDefault().asyncExec(run);

				try
				{
					asyncWaiter.acquire(); // in case of async exec it will wait
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			}

			// this access to servoyModel is not exactly thread safe, but as servoyModel can only be set to something as opposed to being null - it's a turn for the better
			if (servoyModel == null) throw new RuntimeException("Error: ServoyModel creation failed!"); //$NON-NLS-1$

			// notify the client debug handler that servoy model has been initialized.
			// on the mac the debug smart client must wait until the swt main thread is not busy,
			// otherwise the smart client frame will not paint.
			if (ApplicationServerSingleton.get().getDebugClientHandler() != null)
			{
				ApplicationServerSingleton.get().getDebugClientHandler().flagModelInitialised();
			}
		}
		// this access to servoyModel is not exactly thread safe, but as servoyModel can only be set to something as opposed to being null - it's a turn for the better
		return servoyModel;
	}

	public synchronized boolean isServoyModelCreated()
	{
		return servoyModel != null;
	}
}
