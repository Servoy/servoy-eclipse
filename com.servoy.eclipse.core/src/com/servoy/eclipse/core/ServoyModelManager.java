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

import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.widgets.Display;

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
	private volatile ServoyModel servoyModel = null;

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

	private final CountDownLatch latch = new CountDownLatch(1);

	public ServoyModel getServoyModel()
	{
		if (servoyModel == null)
		{
			// create servoy model in the display thread.

			Runnable run = new Runnable()
			{
				public void run()
				{
					synchronized (ServoyModelManager.this)
					{
						if (servoyModel == null)
						{
							try
							{
								servoyModel = new ServoyModel();
								servoyModel.initialize();
							}
							finally
							{
								latch.countDown();
							}
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
				Display.getDefault().asyncExec(run);
				try
				{
					latch.await();
				}
				catch (InterruptedException e)
				{
					ServoyLog.logError(e);
				}
			}
			// notify the client debug handler that servoy model has been initialized.
			// on the mac the debug smart client must wait until the swt main thread is not busy,
			// otherwise the smart client frame will not paint.
			if (ApplicationServerSingleton.get().getDebugClientHandler() != null)
			{
				ApplicationServerSingleton.get().getDebugClientHandler().flagModelInitialised();
			}
		}
		return servoyModel;
	}

	public synchronized boolean isServoyModelCreated()
	{
		return servoyModel != null;
	}
}
