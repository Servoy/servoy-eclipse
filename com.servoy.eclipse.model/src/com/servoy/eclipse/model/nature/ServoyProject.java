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

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ErrorKeeper;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.extensions.IUnexpectedSituationHandler;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.AbstractActiveSolutionHandler;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * models servoy solution as project
 * 
 * @author jblok
 */
public class ServoyProject implements IProjectNature, ErrorKeeper<File, Exception>
{
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyProject"; //$NON-NLS-1$

	private IProject project;
	private Solution editingSolution;// working copy for editing
	private FlattenedSolution editingFlattenedSolution;

	private final HashMap<File, Exception> deserializeExceptions = new HashMap<File, Exception>();

	public ServoyProject()
	{
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
		IDeveloperRepository repository = ApplicationServerSingleton.get().getDeveloperRepository();
		if (repository != null)
		{
			try
			{
				return ((EclipseRepository)repository).isSolutionLoaded(project.getName());
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
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
		IDeveloperRepository repository = ApplicationServerSingleton.get().getDeveloperRepository();
		if (repository != null)
		{
			try
			{
				solution = (Solution)repository.getActiveRootObject(project.getName(), IRepository.SOLUTIONS);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Cannot get solution object for project " + project.getName(), e); //$NON-NLS-1$
			}
		}
		else
		{
			List<IUnexpectedSituationHandler> l = ResourcesUtils.getExtensions(IUnexpectedSituationHandler.EXTENSION_ID);
			for (IUnexpectedSituationHandler e : l)
			{
				e.cannotFindRepository();
			}
			ServoyLog.logError("Repository error. Cannot find Servoy Eclipse repository.", null); //$NON-NLS-1$
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

		for (int i = 0; i < commands.length; ++i)
		{
			if (commands[i].getBuilderName().equals(ServoyBuilder.BUILDER_ID))
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
		return this == ServoyModelFinder.getServoyModel().getActiveProject();
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
				ServoyLog.logError("Could not create a working copy of solution " + getSolution().getName(), e); //$NON-NLS-1$
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
				throw new RepositoryException("Object to save not found in solution"); //$NON-NLS-1$
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
					if (src instanceof IScriptProvider || src instanceof IVariable)
					{
						((AbstractBase)dest).setSerializableRuntimeProperty(IScriptProvider.LINENUMBER,
							((AbstractBase)src).getSerializableRuntimeProperty(IScriptProvider.LINENUMBER));
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

					if (recursive && src instanceof ISupportChilds)
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

					return recursive ? IPersistVisitor.CONTINUE_TRAVERSAL : IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
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
	 * 
	 * @param nodes
	 * @param recursive
	 */
	public void saveEditingSolutionNodes(final IPersist[] nodes, final boolean recursive, boolean runAsJob) throws RepositoryException
	{
		if (nodes == null) return;

		if (getSolution() == null)
		{
			throw new RepositoryException("Cannot load solution"); //$NON-NLS-1$
		}
		for (IPersist node : nodes)
		{
			IPersist searchNode = AbstractRepository.searchPersist(getEditingSolution(), node);
			if (searchNode != null /* object was deleted */&& searchNode != node)
			{
				throw new RepositoryException("Object to save is out of sync"); //$NON-NLS-1$
			}
		}

		EclipseRepository repository = (EclipseRepository)ApplicationServerSingleton.get().getDeveloperRepository();
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
				ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e); //$NON-NLS-1$
			}
		}
		return null;
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

	public IPersist getEditingPersist(UUID uuid) throws RepositoryException
	{
		if (getEditingSolution() == null) return null;

		IPersist editingPersist = AbstractRepository.searchPersist(getEditingSolution(), uuid);
		if (editingPersist == null)
		{
			ISupportChilds parent = null;
			if (editingPersist != null)
			{
				// remove child from parent in editing solution; if found in orig solution, it will be added again
				parent = editingPersist.getParent();
			}

			IPersist original = AbstractRepository.searchPersist(getSolution(), uuid);
			if (original == null)
			{
				if (editingPersist != null && parent != null)
				{
					// persist is deleted.
					parent.removeChild(editingPersist);
				}
				return null;
			}
			editingPersist = copyNodeToEditingSolution(original, true);
		}
		return editingPersist;
	}

	public synchronized FlattenedSolution getEditingFlattenedSolution(boolean loadLoginSolution, boolean loadMainSolution)
	{
		try
		{
			if (editingFlattenedSolution == null)
			{
				if (getEditingSolution() == null) return null;
				editingFlattenedSolution = new FlattenedSolution(true); // flattened form cache will be flushed by ServoyModel when persists change.model
			}
			if (editingFlattenedSolution.getSolution() == null)
			{
				// new or flattened solution was reset
				editingFlattenedSolution.setSolution(getEditingSolution().getSolutionMetaData(), loadLoginSolution, loadMainSolution,
					new AbstractActiveSolutionHandler()
					{
						@Override
						public IRepository getRepository()
						{
							return ApplicationServerSingleton.get().getDeveloperRepository();
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
						protected Solution loadLoginSolution(SolutionMetaData mainSolutionDef, SolutionMetaData loginSolutionDef) throws RemoteException,
							RepositoryException
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

	public synchronized FlattenedSolution getEditingFlattenedSolution()
	{
		return getEditingFlattenedSolution(true, true);
	}

	public synchronized void resetEditingFlattenedSolution(boolean loadLoginSolution, boolean loadMainSolution)
	{
		if (this.editingFlattenedSolution != null)
		{
			editingFlattenedSolution.close(null);
			// do not set editingFlattenedSolution to null, in several places a reference is kept (like in FormLabelProvider)
			// make sure the editing flattened solution is filled ok, otherwise references to it cannot use it.
			getEditingFlattenedSolution(loadLoginSolution, loadMainSolution);
		}
	}


	public List<String> getGlobalScopenames()
	{
		List<String> scopeNames = new ArrayList<String>();
		scopeNames.add(ScriptVariable.GLOBAL_SCOPE);
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

	public void addError(File badObject, Exception error)
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
	public HashMap<File, Exception> getDeserializeExceptions()
	{
		return deserializeExceptions;
	}

	/**
	 * @return
	 */
	public SolutionMetaData getSolutionMetaData()
	{
		IDeveloperRepository repository = ApplicationServerSingleton.get().getDeveloperRepository();
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
}
