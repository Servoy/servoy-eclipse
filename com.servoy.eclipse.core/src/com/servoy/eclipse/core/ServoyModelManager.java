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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;

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
	private final static ServoyModelManager MANAGER = new ServoyModelManager();
	private final AtomicBoolean initializedCalled = new AtomicBoolean(false);

	private final ServoyModel servoyModel = new ServoyModel();
	private final DelegatingServoyModel delegating = new DelegatingServoyModel(servoyModel);

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

	public IDeveloperServoyModel getServoyModel()
	{
		if (initializedCalled.compareAndSet(false, true))
		{
			Job servoyModelCreator = new Job("Creating servoy model")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					// create servoy model in the display thread.
//					Runnable run = new Runnable()
//					{
//						public void run()
//						{
					servoyModel.initialize();
					// notify the client debug handler that servoy model has been initialized.
					// on the mac the debug smart client must wait until the swt main thread is not busy,
					// otherwise the smart client frame will not paint.
					if (ApplicationServerRegistry.get().getDebugClientHandler() != null)
					{
						ApplicationServerRegistry.get().getDebugClientHandler().flagModelInitialised();
					}
//
//						}
//					};
//					Display.getDefault().asyncExec(run);
					return Status.OK_STATUS;
				}
			};
			servoyModelCreator.setSystem(false);
			servoyModelCreator.schedule();
		}
		if (servoyModel.isFlattenedSolutionLoaded()) return servoyModel;
		return delegating;
	}

	public boolean isServoyModelCreated()
	{
		return initializedCalled.get();
	}
}
