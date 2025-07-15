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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.IWorkingSetChangedListener;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.ServoyJSONObject;

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
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyResources";

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
					IResource resource = getProject().getWorkspace().getRoot().findMember(new Path(path));
					if (resource != null && resource.exists() && resource.getProject().getName().equals(solutionName))
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

	/**
	 * Returns true if the working set contains forms (checkFormComponents is false) or formComponents (checkFormComponents is true)
	 * @param workingSetName
	 * @param solutionNames
	 * @param checkFormComponents
	 * @return
	 */
	public boolean hasPersistsInServoyWorkingSets(String workingSetName, String[] solutionNames, boolean checkFormComponents)
	{
		if (workingSetPersists != null)
		{
			List<String> formNames = getFormNames(workingSetPersists.get(workingSetName), solutionNames);
			Iterator<String> iterator = formNames.iterator();

			formNamesWhile : while (iterator.hasNext())
			{
				String formName = iterator.next();
				for (String solutionName : solutionNames)
				{
					Solution solution = ServoyModelFinder.getServoyModel().getServoyProject(solutionName).getSolution();
					if (solution != null)
					{
						Form form = solution.getForm(formName);
						if (form != null && (form.isFormComponent().booleanValue() != checkFormComponents))
						{
							iterator.remove();
							continue formNamesWhile; // removed it already, no point in continuing the for
						}
					}
				}
			}
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

	public String getWorkingSetOfPersist(String persistName, String[] solutionNames)
	{
		if (workingSetPersists != null)
		{
			for (Entry<String, List<String>> workingSetMapEntry : workingSetPersists.entrySet())
			{
				List<String> names = getFormNames(workingSetMapEntry.getValue(), solutionNames);
				if (names != null && names.contains(persistName)) return workingSetMapEntry.getKey();
			}
		}
		return null;
	}

	public List<String> getWorkingSetPersists(String workingSetName, String[] solutionNames)
	{
		if (workingSetPersists != null && workingSetPersists.containsKey(workingSetName))
		{
			return getFormNames(workingSetPersists.get(workingSetName), solutionNames);
		}
		return Collections.emptyList();
	}

	private List<String> getFormNames(List<String> pathsList, String[] solutionNames)
	{
		if (pathsList != null)
		{
			List<String> formNames = new ArrayList<String>();
			for (String path : pathsList)
			{
				IResource resource = getProject().getWorkspace().getRoot().findMember(new Path(path));
				if (resource != null && resource.exists() && solutionNames != null)
				{
					for (String solutionName : solutionNames)
					{
						if (resource.getProject().getName().equals(solutionName))
						{
							String formName = SolutionSerializer.getFormNameFromFile(resource);
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
		return Collections.emptyList();
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

	/**
	 * @param addToWorkingSet
	 * @param solutionName
	 * @param ws
	 */
	public void saveWorkingSet(List<IAdaptable> addToWorkingSet, String solutionName, String WorkingSetName)
	{
		List<String> paths = new ArrayList<String>();
		for (IAdaptable resource : addToWorkingSet)
		{
			if (resource instanceof IResource && ((IResource)resource).exists())
			{
				paths.add(((IResource)resource).getFullPath().toString());
			}
		}
		if (solutionName != null)
		{
			addWorkingSet(new WorkspaceFileAccess(ResourcesPlugin.getWorkspace()), WorkingSetName, paths);
		}
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
				IResource resource = getProject().getWorkspace().getRoot().findMember(new Path(path));
				if (resource != null && resource.exists() && !affectedSolutions.contains(resource.getProject().getName()))
				{
					affectedSolutions.add(resource.getProject().getName());
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

	/**
	 * @throws JSONException if "placedataprovider.preferences" doesn't contain valid JSON.
	 */
	public JSONObject getPlaceDataproviderPreferences() throws JSONException
	{
		ServoyJSONObject prefs;

		WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		final IFile file = getProject().getFile("placedataprovider.preferences");
		if (file.exists())
		{
			try
			{
				String contents = wsa.getUTF8Contents(file.getFullPath().toPortableString());
				prefs = new ServoyJSONObject(contents, false, false, true);
			}
			catch (JSONException e)
			{
				ServoyLog.logError(
					"Corrupt 'placedataprovider.preferences' found in the resources project; will start over with only default place dataprovider preferences...",
					e);
				throw e;
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
				prefs = new ServoyJSONObject();
			}
		}
		else prefs = new ServoyJSONObject();

		prefs.setNoQuotes(false); // important, as configuration names are stored as keys - and without the quotes we would store invalid json (or they would need to be restrictions on spaces and so on)
		prefs.setNewLines(true);
		prefs.setNoBrackets(false);
		return prefs;
	}

	public void backupCorruptedPlaceDataproivderPreferences()
	{
		final IFile file = getProject().getFile("placedataprovider.preferences");
		if (file.exists())
		{
			Job moveJob = new Job("Backing up corrupt 'placedataprovider.preferences' file.")
			{

				@Override
				protected IStatus run(IProgressMonitor monitor)
				{
					IStatus status;
					try
					{
						file.move(file.getFullPath().removeLastSegments(1).append("placedataprovider.preferences.backup"), true, monitor);
						status = Status.OK_STATUS;
					}
					catch (CoreException e)
					{
						ServoyLog.logError("Failed to backup 'placedataprovider.preferences': ", e);
						status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Failed to backup 'placedataprovider.preferences': ", e);
					}
					return status;
				}
			};

			moveJob.setRule(file.getParent());
			moveJob.setUser(true);
			moveJob.schedule();
		}
	}

	public void savePlaceDataproviderPreferences(JSONObject object)
	{
		WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		try
		{
			wsa.setUTF8Contents(getProject().getFile("placedataprovider.preferences").getFullPath().toPortableString(), object.toString());
		}
		catch (IOException e)
		{
			ServoyLog.logError(e);
		}
	}
}