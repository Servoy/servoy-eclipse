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
package com.servoy.eclipse.model.nature;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.DeveloperFlattenedSolution;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ErrorKeeper;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.view.ViewFoundsetsServer;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServer;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * models servoy solution as project
 *
 * @author jblok
 */
public class ServoyProject implements IProjectNature, ErrorKeeper<File, String>, Comparable<ServoyProject>
{
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyProject";

	private IProject project;
	private Solution editingSolution;// working copy for editing
	private FlattenedSolution editingFlattenedSolution;
	// only use for hooks, servoy model FS does not contain the references
	private FlattenedSolution flattenedSolution;

	private final HashMap<File, String> deserializeExceptions = new HashMap<File, String>();

	private MemServer memServer = null;
	private ViewFoundsetsServer viewServer = null;

	public ServoyProject()
	{
		// constructor for when it is created via Project.getNature(natureID)
		// setProject will be called later by NatureManager.createNature() that is called by Project.getNature(natureID)
	}

	ServoyProject(IProject project)
	{
		this.project = project;
	}

	/**
	 * Convenience method that delegates to repository.
	 */
	public boolean isSolutionLoaded()
	{
		IDeveloperRepository repository = ApplicationServerRegistry.get().getDeveloperRepository();
		if (repository != null)
		{
			return ((EclipseRepository)repository).isSolutionLoaded(project.getName());
		}
		return false;
	}

	/**
	 * This method gets the solution for a ServoyProject. If the solution is not yet read from disk it will be deserialized now.
	 * NOTE: Do not call this if you do not need to deserialize the solution of this project.
	 * There are cases in which you can ignore the solution if it's not already deserialized.
	 * @see #isSolutionLoaded()
	 * @return
	 */
	public Solution getSolution()
	{
		Solution solution = null;
		IApplicationServerSingleton registry = ApplicationServerRegistry.get();
		if (registry != null) // otherwise NPE can happen if during eclipse shutdown a DLTK build is running for some reason
		{
			IDeveloperRepository repository = registry.getDeveloperRepository();
			if (repository != null)
			{
				try
				{
					solution = (Solution)repository.getActiveRootObject(project.getName(), IRepository.SOLUTIONS);
				}
				catch (Exception e)
				{
					ServoyLog.logError("Cannot get solution object for project " + project.getName(), e);
				}
			}
			else
			{
				ModelUtils.getUnexpectedSituationHandler().cannotFindRepository();
				ServoyLog.logError("Repository error. Cannot find Servoy Eclipse repository.", null);
			}
		}
		return solution;
	}

	public Solution[] getModules()
	{
		ArrayList<Solution> modules = new ArrayList<Solution>();
		Solution solution = getSolution();
		if (solution != null) getModules(solution.getName(), modules);

		return modules.toArray(new Solution[modules.size()]);
	}

