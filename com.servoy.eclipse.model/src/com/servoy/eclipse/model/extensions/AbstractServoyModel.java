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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.AtomicIntegerWithListener;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.Messages;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
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

	private FlattenedSolution flattenedSolution;
	private IActiveSolutionHandler activeSolutionHandler;

	private final EclipseMessages messagesManager;

	private final AtomicIntegerWithListener resourceChangesHandlerCounter = new AtomicIntegerWithListener();

	public AbstractServoyModel()
	{
		messagesManager = new EclipseMessages();
		Messages.customMessageLoader = messagesManager;
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
	 * The result does not include the active solution's import hooks (which are not part of the flattened solution).
	 * 
	 * @return an array containing the modules of the active project.
	 * @see #getModulesOfActiveProjectWithImportHooks()
	 */
	public ServoyProject[] getModulesOfActiveProject()
	{
		// the set of solutions a user can work with at a given time is determined by the active solution;
		// this means that the only expandable solution nodes will be the active solution and it's referenced modules;
		// all other solutions will appear grayed-out and not-expandable (still allows the user to activate them if necessary)

		List<ServoyProject> moduleProjects = new ArrayList<ServoyProject>();
		addFlattenedSolutionModules(moduleProjects);
		return moduleProjects.toArray(new ServoyProject[moduleProjects.size()]);
	}

	/**
	 * Usually you would use {@link #getModulesOfActiveProject()} or {@link #getModulesOfActiveProjectWithImportHooks()} instead.
	 */
	public void addFlattenedSolutionModules(List<ServoyProject> moduleProjects)
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

	/**
	 * @see #getModulesOfActiveProject()
	 */
	public ServoyProject[] getModulesOfActiveProjectWithImportHooks()
	{
		List<ServoyProject> allModules = new ArrayList<ServoyProject>();
		addFlattenedSolutionModules(allModules);
		addImportHookModules(getActiveProject(), allModules);
		return allModules.toArray(new ServoyProject[allModules.size()]);
	}

	public ServoyProject[] getImportHookModulesOfActiveProject()
	{
		List<ServoyProject> importHookModules = new ArrayList<ServoyProject>();
		addImportHookModules(getActiveProject(), importHookModules);
		return importHookModules.toArray(new ServoyProject[importHookModules.size()]);
	}

	/**
	 * Usually you would use {@link #getModulesOfActiveProjectWithImportHooks()} or {@link #getImportHookModulesOfActiveProject()} instead.
	 */
	public void addImportHookModules(ServoyProject p, List<ServoyProject> importHookModules)
	{
		addImportHookModules(p, importHookModules, new HashSet<ServoyProject>());
	}

	private void addImportHookModules(ServoyProject p, List<ServoyProject> importHookModules, Set<ServoyProject> visited)
	{
		if (p != null && !visited.contains(p))
		{
			visited.add(p);
			Solution s = p.getSolution();
			if (s != null)
			{
				if (SolutionMetaData.isImportHook(s.getSolutionMetaData())) importHookModules.add(p);

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

	public DataModelManager getDataModelManager()
	{
		return dataModelManager;
	}

	protected FlattenedSolution createFlattenedSolution()
	{
		return new FlattenedSolution();
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

	protected void updateFlattenedSolution()
	{
		if (flattenedSolution != null)
		{
			try
			{
				flattenedSolution.close(null);
				if (activeProject != null && activeProject.getSolution() != null)
				{
					flattenedSolution.setSolution(activeProject.getSolution().getSolutionMetaData(), true, true, getActiveSolutionHandler());
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

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
		return new AbstractActiveSolutionHandler()
		{
			@Override
			public IRepository getRepository()
			{
				return ApplicationServerSingleton.get().getDeveloperRepository();
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
			protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef) throws RemoteException,
				RepositoryException
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
			SubMonitor monitor = SubMonitor.convert(m, "Building active solution...", 3 + ((activeResourcesProject != null) ? 7 : 0) +
				((modules != null) ? modules.length * 10 : 0));
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
		ServoyProject[] activeModules = getModulesOfActiveProject();
		for (ServoyProject p : activeModules)
		{
			if (p.getProject().getName().equals(name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 *  Returns true if the given solution name matches one of the import hook modules of the active solution.
	 */
	public boolean isSolutionActiveImportHook(String name)
	{
		ServoyProject[] activeModules = getImportHookModulesOfActiveProject();
		for (ServoyProject p : activeModules)
		{
			if (p.getProject().getName().equals(name))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether or not the solution with given name is or should be a module of the active solution.<br>
	 * It checks modules listed in all current modules of flattened solution + import hook modules; it is able to detect modules that are not part of the actual flattened solution yet, without actually loading them (so for example solutions that the active solution or it's modules listed as a module but was not valid/present previously).
	 */
	public boolean shouldBeModuleOfActiveSolution(String searchForName)
	{
		if (activeProject != null)
		{
			ServoyProject[] modules = getModulesOfActiveProjectWithImportHooks();
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
}