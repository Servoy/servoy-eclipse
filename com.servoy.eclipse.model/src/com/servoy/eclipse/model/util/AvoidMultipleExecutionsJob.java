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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A job to execute code that - when it starts to run makes obsolete any other previously scheduled executions of the same code. For example a job that does a full reload of something, if it is
 * scheduled to run 20 times at once, it only needs to run once. If it is scheduled to run N more times while it is already running, it will start executing the code once more only after current execution is done.<br/><br/>
 * Please use {@link #scheduleIfNeeded()} and {@link #runAvoidingMultipleExecutions(IProgressMonitor)} instead of the usual {@link #schedule()} and {@link #run(IProgressMonitor)} when working with this type of jobs.
 *
 * TODO the same need could potentially be made to disappear if you use proper rules for the jobs that want to execute some code - depending on what each situation needs. Rules can be used with this class as well of course.
 *
 * @author acostescu
 */
public abstract class AvoidMultipleExecutionsJob extends Job
{
	private final AvoidMultipleExecutionsImpl impl;

	/**
	 * Same as AvoidMultipleExecutionsJob(100);
	 */
	public AvoidMultipleExecutionsJob(String name)
	{
		this(name, 100);
	}

	public AvoidMultipleExecutionsJob(String name, int initialDelay)
	{
		super(name);
		this.impl = new AvoidMultipleExecutionsImpl(initialDelay, this);
	}

	/**
	 * Use this instead of {@link #schedule()} to avoid multiple executions.
	 */
	public void scheduleIfNeeded()
	{
		impl.scheduleIfNeeded();
	}

	@Override
	protected final IStatus run(IProgressMonitor monitor)
	{
		impl.runStart();
		try
		{
			return runAvoidingMultipleExecutions(monitor);
		}
		finally
		{
			impl.runEnd();
		}
	}

	protected abstract IStatus runAvoidingMultipleExecutions(IProgressMonitor monitor);

}