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
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.IFormComponentListener;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
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
 *
 * @author acostescu
 */
public class ExportServoyModel extends AbstractServoyModel implements IServoyModel
{

	public void initialize(String solutionName)
	{
		if (getNGPackageManager() == null) initNGPackageManager();

		setActiveProjectReferenceInternal(getServoyProject(solutionName));
		updateFlattenedSolution();

		ServoyResourcesProject servoyResourcesProject = (activeProject != null ? activeProject.getResourcesProject() : null);
		if (activeResourcesProject != servoyResourcesProject)
		{
			setActiveResourcesProject(servoyResourcesProject); // this does reload all security info (not just resources proj. related) as well as dbis/seq. provider/templates/styles
		}
		else
		{
			// resources project remained the same, just the active solution changed; so fully reload security information to be in sync
			((WorkspaceUserManager)ApplicationServerRegistry.get().getUserManager()).reloadAllSecurityInformation();
		}

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
		getNGPackageManager().clearReferencedNGPackageProjectsCache();
		getNGPackageManager().reloadAllNGPackages(ILoadedNGPackagesListener.CHANGE_REASON.RELOAD, null);
	}

	private void setActiveResourcesProject(ServoyResourcesProject servoyResourcesProject)
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

		// refresh old repository level styles
		((EclipseRepository)getActiveSolutionHandler().getRepository()).registerResourceMetaDatas(
			activeResourcesProject != null ? activeResourcesProject.getProject().getName() : null, IRepository.STYLES);

		// refresh repository level templates
		((EclipseRepository)getActiveSolutionHandler().getRepository()).registerResourceMetaDatas(
			activeResourcesProject != null ? activeResourcesProject.getProject().getName() : null, IRepository.TEMPLATES);

		// change user manager resources project and fully reload security information
		((WorkspaceUserManager)ApplicationServerRegistry.get().getUserManager()).setResourcesProject(
			activeResourcesProject != null ? activeResourcesProject.getProject() : null);
	}

	@Override
	protected BaseNGPackageManager createNGPackageManager()
	{
		return new ExportNGPackageManager(this);
	}

	@Override
	public void fireFormComponentChanged()
	{
	}

	@Override
	public void addFormComponentListener(IFormComponentListener listener)
	{
	}

	@Override
	public void removeFormComponentListener(IFormComponentListener listener)
	{
	}

	@Override
	public void addResourceChangeListener(IResourceChangeListener resourceChangeListener)
	{
		// changes are not relevant for command line exports as after a solution is activated everything is refreshed anyway and no-one changes the workspace
	}

	@Override
	public void addResourceChangeListener(IResourceChangeListener postChangeResourceChangeListener, int eventMask)
	{
		// changes are not relevant for command line exports as after a solution is activated everything is refreshed anyway and no-one changes the workspace
	}

	@Override
	public void removeResourceChangeListener(IResourceChangeListener postChangeResourceChangeListener)
	{
		// changes are not relevant for command line exports as after a solution is activated everything is refreshed anyway and no-one changes the workspace
	}

}
