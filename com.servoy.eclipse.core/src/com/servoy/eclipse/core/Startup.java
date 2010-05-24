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
package com.servoy.eclipse.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.progress.WorkbenchJob;

import com.servoy.j2db.server.ApplicationServerSingleton;

/**
 * Initializations for non-critical plugin parts (the plugin should be able to run normally without these being called firs). These tasks can be long-running.
 * 
 * @author Andrei Costescu
 */
public class Startup implements IStartup
{

	public void earlyStartup()
	{
		// this can be long-running
		Job job = new WorkbenchJob("Initializing - restoring active solution")
		{
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				// this is executed in the SWT thread, because we do not want it running in parallel with other
				// operations from GUI (such as solution explorer browsing/actions) - it can lead to
				// concurrent modification exceptions or things like this in Server class for example
				ServoyModelManager.getServoyModelManager().getServoyModel();

				// notify the client debug handler that servoy model has been initialised.
				// on the mac the debug smart client must wait until the swt main thread is not busy,
				// otherwise the smart client frame wil not paint.
				ApplicationServerSingleton.get().getDebugClientHandler().flagModelInitialised();

				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

}
