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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.util.ValueReference;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;

/**
 * This class is responsible for refreshing loaded ng packages as needed depending on workspace resource changes.
 *
 * @author acostescu
 */
public class BaseNGPackageResourcesChangedListener implements IResourceChangeListener
{

	protected final BaseNGPackageManager baseNGPackageManager;

	public BaseNGPackageResourcesChangedListener(BaseNGPackageManager baseNGPackageManager)
	{
		this.baseNGPackageManager = baseNGPackageManager;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event)
	{
		ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		IResourceDelta delta = event.getDelta();
		IResourceDelta[] affectedChildren = delta.getAffectedChildren();

		boolean refreshResourcesServices = false;
		boolean refreshResourcesComponents = false;

		// check for changes in the resources project ngpackages
		if (activeResourcesProject != null)
		{
			IProject resourceProject = activeResourcesProject.getProject();
			refreshResourcesServices = shouldRefresh(resourceProject, affectedChildren, SolutionSerializer.SERVICES_DIR_NAME);
			refreshResourcesComponents = shouldRefresh(resourceProject, affectedChildren, SolutionSerializer.COMPONENTS_DIR_NAME);
		}

		ValueReference<Boolean> refreshAllNGPackageProjects = new ValueReference<>(Boolean.FALSE);
		ValueReference<Boolean> clearReferencedProjectsCache = new ValueReference<>(Boolean.FALSE);
		final List<IProject> newNGPackageProjectsToLoad = new ArrayList<>();
		final List<IProject> oldNGPackageProjectsToUnload = new ArrayList<>();

		checkForChangesInSolutionBinaryPackages(delta);

		checkForChangesInNGPackageProjects(activeProject, affectedChildren, refreshAllNGPackageProjects, clearReferencedProjectsCache,
			newNGPackageProjectsToLoad, oldNGPackageProjectsToUnload);

		boolean somethingChangedInResourcesProject = (refreshResourcesServices || refreshResourcesComponents);
		boolean somethingChangedInNGPackageProjects = (refreshAllNGPackageProjects.value.booleanValue() || newNGPackageProjectsToLoad.size() > 0 ||
			oldNGPackageProjectsToUnload.size() > 0);

		if (somethingChangedInResourcesProject && somethingChangedInNGPackageProjects)
		{
			// we have to reload all to avoid a problem where when moving a package completely between resources project and it's own project
			// it could be loaded twice (if we would reload first resources project and then it's own project, then while reloading from one the others will not yet be unloaded)
			if (clearReferencedProjectsCache.value.booleanValue()) baseNGPackageManager.clearActiveSolutionReferencesCache();
			baseNGPackageManager.reloadAllNGPackages(null, false);
			if (clearReferencedProjectsCache.value.booleanValue()) baseNGPackageManager.ngPackageProjectListChanged();
		}
		else if (somethingChangedInResourcesProject)
		{
			// something changed only in resources project ngpackages
			baseNGPackageManager.reloadResourcesProjectNGPackages(refreshResourcesComponents, refreshResourcesServices, null, false);
		}
		else
		{
			// maybe something changed only in ng package projects
			if (clearReferencedProjectsCache.value.booleanValue()) baseNGPackageManager.clearActiveSolutionReferencesCache();
			if (refreshAllNGPackageProjects.value.booleanValue())
			{
				baseNGPackageManager.reloadAllSolutionReferencedPackages(null, false);
			}
			else if (newNGPackageProjectsToLoad.size() > 0 || oldNGPackageProjectsToUnload.size() > 0)
			{
				baseNGPackageManager.reloadNGPackageProjects(oldNGPackageProjectsToUnload, newNGPackageProjectsToLoad, null, false);
			}
			if (clearReferencedProjectsCache.value.booleanValue()) baseNGPackageManager.ngPackageProjectListChanged();
		}
	}


