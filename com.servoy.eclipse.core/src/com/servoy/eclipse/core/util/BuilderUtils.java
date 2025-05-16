/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.core.util;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Solution;

/**
 * @author gboros
 *
 */
public class BuilderUtils
{
	public static final int HAS_NO_MARKERS = 0;
	public static final int HAS_ERROR_MARKERS = 1;
	public static final int HAS_WARNING_MARKERS = 2;


	public static int getMarkers(ServoyProject servoyProject)
	{
		ArrayList<String> projects = new ArrayList<String>();
		Solution[] modules = servoyProject.getModules();
		for (Solution m : modules)
			projects.add(m.getName());

		return getMarkers(projects.toArray(new String[projects.size()]));
	}

	/**
	 *
	 * @return the HAS_ERROR_MARKERS constant for errors, HAS_WARNING_MARKERS constant for warnings, HAS_NO_MARKERS for no markers
	 */
	public static int getMarkers(String[] projects)
	{
		if (projects != null && projects.length > 0)
		{
			boolean hasWarnings = false;
			try
			{
				for (String projectName : projects)
				{
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project != null && project.exists() && project.isOpen())
					{
						IMarker[] markers = project.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
						for (IMarker marker : markers)
						{
							if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR)
							{
								return HAS_ERROR_MARKERS;
							}
							if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_WARNING)
							{
								hasWarnings = true;
							}
						}
					}
					else
					{
						ServoyLog.logWarning("Cannot find project (or it is closed) \"" + projectName + "\" in workspace while searching for problem markers.",
							null);
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			if (hasWarnings) return HAS_WARNING_MARKERS;
		}
		return HAS_NO_MARKERS;
	}
}
