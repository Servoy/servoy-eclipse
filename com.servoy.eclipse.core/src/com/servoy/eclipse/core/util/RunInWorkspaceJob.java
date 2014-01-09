/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.core.util;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This job should be used when a workspace job should be run without any concurrent resource change listeners fires when this job runs.
 * So instead of using a {@link WorkspaceJob} instance a {@link IWorkspaceRunnable} should be created and this job should be used to run it.
 * The same configuration should be used with the {@link ISchedulingRule} that should just be set on the Job and it will be transfered over to the call
 * {@link IWorkspace#run(IWorkspaceRunnable, ISchedulingRule, int, IProgressMonitor)} with the flag being a {@link IWorkspace#AVOID_UPDATE} to block the eclipse
 * NotificationManager to run its NotifyJob concurrently.
 *  
 * @author jcompagner
 * 
 * @since 6.1
 */
// I think this doesn't offer much of an improvement over a normal WorkspaceJob as IWorkspace.AVOID_UPDATE is more of a hint (see IWorkspace.run(...) docs).
// if resource changes happen on another thread you can still get partial change notifications
public class RunInWorkspaceJob extends Job
{
	private final IWorkspaceRunnable runnable;

	/**
	 * @param name
	 */
	public RunInWorkspaceJob(IWorkspaceRunnable runnable)
	{
		this("", runnable);
	}

	/**
	 * @param name
	 */
	public RunInWorkspaceJob(String name, IWorkspaceRunnable runnable)
	{
		super(name);
		this.runnable = runnable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		try
		{
			ResourcesPlugin.getWorkspace().run(runnable, getRule(), IWorkspace.AVOID_UPDATE, monitor);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