	/** Checks for resources changes of binary packages and updates the  @ResourceProvider if needed
	 * @param delta
	 * @return
	 */
	private void checkForChangesInSolutionBinaryPackages(IResourceDelta delta)
	{
		final Map<String, IPackageReader> addedPackageReaders = new HashMap<String, IPackageReader>();
		final List<File> removedPackageFiles = new ArrayList<>();
		try
		{
			delta.accept(new IResourceDeltaVisitor()
			{

				@Override
				public boolean visit(IResourceDelta resourceDelta) throws CoreException
				{
					IResource resource = resourceDelta.getResource();
					if (resource instanceof IFile)
					{
						IFile binaryFile = (IFile)resource;
						if (binaryFile.getName().endsWith(".zip"))
						{
							if (binaryFile.getParent().getName().equals(SolutionSerializer.NG_PACKAGES_DIR_NAME))
							{//component package
								Pair<String, IPackageReader> readPackageResource = baseNGPackageManager.readPackageResource(resource);
								if ((resourceDelta.getKind() & IResourceDelta.CHANGED) != 0)
								{
									removedPackageFiles.add(new File(resource.getLocationURI()));
									if (readPackageResource != null) addedPackageReaders.put(readPackageResource.getLeft(), readPackageResource.getRight());
								}
								else if ((resourceDelta.getKind() & IResourceDelta.REMOVED) != 0)
								{
									removedPackageFiles.add(new File(resource.getLocationURI()));
								}
								else if ((resourceDelta.getKind() & IResourceDelta.ADDED) != 0)
								{
									if (readPackageResource != null) addedPackageReaders.put(readPackageResource.getLeft(), readPackageResource.getRight());
								}
							}
						}
						return false;
					}
					return true;
				}
			});
		}
		catch (CoreException e)
		{
			Debug.log(e);
		}

		boolean componentsReloaded = false;
		boolean servicesReloaded = false;

		List<String> removedComponentPackageNames = new ArrayList<>();
		List<String> removedServicePackageNames = new ArrayList<>();
		for (File file : removedPackageFiles)
		{
			String packageName = ResourceProvider.getComponentPackageNameForFile(file);
			if (packageName != null) removedComponentPackageNames.add(packageName);
			else
			{
				packageName = ResourceProvider.getServicePackageNameForFile(file);
				if (packageName != null) removedServicePackageNames.add(packageName);
			}
		}
		List<IPackageReader> addedComponentPackageReaders = new ArrayList<>();
		List<IPackageReader> addedServicePackageReaders = new ArrayList<>();
		for (Entry<String, IPackageReader> entry : addedPackageReaders.entrySet())
		{
			if (IPackageReader.WEB_SERVICE.equals(entry.getValue().getPackageType()))
			{
				addedServicePackageReaders.add(entry.getValue());
			}
			else
			{
				// for now always fall back to a component
				addedComponentPackageReaders.add(entry.getValue());
			}
		}


		if (removedComponentPackageNames.size() > 0 || addedComponentPackageReaders.size() > 0)
		{
			ResourceProvider.updateComponentResources(removedComponentPackageNames, addedComponentPackageReaders);
			componentsReloaded = true;
		}
		if (removedServicePackageNames.size() > 0 || addedServicePackageReaders.size() > 0)
		{
			ResourceProvider.updateServiceResources(removedServicePackageNames, addedServicePackageReaders);
			servicesReloaded = true;
		}

		if (componentsReloaded || servicesReloaded) baseNGPackageManager.ngPackagesChanged(componentsReloaded, servicesReloaded);
	}

