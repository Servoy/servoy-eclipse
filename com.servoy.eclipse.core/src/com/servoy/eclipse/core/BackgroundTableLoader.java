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

import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;

/**
 * Loads table lists for servers in background job.<br>
 * When a solution is activated, it also starts loading in background the tables that are used in that solution (except for ones referenced in scripting).
 * 
 * @author acostescu
 */
public class BackgroundTableLoader implements IActiveProjectListener
{

	private final IServerManagerInternal serverManager;
	private boolean running = false;
	private boolean paused = false;
	private boolean tableListsLoaded = false;
	private ServoyProject[] modules = null;

	public BackgroundTableLoader(IServerManagerInternal serverManager)
	{
		this.serverManager = serverManager;
	}

	public synchronized void startLoadingOfServers()
	{
		runInJob(10000); // start 10 sec later so we affect the startup process less
	}

	private void runInJob(int delayBeforeStart)
	{
		Job job = new Job("Loading servers/tables needed by current solution")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				BackgroundTableLoader.this.run();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setPriority(Job.BUILD); // has low prio
		running = true;
		job.schedule(delayBeforeStart);
	}

	public synchronized void pause()
	{
		paused = true;
	}

	public synchronized void resume()
	{
		if (paused)
		{
			paused = false;
			if (running) notify();
		}
	}

	protected void run()
	{
		try
		{
			if (!tableListsLoaded)
			{
				String[] serverNames = serverManager.getServerNames(true, false, false, false);

				// move repository in front
				for (int i = 1; i < serverNames.length; i++)
				{
					if (IServer.REPOSITORY_SERVER.equals(serverNames[i]))
					{
						String tmp = serverNames[0];
						serverNames[0] = serverNames[i];
						serverNames[i] = tmp;
						break;
					}
				}

				// load all table/view names
				for (String serverName : serverNames)
				{
					IServer s = serverManager.getServer(serverName, true, false);
					if (s != null)
					{
						try
						{
							synchronized (this)
							{
								while (paused)
									wait();
								((IServerInternal)s).testConnection(0);
								try
								{
									s.getTableAndViewNames(); //load all table names
								}
								catch (Exception e)
								{
									((IServerInternal)s).flagInvalid();
									Debug.error(e); // Report in log
								}
							}
						}
						catch (Exception ex)
						{
							((IServerInternal)s).forceFlagInvalid();
							Debug.trace(ex); //report only when tracing 
						}
					}
				}
				tableListsLoaded = true;
			}

			ServoyProject[] modulesInUse = null;
			Iterator<String> it = null;
			String dataSource = null;
			synchronized (this)
			{
				modulesInUse = modules;
				if (modulesInUse != null)
				{
					it = getDataSources(modulesInUse).iterator();
					dataSource = it.hasNext() ? it.next() : null;
				}
				if (dataSource == null)
				{
					running = false;
				}
			}
			// load columns for tables used in the solution
			while (dataSource != null)
			{
				String[] serverAndTable = DataSourceUtils.getDBServernameTablename(dataSource);
				IServer s = serverManager.getServer(serverAndTable[0], true, true);
				if (s != null)
				{
					try
					{
						synchronized (this)
						{
							while (paused)
								wait();
							s.getTable(serverAndTable[1]); // loads all columns names as well
						}
					}
					catch (Exception e)
					{
						((IServerInternal)s).flagInvalid();
						Debug.error(e);
					}
				}
				synchronized (this)
				{
					if (modulesInUse != modules)
					{
						// the active solution changed - we must load another set of tables
						modulesInUse = modules;
						if (modulesInUse != null)
						{
							it = getDataSources(modulesInUse).iterator();
							dataSource = it.hasNext() ? it.next() : null;
						}
						else
						{
							dataSource = null;
						}
					}
					else
					{
						dataSource = it.hasNext() ? it.next() : null;
					}
					if (dataSource == null)
					{
						running = false;
					}
				}
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
		finally
		{
			synchronized (this)
			{
				running = false;
			}
		}
	}

	private Set<String> getDataSources(ServoyProject[] modules)
	{
		DataSourceCollectorVisitor datasourceCollector = new DataSourceCollectorVisitor();
		for (ServoyProject sp : modules)
		{
			sp.getSolution().acceptVisitor(datasourceCollector);
		}
		return datasourceCollector.getDataSources();
	}

	public synchronized void activeProjectChanged(ServoyProject activeProject)
	{
		modules = (activeProject == null) ? null : ServoyModelManager.getServoyModelManager().getServoyModel().getModulesOfActiveProject();
		if (!running)
		{
			runInJob(0);
		}
	}

	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
		// not needed
	}

	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		return true;
	}
}