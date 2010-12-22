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
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;

/**
 * @author acostescu
 */
public abstract class AbstractServoyModel implements IServoyModel
{

	protected ServoyProject activeProject;
	protected ServoyResourcesProject activeResourcesProject;

	protected DataModelManager dataModelManager;
	protected Map<String, ServoyProject> servoyProjectCache;

	private FlattenedSolution flattenedSolution;
	private IActiveSolutionHandler activeSolutionHandler;


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
		reloadProjectCacheIfNecessary();
		return servoyProjectCache.values().toArray(new ServoyProject[servoyProjectCache.size()]);
	}

	public ServoyProject getServoyProject(String name)
	{
		reloadProjectCacheIfNecessary();
		return servoyProjectCache.get(name);
	}

	/**
	 * Returns an array containing the modules of the active project (including the active project). If there is no active project, will return an array of size
	 * 0.
	 * 
	 * @return an array containing the modules of the active project.
	 */
	public ServoyProject[] getModulesOfActiveProject()
	{
		// the set of solutions a user can work with at a given time is determined by the active solution;
		// this means that the only expandable solution nodes will be the active solution and it's referenced modules;
		// all other solutions will appear grayed-out and not-expandable (still allows the user to activate them if necessary

		List<ServoyProject> moduleProjects = new ArrayList<ServoyProject>();
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
		return moduleProjects.toArray(new ServoyProject[moduleProjects.size()]);
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

	private void reloadProjectCacheIfNecessary()
	{
		if (servoyProjectCache == null)
		{
			Map<String, ServoyProject> servoyProjects = new HashMap<String, ServoyProject>();
			List<ServoyProject> retval = new ArrayList<ServoyProject>();
			for (IProject project : ResourcesPlugin.getWorkspace().getRoot().getProjects())
			{
				try
				{
					if (project.isOpen() && project.hasNature(ServoyProject.NATURE_ID))
					{
						ServoyProject sp = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
						retval.add(sp);
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
	}

	public DataModelManager getDataModelManager()
	{
		return dataModelManager;
	}

	public FlattenedSolution getFlattenedSolution()
	{
		if (flattenedSolution == null)
		{
			flattenedSolution = new FlattenedSolution();

			try
			{
				if (getActiveProject() == null || getActiveProject().getSolution() == null) return flattenedSolution; // projects might give deserialize exceptions => solution is null
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

	public void buildActiveProjects(IProgressMonitor monitor)
	{
		try
		{
			ServoyBuilder.deleteAllBuilderMarkers();
			if (getModulesOfActiveProject() != null)
			{
				for (ServoyProject module : getModulesOfActiveProject())
				{
					if (module.getProject() != null)
					{
						module.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
					}
				}
			}
			if (activeResourcesProject != null)
			{
				activeResourcesProject.getProject().build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

}