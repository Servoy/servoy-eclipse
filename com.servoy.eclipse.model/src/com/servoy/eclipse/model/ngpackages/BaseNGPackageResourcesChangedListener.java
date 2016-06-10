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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
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
		IResourceDelta delta = event.getDelta();
		checkForChangesInWebPackages(delta);
	}


	/** Checks for resources changes of web packages and updates the  @ResourceProvider if needed
	 * @param delta
	 * @return
	 */
	private void checkForChangesInWebPackages(IResourceDelta delta)
	{
		final Map<String, Pair<Set<String>, Set<IPackageReader>>> changedProjects = new HashMap<String, Pair<Set<String>, Set<IPackageReader>>>();
		try
		{
			delta.accept(new IResourceDeltaVisitor()
			{

				private Set<IPackageReader> getAddedPackageReaders(String projectname)
				{
					if (!changedProjects.containsKey(projectname))
					{
						Set<String> removed = new HashSet<String>();
						Set<IPackageReader> added = new HashSet<IPackageReader>();
						changedProjects.put(projectname, new Pair<Set<String>, Set<IPackageReader>>(removed, added));
					}
					return changedProjects.get(projectname).getRight();
				}

				private Set<String> getRemovedPackageReaders(String projectname)
				{
					if (!changedProjects.containsKey(projectname))
					{
						Set<String> removed = new HashSet<String>();
						Set<IPackageReader> added = new HashSet<IPackageReader>();
						changedProjects.put(projectname, new Pair<Set<String>, Set<IPackageReader>>(removed, added));
					}
					return changedProjects.get(projectname).getLeft();
				}

				@Override
				public boolean visit(IResourceDelta resourceDelta) throws CoreException
				{
					if (resourceDelta.getMarkerDeltas().length > 0 && (resourceDelta.getKind() & IResourceDelta.CHANGED) != 0) return true;
					IResource resource = resourceDelta.getResource();
					if (resource instanceof IFile)
					{
						IFile binaryFile = (IFile)resource;
						if (binaryFile.getName().endsWith(".zip"))
						{
							if (binaryFile.getParent().getName().equals(SolutionSerializer.NG_PACKAGES_DIR_NAME))
							{//web package
								if ((resourceDelta.getKind() & IResourceDelta.CHANGED) != 0)
								{
									List<String> toRemove = new ArrayList<>();
									String componentPackageNameForFile = ResourceProvider.getComponentPackageNameForFile(new File(resource.getLocationURI()));
									if (componentPackageNameForFile == null)
										componentPackageNameForFile = ResourceProvider.getServicePackageNameForFile(new File(resource.getLocationURI()));
									toRemove.add(componentPackageNameForFile);
									IPackageReader reader = baseNGPackageManager.readPackageResource(resource);
									if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
									if (componentPackageNameForFile != null)
										getRemovedPackageReaders(resource.getProject().getName()).add(componentPackageNameForFile);
								}
								else if ((resourceDelta.getKind() & IResourceDelta.REMOVED) != 0)
								{
									String componentPackageNameForFile = ResourceProvider.getComponentPackageNameForFile(new File(resource.getLocationURI()));
									if (componentPackageNameForFile == null)
										componentPackageNameForFile = ResourceProvider.getServicePackageNameForFile(new File(resource.getLocationURI()));
									getRemovedPackageReaders(resource.getProject().getName()).add(componentPackageNameForFile);
								}
								else if ((resourceDelta.getKind() & IResourceDelta.ADDED) != 0)
								{
									IPackageReader reader = baseNGPackageManager.readPackageResource(resource);
									if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
								}
							}
						}
						else
						{ //check if this file change happened in a project

							if (resource.getProject().hasNature(ServoyNGPackageProject.NATURE_ID))
							{
								//we have a change in a file of a package project - reload that package for all referencing solutions
								IProject[] referencingProjects = resource.getProject().getReferencingProjects();
								{
									if (referencingProjects.length > 0)
									{
										String componentPackageNameForFile = ResourceProvider.getComponentPackageNameForFile(
											new File(resource.getProject().getLocationURI()));
										if (componentPackageNameForFile == null) componentPackageNameForFile = ResourceProvider.getServicePackageNameForFile(
											new File(resource.getProject().getLocationURI()));

										IPackageReader reader = baseNGPackageManager.readPackageResource(resource.getProject());

										for (IProject iProject : referencingProjects)
										{
											if (iProject.hasNature(ServoyProject.NATURE_ID))
											{
												if (reader != null) getAddedPackageReaders(iProject.getName()).add(reader);
												if (componentPackageNameForFile != null)
													getRemovedPackageReaders(iProject.getName()).add(componentPackageNameForFile);
											}
										}
									}
								}
							}
							else
							{
								if (resource.getProject().hasNature(ServoyResourcesProject.NATURE_ID))
								{
									//first determine the folder that is the dir package in "components" or in "services"
									IContainer packageDirectory = resource.getParent();

									while ((packageDirectory.getParent() != null &&
										!packageDirectory.getParent().getName().equals(SolutionSerializer.COMPONENTS_DIR_NAME) &&
										!packageDirectory.getParent().getName().equals(SolutionSerializer.SERVICES_DIR_NAME)) ||
										//this is for the case that (for some reason) a component or service has its own "components" or "services" folder
										!resource.getProject().equals(packageDirectory.getParent().getParent()))
									{
										packageDirectory = packageDirectory.getParent();
									}

									String componentPackageNameForFile = ResourceProvider.getComponentPackageNameForFile(
										new File(packageDirectory.getLocationURI()));
									if (componentPackageNameForFile == null) componentPackageNameForFile = ResourceProvider.getServicePackageNameForFile(
										new File(packageDirectory.getLocationURI()));

									IPackageReader reader = baseNGPackageManager.readPackageResource(packageDirectory);
									if (reader != null) getAddedPackageReaders(resource.getProject().getName()).add(reader);
									if (componentPackageNameForFile != null)
										getRemovedPackageReaders(resource.getProject().getName()).add(componentPackageNameForFile);
								}
							}
						}
						return false;
					}
					else if (resource instanceof IProject)
					{ // maybe references to package projects changed
						IProject solutionProject = (IProject)resource;
						if (solutionProject.exists() && solutionProject.hasNature(ServoyProject.NATURE_ID))
						{
							IProject[] referencedProjects = solutionProject.getDescription().getReferencedProjects();
							List<IProject> usedPackageProjects = new ArrayList<>();
							for (IProject project : referencedProjects)
							{
								if (project.hasNature(ServoyNGPackageProject.NATURE_ID))
								{
									usedPackageProjects.add(project);
								}
							}
							List<String> oldUsedPackageList = baseNGPackageManager.getOldUsedPackageProjectsList(solutionProject.getName());
							List<String> oldUsedPackageListCopy = new ArrayList<>();
							oldUsedPackageListCopy.addAll(oldUsedPackageList);

							//retain in oldUsedPackageList only the projects that are not used anymore
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

							//retain in usedPackageProjects only the projects that were not used before
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
							for (String removedProject : oldUsedPackageList)
							{
								getRemovedPackageReaders(solutionProject.getName()).add(removedProject);
							}
							for (IProject addedProject : usedPackageProjects)
							{
								IPackageReader readPackageResource = baseNGPackageManager.readPackageResource(addedProject);
								getAddedPackageReaders(solutionProject.getName()).add(readPackageResource);
							}
						}
					}
					return true;
				}
			});
		}
		catch (CoreException e)
		{
			Debug.log(e);
		}

		for (String projectName : changedProjects.keySet())

		{//TODO this call triggers an eclipse job - maybe move the for inside updateFromResourceChangeListener
			baseNGPackageManager.updateFromResourceChangeListener(projectName, changedProjects.get(projectName).getLeft(),
				changedProjects.get(projectName).getRight());
		}

	}

}