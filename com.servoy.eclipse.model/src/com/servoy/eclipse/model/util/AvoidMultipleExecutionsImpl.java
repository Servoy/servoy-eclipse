/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.model.util;

import org.eclipse.core.runtime.jobs.Job;

/**
 * Helper class for AvoidMultipleExecutionXYZJob classes.
 * @see {@link AvoidMultipleExecutionsJob}, {@link AvoidMultipleExecutionsWorkspaceJob} for more details
 *
 * @author acostescu
 */
public class AvoidMultipleExecutionsImpl
{

	private final int[] numberOfWantedRuns = new int[] { 0 };
	private final int initialDelay;
	private final Job job;

	public AvoidMultipleExecutionsImpl(int initialDelay, Job job)
	{
		this.initialDelay = initialDelay;
		this.job = job;
	}

	public void scheduleIfNeeded()
	{
		synchronized (numberOfWantedRuns)
		{
			if (numberOfWantedRuns[0] == 0)
			{
				numberOfWantedRuns[0] = 1;
				job.schedule(initialDelay);
			}
			else
			{
				numberOfWantedRuns[0]++;
			}
		}
	}

	public void runStart()
	{
		synchronized (numberOfWantedRuns)
		{
			numberOfWantedRuns[0] = 1; // we are starting this operation now; all potential other wanted operations (already requested) are satisfied by this one
		}
	}

	public void runEnd()
	{
		synchronized (numberOfWantedRuns)
		{
			numberOfWantedRuns[0]--;
			if (numberOfWantedRuns[0] > 0) job.schedule(); // schedule operation again as one was needed while current one is in progress
		}
	}

}
