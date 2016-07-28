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

package com.servoy.eclipse.core.ngpackages;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.SerialRule;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;

import sj.jsonschemavalidation.builder.JsonSchemaValidationNature;

/**
 * A class for managing the loaded NG custom web components and services when developer is run as UI (so not jos as a command line app that works on the workspace).
 * These can be found in the resources project as folders or archives or each in a separate project with {@link ServoyNGPackageProject} nature (here only as expanded folder contents).
 *
 * @author acostescu
 */
public class NGPackageManager extends BaseNGPackageManager
{

	private IActiveProjectListener activeProjectListenerForRegisteringResources;
	private final SerialRule serialRule;

	public NGPackageManager()
	{
		serialRule = SerialRule.getNewSerialRule();

		PlatformUI.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				// default packages preferences changed? - flush everything
				// TODO we should really check here if the property that changed does affect web packages!
				WebComponentSpecProvider.disposeInstance();
				WebServiceSpecProvider.disposeInstance();
				reloadAllNGPackages(null);
			}
		});

		if (activeProjectListenerForRegisteringResources == null)
		{
			activeProjectListenerForRegisteringResources = new IActiveProjectListener()
			{
				public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
				{
					return true;
				}

				public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
				{
					if (updateInfo == RESOURCES_UPDATED_ON_ACTIVE_PROJECT)
					{
						reloadAllNGPackages(null);
					}
					else if (updateInfo == MODULES_UPDATED)
					{
						// TODO if we will take referenced ng package projects even from modules, we should enable this code...
						clearReferencedNGPackageProjectsCache();
						//TODO can we improve this?
						reloadAllNGPackages(null);
//						reloadAllSolutionReferencedPackages(new NullProgressMonitor(), false);
					}
				}

				public void activeProjectChanged(ServoyProject activeProject)
				{
					clearReferencedNGPackageProjectsCache();
					reloadAllNGPackages(null);
				}
			};
			((ServoyModel)ServoyModelFinder.getServoyModel()).addActiveProjectListener(activeProjectListenerForRegisteringResources);
		}
	}

	@Override
	public void reloadAllNGPackages(IProgressMonitor m)
	{
		// do what super does but in a job; this is what code prior to the refactor did as well
		Job registerAllNGPackagesJob = new Job("Reading ng packages from resources project...")
		{
			@Override
			public IStatus run(IProgressMonitor monitor)
			{
				// do the actual work
				NGPackageManager.super.reloadAllNGPackages(monitor);
				return Status.OK_STATUS;
			}

		};
		registerAllNGPackagesJob.setRule(MultiRule.combine(ResourcesPlugin.getWorkspace().getRoot(), serialRule));
		registerAllNGPackagesJob.schedule();
	}

	// commented this out as it gets called from a resource changed event; I don't think we need a job/rule to just re-read what changed
	// can be removed in the future
//	@Override
//	protected void updateFromResourceChangeListener(final String projectName, final Set<String> unloadPackages, final Set<IPackageReader> toAdd)
//	{
//		Job registerNgPackagesJob = new Job("Update packages ...")
//		{
//			@Override
//			public IStatus run(IProgressMonitor monitor)
//			{
//				// do the actual work
//				NGPackageManager.super.updateFromResourceChangeListener(projectName, unloadPackages, toAdd);
//				return Status.OK_STATUS;
//			}
//
//		};
//		registerNgPackagesJob.setRule(MultiRule.combine(ResourcesPlugin.getWorkspace().getRoot(), serialRule));
//		registerNgPackagesJob.schedule();
//	}


	@Override
	public void dispose()
	{
		if (activeProjectListenerForRegisteringResources != null)
			((ServoyModel)ServoyModelFinder.getServoyModel()).removeActiveProjectListener(activeProjectListenerForRegisteringResources);
		super.dispose();
	}

	@Override
	protected boolean isDefaultPackageEnabled(String packageName)
	{
		return PlatformUI.getPreferenceStore().getBoolean("com.servoy.eclipse.designer.rfb.packages.enable." + packageName);
	}

	public static IProject createProject(String name) throws CoreException
	{
		IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(name);
		newProject.create(new NullProgressMonitor());
		newProject.open(new NullProgressMonitor());
		IProjectDescription description = newProject.getDescription();
		description.setNatureIds(new String[] { ServoyNGPackageProject.NATURE_ID, JsonSchemaValidationNature.NATURE_ID });
		newProject.setDescription(description, new NullProgressMonitor());

		return newProject;
	}
}
