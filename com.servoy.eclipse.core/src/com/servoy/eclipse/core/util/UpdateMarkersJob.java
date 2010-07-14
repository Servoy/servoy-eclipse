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
package com.servoy.eclipse.core.util;

import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Utility class that speeds up an update resources process if this process is composed of lots of runnables. Helpful to avoid jobs for each update if you
 * expect to have lots of updates happening (hundreds, thousands of jobs).
 * 
 * @author acostescu
 */
public class UpdateMarkersJob
{
	private Job job;
	private final List<Runnable> runners = new Vector<Runnable>();
	private boolean running = false;
	private final String jobName;
	private final ISchedulingRule rule;
	private final boolean system;

	public UpdateMarkersJob(String jobName, ISchedulingRule rule, boolean system)
	{
		this.rule = rule;
		this.jobName = jobName;
		this.system = system;
	}

	private void createNewJob()
	{
		job = new WorkspaceJob(jobName)
		{
			@Override
			public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
			{
				while (running)
				{
					runners.get(0).run();

					synchronized (runners)
					{
						runners.remove(0);
						if (runners.size() == 0)
						{
							running = false;
							break;
						}
					}
				}

				return Status.OK_STATUS;
			}
		};
		job.setRule(rule);
		job.setSystem(system);
		job.schedule();
	}

	public void addRunner(Runnable r)
	{
		synchronized (runners)
		{
			runners.add(r);
			if (!running)
			{
				running = true;
				createNewJob();
			}
		}
	}

}