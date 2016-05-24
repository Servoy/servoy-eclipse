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
import java.util.Set;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.sablo.specification.Package;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.DuplicatePackageException;
import org.sablo.specification.Package.IPackageReader;

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

	private final Map<String, IPackageReader> resourcesProjectComponentReaders = new HashMap<String, IPackageReader>(); // resource name (folder name or archive name without extension) -> package reader
	private final Map<String, IPackageReader> resourcesProjectServiceReaders = new HashMap<String, IPackageReader>(); // resource name (folder name or archive name without extension) -> package reader

	private final Map<String, IPackageReader> ngPackageProjectComponentReaders = new HashMap<String, IPackageReader>(); // resource name (project name) -> package reader
	private final Map<String, IPackageReader> ngPackageProjectServiceReaders = new HashMap<String, IPackageReader>(); // resource name (project name) -> package reader

	private ServoyNGPackageProject[] referencedNGPackageProjects;
	private Set<String> activeSolutionReferencedProjectNames = new HashSet<>();

	private final List<INGPackageChangeListener> webResourceChangedListeners = Collections.synchronizedList(new ArrayList<INGPackageChangeListener>());

	public BaseNGPackageManager()
	{
		if (ServoyModelFinder.getServoyModel().getActiveProject() != null) reloadAllNGPackages(null, true); // initial load

		resourceChangeListener = new BaseNGPackageResourcesChangedListener(this);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
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

	public void clearActiveSolutionReferencesCache()
	{
		referencedNGPackageProjects = null;
		activeSolutionReferencedProjectNames = new HashSet<>();
	}

	public void addNGPackagesChangedListener(INGPackageChangeListener listener)
	{
		webResourceChangedListeners.add(listener);
	}

	public void removeNGPackagesChangedListener(INGPackageChangeListener listener)
	{
		webResourceChangedListeners.remove(listener);
	}

	public void ngPackagesChanged(boolean components, boolean services)
	{
		for (INGPackageChangeListener listener : webResourceChangedListeners)
		{
			listener.ngPackageChanged(components, services);
		}
	}

	public void ngPackageProjectListChanged()
	{
		for (INGPackageChangeListener listener : webResourceChangedListeners)
		{
			listener.ngPackageProjectListChanged();
		}
	}

	public ServoyNGPackageProject[] getReferencedNGPackageProjects()
	{
		if (referencedNGPackageProjects == null)
		{
			ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
			HashSet<ServoyNGPackageProject> referencedNGPackageProjectsSet = new HashSet<ServoyNGPackageProject>();
			ServoyNGPackageProject[] ngPackageProjects = activeSolutionProject.getNGPackageProjects();
			Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
			try
			{
				IProject[] activeSolutionReferencedProjects = activeSolutionProject.getProject().getDescription().getReferencedProjects();
				activeSolutionReferencedProjectNames = new HashSet<>();
				for (IProject p : activeSolutionReferencedProjects)
					activeSolutionReferencedProjectNames.add(p.getName());
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject module : modulesOfActiveProject)
			{
				ngPackageProjects = module.getNGPackageProjects();
				Collections.addAll(referencedNGPackageProjectsSet, ngPackageProjects);
				for (ServoyNGPackageProject packageProject : ngPackageProjects)
				{
					activeSolutionReferencedProjectNames.add(packageProject.getProject().getName());
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
	public void reloadAllNGPackages(IProgressMonitor m, boolean canChangeResources)
	{
		setRemovedPackages();

		SubMonitor monitor = SubMonitor.convert(m, "Reloading all ng packages", 100);


		Map<String, IPackageReader> componentResourcesProjectReaders = new HashMap<>();
		Map<String, IPackageReader> serviceResourcesProjectReaders = new HashMap<>();


		Map<String, IPackageReader> componentProjectReaders = new HashMap<String, IPackageReader>();
		Map<String, IPackageReader> serviceProjectReaders = new HashMap<String, IPackageReader>();

		monitor.subTask("Preparing to reload resources project ng packages");
		prepareReloadOfResourcesProjectNGPackages(true, true, monitor.newChild(8), componentResourcesProjectReaders, serviceResourcesProjectReaders,
			canChangeResources);
		monitor.subTask("Preparing to reload referenced ng package projects");
		prepareToReloadNGPackageProjects(componentProjectReaders, serviceProjectReaders, monitor.newChild(8), canChangeResources);

		monitor.subTask("Preparing to reload solution contained binary packages");
		prepareToReloadSolutionContainedBinaryPackages(componentProjectReaders, serviceProjectReaders, monitor.newChild(8), canChangeResources);

		Map<String, IPackageReader> allComponentsToLoad = new HashMap<>();
		allComponentsToLoad.putAll(componentResourcesProjectReaders);
		allComponentsToLoad.putAll(componentProjectReaders);

		Map<String, IPackageReader> allServicesToLoad = new HashMap<>();
		allServicesToLoad.putAll(serviceResourcesProjectReaders);
		allServicesToLoad.putAll(serviceProjectReaders);

		Map<String, IPackageReader> allComponentsToUnload = new HashMap<>();
		allComponentsToUnload.putAll(resourcesProjectComponentReaders);
		allComponentsToUnload.putAll(ngPackageProjectComponentReaders);

		Map<String, IPackageReader> allServicesToUnload = new HashMap<>();
		allServicesToUnload.putAll(resourcesProjectServiceReaders);
		allServicesToUnload.putAll(ngPackageProjectServiceReaders);

		monitor.subTask("Reloading component packages");
		boolean componentsReloaded = (allComponentsToUnload.size() > 0 || allComponentsToLoad.size() > 0);
		ResourceProvider.updateComponentResources(allComponentsToUnload.keySet(), allComponentsToLoad.values());
		monitor.worked(62);
		resourcesProjectComponentReaders.clear();
		resourcesProjectComponentReaders.putAll(componentResourcesProjectReaders);
		ngPackageProjectComponentReaders.clear();
		ngPackageProjectComponentReaders.putAll(componentProjectReaders);

		monitor.subTask("Reloading service packages");
		boolean servicesReloaded = (allServicesToUnload.size() > 0 || allServicesToLoad.size() > 0);
		ResourceProvider.updateServiceResources(allServicesToUnload.keySet(), allServicesToLoad.values());
		monitor.worked(21);
		resourcesProjectServiceReaders.clear();
		resourcesProjectServiceReaders.putAll(serviceResourcesProjectReaders);
		ngPackageProjectServiceReaders.clear();
		ngPackageProjectServiceReaders.putAll(serviceProjectReaders);

		monitor.setTaskName("Announcing ng packages load");
		if (componentsReloaded || servicesReloaded) ngPackagesChanged(componentsReloaded, servicesReloaded);
		monitor.worked(1);
		monitor.done();
	}

	/**
	 * @param activeSolutionProject
	 * @param componentProjectReaders
	 * @param serviceProjectReaders
	 * @param newChild
	 * @param canChangeResources
	 */
	private void prepareToReloadSolutionContainedBinaryPackages(Map<String, IPackageReader> componentProjectReaders,
		Map<String, IPackageReader> serviceProjectReaders, SubMonitor monitor, boolean canChangeResources)
	{
		ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();

		Map<String, Map<String, IPackageReader>> packagesTypeToReaders = new HashMap<String, Map<String, IPackageReader>>();
		packagesTypeToReaders.put(IPackageReader.WEB_COMPONENT, componentProjectReaders);
		packagesTypeToReaders.put(IPackageReader.WEB_LAYOUT, componentProjectReaders);
		packagesTypeToReaders.put(IPackageReader.WEB_SERVICE, serviceProjectReaders);

		for (ServoyProject solution : modules)
		{
			IProject project = solution.getProject();
			{
				IFolder folder = project.getFolder(SolutionSerializer.NG_PACKAGES_DIR_NAME);
				if (folder.exists())
				{
					try
					{
						if (canChangeResources) folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
						IResource[] members = folder.members();
						for (IResource resource : members)
						{
							Pair<String, IPackageReader> nameAndReader = readPackageResource(resource);
							if (nameAndReader != null)
							{
								String packageType = nameAndReader.getRight().getPackageType();
								packagesTypeToReaders.get(packageType).put(nameAndReader.getLeft(), nameAndReader.getRight());
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
	}

	/**
	 * Reloads all ng packages from the active resources project.
	 * IMPORTANT: only call this if these packages are the only ones that changed (so there were no changes in the ng packages from the solution project
	 * referenced ng package projects; cause if for example a package is moved between the resources project and a separate ng pacakge project it would
	 * error out because it might not be unloaded properly before being loaded again if you sequentially call reload on resources project packages and on ng package projects)
	 */
	protected void reloadResourcesProjectNGPackages(boolean reloadComponents, boolean reloadServices, IProgressMonitor m, boolean canChangeResources)
	{
		SubMonitor monitor = SubMonitor.convert(m, "Reloading ng packages from resources project", 27);
		Map<String, IPackageReader> newComponents = new HashMap<>();
		Map<String, IPackageReader> newServices = new HashMap<>();

		prepareReloadOfResourcesProjectNGPackages(reloadComponents, reloadServices, monitor.newChild(8), newComponents, newServices, canChangeResources);
		monitor.setWorkRemaining(19);

		boolean componentsReloaded = false;
		boolean servicesReloaded = false;

		if (reloadComponents)
		{
			componentsReloaded = (resourcesProjectComponentReaders.size() > 0 || newComponents.size() > 0);
			ResourceProvider.updateComponentResources(resourcesProjectComponentReaders.keySet(), newComponents.values());
			monitor.worked(9);
			resourcesProjectComponentReaders.clear();
			resourcesProjectComponentReaders.putAll(newComponents);
		}
		monitor.setWorkRemaining(10);

		if (reloadServices)
		{
			servicesReloaded = (resourcesProjectServiceReaders.size() > 0 || newServices.size() > 0);
			ResourceProvider.updateServiceResources(resourcesProjectServiceReaders.keySet(), newServices.values());
			monitor.worked(9);
			resourcesProjectServiceReaders.clear();
			resourcesProjectServiceReaders.putAll(newServices);
		}
		monitor.setWorkRemaining(1);
		monitor.setTaskName("Ng packages were loaded; notifying interested parties");

		if (componentsReloaded || servicesReloaded) ngPackagesChanged(componentsReloaded, servicesReloaded);
		monitor.worked(1);
		monitor.done();
	}

	/**
	 * Reloads all ng packages that are referenced projects of the active solution project.
	 *
	 * IMPORTANT: only call this if these packages are the only ones that changed (so there were no changes in the ng packages from the resouces project; cause if
	 * for example a package is moved between the resources project and a separate ng pacakge project it would error out because it might not be unloaded
	 * properly before being loaded again if you sequentially call reload on resources project packages and on ng package projects)
	 */
	protected void reloadAllSolutionReferencedPackages(IProgressMonitor m, boolean canChangeResources)
	{
		Map<String, IPackageReader> componentReaders = new HashMap<String, IPackageReader>();
		Map<String, IPackageReader> serviceReaders = new HashMap<String, IPackageReader>();
		SubMonitor monitor = SubMonitor.convert(m, "Reloading all referenced ng package projects", 100);

		prepareToReloadNGPackageProjects(componentReaders, serviceReaders, monitor.newChild(10), canChangeResources);

		prepareToReloadSolutionContainedBinaryPackages(componentReaders, serviceReaders, monitor.newChild(8), canChangeResources);

		monitor.setWorkRemaining((serviceReaders.size() + componentReaders.size()) * 10 + 3);
		HashMap<String, IPackageReader> thisComponentReaders = new HashMap<String, IPackageReader>();
		thisComponentReaders.putAll(ngPackageProjectComponentReaders);
		HashMap<String, IPackageReader> thisServiceReaders = new HashMap<String, IPackageReader>();
		thisServiceReaders.putAll(ngPackageProjectServiceReaders);
		reloadActualSpecs(thisComponentReaders, componentReaders, thisServiceReaders, serviceReaders,
			monitor.newChild((serviceReaders.size() + componentReaders.size()) * 10 + 3));
		monitor.done();
	}

	private void prepareToReloadNGPackageProjects(Map<String, IPackageReader> componentReaders, Map<String, IPackageReader> serviceReaders, IProgressMonitor m,
		boolean canChangeResources)
	{
		ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
		SubMonitor monitor = SubMonitor.convert(m, 10);
		if (activeSolutionProject != null)
		{
			ServoyNGPackageProject[] ngPackageProjects = getReferencedNGPackageProjects();
			monitor.worked(3);

			monitor.setWorkRemaining(ngPackageProjects.length);

			for (ServoyNGPackageProject ngPackageProject : ngPackageProjects)
			{
				IProject project = ngPackageProject.getProject();
				readNewNGPackageProject(componentReaders, serviceReaders, project, monitor.newChild(1), canChangeResources);
			}
		}
		monitor.done();
	}

	protected void prepareReloadOfResourcesProjectNGPackages(boolean reloadComponents, boolean reloadServices, IProgressMonitor m,
		Map<String, IPackageReader> newComponents, Map<String, IPackageReader> newServices, boolean canChangeResources)
	{
		ServoyResourcesProject activeResourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();
		SubMonitor monitor = SubMonitor.convert(m, 8);
		if (activeResourcesProject != null)
		{
			if (canChangeResources)
			{
				try
				{
					IFolder components = activeResourcesProject.getProject().getFolder(SolutionSerializer.COMPONENTS_DIR_NAME);
					if (components != null && components.exists())
					{
						components.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
						components.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_INFINITE);
						components.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
					}
					monitor.setWorkRemaining(25);
					IFolder services = activeResourcesProject.getProject().getFolder(SolutionSerializer.SERVICES_DIR_NAME);
					if (services != null && services.exists())
					{
						services.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
						services.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_INFINITE);
						services.refreshLocal(IResource.DEPTH_INFINITE, monitor.newChild(1));
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
			monitor.setWorkRemaining(23);
			if (reloadComponents)
			{
				readParentOfPackagesDir(newComponents, activeResourcesProject.getProject(), SolutionSerializer.COMPONENTS_DIR_NAME, monitor.newChild(2),
					canChangeResources);
			}
			monitor.setWorkRemaining(21);
			if (reloadServices)
			{
				readParentOfPackagesDir(newServices, activeResourcesProject.getProject(), SolutionSerializer.SERVICES_DIR_NAME, monitor.newChild(2),
					canChangeResources);
			}
		}
		monitor.done();
	}

	/**
	 * Unloads some previously loaded ngPackage projects and loads others as needed (as they change in workspace).
	
	 * IMPORTANT: only call this if these packages are the only ones that changed (so there were no changes in the ng packages from the resouces project; cause if
	 * for example a package is moved between the resources project and a separate ng pacakge project it would error out because it might not be unloaded
	 * properly before being loaded again if you sequentially call reload on resources project packages and on ng package projects)
	 *
	 * @param oldNGPackageProjectsToUnload the projects to unload
	 * @param newNGPackageProjectsToLoad the projects to load
	 */
	protected void reloadNGPackageProjects(List<IProject> oldNGPackageProjectsToUnload, List<IProject> newNGPackageProjectsToLoad, IProgressMonitor m,
		boolean canChangeResources)
	{
		// split them into services and components; prepare readers for new ones in order to do that
		Map<String, IPackageReader> oldNGComponentProjectsToUnload = new HashMap<>();
		Map<String, IPackageReader> oldNGServiceProjectsToUnload = new HashMap<>();

		Map<String, IPackageReader> newNGComponentProjectsToLoad = new HashMap<>();
		Map<String, IPackageReader> newNGServiceProjectsToLoad = new HashMap<>();

		SubMonitor monitor = SubMonitor.convert(m, "Reloading changed referenced ng package projects", newNGPackageProjectsToLoad.size() * 11 + 3);
		// split old/removed
		for (IProject oldP : oldNGPackageProjectsToUnload)
		{
			IPackageReader oldR = ngPackageProjectComponentReaders.get(oldP.getName());
			if (oldR != null)
			{
				oldNGComponentProjectsToUnload.put(oldP.getName(), oldR);
			}
			else
			{
				oldR = ngPackageProjectServiceReaders.get(oldP.getName());
				if (oldR != null)
				{
					oldNGServiceProjectsToUnload.put(oldP.getName(), oldR);
				}
			}
		}

		// read and split new/to be loaded
		for (IProject newP : newNGPackageProjectsToLoad)
		{
			readNewNGPackageProject(newNGComponentProjectsToLoad, newNGServiceProjectsToLoad, newP, monitor.newChild(1), canChangeResources);
		}

		reloadActualSpecs(oldNGComponentProjectsToUnload, newNGComponentProjectsToLoad, oldNGServiceProjectsToUnload, newNGServiceProjectsToLoad,
			monitor.newChild(newNGPackageProjectsToLoad.size() * 10 + 3));
		monitor.done();
	}

	protected void reloadActualSpecs(Map<String, IPackageReader> componentReadersToUnload, Map<String, IPackageReader> componentReadersToLoad,
		Map<String, IPackageReader> serviceReadersToUnload, Map<String, IPackageReader> serviceReadersToLoad, IProgressMonitor monitor)
	{
		boolean componentsReloaded = (componentReadersToUnload.size() > 0 || componentReadersToLoad.size() > 0);
		boolean servicesReloaded = (serviceReadersToUnload.size() > 0 || serviceReadersToLoad.size() > 0);

		monitor.beginTask("Reading actual specs", (componentReadersToLoad.size() + serviceReadersToLoad.size()) * 10 + 3);

		ResourceProvider.updateServiceResources(serviceReadersToUnload.keySet(), serviceReadersToLoad.values());
		monitor.worked(serviceReadersToLoad.size() * 10 + 1);
		for (String x : serviceReadersToUnload.keySet())
		{
			ngPackageProjectServiceReaders.remove(x);
		}
		ngPackageProjectServiceReaders.putAll(serviceReadersToLoad);

		ResourceProvider.updateComponentResources(componentReadersToUnload.keySet(), componentReadersToLoad.values());
		monitor.worked(componentReadersToLoad.size() * 10 + 1);
		for (String x : componentReadersToUnload.keySet())
		{
			ngPackageProjectComponentReaders.remove(x);
		}
		ngPackageProjectComponentReaders.putAll(componentReadersToLoad);

		if (componentsReloaded || servicesReloaded) ngPackagesChanged(componentsReloaded, servicesReloaded);
		monitor.worked(1);
		monitor.done();
	}

	protected void readNewNGPackageProject(Map<String, IPackageReader> newComponentReaders, Map<String, IPackageReader> newServiceReaders, IProject project,
		IProgressMonitor m, boolean canChangeResources)
	{
		try
		{
			if (canChangeResources) project.refreshLocal(IResource.DEPTH_INFINITE, m);
			project.deleteMarkers(SPEC_READ_MARKER, false, IResource.DEPTH_ONE);
			Pair<String, IPackageReader> nameAndReader = readPackageResource(project);
			if (nameAndReader != null)
			{
				// see if it is a component package or a service package
				if (IPackageReader.WEB_SERVICE.equals(nameAndReader.getRight().getPackageType()))
				{
					newServiceReaders.put(nameAndReader.getLeft(), nameAndReader.getRight());
				}
				else if (IPackageReader.WEB_COMPONENT.equals(nameAndReader.getRight().getPackageType()) ||
					IPackageReader.WEB_LAYOUT.equals(nameAndReader.getRight().getPackageType()))
				{
					newComponentReaders.put(nameAndReader.getLeft(), nameAndReader.getRight());
				}
				else
				{
					IMarker marker = project.createMarker(SPEC_READ_MARKER);
					marker.setAttribute(IMarker.MESSAGE,
						"NG Package Project '" + project.getName() + "' cannot be loaded; no web component or service found in it's manifest file.");
					marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
					marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
					marker.setAttribute(IMarker.LOCATION, project.getLocation().toString());
				}
			}
			else
			{
				IMarker marker = project.createMarker(SPEC_READ_MARKER);
				marker.setAttribute(IMarker.MESSAGE, "NG Package Project '" + project.getName() +
					" cannot be loaded; please check the contents/structure of that project. Does it have a manifest file?");
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
				marker.setAttribute(IMarker.LOCATION, project.getLocation().toString());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * Checks whether or not a default (Servoy supplied) component or service packace should be loaded/used.
	 * This for example when running from the UI can be changed in Servoy preferences pages; command line exporter could get this info as an argument.
	 *
	 * @param packageName the name of the package to be checked
	 * @return true if the package should be used; false otherwise.
	 */
	protected abstract boolean isDefaultPackageEnabled(String packageName);

	protected void readParentOfPackagesDir(Map<String, IPackageReader> readers, IProject iProject, String folderName, IProgressMonitor monitor,
		boolean canChangeResources)
	{
		IFolder folder = iProject.getFolder(folderName);
		if (folder.exists())
		{
			try
			{
				if (canChangeResources) folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				IResource[] members = folder.members();
				for (IResource resource : members)
				{
					Pair<String, IPackageReader> nameAndReader = readPackageResource(resource);
					if (nameAndReader != null) readers.put(nameAndReader.getLeft(), nameAndReader.getRight());
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	protected Pair<String, IPackageReader> readPackageResource(IResource resource)
	{
		if (resource instanceof IContainer)
		{
			if (((IContainer)resource).getFile(new Path("META-INF/MANIFEST.MF")).exists())
			{
				return new Pair<String, IPackageReader>(resource.getName(),
					new ContainerPackageReader(new File(resource.getLocationURI()), (IContainer)resource));
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
				return new Pair<String, IPackageReader>(name, reader);
			}
		}
		return null;
	}

	public boolean isComponentPackageProject(String resourceName)
	{
		return ngPackageProjectComponentReaders.containsKey(resourceName);
	}

	public boolean isServicePackageProject(String resourceName)
	{
		return ngPackageProjectServiceReaders.containsKey(resourceName);
	}

	protected static void addErrorMarker(IResource resource, Exception e)
	{
		try
		{
			IMarker marker = null;
			if (e instanceof DuplicatePackageException)
			{
				resource.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_ONE);
				marker = resource.createMarker(DUPLICATE_COMPONENT_MARKER);
			}
			else
			{
				marker = resource.createMarker(SPEC_READ_MARKER);
			}
			marker.setAttribute(IMarker.MESSAGE, e.getMessage());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
			marker.setAttribute(IMarker.LOCATION, resource.getLocation().toString());
		}
		catch (CoreException ex)
		{
			ServoyLog.logError(ex);
		}
	}

	protected ServoyNGPackageProject[] getReferencedNGPackageProjectsInternal()
	{
		return referencedNGPackageProjects == null ? new ServoyNGPackageProject[0] : referencedNGPackageProjects;
	}

	protected Set<String> getActiveSolutionReferencedProjectNamesInternal()
	{
		return activeSolutionReferencedProjectNames;
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
			try
			{
				file.deleteMarkers(SPEC_READ_MARKER, false, IResource.DEPTH_ONE);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
			try
			{
				return super.getManifest();
			}
			catch (IOException ex)
			{
				addErrorMarker(file, ex);
				throw ex;
			}
		}

		@Override
		public String readTextFile(String path, Charset charset) throws IOException
		{
			IFile file = container.getFile(new Path(path));
			if (file != null && file.exists())
			{
				try
				{
					file.deleteMarkers(SPEC_READ_MARKER, false, IResource.DEPTH_ONE);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
			return super.readTextFile(path, charset);
		}

		@Override
		public void reportError(String specpath, Exception e)
		{
			super.reportError(specpath, e);
			addErrorMarker(e instanceof DuplicatePackageException ? container : container.getFile(new Path(specpath)), e);
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
			addErrorMarker(resource, e);
		}
	}

	public void dispose()
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		ResourceProvider.updateComponentResources(resourcesProjectComponentReaders.keySet(), new ArrayList<IPackageReader>());
		ResourceProvider.updateServiceResources(resourcesProjectServiceReaders.keySet(), new ArrayList<IPackageReader>());
	}

}