	private void getModules(String solutionName, ArrayList<Solution> modules)
	{
		IProject solutionProject = ResourcesPlugin.getWorkspace().getRoot().getProject(solutionName);
		try
		{
			Solution solution;
			if (solutionProject != null && solutionProject.isOpen() && solutionProject.hasNature(ServoyProject.NATURE_ID) &&
				(solution = ((ServoyProject)solutionProject.getNature(ServoyProject.NATURE_ID)).getSolution()) != null && modules.indexOf(solution) == -1)
			{
				modules.add(solution);
				String[] solutionModules = Utils.getTokenElements(solution.getModulesNames(), ",", true);
				if (solutionModules != null)
				{
					for (String module : solutionModules)
						getModules(module, modules);
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
	}

	public void configure() throws CoreException
	{
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		for (ICommand command : commands)
		{
			if (command.getBuilderName().equals(ServoyBuilder.BUILDER_ID))
			{
				return;
			}
		}

		ICommand[] newCommands = new ICommand[commands.length + 1];
		System.arraycopy(commands, 0, newCommands, 0, commands.length);
		ICommand command = desc.newCommand();
		command.setBuilderName(ServoyBuilder.BUILDER_ID);
		newCommands[newCommands.length - 1] = command;
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
	}

	public void deconfigure() throws CoreException
	{
		IProjectDescription description = project.getDescription();
		ICommand[] commands = description.getBuildSpec();
		for (int i = 0; i < commands.length; ++i)
		{
			if (commands[i].getBuilderName().equals(ServoyBuilder.BUILDER_ID))
			{
				ICommand[] newCommands = new ICommand[commands.length - 1];
				System.arraycopy(commands, 0, newCommands, 0, i);
				System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
				description.setBuildSpec(newCommands);
				project.setDescription(description, null);
				return;
			}
		}
	}

	/**
	 * Returns true if this is the currently active solution project in the workspace and false otherwise.
	 *
	 * @return true if this is the currently active solution project in the workspace and false otherwise.
	 */
	public boolean isActive()
	{
		ServoyProject ap = ServoyModelFinder.getServoyModel().getActiveProject();
		return project != null && ap != null && (ap == this || project.equals(ap.getProject())); // resource changes can end up creating different ServoyProject or even IProject instances for the same project; most of the time it does not matter as ServoyModel handles those POST_CHANGE events first, but still
	}

	public IProject getProject()
	{
		return project;
	}

	public void setProject(IProject project)
	{
		this.project = project;
	}

	private boolean loadingEditingSolution = false;

	/**
	 * Get a working copy of the solution. Changes to this solution will not affect the real solution.
	 *
	 * @return
	 */
	public Solution getEditingSolution()
	{
		if (editingSolution == null && !loadingEditingSolution)
		{
			loadingEditingSolution = true;
			try
			{
				if (getSolution() != null)
				{
					editingSolution = ((AbstractRepository)getSolution().getRepository()).createSolutionCopy(getSolution());
					copyNodeToEditingSolution(getSolution(), true);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not create a working copy of solution " + getSolution().getName(), e);
			}
			finally
			{
				loadingEditingSolution = false;
			}
		}
		return editingSolution;
	}

	/**
	 * Get a working copy of the node in the editing solution. Changes to this solution will not affect the real solution.
	 *
	 * @return
	 * @throws RepositoryException
	 */
	protected IPersist copyNodeToEditingSolution(IPersist srcNode, final boolean recursive) throws RepositoryException
	{
		IPersist destNode = AbstractRepository.searchPersist(editingSolution, srcNode);
		if (destNode == null)
		{
			// the node to save was a new node, add the parent first
			if (srcNode.getParent() == null)
			{
				throw new RepositoryException("Object to save not found in solution");
			}
			// copy the parent over if needed
			if (AbstractRepository.searchPersist(editingSolution, srcNode.getParent()) == null)
			{
				copyNodeToEditingSolution(srcNode.getParent(), false);// recursive=false, we do not want to save all siblings of node
			}
		}

		try
		{
			srcNode.acceptVisitor(new IPersistVisitor()
			{
				private ISupportChilds parent = editingSolution;
				private final boolean hasChilds = parent.getAllObjects().hasNext();

				public Object visit(IPersist src)
				{
					boolean goDeeperInItems = (recursive && src instanceof ISupportChilds &&
						SolutionSerializer.isCompositeWithIndependentSerializationOfSubItems(src));

					ISupportChilds currentParent = parent;
					while (!(currentParent instanceof Solution) && !currentParent.getUUID().equals(src.getParent().getUUID()))
					{
						currentParent = currentParent.getParent();
					}
					IPersist dest;
					try
					{
						dest = ((AbstractRepository)editingSolution.getRepository()).copyPersistIntoSolution(src, currentParent, hasChilds);
					}
					catch (RepositoryException e)
					{
						throw new RuntimeException(e);
					}
					if (src instanceof Media)
					{
						// media bytes are not in content spec
						((Media)dest).setPermMediaData(((Media)src).getMediaData());
					}
					if (src instanceof IScriptElement)
					{
						((AbstractBase)dest).setSerializableRuntimeProperty(IScriptProvider.FILENAME,
							((AbstractBase)src).getSerializableRuntimeProperty(IScriptProvider.FILENAME));
						((AbstractBase)dest).setSerializableRuntimeProperty(IScriptProvider.TYPE,
							((AbstractBase)src).getSerializableRuntimeProperty(IScriptProvider.TYPE));
					}
					if (src instanceof ScriptVariable)
					{
						((ScriptVariable)dest).setComment(((ScriptVariable)src).getComment());
					}
					editingSolution.clearEditingState(dest);

					if (goDeeperInItems)
					{
						/// parent for the next time.
						parent = (ISupportChilds)dest;
						if (!(dest instanceof AbstractBase && ((AbstractBase)dest).getAllObjectsAsList().size() == 0))
						{
							// remove the children that are in editing persist and not in real one.
							Iterator<IPersist> srcChildren = ((ISupportChilds)src).getAllObjects();
							Set<UUID> uuids = new HashSet<UUID>();
							while (srcChildren.hasNext())
							{
								uuids.add(srcChildren.next().getUUID());
							}

							Iterator<IPersist> destChildren = ((ISupportChilds)dest).getAllObjects();
							List<IPersist> removedChildren = new ArrayList<IPersist>();
							while (destChildren.hasNext())
							{
								IPersist child = destChildren.next();
								if (!uuids.contains(child.getUUID()))
								{
									removedChildren.add(child);
								}
							}

							for (IPersist removed : removedChildren)
							{
								((ISupportChilds)dest).removeChild(removed);
							}
						}
					}

					return goDeeperInItems ? IPersistVisitor.CONTINUE_TRAVERSAL : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
			});
		}
		catch (RuntimeException re)
		{
			if (re.getCause() instanceof RepositoryException) throw (RepositoryException)re.getCause();
			throw re;
		}
		if (destNode == null)
		{
			destNode = AbstractRepository.searchPersist(editingSolution, srcNode);
		}
		return destNode;
	}

	public void saveEditingSolutionNodes(IPersist[] nodes, boolean recursive) throws RepositoryException
	{
		saveEditingSolutionNodes(nodes, recursive, true);
	}

	/**
	 * Apply all changes in editingSolution to the solution, starting from nodes.
	 */
	public void saveEditingSolutionNodes(final IPersist[] nodes, final boolean recursive, boolean runAsJob) throws RepositoryException
	{
		if (nodes == null) return;

		if (getSolution() == null)
		{
			throw new RepositoryException("Cannot load solution");
		}
		for (IPersist node : nodes)
		{
			IPersist searchNode = AbstractRepository.searchPersist(getEditingSolution(), node);
			if (searchNode != null /* object was not deleted */ && searchNode != node)
			{
				throw new RepositoryException("Object to save is out of sync");
			}
		}

		EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
		if (runAsJob)
		{
			repository.updateNodesInWorkspace(nodes, recursive);
		}
		else
		{
			repository.updateNodes(nodes, recursive);
		}
	}

	/**
	 * Returns the ServoyResourcesProject instance that this project references. Returns null if there is no such reference or if there is more than one project
	 * that matches this criteria.
	 *
	 * @return the ServoyResourcesProject instance that this project references.
	 */
	public ServoyResourcesProject getResourcesProject()
	{
		if (project.exists() && project.isOpen())
		{
			try
			{
				final IProject[] referencedProjects = project.getDescription().getReferencedProjects();
				final ArrayList<ServoyResourcesProject> resourcesProjects = new ArrayList<ServoyResourcesProject>();
				for (IProject p : referencedProjects)
				{
					if (p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						resourcesProjects.add((ServoyResourcesProject)p.getNature(ServoyResourcesProject.NATURE_ID));
					}
				}

				if (resourcesProjects.size() == 1)
				{
					return resourcesProjects.get(0);
				}
				// if size > 1 an error marker will be set on the project + a quick fix provided
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e);
			}
		}
		return null;
	}

	/**
	 * Returns the ServoyNGPackageProjects that this project references. Returns an empty array if there is no such reference.
	 *
	 * @return the ServoyNGPackageProjects that this project references.
	 */
	public ServoyNGPackageProject[] getNGPackageProjects()
	{
		final ArrayList<ServoyNGPackageProject> ngPackageProjects = new ArrayList<ServoyNGPackageProject>();
		if (project.exists() && project.isOpen())
		{
			try
			{
				final IProject[] referencedProjects = project.getDescription().getReferencedProjects();
				for (IProject p : referencedProjects)
				{
					if (p.exists() && p.isOpen() && p.hasNature(ServoyNGPackageProject.NATURE_ID))
					{
						ngPackageProjects.add((ServoyNGPackageProject)p.getNature(ServoyNGPackageProject.NATURE_ID));
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Exception while reading referenced projects (ngp) for " + project.getName(), e);
			}
		}
		return ngPackageProjects.toArray(new ServoyNGPackageProject[ngPackageProjects.size()]);
	}

	/**
	 * Push the version of the persist in the real solution to the in-memory editing solution.
	 *
	 * @param searchPersist
	 * @param recursive
	 * @return
	 * @throws RepositoryException
	 */
	public IPersist updateEditingPersist(IPersist searchPersist, boolean recursive) throws RepositoryException
	{
		if (getEditingSolution() == null) return null;

		IPersist editingPersist = AbstractRepository.searchPersist(getEditingSolution(), searchPersist);
		ISupportChilds parent = null;
		if (editingPersist != null)
		{
			parent = editingPersist.getParent();
		}

		IPersist original = AbstractRepository.searchPersist(getSolution(), searchPersist);
		if (original == null)
		{
			if (editingPersist != null && parent != null)
			{
				// persist is deleted.
				parent.removeChild(editingPersist);
			}
		}
		else
		{
			editingPersist = copyNodeToEditingSolution(original, recursive);
		}
		return editingPersist;
	}

	public IPersist getEditingPersist(UUID uuid)
	{
		if (getEditingSolution() == null) return null;

		return AbstractRepository.searchPersist(getEditingSolution(), uuid);
	}

	public synchronized FlattenedSolution getEditingFlattenedSolution(boolean loadMainSolution)
	{
		try
		{
			if (editingFlattenedSolution == null)
			{
				if (getEditingSolution() == null) return null;
				editingFlattenedSolution = new DeveloperFlattenedSolution(true); // flattened form cache will be flushed by ServoyModel when persists change.model
			}
			if (editingFlattenedSolution.getSolution() == null)
			{
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				// new or flattened solution was reset
				editingFlattenedSolution.setSolution(getEditingSolution().getSolutionMetaData(), false, loadMainSolution, new AbstractActiveSolutionHandler(as)
				{
					@Override
					public IRepository getRepository()
					{
						return ApplicationServerRegistry.get().getDeveloperRepository();
					}

					@Override
					protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
					{
						ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionDef.getName());
						if (servoyProject != null)
						{
							return servoyProject.getEditingSolution();
						}
						return null;
					}

					@Override
					protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef)
						throws RemoteException, RepositoryException
					{
						return loadSolution(loginSolutionDef);
					}
				});
			}
		}
		catch (RemoteException e)
		{
			ServoyLog.logError(e);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return editingFlattenedSolution;
	}

	/**
	 * This should only be called if you really need to none editing flattened solution of this specific Project (solution)
	 * Normally you would use ServoyModel.getFlattenedSolution() instead.
	 * So only if you need to by pass this, for example you need to he FS onyl for the HookModule then call this.
	 * Because this will result in a FS being cached for this specific solution, we should avoid doing that over all modules a solution has.
	 *
	 * @return The FlattenedSolution for this specific Project/Solution
	 */
	public FlattenedSolution getFlattenedSolution()
	{
		try
		{
			if (flattenedSolution == null)
			{
				flattenedSolution = new DeveloperFlattenedSolution(true); // flattened form cache will be flushed by ServoyModel when persists change.model
			}
			if (flattenedSolution.getSolution() == null)
			{
				IApplicationServer as = ApplicationServerRegistry.getService(IApplicationServer.class);
				// new or flattened solution was reset
				flattenedSolution.setSolution(getSolution().getSolutionMetaData(), false, true, new AbstractActiveSolutionHandler(as)
				{
					@Override
					public IRepository getRepository()
					{
						return ApplicationServerRegistry.get().getDeveloperRepository();
					}

					@Override
					protected Solution loadSolution(RootObjectMetaData solutionDef) throws RemoteException, RepositoryException
					{
						ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionDef.getName());
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
				});
			}
		}
		catch (RemoteException e)
		{
			ServoyLog.logError(e);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return flattenedSolution;
	}

	public synchronized FlattenedSolution getEditingFlattenedSolution()
	{
		return getEditingFlattenedSolution(true);
	}

	public synchronized void resetFlattenedSolution(boolean loadMainSolution)
	{
		if (this.editingFlattenedSolution != null)
		{
			editingFlattenedSolution.close(null);
			// do not set editingFlattenedSolution to null, in several places a reference is kept (like in FormLabelProvider)
			// make sure the editing flattened solution is filled ok, otherwise references to it cannot use it.
			getEditingFlattenedSolution(loadMainSolution);
		}
	}


	public List<String> getGlobalScopenames()
	{
		List<String> scopeNames = new ArrayList<String>();
		if (project != null)
		{
			try
			{
				// look for js files in the project directory
				for (IResource member : getProject().members())
				{
					if (member instanceof IFile && member.getName().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
					{
						String scopeName = member.getName().substring(0, member.getName().length() - SolutionSerializer.JS_FILE_EXTENSION.length());
						if (!scopeNames.contains(scopeName)) scopeNames.add(scopeName);
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Could not list members for project '" + this + '\'', e);
			}
		}
		return scopeNames;
	}


	@Override
	public String toString()
	{
		return project != null ? project.getName() : super.toString();
	}

	public void addError(File badObject, String error)
	{
		deserializeExceptions.put(badObject, error);
	}

	public void removeError(File badObject)
	{
		deserializeExceptions.remove(badObject);
	}

	/**
	 * Returns a map of exceptions encountered during deserialization.
	 *
	 * @return a map of exceptions encountered during deserialization.
	 */
	public HashMap<File, String> getDeserializeExceptions()
	{
		return deserializeExceptions;
	}

	public SolutionMetaData getSolutionMetaData()
	{
		IDeveloperRepository repository = ApplicationServerRegistry.get().getDeveloperRepository();
		try
		{
			return (SolutionMetaData)repository.getRootObjectMetaData(project.getName(), IRepository.SOLUTIONS);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	public MemServer getMemServer()
	{
		if (memServer == null) refreshMemServer();
		return memServer;
	}

	public void refreshMemServer()
	{
		if (memServer == null) memServer = new MemServer(this);
		else memServer.init();
	}

//	/**
//	 * Returns a list of all IProjects referenced by this solution's project (both static references (.project) and dynamic ones (dltk js build refs)) in-depth, including the solution project itself.
//	 * So it should return main solution project, could return all module projects (BUT SOMETIMES this is wrong because solution modules are specified via a solution property
//	 * not project references), resources projects, all web package projects of all found modules and any other project that is referenced.<br/><br/>
//	 *
//	 * DO NOT count on this method returning all modules, as modules might not be referenced and still work.
//	 *
//	 * @return the full list of referenced projects no matter how deep the nesting is (including this solution's project).
//	 * @throws CoreException if something went wrong.
//	 */
//	public List<IProject> getAllReferencedProjectsIdDepth() throws CoreException
//	{
//		List<IProject> allReferencedProjects = new ArrayList<>();
//		fillAllReferencedProjects(getProject(), allReferencedProjects);
//		return allReferencedProjects;
//	}
//
//	public static void fillAllReferencedProjects(IProject project, List<IProject> allReferencedProjects) throws CoreException
//	{
//		if (allReferencedProjects.indexOf(project) != -1) return; // TODO change this list into a hashmap so that it's faster?
//		allReferencedProjects.add(project);
//		if (project.isAccessible()) for (IProject iProject : project.getReferencedProjects())
//		{
//			fillAllReferencedProjects(iProject, allReferencedProjects);
//		}
//	}

	/**
	 * Returns a list of all IProjects referenced by this solution's project (both static references (.project) and dynamic ones (dltk js build refs)) and it's modules, in-depth, including the solution project itself and it's modules.
	 * Modules are computed correctly via solution property instead of via project references which are not reliable.
	 *
	 * @return the full list of referenced projects no matter how deep the module nesting is (including this solution's project); only goes in depth in modules.
	 * @throws CoreException if something went wrong.
	 */
	public List<IProject> getSolutionAndModuleReferencedProjects() throws CoreException
	{
		List<IProject> allSolutionAndModuleReferencedProjects = new ArrayList<>();
		Set<ServoyProject> visitedModules = new HashSet<>(); // this has to be separate because is a module is also a referenced project we still want to get that module's references even if it was adeed before to the list because if was a project reference of another solution
		fillSolutionAndModulenReferencedProjectsIdDepth(this, visitedModules, allSolutionAndModuleReferencedProjects);
		return allSolutionAndModuleReferencedProjects;
	}

	public static void fillSolutionAndModulenReferencedProjectsIdDepth(ServoyProject servoyProject, Set<ServoyProject> visitedModules,
		List<IProject> allSolutionAndModuleReferencedProjects) throws CoreException
	{
		if (visitedModules.contains(servoyProject)) return;
		visitedModules.add(servoyProject);

		if (!allSolutionAndModuleReferencedProjects.contains(servoyProject.getProject()))
			allSolutionAndModuleReferencedProjects.add(servoyProject.getProject());

		// add all direct project references
		for (IProject iProject : servoyProject.getProject().getReferencedProjects())
		{
			allSolutionAndModuleReferencedProjects.add(iProject);
		}

		for (Solution module : servoyProject.getModules())
		{
			fillSolutionAndModulenReferencedProjectsIdDepth(
				(ServoyProject)servoyProject.getProject().getWorkspace().getRoot().getProject(module.getName()).getNature(ServoyProject.NATURE_ID),
				visitedModules, allSolutionAndModuleReferencedProjects);
		}
	}

	@Override
	public int compareTo(ServoyProject o)
	{
		return project.getName().compareTo(o.getProject().getName());
	}

	public ViewFoundsetsServer getViewFoundsetsServer()
	{
		if (viewServer == null) refreshViewServer();
		return viewServer;
	}

	public void refreshViewServer()
	{
		if (viewServer == null) viewServer = new ViewFoundsetsServer(this);
		else viewServer.init();
	}

	/**
	 * @return
	 */
	public String[] getDeveloperProjectNames()
	{
		if (project.exists() && project.isOpen())
		{
			try
			{
				final IProject[] referencedProjects = project.getDescription().getReferencedProjects();
				final ArrayList<String> developerProjects = new ArrayList<String>();
				for (IProject p : referencedProjects)
				{
					if (p.exists() && p.isOpen() && p.hasNature(ServoyDeveloperProject.NATURE_ID))
					{
						developerProjects.add(p.getName());
					}
				}
				return developerProjects.toArray(new String[developerProjects.size()]);
			}
			catch (CoreException e)
			{
				ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e);
			}
		}
		return null;
	}

	/**
	 * @param newDeveloperProject
	 */
	public void addDeveloperProject(IProject newDeveloperProject)
	{
		try
		{
			IProjectDescription description = project.getDescription();
			IProject[] referencedProjects = description.getReferencedProjects();
			ArrayList<IProject> newProjects = new ArrayList<IProject>(Arrays.asList(referencedProjects));
			newProjects.add(newDeveloperProject);
			description.setReferencedProjects(
				newProjects.toArray(new IProject[newProjects.size()]));
			project.setDescription(description, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while adding developer project " + newDeveloperProject.getName() + " to " + project.getName(), e);
		}

	}

}
