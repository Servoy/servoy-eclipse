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

package com.servoy.eclipse.exporter.apps.common;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.EclipseSequenceProvider;
import com.servoy.eclipse.model.repository.WorkspaceUserManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * IServoyModel used when exporting solutions with stand-alone product.
 * @author acostescu
 */
public class ExportServoyModel extends AbstractServoyModel implements IServoyModel
{

	public void initialize(String solutionName)
	{
		activeProject = getServoyProject(solutionName);
		setActiveResourcesProject(activeProject != null ? activeProject.getResourcesProject() : null);
		updateFlattenedSolution();

		if (activeProject != null)
		{
			if (activeResourcesProject == null)
			{
				System.out.println("Cannot find solution project's resources project. (" + solutionName + ")");
			}
		}
		else
		{
			System.out.println("Cannot find solution project named '" + solutionName + "'. Are you sure you specified the correct workspace location?");
		}

	}

	private void setActiveResourcesProject(ServoyResourcesProject servoyResourcesProject)
	{
		if (activeResourcesProject != servoyResourcesProject)
		{
			activeResourcesProject = servoyResourcesProject;
			try
			{
				if (activeResourcesProject != null) activeResourcesProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}

			IServerManagerInternal sm = ApplicationServerRegistry.get().getServerManager();

			if (dataModelManager != null) sm.removeGlobalColumnInfoProvider(dataModelManager);
			dataModelManager = (activeResourcesProject != null ? new DataModelManager(activeResourcesProject.getProject(), sm) : null);
			if (dataModelManager != null) sm.addGlobalColumnInfoProvider(dataModelManager);
			sm.setGlobalSequenceProvider(dataModelManager != null ? new EclipseSequenceProvider(dataModelManager) : null);
			((EclipseRepository)getActiveSolutionHandler().getRepository()).registerResourceMetaDatas(activeResourcesProject != null
				? activeResourcesProject.getProject().getName() : null, IRepository.STYLES);
			((EclipseRepository)getActiveSolutionHandler().getRepository()).registerResourceMetaDatas(activeResourcesProject != null
				? activeResourcesProject.getProject().getName() : null, IRepository.TEMPLATES);

		}
		((WorkspaceUserManager)ApplicationServerRegistry.get().getUserManager()).setResourcesProject(activeResourcesProject != null
			? activeResourcesProject.getProject() : null); // this needs to always be done to refresh in case the main solution changed
	}

}
