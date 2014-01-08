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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.IWorkingSetChangedListener;
import com.servoy.j2db.persistence.NameComparator;

/**
 * Project nature for Resources Servoy projects.
 * 
 * @author acostescu
 */
public class ServoyResourcesProject implements IProjectNature
{
	// key - working set name, paths to persist files
	private Map<String, List<String>> workingSetPersists;
	private List<IWorkingSetChangedListener> listeners;
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyResources"; //$NON-NLS-1$

	private IProject project;

	public ServoyResourcesProject()
	{
	}

	public ServoyResourcesProject(IProject project)
	{
		this.project = project;
	}

	public void configure() throws CoreException
	{
	}

	public void deconfigure() throws CoreException
	{
	}

	public IProject getProject()
	{
		return project;
	}

	public void setProject(IProject project)
	{
		this.project = project;
	}

	@Override
	public String toString()
	{
		return (project != null ? project.getName() : null);
	}

	public List<String> getServoyWorkingSets(String[] solutionNames)
	{
		if (workingSetPersists != null)
		{
			List<String> workingSets = new ArrayList<String>();
			for (String workingSetName : workingSetPersists.keySet())
			{
				for (String solutionName : solutionNames)
				{
					if (hasFilesInServoyWorkingSet(workingSetName, solutionName))
					{
						workingSets.add(workingSetName);
						break;
					}
				}
			}
			Collections.sort(workingSets, NameComparator.INSTANCE);
			return workingSets;
		}
		return null;
	}

	private boolean hasFilesInServoyWorkingSet(String workingSetName, String solutionName)
	{
		if (workingSetPersists != null)
		{
			List<String> pathsList = workingSetPersists.get(workingSetName);
			if (pathsList != null)
			{
				for (String path : pathsList)
				{
					IFile file = getProject().getWorkspace().getRoot().getFile(new Path(path));
					if (file.exists() && file.getProject().getName().equals(solutionName))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean hasServoyWorkingSets(String[] solutionNames)
	{
		List<String> workingSets = getServoyWorkingSets(solutionNames);
		if (workingSets != null && workingSets.size() > 0)
		{
			return true;
		}
		return false;
	}

	public boolean hasPersistsInServoyWorkingSets(String workingSetName, String[] solutionNames)
	{
		if (workingSetPersists != null)
		{
			List<String> formNames = getFormNames(workingSetPersists.get(workingSetName), solutionNames);
			if (formNames != null && formNames.size() > 0)
			{
				return true;
			}
		}
		return false;
	}

	public boolean isContainedInWorkingSets(String persistName, String[] solutionNames)
	{
		if (workingSetPersists != null)
		{
			for (List<String> persistList : workingSetPersists.values())
			{
				List<String> names = getFormNames(persistList, solutionNames);
				if (names != null && names.contains(persistName)) return true;
			}
		}
		return false;
	}

	public List<String> getWorkingSetPersists(String workingSetName, String[] solutionNames)
	{
		if (workingSetPersists != null && workingSetPersists.containsKey(workingSetName))
		{
			return getFormNames(workingSetPersists.get(workingSetName), solutionNames);
		}
		return null;
	}

	private List<String> getFormNames(List<String> pathsList, String[] solutionNames)
	{
		if (pathsList != null)
		{
			List<String> formNames = new ArrayList<String>();
			for (String path : pathsList)
			{
				IFile file = getProject().getWorkspace().getRoot().getFile(new Path(path));
				if (solutionNames != null)
				{
					for (String solutionName : solutionNames)
					{
						if (file.exists() && file.getProject().getName().equals(solutionName))
						{
							String formName = SolutionSerializer.getFormNameFromFile(file);
							if (formName != null && !formNames.contains(formName))
							{
								formNames.add(formName);
							}
						}
					}
				}
			}
			Collections.sort(formNames, NameComparator.INSTANCE);
			return formNames;
		}
		return null;
	}

	public String getContainingWorkingSet(String formName, String[] solutionNames)
	{
		if (workingSetPersists != null)
		{
			for (String workingSetName : workingSetPersists.keySet())
			{
				List<String> formNames = getFormNames(workingSetPersists.get(workingSetName), solutionNames);
				if (formNames != null && formNames.contains(formName))
				{
					return workingSetName;
				}
			}
		}
		return null;
	}

	public void refreshServoyWorkingSets(Map<String, List<String>> workingSetPaths)
	{
		this.workingSetPersists = workingSetPaths;
	}

	public void addWorkingSet(IFileAccess fileAccess, String workingSetName, List<String> paths)
	{
		workingSetPersists.put(workingSetName, paths);
		serializeServoyWorkingSets(fileAccess);
		fireWorkingSetChanged(paths);
	}

	public void removeWorkingSet(IFileAccess fileAccess, String workingSetName)
	{
		List<String> pathsList = workingSetPersists.remove(workingSetName);
		serializeServoyWorkingSets(fileAccess);
		fireWorkingSetChanged(pathsList);
	}

	public void renameWorkingSet(IFileAccess fileAccess, String oldName, String newName)
	{
		List<String> pathsList = workingSetPersists.remove(oldName);
		workingSetPersists.put(newName, pathsList);
		serializeServoyWorkingSets(fileAccess);
		fireWorkingSetChanged(pathsList);
	}

	public Set<String> getWorkingSetNames()
	{
		return new HashSet<String>(workingSetPersists.keySet());
	}

	private void fireWorkingSetChanged(List<String> pathsList)
	{
		if (listeners != null && listeners.size() > 0 && pathsList != null)
		{
			List<String> affectedSolutions = new ArrayList<String>();
			for (String path : pathsList)
			{
				IFile file = getProject().getWorkspace().getRoot().getFile(new Path(path));
				if (file.exists() && !affectedSolutions.contains(file.getProject().getName()))
				{
					affectedSolutions.add(file.getProject().getName());
				}
			}
			for (IWorkingSetChangedListener listener : listeners)
			{
				listener.workingSetChanged(affectedSolutions.toArray(new String[0]));
			}
		}
	}

	private void serializeServoyWorkingSets(IFileAccess fileAccess)
	{
		SolutionSerializer.serializeWorkingSetInfo(fileAccess, getProject().getName(), workingSetPersists);
	}

	public void setListeners(List<IWorkingSetChangedListener> listeners)
	{
		this.listeners = listeners;
	}

	public void removeListener(IWorkingSetChangedListener ws)
	{
		if (listeners != null)
		{
			listeners.remove(ws);
		}
	}

	public void destroy()
	{
		listeners = null;
	}
}