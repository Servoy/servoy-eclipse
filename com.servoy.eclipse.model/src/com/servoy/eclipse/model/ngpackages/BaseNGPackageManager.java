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
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.ngclient.startup.resourceprovider.ResourceProvider;

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
	private Set<String> activeSolutionReferencedProjectNames = new HashSet<>();

	private final Map<String, Map<String, IPackageReader>> projectNameToContainedPackages = new HashMap<>();

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
		projectNameToContainedPackages.clear();
		setRemovedPackages();
		SubMonitor monitor = SubMonitor.convert(m, "Reloading all ng packages", 100);//TODO check monitor steps counter
		Map<String, IPackageReader> allPackageReaders = new HashMap<String, IPackageReader>();

		monitor.subTask("Preparing to reload resources project ng packages");
		collectResourcesProjectNGPackages(true, true, monitor.newChild(8), allPackageReaders, canChangeResources);

		monitor.subTask("Preparing to reload referenced ng package projects");
		collectNGPackageProjects(allPackageReaders, monitor.newChild(8), canChangeResources);

		monitor.subTask("Preparing to reload solution contained binary packages");
		collectSolutionContainedBinaryPackages(allPackageReaders, monitor.newChild(8), canChangeResources);

		monitor.subTask("Reloading component packages");
		ResourceProvider.setPackages(allPackageReaders.values());
		monitor.worked(62);
		monitor.worked(21);
		monitor.setTaskName("Announcing ng packages load");
		ngPackagesChanged(true, true);
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
	private void collectSolutionContainedBinaryPackages(Map<String, IPackageReader> componentProjectReaders, SubMonitor monitor, boolean canChangeResources)
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
						if (canChangeResources) folder.refreshLocal(IResource.DEPTH_INFINITE, monitor);
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
	}

	protected void updateFromResourceChangeListener(String projectName, Set<String> unloadPackages, Set<IPackageReader> toAdd)
	{
		//first update our cache
		Map<String, IPackageReader> projectContainedPackagesMap = getProjectContainedPackagesMap(projectName);
		for (String packageName : unloadPackages)
		{
			projectContainedPackagesMap.remove(packageName);
		}
		for (IPackageReader iPackageReader : toAdd)
		{
			projectContainedPackagesMap.put(iPackageReader.getPackageName(), iPackageReader);
		}

		//for each change add/remove we first check that the project isn't already loaded in the spec readers
		//and also split up into services and non-services
		Set<String> componentsReallyToUnload = new HashSet<>();
		Set<String> serviceReallyToUnload = new HashSet<>();
		for (String packageName : unloadPackages)
		{
			if (notContainedByOtherProjects(projectName, packageName))
			{
				if (WebComponentSpecProvider.getInstance().getPackageNames().contains(packageName)) componentsReallyToUnload.add(packageName);
				else serviceReallyToUnload.add(packageName);
			}
		}
		Set<IPackageReader> componentsReallyToLoad = new HashSet<>();
		Set<IPackageReader> serviceReallyToLoad = new HashSet<>();
		for (IPackageReader packageReader : toAdd)
		{
			if (notContainedByOtherProjects(projectName, packageReader.getPackageName()))
			{
				if (packageReader.getPackageType().equals(IPackageReader.WEB_SERVICE)) serviceReallyToLoad.add(packageReader);
				else componentsReallyToLoad.add(packageReader);
			}
		}

		ResourceProvider.updatePackageResources(componentsReallyToUnload, componentsReallyToLoad, serviceReallyToUnload, serviceReallyToLoad);
		ngPackagesChanged(true, true);
	}


	/**	Search in all other projects if the given package is not contained also there
	 * @param projectName
	 * @param name
	 * @return
	 */
	private boolean notContainedByOtherProjects(String projectName, String name)
	{
		for (String currentProjectName : projectNameToContainedPackages.keySet())
		{
			if (currentProjectName.equals(projectName)) continue;
			if (projectNameToContainedPackages.get(currentProjectName) != null && projectNameToContainedPackages.get(currentProjectName).containsKey(name))
				return false;
		}
		return true;
	}

	/**
	 * Collects all package project readers of the active solution and its modules into componentReaders and caches a list of readers per each solution project into @{projectNameToContainedPackages}
	 * @param componentReaders
	 * @param m
	 * @param canChangeResources
	 */
	private void collectNGPackageProjects(Map<String, IPackageReader> componentReaders, IProgressMonitor m, boolean canChangeResources)
	{ //TODO check monitor steps!!!
		ServoyProject activeSolutionProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeSolutionProject != null)
		{
			ServoyProject[] modulesOfActiveProject = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			for (ServoyProject servoyProject : modulesOfActiveProject)
			{
				ServoyNGPackageProject[] ngPackageProjects = servoyProject.getNGPackageProjects();
				for (ServoyNGPackageProject servoyNGPackageProject : ngPackageProjects)
				{
					collectReferencedProjectAsPackageReader(componentReaders, servoyProject.getProject().getName(), servoyNGPackageProject.getProject(), m,
						canChangeResources);
				}
			}
			m.worked(3);
		}
	}

	protected void collectResourcesProjectNGPackages(boolean reloadComponents, boolean reloadServices, IProgressMonitor m,
		Map<String, IPackageReader> newComponents, boolean canChangeResources)
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
				readParentOfPackagesDir(newComponents, activeResourcesProject.getProject(), SolutionSerializer.SERVICES_DIR_NAME, monitor.newChild(2),
					canChangeResources);
			}
		}
		monitor.done();
	}

	/**
	 * Transform this package project into a package reader and add it to newComponentReaders and to our local map
	 * @param newComponentReaders
	 * @param solutionProjectName
	 * @param project
	 * @param m
	 * @param canChangeResources
	 */
	private void collectReferencedProjectAsPackageReader(Map<String, IPackageReader> newComponentReaders, String solutionProjectName, IProject project,
		IProgressMonitor m, boolean canChangeResources)
	{
		try
		{
			if (canChangeResources) project.refreshLocal(IResource.DEPTH_INFINITE, m);
			project.deleteMarkers(SPEC_READ_MARKER, false, IResource.DEPTH_ONE);
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
	 * Checks whether or not a default (Servoy supplied) component or service package should be loaded/used.
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
					IPackageReader reader = readPackageResource(resource);
					if (reader != null)
					{
						readers.put(reader.getPackageName(), reader);
						getProjectContainedPackagesMap(iProject.getName()).put(reader.getPackageName(), reader);
					}
				}
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

	protected static void addErrorMarker(IResource resource, Exception e)
	{
		try
		{
			IResource res = resource;
			if (!res.exists())
			{
				res = res.getParent();
			}
			IMarker marker = null;
			if (e instanceof DuplicatePackageException)
			{
				res.deleteMarkers(DUPLICATE_COMPONENT_MARKER, false, IResource.DEPTH_ONE);
				marker = resource.createMarker(DUPLICATE_COMPONENT_MARKER);
			}
			else
			{
				marker = res.createMarker(SPEC_READ_MARKER);
			}
			marker.setAttribute(IMarker.MESSAGE, e.getMessage());
			marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_NORMAL);
			marker.setAttribute(IMarker.LOCATION, res.getLocation().toString());
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

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode()
		{
			return container.hashCode();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
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
			addErrorMarker(resource, e);
		}
	}

	public void dispose()
	{
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
		ResourceProvider.setPackages(new ArrayList<IPackageReader>());
	}

	/** Get the cached package project references of this project
	 * @param name
	 * @return
	 */
	public List<String> getOldUsedPackageProjectsList(String projectName)
	{
		List<String> result = new ArrayList<>();
		Map<String, IPackageReader> map = getProjectContainedPackagesMap(projectName);
		for (String packageName : map.keySet())
		{
			if (ResourcesPlugin.getWorkspace().getRoot().getProject(packageName).exists()) result.add(packageName);
		}
		return result;
	}

}