	protected void checkForChangesInNGPackageProjects(ServoyProject activeProject, IResourceDelta[] affectedChildren,
		ValueReference<Boolean> refreshAllNGPackageProjects, ValueReference<Boolean> clearReferencedProjectsCache,
		final List<IProject> newNGPackageProjectsToLoad, final List<IProject> oldNGPackageProjectsToUnload)
	{
		if (activeProject != null)
		{
			for (IResourceDelta rd : affectedChildren)
			{
				if (rd.getFlags() == IResourceDelta.MARKERS) continue;
				IResource resource = rd.getResource();

				// check for changes in the list of projects that the active solution references
				IResourceDelta[] affectedProjectChildren = rd.getAffectedChildren(IResourceDelta.CHANGED, IResource.HIDDEN);
				for (IResourceDelta firstLevelChanges : affectedProjectChildren)
				{
					if (".project".equals(firstLevelChanges.getResource().getName()))
					{
						refreshAllNGPackageProjects.value = Boolean.TRUE; // this could be refined further to check new vs. old (that we have cached) and only refresh what is needed, not all ng package projects
						clearReferencedProjectsCache.value = Boolean.TRUE;
						break;
					}
				}
				if (refreshAllNGPackageProjects.value.booleanValue()) break;
				// - we need to know if it's a project that has been referenced before but was missing and now it is available and of type ngPackage
				// or
				// - if it was previously available and loaded as an ngPackage project, we must check to see if manifest or .spec files changed or if the project is no longer available
				boolean wasPreviouslyLoaded = false;
				for (ServoyNGPackageProject p : baseNGPackageManager.getReferencedNGPackageProjectsInternal())
				{
					if (resource.equals(p.getProject()))
					{
						wasPreviouslyLoaded = true;
						break;
					}
				}

				final IProject p = resource.getProject(); // kind of a cast cause it's already a project on this branch
				boolean isValidNGPackageProject;
				try
				{
					isValidNGPackageProject = (p.exists() && p.isOpen() && p.hasNature(ServoyNGPackageProject.NATURE_ID));
				}
				catch (CoreException e1)
				{
					ServoyLog.logError(e1);
					isValidNGPackageProject = false;
				}
				if (wasPreviouslyLoaded)
				{
					if (isValidNGPackageProject)
					{
						// check for changes in spec or manifest files
						try
						{
							rd.accept(new IResourceDeltaVisitor()
							{
								boolean continueSearching = true;

								@Override
								public boolean visit(IResourceDelta delta) throws CoreException
								{
									if (continueSearching)
									{
										if (delta.getFlags() == IResourceDelta.MARKERS) return false;

										if (delta.getResource().getName().toLowerCase().endsWith(".spec") ||
											delta.getResource().getName().equalsIgnoreCase("MANIFEST.MF"))
										{
											oldNGPackageProjectsToUnload.add(p);
											newNGPackageProjectsToLoad.add(p);
											continueSearching = false;
										}
									}
									return continueSearching;
								}
							});
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
							oldNGPackageProjectsToUnload.add(p);
							newNGPackageProjectsToLoad.add(p);
						}
					}
					else
					{
						// that means it's no longer available although it was loaded before; it needs to be unloaded
						clearReferencedProjectsCache.value = Boolean.TRUE;
						oldNGPackageProjectsToUnload.add(p);
					}
				}
				else
				{
					if (isValidNGPackageProject)
					{
						// a new referenced ngPackage project is available; load it
						clearReferencedProjectsCache.value = Boolean.TRUE;
						if (p.getReferencingProjects().length > 0) newNGPackageProjectsToLoad.add(p);
					} // else just some other type of referenced project changed; ignore
				}
			}
		}
	}

	private boolean shouldRefresh(IProject resourceProject, IResourceDelta[] affectedChildren, String parentDir)
	{
		for (IResourceDelta rd : affectedChildren)
		{
			if (rd.getFlags() == IResourceDelta.MARKERS) continue;
			IResource resource = rd.getResource();
			if (resourceProject.equals(resource.getProject()))
			{
				IPath path = resource.getProjectRelativePath();
				if (path.segmentCount() > 1)
				{
					if (path.segment(0).equals(parentDir))
					{
						if (path.segmentCount() == 2 && resource instanceof IFile)
						{
							// a zip is changed refresh
							return true;
						}
						else if (path.lastSegment().equalsIgnoreCase("MANIFEST.MF") || path.lastSegment().toLowerCase().endsWith(".spec"))
						{
							return true;
						}
					}
				}
				if (path.segmentCount() == 0 || (path.segmentCount() > 0 && path.segment(0).equals(parentDir)))
				{
					if (shouldRefresh(resourceProject, rd.getAffectedChildren(), parentDir))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

}