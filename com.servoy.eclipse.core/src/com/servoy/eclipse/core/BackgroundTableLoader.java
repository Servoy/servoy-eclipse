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

import static java.lang.Boolean.TRUE;

import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
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
	private boolean running = false;
	private boolean paused = false;
	private boolean tableListsLoaded = false;
	private ServoyProject[] modules = null;

	public BackgroundTableLoader()
	{
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
			IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
			boolean buildAlreadyRun = false;
			if (!tableListsLoaded)
			{
				boolean invalidServersFound = false;
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
					if (s != null && !TRUE.equals(s.getSettings().getClientOnlyConnections()))
					{
						try
						{
							((IServerInternal)s).testConnection(0);
							synchronized (this)
							{
								while (paused)
									wait();
								try
								{
									s.getTableAndViewNames(false); //load all table names
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
							((IServerInternal)s).flagInvalid();
							Debug.trace(ex); //report only when tracing
						}
						if (!s.isValid()) invalidServersFound = true;
					}
				}

				if (invalidServersFound)
				{
					// a server might be invalid only at this startup - we need to run build in order to generate problem markers
					buildAlreadyRun = true;
					ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
				}
				tableListsLoaded = true;
			}

			ServoyProject[] modulesInUse = null;
			Iterator<String> it = null;
			String dataSource = null;
			boolean missingTablesUsedByActive = false;
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
				if (serverAndTable != null)
				{
					IServer s = serverManager.getServer(serverAndTable[0], true, true);
					if (s != null)
					{
						try
						{
							synchronized (this)
							{
								while (paused)
									wait();
								if (s.getTable(serverAndTable[1]) == null) // loads all columns names as well - this is the main purpose
								{
									missingTablesUsedByActive = true;

									// if there is a dbi file for this table - add a dbi marker as well (as it might be helpful to use quick-fixes to create the table used by solution)
									DataModelManager dmm = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager();
									if (dmm != null && dmm.getDBIFile(dataSource).exists())
									{
										dmm.updateMarkerStatesForMissingTable(null, serverAndTable[0], serverAndTable[1]);
									}
								}
							}
						}
						catch (Exception e)
						{
							((IServerInternal)s).flagInvalid();
							missingTablesUsedByActive = true;
							Debug.error(e);
						}
					}
					else
					{
						missingTablesUsedByActive = true;
					}
				}
				synchronized (this)
				{
					if (modulesInUse != modules)
					{
						// the active solution changed - we must load another set of tables
						missingTablesUsedByActive = false;
						buildAlreadyRun = false;
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

			if (!buildAlreadyRun && missingTablesUsedByActive)
			{
				// a might have become missing only at this startup - we need to run build in order to generate problem markers
				ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
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