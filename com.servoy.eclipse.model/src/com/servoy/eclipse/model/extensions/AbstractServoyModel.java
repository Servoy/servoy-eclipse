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

package com.servoy.eclipse.model.extensions;

import java.rmi.RemoteException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.servoy.eclipse.model.DeveloperFlattenedSolution;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.inmemory.MenuTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.AtomicIntegerWithListener;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetTable;
import com.servoy.eclipse.model.view.ViewFoundsetsServer;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * @author acostescu
 */
public abstract class AbstractServoyModel implements IServoyModel
{
	protected ServoyProject activeProject;
	protected ServoyResourcesProject activeResourcesProject;

	protected DataModelManager dataModelManager;
	private volatile Map<String, ServoyProject> servoyProjectCache;

	private volatile FlattenedSolution flattenedSolution;
	private IActiveSolutionHandler activeSolutionHandler;

	private final EclipseMessages messagesManager;

	private BaseNGPackageManager ngPackageManager;

	private final AtomicIntegerWithListener resourceChangesHandlerCounter = new AtomicIntegerWithListener();

	public AbstractServoyModel()
	{
		messagesManager = new EclipseMessages();
		Messages.customMessageLoader = messagesManager;
	}

	protected void initNGPackageManager()
	{
		ngPackageManager = createNGPackageManager();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getDataSourceManager()
	 */
	@Override
	public IDataSourceManager getDataSourceManager()
	{
		return new IDataSourceManager()
		{

			@Override
			public ITable getDataSource(String dataSource)
			{
				String inMemTableName = DataSourceUtils.getInmemDataSourceName(dataSource);
				if (inMemTableName != null)
				{
					try
					{
						return findInMemITable(inMemTableName);
					}
					catch (Exception e)
					{
						ServoyLog.logError("couldn't find in mem table for datasource: " + dataSource, e);
					}
				}
				else if (dataSource != null && dataSource.startsWith(DataSourceUtils.VIEW_DATASOURCE_SCHEME_COLON))
				{
					try
					{
						return findViewITable(DataSourceUtils.getViewDataSourceName(dataSource));
					}
					catch (Exception e)
					{
						ServoyLog.logError("couldn't find view table for datasource: " + dataSource, e);
					}
				}
				else if (dataSource != null && dataSource.startsWith(DataSourceUtils.MENU_DATASOURCE_SCHEME_COLON))
				{
					try
					{
						// should we cache this?
						String menuName = DataSourceUtils.getMenuDataSourceName(dataSource);
						return new MenuTable(menuName);
					}
					catch (Exception e)
					{
						ServoyLog.logError("couldn't find view table for datasource: " + dataSource, e);
					}
				}
				else
				{
					String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(dataSource);
					if (dbServernameTablename != null)
					{
						try
						{
							IServer server = ApplicationServerRegistry.get().getServerManager().getServer(dbServernameTablename[0]);
							if (server != null)
							{
								return server.getTable(dbServernameTablename[1]);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError("couldn't find in db table for datasource: " + dataSource, e);
						}
					}
				}
				return null;
			}

			@Override
			public IServerInternal getServer(String dataSource)
			{
				String inMemTableName = DataSourceUtils.getInmemDataSourceName(dataSource);
				if (inMemTableName != null)
				{
					try
					{
						return ServoyModelFinder.getServoyModel().getMemServer(inMemTableName);
					}
					catch (Exception e)
					{
						ServoyLog.logError("couldn't find in mem table for datasource: " + dataSource, e);
					}
				}
				else
				{
					String[] dbServernameTablename = DataSourceUtils.getDBServernameTablename(dataSource);
					if (dbServernameTablename != null)
					{
						try
						{
							return (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(dbServernameTablename[0]);
						}
						catch (Exception e)
						{
							ServoyLog.logError("couldn't find in db table for datasource: " + dataSource, e);
						}
					}
				}
				return null;
			}
		};
	}

	public ServoyProject getActiveProject()
	{
		// if we would want to call autoSelectActiveProjectIfNull() here, it could generate a activeProjectChanged
		// event inside a get method (not very nice + that can result in reentrant calls)
		return activeProject;
	}

	/**
	 * Returns the active resources project. This is the only resources project referenced by the current active project. Will return null if the active project
	 * is null or if the number of resources projects referenced by the active project != 1.
	 *
	 * @return the active resources project.
	 */
	public ServoyResourcesProject getActiveResourcesProject()
	{
		return activeResourcesProject;
	}

	public ServoyProject[] getServoyProjects()
	{
		Map<String, ServoyProject> servoyProjects = reloadProjectCacheIfNecessary();
		return servoyProjects.values().toArray(new ServoyProject[servoyProjects.size()]);
	}

	public ServoyProject getServoyProject(String name)
	{
		return reloadProjectCacheIfNecessary().get(name);
	}

	/**
	 * Returns an array containing the modules of the active project (including the active project).
	 * If there is no active project, will return an array of size 0.
	 *
	 * The result does also include the active solution's import hooks (which are not part of the flattened solution).
	 *
	 * @return an array containing the modules of the active project.
	 * @see #getModulesOfActiveProjectWithImportHooks()
	 */
	public ServoyProject[] getModulesOfActiveProject()
	{
		// the set of solutions a user can work with at a given time is determined by the active solution;
		// this means that the only expandable solution nodes will be the active solution and it's referenced modules;
		// all other solutions will appear grayed-out and not-expandable (still allows the user to activate them if necessary)
		return getModulesOfActiveProject(false);
	}

	public ServoyProject[] getModulesOfActiveProject(boolean needSortedModules)
	{
		// the set of solutions a user can work with at a given time is determined by the active solution;
		// this means that the only expandable solution nodes will be the active solution and it's referenced modules;
		// all other solutions will appear grayed-out and not-expandable (still allows the user to activate them if necessary)
		AbstractCollection<ServoyProject> moduleProjects = needSortedModules ? new TreeSet<ServoyProject>() : new ArrayList<ServoyProject>();
		addFlattenedSolutionModules(moduleProjects);
		addImportHookModules(getActiveProject(), moduleProjects);
		return moduleProjects.toArray(new ServoyProject[moduleProjects.size()]);
	}

	public boolean isProjectActive(ServoyProject servoyProject)
	{
		return Arrays.asList(getModulesOfActiveProject()).contains(servoyProject);
	}

	/**
	 * Usually you would use {@link #getModulesOfActiveProject()} or {@link #getModulesOfActiveProjectWithImportHooks()} instead.
	 */
	public void addFlattenedSolutionModules(AbstractCollection<ServoyProject> moduleProjects)
	{
		// get all modules of the active solution (related solutions)
		FlattenedSolution flatActiveSolution = getFlattenedSolution();
		if (flatActiveSolution != null)
		{
			Solution[] relatedSolutions = flatActiveSolution.getModules();
			if (relatedSolutions != null)
			{
				for (Solution s : relatedSolutions)
				{
					if (s != null)
					{
						ServoyProject tmp = getServoyProject(s.getName());
						if (tmp != null)
						{
							moduleProjects.add(tmp);
						}
					}
				}
			}
		}
		if (activeProject != null && (!moduleProjects.contains(activeProject)))
		{
			moduleProjects.add(activeProject);
		}
	}


	public ServoyProject[] getImportHookModulesOfActiveProject()
	{
		AbstractCollection<ServoyProject> importHookModules = new ArrayList<ServoyProject>();
		addImportHookModules(getActiveProject(), importHookModules);
		return importHookModules.toArray(new ServoyProject[importHookModules.size()]);
	}

	/**
	 * Usually you would use {@link #getModulesOfActiveProjectWithImportHooks()} or {@link #getImportHookModulesOfActiveProject()} instead.
	 */
	public void addImportHookModules(ServoyProject p, AbstractCollection<ServoyProject> importHookModules)
	{
		addImportHookModules(p, importHookModules, new HashSet<ServoyProject>());
	}

	private void addImportHookModules(ServoyProject p, AbstractCollection<ServoyProject> importHookModules, Set<ServoyProject> visited)
	{
		if (p != null && !visited.contains(p))
		{
			visited.add(p);
			Solution s = p.getSolution();
			if (s != null)
			{
				if (SolutionMetaData.isImportHook(s.getSolutionMetaData()) && !importHookModules.contains(p)) importHookModules.add(p);

				String[] moduleNames = Utils.getTokenElements(s.getModulesNames(), ",", true);
				for (String moduleName : moduleNames)
				{
					addImportHookModules(getServoyProject(moduleName), importHookModules, visited);
				}
			}
		}
	}

	/**
	 * Gives the list of resource projects in the workspace. If you are looking for a resource project related to a solution project, please use
	 * {@link #getActiveResourcesProject()} or {@link ServoyProject#getResourcesProject()} instead.
	 *
	 * @return the list of resource projects in the workspace.
	 */
	public ServoyResourcesProject[] getResourceProjects()
	{
		List<ServoyResourcesProject> retval = new ArrayList<ServoyResourcesProject>();
		for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
		{
			try
			{
				if (project.isOpen() && project.hasNature(ServoyResourcesProject.NATURE_ID))
				{
					ServoyResourcesProject prj = (ServoyResourcesProject)project.getNature(ServoyResourcesProject.NATURE_ID);
					if (prj == null)
					{
						prj = new ServoyResourcesProject(project);
					}
					retval.add(prj);
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		return retval.toArray(new ServoyResourcesProject[retval.size()]);
	}

	public AbstractServoyModel refreshServoyProjects()
	{
		servoyProjectCache = null;
		return this;
	}

	private Map<String, ServoyProject> reloadProjectCacheIfNecessary()
	{
		Map<String, ServoyProject> servoyProjects = servoyProjectCache;
		if (servoyProjects == null)
		{
			servoyProjects = new HashMap<String, ServoyProject>();
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
			{
				try
				{
					if (project.isOpen() && project.hasNature(ServoyProject.NATURE_ID))
					{
						ServoyProject sp = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
						if (activeProject != null && activeProject.getProject().equals(project))
						{
							// in case active project was replaced/overwritten we must update the reference as well (so we don't have trouble when comparing IProject or ServoyProject instances...)
							setActiveProjectReferenceInternal(sp);
						}
						servoyProjects.put(project.getName(), sp);
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
			servoyProjectCache = servoyProjects;
		}
		return servoyProjects;
	}

	protected void setActiveProjectReferenceInternal(final ServoyProject project)
	{
		activeProject = project;
	}

	public DataModelManager getDataModelManager()
	{
		return dataModelManager;
	}

	protected FlattenedSolution createFlattenedSolution()
	{
		return new DeveloperFlattenedSolution(true);
	}

	public FlattenedSolution getFlattenedSolution()
	{
		if (flattenedSolution == null)
		{
			flattenedSolution = createFlattenedSolution();

			if (getActiveProject() != null && getActiveProject().getSolution() != null)
			{
				// projects might give deserialize exceptions => solution is null
				try
				{
					flattenedSolution.setSolution(getActiveProject().getSolution().getSolutionMetaData(), true, true, getActiveSolutionHandler());
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				catch (RemoteException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return flattenedSolution;
	}

	protected void updateFlattenedSolution(boolean forceUpdate)
	{
		if (flattenedSolution != null)
		{
			synchronized (flattenedSolution)
			{
				if (!forceUpdate && activeProject != null && flattenedSolution.getSolution() != null &&
					flattenedSolution.getSolution().equals(activeProject.getSolution()))
				{
					flattenedSolution.reload();
					return;
				}
				try
				{
					flattenedSolution.close(null);
					if (activeProject != null && activeProject.getSolution() != null)
					{
						flattenedSolution.setSolution(activeProject.getSolution().getSolutionMetaData(), false, true, getActiveSolutionHandler());
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	public boolean isFlattenedSolutionLoaded()
	{
		return flattenedSolution != null && flattenedSolution.isMainSolutionLoaded();
	}

	public BaseNGPackageManager getNGPackageManager()
	{
		return ngPackageManager;
	}

	protected abstract BaseNGPackageManager createNGPackageManager();

	public IActiveSolutionHandler getActiveSolutionHandler()
	{
		if (activeSolutionHandler == null)
		{
			activeSolutionHandler = createActiveSolutionHandler();
		}
		return activeSolutionHandler;
	}

	protected IActiveSolutionHandler createActiveSolutionHandler()
	{
		IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
		return new AbstractActiveSolutionHandler(as)
		{
			@Override
			public IRepository getRepository()
			{
				return ApplicationServerRegistry.get().getDeveloperRepository();
			}

			@Override
			protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
			{
				ServoyProject servoyProject = getServoyProject(solutionDef.getName());
				if (servoyProject != null)
				{
					return servoyProject.getSolution();
				}
				return null;
			}

			@Override
			protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef)
				throws RemoteException, RepositoryException
			{
				return loadSolution(loginSolutionDef);
			}
		};
	}

	public void buildActiveProjects(IProgressMonitor m)
	{
		buildActiveProjects(m, false);
	}

	public void buildActiveProjects(IProgressMonitor m, boolean onlyServoyBuild)
	{
		try
		{
			ServoyProject[] modules = getModulesOfActiveProject();
			SubMonitor monitor = SubMonitor.convert(m, "Building active solution...",
				3 + ((activeResourcesProject != null) ? 7 : 0) + ((modules != null) ? modules.length * 10 : 0));
			ServoyBuilder.deleteAllBuilderMarkers();
			monitor.internalWorked(3);
			if (modules != null)
			{
				for (ServoyProject module : modules)
				{
					SubMonitor cm = monitor.newChild(10);
					if (module.getProject() != null)
					{
						if (onlyServoyBuild)
						{
							module.getProject().build(IncrementalProjectBuilder.FULL_BUILD, ServoyBuilder.BUILDER_ID, null, cm);
						}
						else
						{
							module.getProject().build(IncrementalProjectBuilder.FULL_BUILD, cm);
						}
					}
				}
			}
			if (activeResourcesProject != null)
			{
				activeResourcesProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor.newChild(7));
			}
			monitor.done();
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	/**
	 *  Returns true if the given solution name is the active solution or part of the active
	 *  solution's non-import hook modules.
	 */
	public boolean isSolutionActive(String name)
	{
		if (activeProject != null)
		{
			// first check it is not just the main solution.
			if (name.equals(getFlattenedSolution().getName())) return true;
			// check all the modules (these dont include the import hooks)
			Solution[] modules = getFlattenedSolution().getModules();
			if (modules == null)
			{
				return false;
			}
			for (Solution solution : modules)
			{
				if (name.equals(solution.getName()))
				{
					return true;
				}
			}
			// now check all modules (including module hooks)
			if (hasModuleName(getFlattenedSolution().getSolution(), name)) return true;
			for (Solution solution : modules)
			{
				if (hasModuleName(solution, name)) return true;
			}
		}
		return false;
	}

	private boolean hasModuleName(Solution sol, String name)
	{
		String[] moduleNames = Utils.getTokenElements(sol.getModulesNames(), ",", true);
		Arrays.sort(moduleNames);
		return Arrays.binarySearch(moduleNames, name) >= 0;
	}

	/**
	 * Checks whether or not the solution with given name is or should be a module of the active solution.<br>
	 * It checks modules listed in all current modules of flattened solution + import hook modules; it is able to detect modules that are not part of the actual flattened solution yet, without actually loading them (so for example solutions that the active solution or it's modules listed as a module but was not valid/present previously).
	 */
	public boolean shouldBeModuleOfActiveSolution(String searchForName)
	{
		if (activeProject != null)
		{
			ServoyProject[] modules = getModulesOfActiveProject();
			for (ServoyProject spm : modules)
			{
				Solution s = spm.getSolution();
				if (s != null)
				{
					String[] moduleNames = Utils.getTokenElements(s.getModulesNames(), ",", true);
					for (String mn : moduleNames)
					{
						if (searchForName.equals(mn))
						{
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public AtomicIntegerWithListener getResourceChangesHandlerCounter()
	{
		return resourceChangesHandlerCounter;
	}

	public EclipseMessages getMessagesManager()
	{
		return messagesManager;
	}

	public void reportSaveError(Exception e)
	{
		ServoyLog.logError(e);
	}

	@Override
	public IServerInternal getMemServer(String tablename)
	{
		MemTable table = findInMemITable(tablename);
		if (table != null) return table.getParent();
		return null;
	}

	private MemTable findInMemITable(String tablename)
	{

		MemServer memServer = getActiveProject().getMemServer();
		try
		{
			MemTable table = memServer.getTable(tablename);
			if (table != null) return table;
			ServoyProject[] modulesOfActiveProject = getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				table = servoyProject.getMemServer().getTable(tablename);
				if (table != null) return table;
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}

	private ViewFoundsetTable findViewITable(String tablename)
	{

		ViewFoundsetsServer memServer = getActiveProject().getViewFoundsetsServer();
		try
		{
			ViewFoundsetTable table = memServer.getTable(tablename);
			if (table != null) return table;
			ServoyProject[] modulesOfActiveProject = getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				table = servoyProject.getViewFoundsetsServer().getTable(tablename);
				if (table != null) return table;
			}
		}
		catch (RepositoryException e)
		{
			Debug.error(e);
		}
		return null;
	}


	public void dispose()
	{
		if (ngPackageManager != null)
		{
			ngPackageManager.dispose();
			ngPackageManager = null;
		}
	}

}