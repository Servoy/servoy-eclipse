/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.sablo.specification.Package;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.DuplicateEntityException;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;
import com.servoy.j2db.util.Pair;

/**
 * A class for managing the loaded NG custom web components and services.
 * These can be found in the resources project as folders or archives or each in a separate project with {@link ServoyNGPackageProject} nature (again either zipped or as a expanded folder).
 *
 * @author acostescu
 */
// a lot of the initial code in this class was just taken out of the huge core plugin Activator and refactored to make it easier to follow and to maintain
public abstract class BaseNGPackageManager
{
	private static final String DUPLICATE_COMPONENT_MARKER = "com.servoy.eclipse.debug.DUPLICATE_COMPONENT_MARKER";
	private static final String SPEC_READ_MARKER = "com.servoy.eclipse.debug.SPEC_READ_MARKER";

	private final BaseNGPackageResourcesChangedListener resourceChangeListener;

	private ServoyNGPackageProject[] referencedNGPackageProjects;

	private final Map<String, Map<String, IPackageReader>> projectNameToContainedPackages = new HashMap<>();

	private final List<ILoadedNGPackagesListener> loadedNGPackagesListeners = Collections.synchronizedList(new ArrayList<ILoadedNGPackagesListener>());
	private final List<IAvailableNGPackageProjectsListener> availableNGPackageProjectsListeners = Collections.synchronizedList(
		new ArrayList<IAvailableNGPackageProjectsListener>());

