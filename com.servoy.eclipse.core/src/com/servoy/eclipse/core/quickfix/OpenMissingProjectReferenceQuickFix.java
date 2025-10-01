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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author Diana
 *
 */
public class OpenMissingProjectReferenceQuickFix implements IMarkerResolution
{

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return "Open missing referenced project if it exists.";
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
			final IProject[] referencedProjects = servoyProject.getDescription().getReferencedProjects();
			for (IProject p : referencedProjects)
			{
				if (p.getName().equalsIgnoreCase(marker.getAttribute(ServoyBuilder.PROJECT_REFERENCE_NAME, null)))
				{
					if (p.exists() && !p.isOpen())
					{
						p.open(new NullProgressMonitor());
					}
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while reading referenced projects for " + servoyProject.getName(), e);
		}

	}

}
