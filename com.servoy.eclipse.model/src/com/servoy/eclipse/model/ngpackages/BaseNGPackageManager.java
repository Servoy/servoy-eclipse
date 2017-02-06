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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Attributes;
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
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.Activator;
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
	public static final String DUPLICATE_COMPONENT_MARKER = "com.servoy.eclipse.debug.DUPLICATE_COMPONENT_MARKER";
	public static final String SPEC_READ_MARKER = "com.servoy.eclipse.debug.SPEC_READ_MARKER";

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
		all.addAll(Arrays.asList(WebComponentSpecProvider.getSpecProviderState().getAllPackageReaders()));
		all.addAll(Arrays.asList(WebServiceSpecProvider.getSpecProviderState().getAllPackageReaders()));
		return all;
	}


	public ServoyNGPackageProject[] getReferencedNGPackageProjects()
	{
		if (referencedNGPackageProjects == null)
		{
			HashSet<ServoyNGPackageProject> referencedNGPackageProjectsSet = new HashSet<ServoyNGPackageProject>();
			ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
			if (activeSolutionProject != null)
			{
				ServoyNGPackageProject[] ngPackageProjects = activeSolutionProject.getNGPackageProjects();
				Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
				ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
				for (ServoyProject module : modulesOfActiveProject)
				{
					ngPackageProjects = module.getNGPackageProjects();
					Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
				}
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
		Map<String, List<IPackageReader>> allPackageReaders = new HashMap<>();
		monitor.subTask("Preparing to reload resources project ng packages");
		collectResourcesProjectNGPackages(monitor.newChild(8), allPackageReaders);
		monitor.worked(30);
		monitor.subTask("Preparing to reload referenced ng package projects");
		collectNGPackageProjects(allPackageReaders, monitor.newChild(8));
		monitor.worked(30);
		monitor.subTask("Preparing to reload solution contained binary ng packages");
		collectSolutionContainedBinaryPackages(allPackageReaders, monitor.newChild(8));
		monitor.worked(30);
		monitor.subTask("Reloading NG packages");
		ResourceProvider.setPackages(allPackageReaders.values());

		monitor.worked(9);
		monitor.setTaskName("Announcing ng packages load");
		ngPackagesChanged(false);
		monitor.worked(1);
		monitor.done();
	}

	private void collectSolutionContainedBinaryPackages(Map<String, List<IPackageReader>> packageReaders, SubMonitor monitor)
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
						IResource[] members = folder.members();
						for (IResource resource : members)
						{
							IPackageReader reader = readPackageResource(resource);
							if (reader != null)
							{
								List<IPackageReader> list = packageReaders.get(reader.getPackageName());
								if (list == null)
								{
									list = new ArrayList<>();
									packageReaders.put(reader.getPackageName(), list);
								}
								list.add(reader);
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
	protected void updateFromResourceChangeListener(Map<String, Pair<Set<IPackageReader>, Set<IPackageReader>>> ngPackageChangesPerReferencingProject)
	{
		if (ngPackageChangesPerReferencingProject == null || ngPackageChangesPerReferencingProject.size() == 0) return; // no changes

		boolean reallyChanged = false;

		// for each change add/remove we first check that the project isn't already loaded in the spec readers
		// and also split up into services and non-services

		// in case multiple modules of the active solution reference the same ng package project, if only one reference is removed we don't need to unload that project
		// (if the project is changed, not removed/dereferenced a remove of all locations will be done and then a re-load - so we can detect that and unload then anyway)
		Set<IPackageReader> componentsReallyToUnload = new HashSet<>();
		Set<IPackageReader> serviceReallyToUnload = new HashSet<>();
		Map<File, IPackageReader> componentsReallyToLoad = new HashMap<>();
		Map<File, IPackageReader> serviceReallyToLoad = new HashMap<>();

		for (String projectName : ngPackageChangesPerReferencingProject.keySet())
		{
			Pair<Set<IPackageReader>, Set<IPackageReader>> projectChanges = ngPackageChangesPerReferencingProject.get(projectName);
			Set<IPackageReader> packagesToUnload = projectChanges.getLeft();
			Set<IPackageReader> packagesToLoad = projectChanges.getRight();

			// first update our cache
			Map<String, IPackageReader> projectContainedPackagesMap = getProjectContainedPackagesMap(projectName);
			for (IPackageReader pr : packagesToUnload)
			{
				// TODO get to the key
				projectContainedPackagesMap.remove(pr.getPackageName());
			}
			for (IPackageReader iPackageReader : packagesToLoad)
			{
				if (iPackageReader == null) continue;//spec reader marker will show in this case
				projectContainedPackagesMap.put(iPackageReader.getPackageName(), iPackageReader);
			}


			SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
			for (IPackageReader pr : packagesToUnload)
			{
				if (notContainedByOtherProjectsAfterUnloads(projectName, pr, ngPackageChangesPerReferencingProject))
				{
					if (componentsSpecProviderState.getPackageNames().contains(pr.getPackageName())) componentsReallyToUnload.add(pr);
					else serviceReallyToUnload.add(pr);
				}
			}

			for (IPackageReader packageReader : packagesToLoad)
			{
				if (packageReader == null) continue;//spec reader marker will show in this case
				if (notContainedByOtherProjectsAfterUnloads(projectName, packageReader, ngPackageChangesPerReferencingProject))
				{
					if (IPackageReader.WEB_SERVICE.equals(packageReader.getPackageType())) serviceReallyToLoad.put(packageReader.getResource(), packageReader); // could potentially replace another package reader in case it's referenced from multiple solution projects
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
	private boolean notContainedByOtherProjectsAfterUnloads(String referencingProjectName, IPackageReader pr,
		Map<String, Pair<Set<IPackageReader>, Set<IPackageReader>>> ngPackageChangesPerReferencingProject)
	{
		for (String currentProjectName : projectNameToContainedPackages.keySet())
		{
			if (currentProjectName.equals(referencingProjectName)) continue;
			if (projectNameToContainedPackages.get(currentProjectName) != null && projectNameToContainedPackages.get(currentProjectName).containsValue(pr))
			{
				// check that it won't be removed from this other found referencing project as well
				Pair<Set<IPackageReader>, Set<IPackageReader>> changesForCurrentProject = ngPackageChangesPerReferencingProject.get(currentProjectName);
				if (changesForCurrentProject == null || !changesForCurrentProject.getLeft().contains(pr)) return false; // getLeft() is contains the stuff that will be unloaded
			}
		}
		return true;
	}

	/**
	 * Collects all package project readers of the active solution and its modules into componentReaders and caches a list of readers per each solution project into @{projectNameToContainedPackages}
	 */
	private void collectNGPackageProjects(Map<String, List<IPackageReader>> componentReaders, IProgressMonitor m)
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

	protected void collectResourcesProjectNGPackages(IProgressMonitor m, Map<String, List<IPackageReader>> newPackages)
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
			readParentOfPackagesDir(newPackages, activeResourcesProject.getProject(), SolutionSerializer.COMPONENTS_DIR_NAME, monitor.newChild(2));
			monitor.setWorkRemaining(21);
			readParentOfPackagesDir(newPackages, activeResourcesProject.getProject(), SolutionSerializer.SERVICES_DIR_NAME, monitor.newChild(2));
		}
		monitor.done();
	}

	/**
	 * Transform this package project into a package reader and add it to newComponentReaders and to our local map
	 */
	private void collectReferencedProjectAsPackageReader(Map<String, List<IPackageReader>> newPackageReaders, String solutionProjectName, IProject project,
		IProgressMonitor m)
	{
		m.beginTask("Reading package project '" + project.getName() + "'.", 1);
		try
		{
			IPackageReader reader = readPackageResource(project);
			m.worked(1);
			if (reader != null)
			{
				List<IPackageReader> list = newPackageReaders.get(reader.getPackageName());
				if (list == null)
				{
					list = new ArrayList<>();
					newPackageReaders.put(reader.getPackageName(), list);
				}
				list.add(reader);
				getProjectContainedPackagesMap(solutionProjectName).put(reader.getPackageName(), reader);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		finally
		{
			m.done();
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

	protected void readParentOfPackagesDir(Map<String, List<IPackageReader>> readers, IProject iProject, String folderName, IProgressMonitor m)
	{
		IFolder folder = iProject.getFolder(folderName);
		if (folder.exists())
		{
			try
			{
				IResource[] members = folder.members();
				m.beginTask("Reading packages", members.length);
				for (IResource resource : members)
				{
					IPackageReader reader = readPackageResource(resource);
					if (reader != null)
					{
						List<IPackageReader> list = readers.get(reader.getPackageName());
						if (list == null)
						{
							list = new ArrayList<>();
							readers.put(reader.getPackageName(), list);
						}
						list.add(reader);
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
		IPackageReader reader = null;

		if (resource instanceof IContainer)
		{
			if (((IContainer)resource).getFile(new Path("META-INF/MANIFEST.MF")).exists())
			{
				reader = new ContainerPackageReader(new File(resource.getLocationURI()), (IContainer)resource);
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
				reader = new ZipFilePackageReader(resource);
			}
		}

		// see if it could be read and if it is a component package or a service package or a layout package; one of those must be present, otherwise it will not get loaded
		reader = verifyNGPackage(resource, reader);

		return reader;
	}

	private IPackageReader verifyNGPackage(IResource resource, IPackageReader reader)
	{
		refreshResourceLater(resource, IResource.DEPTH_INFINITE);
		removeSpecMarkersInJob(resource);

		if (reader == null)
		{
			addErrorMarker(resource,
				"NG Package '" + resource.getName() + " cannot be loaded; please check the contents/structure of that package. Does it have a manifest file?",
				null);
		}
		else if (!(IPackageReader.WEB_COMPONENT.equals(reader.getPackageType()) || IPackageReader.WEB_LAYOUT.equals(reader.getPackageType()) ||
			IPackageReader.WEB_SERVICE.equals(reader.getPackageType())))
		{
			addErrorMarker(resource,
				"NG Package '" + resource.getName() +
					"' cannot be loaded; the manifest file does not declare Package-Type correctly (must be one of Web-Component, Web-Service or Web-Layout)",
				null);
			return null;
		}

		return reader;
	}

	protected static void clearErrorMarker(IResource resource)
	{
		IResource res = resource;
		// I think this search in parents is for unzipped packages only - where a file nested somewhere inside the container is given here as resource - which means
		// that the parent search should always end at most in ng package dir, not higher (so normally when this mehod gets called the ng package root container should always exist);
		// so remove markers call will remove markers correctly when it is called on the ng package dir or zip directly (when that package is reloaded...)
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
						r.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_ONE);
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

	protected static void addErrorMarker(IResource resource, final String markerMessage, final Exception e)
	{
		IResource res = resource;
		// I think this search in parents is for unzipped packages only - where a file nested somewhere inside the container is given here as resource - which means
		// that the parent search should always end at most in ng package dir, not higher (so normally when this mehod gets called the ng package root container should always exist);
		// so remove markers call will remove markers correctly when it is called on the ng package dir or zip directly (when that package is reloaded...)
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
		public void clearError()
		{
			clearErrorMarker(container);
		}


		@Override
		public String getPackageType()
		{
			try
			{
				final Manifest man = getManifest();
				if (man.getMainAttributes().getValue("Package-Type") != null) return Package.getPackageType(man);
				else
				{
					String result = Package.getPackageType(man);
					if (result != null)
					{
						// this package does not have the 'Package-Type' attribute, but it does contain at least one item
						// so if we know the kind of package, we should add the type to the manifest of this DirPackage

						man.getMainAttributes().put(new Attributes.Name(Package.PACKAGE_TYPE), result);
						File mfFile = new File(dir, "META-INF/MANIFEST.MF");

						final IFile[] workspaceMFs = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(mfFile.toURI());
						if (workspaceMFs.length == 1 && workspaceMFs[0] != null)
						{
							// we do this later as currently we are executing during a read/get (maybe resource change listener notification) - and we might not be allowed to write or event want to write right away
							scheduleSystemJob(new Job("Updating manifest file in package from '" + dir.getAbsolutePath() + "'; auto-adding package type...")
							{
								@Override
								protected IStatus run(IProgressMonitor monitor)
								{
									if (workspaceMFs[0].exists())
									{
										try
										{
											ByteArrayOutputStream contentWriter = new ByteArrayOutputStream(1024);
											man.write(contentWriter);

											workspaceMFs[0].setContents(new ByteArrayInputStream(contentWriter.toByteArray()),
												IResource.FORCE | IResource.KEEP_HISTORY, monitor);
										}
										catch (IOException | CoreException e)
										{
											ServoyLog.logError(e);
											return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to auto-add package type to manifest file of '" +
												dir.getAbsolutePath() + "'. Check workspace log for more details.");
										}
									}
									return Status.OK_STATUS;
								}
							}, workspaceMFs[0]);
						}

					}

					return result;
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error getting package type." + getName(), e);
			}
			return null;
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

		@Override
		public void clearError()
		{
			clearErrorMarker(resource);
		}
	}

	public void dispose()
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		ResourceProvider.setPackages(new ArrayList<List<IPackageReader>>());
	}

	/**
	 * Gets all the project names of referencing projects that have loaded the given package (by package name) from the given source (the "romResource").
	 */
	public List<String> getReferencingProjectsThatLoaded(String ngPackageName, File fromResource)
	{
		List<String> referencingProjectsThatLoadedIt = new ArrayList<String>();
		for (Entry<String, Map<String, IPackageReader>> e : projectNameToContainedPackages.entrySet())
		{
			IPackageReader reader = e.getValue().get(ngPackageName);
			if (reader != null && fromResource.equals(reader.getResource())) referencingProjectsThatLoadedIt.add(e.getKey());
		}
		return referencingProjectsThatLoadedIt;
	}

	/**
	 * Gets all the project names of referencing projects and the location (zip or project) from where the package was loaded.
	 */
	public List<Pair<String, File>> getReferencingProjectsThatLoaded(String ngPackageName)
	{
		List<Pair<String, File>> referencingProjectsThatLoadedIt = new ArrayList<>();
		for (Entry<String, Map<String, IPackageReader>> e : projectNameToContainedPackages.entrySet())
		{
			IPackageReader reader = e.getValue().get(ngPackageName);
			if (reader != null) referencingProjectsThatLoadedIt.add(new Pair<>(e.getKey(), reader.getResource()));
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
		}, (resource.getProject() == resource || resource.getWorkspace().getRoot() == resource) ? resource : resource.getParent()); // refresh actually needs the parent rule for non-projects and non-workspace-root, cause the resource itself might have disappeared
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
						resource.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_ONE);
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
