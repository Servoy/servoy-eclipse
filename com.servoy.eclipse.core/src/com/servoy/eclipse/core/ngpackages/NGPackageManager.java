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
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.ngpackages.ILoadedNGPackagesListener;
import com.servoy.eclipse.model.util.AvoidMultipleExecutionsJob;
import com.servoy.j2db.persistence.WebObjectRegistry;

import sj.jsonschemavalidation.builder.JsonSchemaValidationNature;

/**
 * A class for managing the loaded NG custom web components and services when developer is run as UI (so not just as a command line app that works on the workspace).
 * These can be found in the resources project as folders or archives or each in a separate project with {@link ServoyNGPackageProject} nature (here only as expanded folder contents).
 *
 * @author acostescu
 */
public class NGPackageManager extends BaseNGPackageManager
{

	private IActiveProjectListener activeProjectListenerForRegisteringResources;
	private AvoidMultipleExecutionsJob reloadAllNGPackagesJob;

	public NGPackageManager(IDeveloperServoyModel servoyModel)
	{
		super(servoyModel);
		PlatformUI.getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener()
		{

			@Override
			public void propertyChange(PropertyChangeEvent event)
			{
				// default packages preferences changed? - flush everything
				// TODO we should really check here if the property that changed does affect web packages!
				WebComponentSpecProvider.disposeInstance();
				WebServiceSpecProvider.disposeInstance();
				reloadAllNGPackages(ILoadedNGPackagesListener.CHANGE_REASON.RELOAD, null);
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
						reloadAllNGPackages(ILoadedNGPackagesListener.CHANGE_REASON.RESOURCES_UPDATED_ON_ACTIVE_PROJECT, null);
					}
				}

				public void activeProjectChanged(ServoyProject activeProject)
				{
					clearReferencedNGPackageProjectsCache();
					reloadAllNGPackages(ILoadedNGPackagesListener.CHANGE_REASON.ACTIVE_PROJECT_CHANGED, null);
				}
			};
			// update this before build runs
			servoyModel.addActiveProjectListener(activeProjectListenerForRegisteringResources);
		}

		WebObjectRegistry.startTracking();
	}

	@Override
	public void reloadAllNGPackages(final ILoadedNGPackagesListener.CHANGE_REASON changeReason, IProgressMonitor m)
	{
		// we need to call it right away if the spec provider is not initalized yet, else we will be to late for certain calls to the spec provider
		if (WebComponentSpecProvider.getInstance() == null)
		{
			super.reloadAllNGPackages(changeReason, m);
		}
		else
		{
			synchronized (this)
			{
				// do what super does but in a job; this is what code prior to the refactor did as well
				// do this in such a way that if 100 reloadAllNGPackages calls happen before the actual reload happens/finishes in the job, the reload only occurs once/twice
				if (reloadAllNGPackagesJob == null)
				{
					// TODO should we explicitly provide an NGPackageManager.runAfterReloadIsDone for code such
					// as the one in Activator that checks for missing packages ("Missing package was detected in solution...")
					// instead of letting it rely on the fact that this job was scheduled first with delay 0 an uses the same (workspace) rule?
					reloadAllNGPackagesJob = new AvoidMultipleExecutionsJob("Reading ng packages from resources project...", 0)
					{

						@Override
						protected IStatus runAvoidingMultipleExecutions(IProgressMonitor monitor)
						{
							// do the actual work
							NGPackageManager.super.reloadAllNGPackages(changeReason, monitor);
							return Status.OK_STATUS;
						}
					};
					reloadAllNGPackagesJob.setRule(ResourcesPlugin.getWorkspace().getRoot());
				}
			}
			reloadAllNGPackagesJob.scheduleIfNeeded();
		}
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
			((IDeveloperServoyModel)ServoyModelFinder.getServoyModel()).removeActiveProjectListener(activeProjectListenerForRegisteringResources);
		super.dispose();
	}

	@Override
	protected boolean isDefaultPackageEnabled(String packageName)
	{
		return PlatformUI.getPreferenceStore().getBoolean("com.servoy.eclipse.designer.rfb.packages.enable." + packageName);
	}

	public static IProject createNGPackageProject(String name, String location) throws CoreException
	{
		IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(name);
		IProjectDescription description = ServoyModel.getWorkspace().newProjectDescription(name);
		if (location != null)
		{
			IPath path = new Path(location);
			path = path.append(name);
			description.setLocation(path);
		}
		newProject.create(description, new NullProgressMonitor());
		newProject.open(new NullProgressMonitor());
		description.setNatureIds(new String[] { ServoyNGPackageProject.NATURE_ID, JsonSchemaValidationNature.NATURE_ID });
		newProject.setDescription(description, new NullProgressMonitor());

		return newProject;
	}

	@Override
	public void ngPackagesChanged(ILoadedNGPackagesListener.CHANGE_REASON changeReason, boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged)
	{
		super.ngPackagesChanged(changeReason, loadedPackagesAreTheSameAlthoughReferencingModulesChanged);
		WebObjectRegistry.clearWebObjectCaches();
	}

}
