/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2021 Servoy BV

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

package com.servoy.eclipse.core.quickfix;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author Diana
 *
 */
public class DeleteMissingProjectReferenceQuickFix implements IMarkerResolution
{

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return "Remove reference to missing or closed project from the active solution or module.";
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker)
	{
		final IProject servoyProject = (IProject)marker.getResource();
		try
		{
			IProjectDescription description = servoyProject.getDescription();
			final IProject[] referencedProjects = description.getReferencedProjects();
			final List<IProject> newReferencedProjectsList = new ArrayList<>();
			for (IProject p : referencedProjects)
			{
				if (!p.getName().equalsIgnoreCase(marker.getAttribute(ServoyBuilder.PROJECT_REFERENCE_NAME, null)))
				{
					newReferencedProjectsList.add(p);
				}
			}

			description.setReferencedProjects(newReferencedProjectsList.toArray(new IProject[newReferencedProjectsList.size()]));
			servoyProject.setDescription(description, null);

		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while reading referenced projects for " + servoyProject.getName(), e);
		}
	}

}
