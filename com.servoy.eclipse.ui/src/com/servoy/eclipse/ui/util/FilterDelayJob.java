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
package com.servoy.eclipse.ui.util;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * A WorkbenchJob that can be used to apply a filter only after the user stops typing.<br>
 * The user stops typing when "setFilterText()" is not called for "stopTypingInterval" ms.
 */
public class FilterDelayJob extends Job
{

	private String text;
	private final FilteredEntity toFilter;
	private final long stopTypingInterval;

	/**
	 * Creates a new apply filter job.
	 * 
	 * @param toFilter the object that supports filtering that will be used.
	 * @param stopTypingInterval the minimum interval (in ms) between consecutive setFilterText() that will trigger the filtering.
	 */
	public FilterDelayJob(FilteredEntity toFilter, long stopTypingInterval, String name)
	{
		super(name);
		this.toFilter = toFilter;
		this.stopTypingInterval = stopTypingInterval;
		setSystem(false);
		setUser(false);
	}

	/**
	 * Set the text that will be used for filtering. Should be called from the UI thread.
	 * 
	 * @param text the text that will be used for filtering..
	 */
	public void setFilterText(String text)
	{
		cancel();
		this.text = text;
		schedule(stopTypingInterval);
	}

	@Override
	public IStatus run(final IProgressMonitor monitor)
	{
		monitor.beginTask("Filtering Solution Explorer -> '" + text + "'", IProgressMonitor.UNKNOWN);
		Timer progress = new Timer();
		try
		{
			progress.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					monitor.worked(1);
				}
			}, 500, 500);
			toFilter.filter(text);
		}
		finally
		{
			progress.cancel();
			monitor.done();
		}
		return Status.OK_STATUS;
	}

}