/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.model.ngpackages;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.util.Pair;

/**
 * This class is responsible for refreshing loaded ng packages as needed depending on workspace resource changes.
 *
 * @author acostescu
 */
public class BaseNGPackageResourcesChangedListener implements IResourceChangeListener
{

	protected final BaseNGPackageManager baseNGPackageManager;
	protected final Set<String> allAvailableNGPackageProjectNames = new HashSet<String>();
	private final List<NGPackageResourceChangeHandler> preChangeProcessedHandlersForLater = new ArrayList<>();

	public BaseNGPackageResourcesChangedListener(BaseNGPackageManager baseNGPackageManager)
	{
		this.baseNGPackageManager = baseNGPackageManager;
		cacheAllAvailableNGPackages();
	}

	private void cacheAllAvailableNGPackages()
	{
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject iProject : projects)
		{
			try
			{
				if (iProject.isAccessible() && iProject.hasNature(ServoyNGPackageProject.NATURE_ID))
				{
					allAvailableNGPackageProjectNames.add(iProject.getName());
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event)
	{
		checkForChangesInWebPackages(event);
	}

	/**
	 * Checks for resources changes of web packages and updates the  @ResourceProvider if needed
	 */
	private void checkForChangesInWebPackages(final IResourceChangeEvent event)
	{
		NGPackageResourceChangeHandler recourceChangeHandler = new NGPackageResourceChangeHandler(event);

		try
		{
			if ((event.getType() & (IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_CLOSE)) != 0)
			{
				IProject project = (IProject)event.getResource(); // PRE_DELETE will give the project here in the event, not in the resource delta (which will be null)
				recourceChangeHandler.handleProjectChanged(project);

				// we did process the pre-delete or pre-close, but we don't fire it right now because it will trigger things like refreshing the
				// solex tree - which then end up reading stale Resource states (workspace thinks they are still there but on disk they are already deleted);
				// that can lead to exceptions for example meta-inf not existing on disk but the IResource.exists() for that says true => exception when trying to read contents

				// because of that we just keep the changes for later; when the first post-change event happens we notify those as well...
				preChangeProcessedHandlersForLater.add(recourceChangeHandler);
			}
			else
			{
				// this is then a post-change event; notify any pending/delayed PRE_DELETE or PRE_CLOSE
				for (NGPackageResourceChangeHandler x : preChangeProcessedHandlersForLater)
				{
					try
					{
						notifyChanges(x);
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
				preChangeProcessedHandlersForLater.clear();

				IResourceDelta delta = event.getDelta();
				delta.accept(recourceChangeHandler);

				// now we'll really update the loaded ng packages according to the changes detected above
				if (recourceChangeHandler.detectedChanges())
				{
					notifyChanges(recourceChangeHandler);
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void notifyChanges(final NGPackageResourceChangeHandler recourceChangeHandler)
	{
		// commented out the job for now as we can get multiple change notifications that detect new packages - but for the same resources
		// so the first one should execute (loading the new packages) before the next resource change arrives - so that it knows it doesn't have to load them again;
		// otherwise invalid error markers can appear with a package being loaded twice...
		// the job is no longer needed I thing because we already delay PRE_CLOSE and PRE_DELETE now until the first POST_CHANGE arrives; see above - so that seems to be enough

//		// just execute under a workspace rule to make sure changes to eclipse's IResource structure are finished (when renaming a backup copy of an ngpackage project back to it's original
//		// name that is referenced by modules of active solutions it seems that refreshes on UI thread see the project that is already deleted on disk as still accessible and existing
//		// and try to read from it)
//		Job j = new Job("Broadcasting ng package changes...")
//		{
//
//			@Override
//			protected IStatus run(IProgressMonitor monitor)
//			{
		baseNGPackageManager.updateFromResourceChangeListener(recourceChangeHandler.getNgPackageChangesPerReferencingProject());
		if (recourceChangeHandler.getNgPackageListChanged() != null)
		{
			baseNGPackageManager.ngPackageProjectListChanged(recourceChangeHandler.getNgPackageListChanged()[0]);
		}
//				return Status.OK_STATUS;
//			}
//
//		};
//		j.setSystem(true);
//		j.setUser(false);
//		j.setRule(ResourcesPlugin.getWorkspace().getRoot());
//		j.schedule();
	}

	/**
	 * Class that handles the received resources changed events.
	 *
	 * @author acostescu
	 */
	public class NGPackageResourceChangeHandler implements IResourceDeltaVisitor
	{

		// keys are projects that contain or reference a modified ng package, values is a pair of removed packages and added packages (as references or binaries of that project)
		// project can be a solution/module project that either references ng package projects or contains ng package binary/zip, can be a resources project
		private final Map<String, Pair<Set<IPackageReader>, Set<IPackageReader>>> ngPackageChangesPerReferencingProject = new HashMap<>();
		boolean[] ngPackageListChanged = null;

		private final IResourceChangeEvent event;

		public NGPackageResourceChangeHandler(IResourceChangeEvent event)
		{
			this.event = event;
		}

		public boolean detectedChanges()
		{
			return ngPackageChangesPerReferencingProject.size() > 0 || ngPackageListChanged != null;
		}

		public Map<String, Pair<Set<IPackageReader>, Set<IPackageReader>>> getNgPackageChangesPerReferencingProject()
		{
			return ngPackageChangesPerReferencingProject;
		}

		public boolean[] getNgPackageListChanged()
		{
			return ngPackageListChanged;
		}

		private Set<IPackageReader> getAddedPackageReaders(String projectname)
		{
			if (!ngPackageChangesPerReferencingProject.containsKey(projectname))
			{
				Set<IPackageReader> removed = new HashSet<>();
				Set<IPackageReader> added = new HashSet<>();
				ngPackageChangesPerReferencingProject.put(projectname, new Pair<Set<IPackageReader>, Set<IPackageReader>>(removed, added));
			}
			return ngPackageChangesPerReferencingProject.get(projectname).getRight();
		}

		private Set<IPackageReader> getRemovedPackageReaders(String projectname)
		{
			if (!ngPackageChangesPerReferencingProject.containsKey(projectname))
			{
				Set<IPackageReader> removed = new HashSet<IPackageReader>();
				Set<IPackageReader> added = new HashSet<IPackageReader>();
				ngPackageChangesPerReferencingProject.put(projectname, new Pair<Set<IPackageReader>, Set<IPackageReader>>(removed, added));
			}
			return ngPackageChangesPerReferencingProject.get(projectname).getLeft();
		}

		@Override
		public boolean visit(IResourceDelta resourceDelta) throws CoreException
		{
			// ignore the change if it only contains marker changes (notice the == used in comparing flags)
			if ((resourceDelta.getKind() & IResourceDelta.CHANGED) != 0 && resourceDelta.getFlags() == IResourceDelta.MARKERS) return true;

			IResource resource = resourceDelta.getResource();
			if (resource instanceof IFile)
			{
				handleFileChanged(resourceDelta, resource);
				return false;
			}
			else if (resource instanceof IFolder && resource.getName().equals("node_modules")) return false;
			else if (resource instanceof IProject && ((resourceDelta.getKind() & IResourceDelta.CHANGED) == 0 ||
				(resourceDelta.getFlags() != 0 && resourceDelta.getFlags() != IResourceDelta.MARKERS))) // the project itself hasn't actually changed if it has no change flags; also we are not interested in marker changes only either
			{
				return handleProjectChanged((IProject)resource);
			}
			return true;
		}

		private void handleFileChanged(IResourceDelta resourceDelta, IResource resource) throws CoreException
		{
			IFile binaryFile = (IFile)resource;
			if (binaryFile.getName().endsWith(".zip"))
			{
				if (isZipFileWebPackageInActiveModulesOrResources(binaryFile))
				{
					// web package binary
					if ((resourceDelta.getKind() & IResourceDelta.CHANGED) != 0)
					{
						IPackageReader webPackageNameForFile = ResourceProvider.getComponentPackageReader(new File(resource.getLocationURI()));
						if (webPackageNameForFile == null)
							webPackageNameForFile = ResourceProvider.getServicePackageReader(new File(resource.getLocationURI()));
						IPackageReader reader = baseNGPackageManager.readPackageResource(resource);
						if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
						if (webPackageNameForFile != null) getRemovedPackageReaders(resource.getProject().getName()).add(webPackageNameForFile);
					}
					else if ((resourceDelta.getKind() & IResourceDelta.REMOVED) != 0)
					{
						if (resource.getLocationURI() != null)
						{
							IPackageReader componentPackageNameForFile = ResourceProvider.getComponentPackageReader(new File(resource.getLocationURI()));
							if (componentPackageNameForFile == null)
								componentPackageNameForFile = ResourceProvider.getServicePackageReader(new File(resource.getLocationURI()));
							if (componentPackageNameForFile != null) getRemovedPackageReaders(resource.getProject().getName()).add(componentPackageNameForFile);
						} // otherwise it's probably a post-delete event which was already handled by the pre-delete step handled before; so do nothing here
					}
					else if ((resourceDelta.getKind() & IResourceDelta.ADDED) != 0)
					{
						IPackageReader reader = baseNGPackageManager.readPackageResource(resource);
						if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
					}
				}
			}
			else if (resource.getProject().isAccessible())
			{
				// check if this file change happened in a resouces project components/services subtree or in an referenced ngpackage project
				if (resource.getProject().hasNature(ServoyNGPackageProject.NATURE_ID))
				{
					// we have a change in a file of a package project - reload that package in case any of the referencing solutions/modules are active
					IProject[] referencingProjects = resource.getProject().getReferencingProjects();
					if (referencingProjects.length > 0)
					{
						IPackageReader webPackageNameForFile = ResourceProvider.getComponentPackageReader(new File(resource.getProject().getLocationURI()));
						if (webPackageNameForFile == null)
							webPackageNameForFile = ResourceProvider.getServicePackageReader(new File(resource.getProject().getLocationURI()));

						// see if this package project is referenced by any active solution/module
						for (IProject iProject : referencingProjects)
						{
							if (iProject.hasNature(ServoyProject.NATURE_ID) && ServoyModelFinder.getServoyModel().isSolutionActive(iProject.getName()))
							{
								IPackageReader reader = baseNGPackageManager.readPackageResource(resource.getProject());
								if (reader != null) getAddedPackageReaders(iProject.getName()).add(reader);
								if (webPackageNameForFile != null) getRemovedPackageReaders(iProject.getName()).add(webPackageNameForFile);
								// we don't do "break;" here because a package could be referenced by multiple active solutions/modules - and we need to know which was
								// added and which one was removed later on; there is some code that will not unload it if there are still references to it left...
							}
						}
					}
				}
				else
				{
					// see if this is a change in the old locations (resources project components/services) - in expanded web packages subtrees (not zips)
					ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
					if (resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID) && activeResourcesProject != null &&
						resource.getProject().equals(activeResourcesProject.getProject()))
					{
						// we need to determine the root dir package in "components" or in "services" that was affected if any
						IProject resourcesProject = resource.getProject();
						IFolder componentsFolderInResources = resourcesProject.getFolder(SolutionSerializer.COMPONENTS_DIR_NAME);
						IFolder servicesFolderInResources = resourcesProject.getFolder(SolutionSerializer.SERVICES_DIR_NAME);

						IResource r = resource;
						IContainer parent = resource.getParent();

						while (parent != null && !componentsFolderInResources.equals(parent) && !servicesFolderInResources.equals(parent))
						{
							r = parent;
							parent = r.getParent();
						}

						if (parent != null)
						{
							// then parent is either componentsFolderInResources or servicesFolderInResources; so r is the root folder of what should be a the web package
							IPackageReader webPackageNameForFile = ResourceProvider.getComponentPackageReader(new File(r.getLocationURI()));
							if (webPackageNameForFile == null) webPackageNameForFile = ResourceProvider.getServicePackageReader(new File(r.getLocationURI()));

							IPackageReader reader = baseNGPackageManager.readPackageResource(r);
							if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
							if (webPackageNameForFile != null) getRemovedPackageReaders(resource.getProject().getName()).add(webPackageNameForFile);
						}
					}
				}
			}
		}

		public boolean handleProjectChanged(IProject changedProject) throws CoreException
		{
			boolean continueDeeperWithChanges = true;

			// we don't care here about active solution's module changes (add/remove of modules) - that is already handled by BaseNGPackageManager via the active project listener

			// see if references to package projects for an active solution changed
			// or if something happened to a referenced web package project
			if (changedProject.isAccessible() && changedProject.hasNature(ServoyProject.NATURE_ID))
			{
				// see if what we loaded as references for that solution project has changed
				IProject[] referencedProjects = changedProject.getDescription().getReferencedProjects();
				List<IProject> usedPackageProjects = new ArrayList<>();
				for (IProject referencedProject : referencedProjects)
				{
					if (referencedProject.isAccessible() && referencedProject.hasNature(ServoyNGPackageProject.NATURE_ID))
					{
						usedPackageProjects.add(referencedProject);
					}
				}
				List<String> oldUsedPackageList = baseNGPackageManager.getOldUsedPackageProjectsList(changedProject.getName(),
					changedProject.getWorkspace().getRoot());
				List<String> oldUsedPackageListCopy = new ArrayList<>();
				oldUsedPackageListCopy.addAll(oldUsedPackageList);

				// retain in oldUsedPackageList only the projects that are not used/accessible anymore; those should be removed
				Iterator<String> oldUsedPackageNameIterator = oldUsedPackageList.iterator();
				while (oldUsedPackageNameIterator.hasNext())
				{
					String next = oldUsedPackageNameIterator.next();
					for (IProject usedProject : usedPackageProjects)
					{
						if (next.equals(usedProject.getName()))
						{
							oldUsedPackageNameIterator.remove();
							break;
						}
					}
				}

				// retain in usedPackageProjects only the projects that were not used before
				Iterator<IProject> usedPackageProjectIterator = usedPackageProjects.iterator();
				while (usedPackageProjectIterator.hasNext())
				{
					IProject next = usedPackageProjectIterator.next();
					for (String oldUsedPackageName : oldUsedPackageListCopy)
					{
						if (next.getName().equals(oldUsedPackageName))
						{
							usedPackageProjectIterator.remove();
							break;
						}
					}
				}
				if (oldUsedPackageList.size() > 0 || usedPackageProjects.size() > 0)
				{
					baseNGPackageManager.clearReferencedNGPackageProjectsCache();
				}
				for (String removedProject : oldUsedPackageList)
				{
					File projectDir = new File(changedProject.getWorkspace().getRoot().getProject(removedProject).getLocationURI());
					IPackageReader webPackageNameForFile = ResourceProvider.getComponentPackageReader(projectDir);
					if (webPackageNameForFile == null) webPackageNameForFile = ResourceProvider.getServicePackageReader(projectDir);

					if (webPackageNameForFile != null) getRemovedPackageReaders(changedProject.getName()).add(webPackageNameForFile);
				}
				for (IProject addedProject : usedPackageProjects)
				{
					IPackageReader readPackageResource = baseNGPackageManager.readPackageResource(addedProject);
					getAddedPackageReaders(changedProject.getName()).add(readPackageResource);
				}
			}
			else
			{
				// see if any ng package projects were added, removed, closed, changed nature, ... fire listeners as needed
				// also see if the changed projects are part of the active solution's dependencies and update if needed

				if (allAvailableNGPackageProjectNames.contains(changedProject.getName()))
				{
					if (((event.getType() & (IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.PRE_CLOSE)) != 0) || !changedProject.isAccessible() ||
						!changedProject.hasNature(ServoyNGPackageProject.NATURE_ID))
					{
						// so the project is either already no longer available/don't have the nature anymore or it will be closed/deleted soon;
						// we do listen for the PRE_CLOSE and PRE_DELETE because in those situations we need to the changedProject.getLocationURI() to properly refresh and
						// that on is no longer available after the fact
						allAvailableNGPackageProjectNames.remove(changedProject.getName());

						// an ng package project is no longer available; see if it is active as well
						ServoyNGPackageProject[] referencedProjects = baseNGPackageManager.getReferencedNGPackageProjects();
						boolean foundInReferencedProjects = false;
						if (referencedProjects != null) for (ServoyNGPackageProject rp : referencedProjects)
						{
							if (rp.getProject().getName().equals(changedProject.getName()))
							{
								foundInReferencedProjects = true;
								break;
							}
						}

						setNGPackagesListChanged(foundInReferencedProjects);

						if (foundInReferencedProjects)
						{
							// remove it from all referencing active projects; but we need to know the package name that it had and the projects to add to the removed map...
							File projectDir = new File(changedProject.getLocationURI());
							IPackageReader webPackageNameForFile = ResourceProvider.getComponentPackageReader(projectDir);
							if (webPackageNameForFile == null) webPackageNameForFile = ResourceProvider.getServicePackageReader(projectDir);
							if (webPackageNameForFile != null)
							{
								// we have the package name; now we need the referencing project(s) as well
								List<String> referencingProjectsThatLoadedThis = baseNGPackageManager.getReferencingProjectsThatLoaded(
									webPackageNameForFile.getPackageName(), projectDir);
								for (String x : referencingProjectsThatLoadedThis)
								{
									getRemovedPackageReaders(x).add(webPackageNameForFile);
								}
							}

							baseNGPackageManager.clearReferencedNGPackageProjectsCache();
						}

						continueDeeperWithChanges = false; // no need to search for changes deeper in this project - as it's no longer an ng package project
					}
				}
				else if (changedProject.isAccessible() && changedProject.hasNature(ServoyNGPackageProject.NATURE_ID))
				{
					// a new one was found; deal with it
					allAvailableNGPackageProjectNames.add(changedProject.getName());

					List<String> referencedActiveSolutions = new ArrayList<String>();
					IProject[] referencingProjects = changedProject.getReferencingProjects();
					if (referencingProjects.length > 0)
					{
						// see if this package project is referenced by any active solution/module
						for (IProject iProject : referencingProjects)
						{
							if (iProject.hasNature(ServoyProject.NATURE_ID) && ServoyModelFinder.getServoyModel().isSolutionActive(iProject.getName()))
							{
								referencedActiveSolutions.add(iProject.getName());
							}
						}
					}

					setNGPackagesListChanged(referencedActiveSolutions.size() > 0);

					if (referencedActiveSolutions.size() > 0)
					{
						baseNGPackageManager.clearReferencedNGPackageProjectsCache();

						IPackageReader reader = baseNGPackageManager.readPackageResource(changedProject);
						if (reader != null) for (String referencingSolutionName : referencedActiveSolutions)
						{
							getAddedPackageReaders(referencingSolutionName).add(reader);
						}
					}

					continueDeeperWithChanges = false; // no use checking deeper for changes as it wasn't loaded before and we will try to load it completely now if isReferencedByActiveSolution == true anyway
				}
			}
			return continueDeeperWithChanges;
		}

		private void setNGPackagesListChanged(boolean foundInReferencedProjects)
		{
			if (ngPackageListChanged == null)
			{
				ngPackageListChanged = new boolean[] { foundInReferencedProjects };
			}
			else
			{
				ngPackageListChanged[0] |= foundInReferencedProjects;
			}
		}

		private boolean isZipFileWebPackageInActiveModulesOrResources(IFile binaryFile)
		{
			String parentName = binaryFile.getParent().getName();
			boolean isZipFileWebPackageInActiveSolutionOrResourcesPrj = parentName.equals(SolutionSerializer.NG_PACKAGES_DIR_NAME);
			if (isZipFileWebPackageInActiveSolutionOrResourcesPrj)
			{
				IContainer grandParent = binaryFile.getParent().getParent();
				isZipFileWebPackageInActiveSolutionOrResourcesPrj = (grandParent instanceof IProject);
				if (isZipFileWebPackageInActiveSolutionOrResourcesPrj)
				{
					// see that grandparent is an active solution or module
					ServoyProject[] allActiveSolutions = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
					boolean found = false;
					for (int i = allActiveSolutions.length - 1; i >= 0 && !found; i--)
					{
						found = (allActiveSolutions[i].getProject().equals(grandParent));
					}
					isZipFileWebPackageInActiveSolutionOrResourcesPrj = found;
				}
			}
			else
			{
				// see if this is a change in the old locations (resources project components/services) zip files
				isZipFileWebPackageInActiveSolutionOrResourcesPrj = (parentName.equals(SolutionSerializer.COMPONENTS_DIR_NAME) ||
					parentName.equals(SolutionSerializer.SERVICES_DIR_NAME));
				if (isZipFileWebPackageInActiveSolutionOrResourcesPrj)
				{
					IContainer grandParent = binaryFile.getParent().getParent();
					ServoyResourcesProject activeResourcesPrj = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
					isZipFileWebPackageInActiveSolutionOrResourcesPrj = (grandParent instanceof IProject && activeResourcesPrj != null &&
						activeResourcesPrj.getProject().equals(grandParent));
				}
			}
			return isZipFileWebPackageInActiveSolutionOrResourcesPrj;
		}

		@Override
		public String toString()
		{
			return "Changes: " + ngPackageChangesPerReferencingProject.toString() + ". List changed: " + (ngPackageListChanged != null);
		}

	}

	public void clearAnyPendingChangesBecauseFullReloadWasDone()
	{
		preChangeProcessedHandlersForLater.clear();
	}

}