	public BaseNGPackageManager()
	{
		if (ServoyModelFinder.getServoyModel().getActiveProject() != null) reloadAllNGPackages(null); // initial load

		resourceChangeListener = new BaseNGPackageResourcesChangedListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener,
			IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE);
		setRemovedPackages();
	}

	private void setRemovedPackages()
	{
		Set<String> defaultPackageNames = ResourceProvider.getDefaultPackageNames();
		List<String> toRemove = new ArrayList<String>();
		for (String packageName : defaultPackageNames)
		{
			if (!isDefaultPackageEnabled(packageName)) toRemove.add(packageName);
		}
		ResourceProvider.setRemovedPackages(toRemove);
	}

	public void clearReferencedNGPackageProjectsCache()
	{
		referencedNGPackageProjects = null;
	}

	public void addLoadedNGPackagesListener(ILoadedNGPackagesListener listener)
	{
		loadedNGPackagesListeners.add(listener);
	}

	public void removeLoadedNGPackagesListener(ILoadedNGPackagesListener listener)
	{
		loadedNGPackagesListeners.remove(listener);
	}

	public void ngPackagesChanged(boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		for (ILoadedNGPackagesListener listener : loadedNGPackagesListeners)
		{
			listener.ngPackagesChanged(loadedPackagesAreTheSameAlthoughReferencingModulesChanged);
		}
	}

	public void addAvailableNGPackageProjectsListener(IAvailableNGPackageProjectsListener listener)
	{
		availableNGPackageProjectsListeners.add(listener);
	}

	public void removeAvailableNGPackageProjectsListener(IAvailableNGPackageProjectsListener listener)
	{
		availableNGPackageProjectsListeners.remove(listener);
	}

	public void ngPackageProjectListChanged(boolean activePackageProjectsChanged)
	{
		for (IAvailableNGPackageProjectsListener listener : availableNGPackageProjectsListeners)
		{
			listener.ngPackageProjectListChanged(activePackageProjectsChanged);
		}
	}

	public List<IPackageReader> getAllPackageReaders()
	{
		List<IPackageReader> all = new ArrayList<>();
		IPackageReader[] allPackageReaders = WebComponentSpecProvider.getInstance().getAllPackageReaders();
		for (IPackageReader iPackageReader : allPackageReaders)
		{
			all.add(iPackageReader);
		}
		allPackageReaders = WebServiceSpecProvider.getInstance().getAllPackageReaders();
		for (IPackageReader iPackageReader : allPackageReaders)
		{
			all.add(iPackageReader);
		}
		return all;
	}


	public ServoyNGPackageProject[] getReferencedNGPackageProjects()
	{
		if (referencedNGPackageProjects == null)
		{
			ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
			HashSet<ServoyNGPackageProject> referencedNGPackageProjectsSet = new HashSet<ServoyNGPackageProject>();
			ServoyNGPackageProject[] ngPackageProjects = activeSolutionProject.getNGPackageProjects();
			Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject module : modulesOfActiveProject)
			{
				ngPackageProjects = module.getNGPackageProjects();
				Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
			}
			referencedNGPackageProjects = referencedNGPackageProjectsSet.toArray(new ServoyNGPackageProject[referencedNGPackageProjectsSet.size()]);
		}
		return referencedNGPackageProjects;
	}

	/**
	 * Reloads all ng packages (both resources project ones and referenced ng package project ones).
	 * It takes care to unload all from all places before loading in the new ones to avoid generating wrong duplicate conflicts problems (if a package is moved between it's own project and the resources project).
	 */
	public void reloadAllNGPackages(IProgressMonitor m)
	{
		projectNameToContainedPackages.clear();
		setRemovedPackages();
		SubMonitor monitor = SubMonitor.convert(m, "Reloading all ng packages", 100);//TODO check monitor steps counter
		Map<String, IPackageReader> allPackageReaders = new HashMap<String, IPackageReader>();
		monitor.subTask("Preparing to reload resources project ng packages");
		collectResourcesProjectNGPackages(true, true, monitor.newChild(8), allPackageReaders);
		monitor.worked(30);
		monitor.subTask("Preparing to reload referenced ng package projects");
		collectNGPackageProjects(allPackageReaders, monitor.newChild(8));
		monitor.worked(30);
		monitor.subTask("Preparing to reload solution contained binary packages");
		collectSolutionContainedBinaryPackages(allPackageReaders, monitor.newChild(8));
		monitor.worked(30);
		monitor.subTask("Reloading component packages");
		ResourceProvider.setPackages(allPackageReaders.values());

		monitor.worked(9);
		monitor.setTaskName("Announcing ng packages load");
		ngPackagesChanged(false);
		monitor.worked(1);
		monitor.done();
	}

	private void collectSolutionContainedBinaryPackages(Map<String, IPackageReader> componentProjectReaders, SubMonitor monitor)
	{
		ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();

		for (ServoyProject solution : modules)
		{
			IProject project = solution.getProject();
			{
				IFolder folder = project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME);
				if (folder.exists())
				{
					try
					{
						refreshResourceLater(folder, IResource.DEPTH_INFINITE);
						IResource[] members = folder.members();
						for (IResource resource : members)
						{
							IPackageReader reader = readPackageResource(resource);
							if (reader != null)
							{
								componentProjectReaders.put(reader.getPackageName(), reader);
								getProjectContainedPackagesMap(project.getName()).put(reader.getPackageName(), reader);
							}
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		monitor.worked(8);
	}

	/**
	 * @param ngPackageChangesPerReferencingProject keys of this map are projects that contain or reference a modified ng package, values is a pair of removed packages and added packages (as references or binaries of that project)
	 * project can be a solution/module project that either references ng package projects or contains ng package binary/zip, can be a resources project.
	 */
	protected void updateFromResourceChangeListener(Map<String, Pair<Set<String>, Set<IPackageReader>>> ngPackageChangesPerReferencingProject)
	{
		if (ngPackageChangesPerReferencingProject == null || ngPackageChangesPerReferencingProject.size() == 0) return; // no changes

		boolean reallyChanged = false;

		// for each change add/remove we first check that the project isn't already loaded in the spec readers
		// and also split up into services and non-services

		// in case multiple modules of the active solution reference the same ng package project, if only one reference is removed we don't need to unload that project
		// (if the project is changed, not removed/dereferenced a remove of all locations will be done and then a re-load - so we can detect that and unload then anyway)
		Set<String> componentsReallyToUnload = new HashSet<>();
		Set<String> serviceReallyToUnload = new HashSet<>();
		Map<File, IPackageReader> componentsReallyToLoad = new HashMap<>();
		Map<File, IPackageReader> serviceReallyToLoad = new HashMap<>();

		for (String projectName : ngPackageChangesPerReferencingProject.keySet())
		{
			Pair<Set<String>, Set<IPackageReader>> projectChanges = ngPackageChangesPerReferencingProject.get(projectName);
			Set<String> packagesToUnload = projectChanges.getLeft();
			Set<IPackageReader> packagesToLoad = projectChanges.getRight();

			// first update our cache
			Map<String, IPackageReader> projectContainedPackagesMap = getProjectContainedPackagesMap(projectName);
			for (String packageName : packagesToUnload)
			{
				projectContainedPackagesMap.remove(packageName);
			}
			for (IPackageReader iPackageReader : packagesToLoad)
			{
				projectContainedPackagesMap.put(iPackageReader.getPackageName(), iPackageReader);
			}


			for (String packageName : packagesToUnload)
			{
				if (notContainedByOtherProjectsAfterUnloads(projectName, packageName, ngPackageChangesPerReferencingProject))
				{
					if (WebComponentSpecProvider.getInstance().getPackageNames().contains(packageName)) componentsReallyToUnload.add(packageName);
					else serviceReallyToUnload.add(packageName);
				}
			}

			for (IPackageReader packageReader : packagesToLoad)
			{
				if (notContainedByOtherProjectsAfterUnloads(projectName, packageReader.getPackageName(), ngPackageChangesPerReferencingProject))
				{
					if (packageReader.getPackageType().equals(IPackageReader.WEB_SERVICE)) serviceReallyToLoad.put(packageReader.getResource(), packageReader); // could potentially replace another package reader in case it's referenced from multiple solution projects
					else componentsReallyToLoad.put(packageReader.getResource(), packageReader); // could potentially replace another package reader in case it's referenced from multiple solution projects
				}
			}
		}

		reallyChanged = (reallyChanged || componentsReallyToUnload.size() > 0 || componentsReallyToLoad.size() > 0 || serviceReallyToUnload.size() > 0 ||
			serviceReallyToLoad.size() > 0);
		ResourceProvider.updatePackageResources(componentsReallyToUnload, new HashSet<IPackageReader>(componentsReallyToLoad.values()), serviceReallyToUnload,
			new HashSet<IPackageReader>(serviceReallyToLoad.values()));

		ngPackagesChanged(!reallyChanged); // we always have to notify, even for example when loaded packages are really the same, just a second (doubled) reference to a project was removed, because solex has to know that case as well
	}

	/**
	 * Search in all other projects if the given package is not contained also there. An ng package that will be removed (see ngPackageChangesPerReferencingProject) from referencing projects is ignored.
	 *
	 * @param ngPackageChangesPerReferencingProject keys of this map are projects that contain or reference a modified ng package, values is a pair of removed packages and added packages (as references or binaries of that project)
	 * project can be a solution/module project that either references ng package projects or contains ng package binary/zip, can be a resources project.
	 */
	private boolean notContainedByOtherProjectsAfterUnloads(String referencingProjectName, String packageName,
		Map<String, Pair<Set<String>, Set<IPackageReader>>> ngPackageChangesPerReferencingProject)
	{
		for (String currentProjectName : projectNameToContainedPackages.keySet())
		{
			if (currentProjectName.equals(referencingProjectName)) continue;
			if (projectNameToContainedPackages.get(currentProjectName) != null &&
				projectNameToContainedPackages.get(currentProjectName).containsKey(packageName))
			{
				// check that it won't be removed from this other found referencing project as well
				Pair<Set<String>, Set<IPackageReader>> changesForCurrentProject = ngPackageChangesPerReferencingProject.get(currentProjectName);
				if (changesForCurrentProject == null || !changesForCurrentProject.getLeft().contains(packageName)) return false; // getLeft() is contains the stuff that will be unloaded
			}
		}
		return true;
	}

	/**
	 * Collects all package project readers of the active solution and its modules into componentReaders and caches a list of readers per each solution project into @{projectNameToContainedPackages}
	 */
	private void collectNGPackageProjects(Map<String, IPackageReader> componentReaders, IProgressMonitor m)
	{
		ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeSolutionProject != null)
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				ServoyNGPackageProject[] ngPackageProjects = servoyProject.getNGPackageProjects();
				for (ServoyNGPackageProject servoyNGPackageProject : ngPackageProjects)
				{
					collectReferencedProjectAsPackageReader(componentReaders, servoyProject.getProject().getName(), servoyNGPackageProject.getProject(), m);
				}
			}
			m.worked(8);
		}
	}

	protected void collectResourcesProjectNGPackages(boolean reloadComponents, boolean reloadServices, IProgressMonitor m,
		Map<String, IPackageReader> newComponents)
	{
		ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		SubMonitor monitor = SubMonitor.convert(m, 8);
		if (activeResourcesProject != null)
		{
			final IFolder components = activeResourcesProject.getProject().getFolder(SolutionSerializer.COMPONENTS_DIR_NAME);
			scheduleSystemJob(new Job("Removing any duplicate markers and refreshing...")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					monitor.beginTask("Refreshing...", 5);
					try
					{
						if (components != null && components.exists())
						{
							components.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
							monitor.setTaskName("Removing markers...");
							components.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_INFINITE);
							monitor.worked(3);
							monitor.setTaskName("Refreshing...");
							components.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
						}
					}
					catch (CoreException ex)
					{
						ServoyLog.logError(ex);
					}
					monitor.done();
					return Status.OK_STATUS;
				}
			}, components.getParent());
			monitor.setWorkRemaining(25);
			final IFolder services = activeResourcesProject.getProject().getFolder(SolutionSerializer.SERVICES_DIR_NAME);
			scheduleSystemJob(new Job("Removing any duplicate markers and refreshing...")
			{
				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					monitor.beginTask("Refreshing...", 5);
					try
					{
						if (services != null && services.exists())
						{
							services.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
							monitor.setTaskName("Removing markers...");
							services.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_INFINITE);
							monitor.setTaskName("Refreshing...");
							monitor.worked(3);
							services.refreshLocal(IResource.DEPTH_INFINITE, new SubProgressMonitor(monitor, 1));
						}
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
					monitor.done();
					return Status.OK_STATUS;
				}
			}, services.getParent());
			monitor.setWorkRemaining(23);
			if (reloadComponents)
			{
				readParentOfPackagesDir(newComponents, activeResourcesProject.getProject(), SolutionSerializer.COMPONENTS_DIR_NAME, monitor.newChild(2));
			}
			monitor.setWorkRemaining(21);
			if (reloadServices)
			{
				readParentOfPackagesDir(newComponents, activeResourcesProject.getProject(), SolutionSerializer.SERVICES_DIR_NAME, monitor.newChild(2));
			}
		}
		monitor.done();
	}

	/**
	 * Transform this package project into a package reader and add it to newComponentReaders and to our local map
	 */
	private void collectReferencedProjectAsPackageReader(Map<String, IPackageReader> newComponentReaders, String solutionProjectName, IProject project,
		IProgressMonitor m)
	{
		try
		{
			refreshResourceLater(project, IResource.DEPTH_INFINITE);
			removeSpecMarkersInJob(project);

			IPackageReader reader = readPackageResource(project);
			if (reader != null)
			{
				// see if it is a component package or a service package
				if (IPackageReader.WEB_COMPONENT.equals(reader.getPackageType()) || IPackageReader.WEB_LAYOUT.equals(reader.getPackageType()) ||
					IPackageReader.WEB_SERVICE.equals(reader.getPackageType()))
				{
					newComponentReaders.put(reader.getPackageName(), reader);
					getProjectContainedPackagesMap(solutionProjectName).put(reader.getPackageName(), reader);
				}
				else
				{
					addErrorMarker(project,
						"NG Package Project '" + project.getName() + "' cannot be loaded; no web component or service found in it's manifest file.", null);
				}
			}
			else
			{
				addErrorMarker(project, "NG Package Project '" + project.getName() +
					" cannot be loaded; please check the contents/structure of that project. Does it have a manifest file?", null);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * Checks whether or not a default (Servoy supplied) component or service package should be loaded/used.
	 * This for example when running from the UI can be changed in Servoy preferences pages; command line exporter could get this info as an argument.
	 *
	 * @param packageName the name of the package to be checked
	 * @return true if the package should be used; false otherwise.
	 */
	protected abstract boolean isDefaultPackageEnabled(String packageName);

	protected void readParentOfPackagesDir(Map<String, IPackageReader> readers, IProject iProject, String folderName, IProgressMonitor m)
	{
		IFolder folder = iProject.getFolder(folderName);
		if (folder.exists())
		{
			try
			{
				refreshResourceLater(folder, IResource.DEPTH_INFINITE);
				IResource[] members = folder.members();
				m.beginTask("Reading packages", members.length);
				for (IResource resource : members)
				{
					IPackageReader reader = readPackageResource(resource);
					if (reader != null)
					{
						readers.put(reader.getPackageName(), reader);
						getProjectContainedPackagesMap(iProject.getName()).put(reader.getPackageName(), reader);
					}
					m.worked(1);
				}
				m.done();
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private Map<String, IPackageReader> getProjectContainedPackagesMap(String projectName)
	{
		Map<String, IPackageReader> map = projectNameToContainedPackages.get(projectName);
		if (map == null)
		{
			map = new HashMap<>();
			projectNameToContainedPackages.put(projectName, map);
		}
		return map;

	}

	protected IPackageReader readPackageResource(IResource resource)
	{
		if (resource instanceof IContainer)
		{
			if (((IContainer)resource).getFile(new Path("META-INF/MANIFEST.MF")).exists())
			{
				return new ContainerPackageReader(new File(resource.getLocationURI()), (IContainer)resource);
			}
		}
		else if (resource instanceof IFile)
		{
			String name = resource.getName();
			int index = name.lastIndexOf('.');
			if (index != -1)
			{
				name = name.substring(0, index);
			}
			if (resource.getName().endsWith(".zip") || resource.getName().endsWith(".jar"))
			{
				ZipFilePackageReader reader = new ZipFilePackageReader(resource);
				if (reader.getPackageType() == null) return null;
				return reader;
			}
		}
		return null;
	}

	protected static void addErrorMarker(IResource resource, final String markerMessage, final Exception e)
	{
		IResource res = resource;
		if (!res.exists())
		{
			res = res.getParent();
		}
		final IResource r = res;

		scheduleSystemJob(new Job("Adding spec error marker...")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					if (r != null && r.isAccessible())
					{
						IMarker marker = null;
						if (e instanceof DuplicateEntityException)
						{
							r.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_ONE);
							marker = r.createMarker(DUPLICATE_COMPONENT_MARKER);
						}
						else
						{
							marker = r.createMarker(SPEC_READ_MARKER);
						}
						marker.setAttribute(IMarker.MESSAGE, (markerMessage != null) ? markerMessage : e.getMessage());
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
						marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
						marker.setAttribute(IMarker.LOCATION, r.getLocation().toString());
					}
				}
				catch (CoreException ex)
				{
					ServoyLog.logError(ex);
				}
				return Status.OK_STATUS;
			}
		}, r);
	}

	protected ServoyNGPackageProject[] getReferencedNGPackageProjectsInternal()
	{
		return referencedNGPackageProjects == null ? new ServoyNGPackageProject[0] : referencedNGPackageProjects;
	}

	public static class ContainerPackageReader extends DirPackageReader
	{
		private final IContainer container;

		public ContainerPackageReader(File dir, IContainer folder)
		{
			super(dir);
			this.container = folder;
		}

		@Override
		public Manifest getManifest() throws IOException
		{
			if (manifest != null) return manifest;
			IFile file = container.getFile(new Path("META-INF/MANIFEST.MF"));
			removeSpecMarkersInJob(file);
			try
			{
				return super.getManifest();
			}
			catch (IOException ex)
			{
				addErrorMarker(file, null, ex);
				throw ex;
			}
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			IFile file = container.getFile(new Path(path));
			if (file != null && file.exists())
			{
				removeSpecMarkersInJob(file);
			}
			return super.readTextFile(path, charset);
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			super.reportError(specpath, e);
			addErrorMarker(e instanceof DuplicateEntityException ? container : container.getFile(new Path(specpath)), null, e);
		}

		@Override
		public int hashCode()
		{
			return container.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof ContainerPackageReader)
			{
				return container.equals(((ContainerPackageReader)obj).container);
			}
			return false;
		}
	}

	private class ZipFilePackageReader extends Package.ZipPackageReader
	{
		private final IResource resource;

		public ZipFilePackageReader(IResource resource)
		{
			super(new File(resource.getLocationURI()), resource.getName().substring(0, resource.getName().length() - 4));
			this.resource = resource;
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			super.reportError(specpath, e);
			addErrorMarker(resource, null, e);
		}
	}

	public void dispose()
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		ResourceProvider.setPackages(new ArrayList<IPackageReader>());
	}

	/**
	 * Gets all the project names of referencing projects that have loaded the given package (by package name) from the given source (the projectDir).
	 */
	public List<String> getReferencingProjectsThatLoaded(String ngPackageName, File projectDir)
	{
		List<String> referencingProjectsThatLoadedIt = new ArrayList<String>();
		for (Entry<String, Map<String, IPackageReader>> e : projectNameToContainedPackages.entrySet())
		{
			IPackageReader reader = e.getValue().get(ngPackageName);
			if (reader != null && projectDir.equals(reader.getResource())) referencingProjectsThatLoadedIt.add(e.getKey());
		}
		return referencingProjectsThatLoadedIt;
	}

	/**
	 * Get the names of the currently loaded/cached package project references of the given (solution) project.
	 */
	public List<String> getOldUsedPackageProjectsList(String solutionProjectName, IWorkspaceRoot workspaceRoot)
	{
		List<String> result = new ArrayList<>();
		Map<String, IPackageReader> map = getProjectContainedPackagesMap(solutionProjectName);
		for (IPackageReader containedPackage : map.values())
		{
			// get only the referenced web package projects, ignore the contained binary packages
			IContainer[] containers = workspaceRoot.findContainersForLocationURI(containedPackage.getResource().toURI());
			if (containers.length == 1 && containers[0] instanceof IProject) result.add(containers[0].getName());
		}
		return result;
	}

	protected static void refreshResourceLater(final IResource resource, final int levels)
	{
		scheduleSystemJob(new Job("Refreshing package resources...")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				try
				{
					resource.refreshLocal(levels, monitor);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
				return Status.OK_STATUS;
			}
		}, (resource.getProject() == resource || resource.getWorkspace().getRoot() == resource) ? resource : resource.getParent()); // refresh actually needs the parent rule for non-projects and non-workspace-root, cause the resource itself might have dissapeared
	}

	protected static void removeSpecMarkersInJob(final IResource resource)
	{
		scheduleSystemJob(new Job("Removing spec file markers...")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				if (resource != null && resource.isAccessible())
				{
					try
					{
						resource.deleteMarkers(SPEC_READ_MARKER, false, IResource.DEPTH_ONE);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
				return Status.OK_STATUS;
			}
		}, resource);
	}

	protected static void scheduleSystemJob(Job job, IResource r)
	{
		job.setRule(r);
		job.setSystem(true);
		job.setUser(false);
		job.schedule();
	}

}
