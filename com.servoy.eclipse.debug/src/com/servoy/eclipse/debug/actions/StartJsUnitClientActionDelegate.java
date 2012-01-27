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
package com.servoy.eclipse.debug.actions;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.dltk.debug.ui.DLTKDebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.IDebugJ2DBClient;

/**
 * @author jcompagner
 * 
 */
public class StartJsUnitClientActionDelegate extends StartDebugAction implements IRunnableWithProgress
{
	private boolean started = false;

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action)
	{
		started = false;
		//make sure the plugins are loaded
		DLTKDebugUIPlugin.getDefault();
		DebugPlugin.getDefault();

		Job job = new Job("JSUnit test client start") //$NON-NLS-1$
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					StartJsUnitClientActionDelegate.this.run(monitor);
				}
				catch (InvocationTargetException itex)
				{
					ServoyLog.logError(itex);
				}
				catch (InterruptedException intex)
				{
					ServoyLog.logError(intex);
				}
				return Status.OK_STATUS;
			}
		};
		job.setUser(true);
		job.schedule();
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
	{
		if (testAndStartDebugger())
		{
			IDebugJ2DBClient jSUnitJ2DBClient = Activator.getDefault().getJSUnitJ2DBClient();
			jSUnitJ2DBClient.show();
			started = true;
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
