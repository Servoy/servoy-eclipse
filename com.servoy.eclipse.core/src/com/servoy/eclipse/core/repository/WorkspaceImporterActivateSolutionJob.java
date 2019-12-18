/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.core.repository;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * During .servoy import it is needed a few times to change the active solution of developer. (see {@link XMLEclipseWorkspaceImportHandlerVersions11AndHigher#importFromJarFile(com.servoy.j2db.util.xmlxport.IXMLImportEngine, com.servoy.j2db.util.xmlxport.IXMLImportHandlerVersions11AndHigher, com.servoy.j2db.util.xmlxport.IXMLImportUserChannel, com.servoy.eclipse.model.repository.EclipseRepository, String, com.servoy.eclipse.model.nature.ServoyResourcesProject, org.eclipse.core.runtime.IProgressMonitor, boolean, boolean, String)})
 * But as {@link ServoyModel#setActiveProject(com.servoy.eclipse.model.nature.ServoyProject, boolean)} uses Display.syncExec if it's not already on UI thread, import has to call it outside the workspace lock/runnables
 * so in this separate job in order to avoid potential deadlock situations (see SVY-12994).
 *
 * @author acostescu
 */
class WorkspaceImporterActivateSolutionJob extends Job
{

	private final IProject solutionProjectToActivate;
	private final Job nextJob;
	private final Exception[] exception;
	private final boolean[] finishedFlag;
	private final IProject solutionProjectToDeleteIfNextJobWillNotBeScheduled;

	public WorkspaceImporterActivateSolutionJob(String name, IProject solutionProjectToActivate, Job nextJob, Exception[] exception, boolean[] finishedFlag,
		IProject solutionProjectToDeleteIfNextJobWillNotBeScheduled)
	{
		super(name);
		if (solutionProjectToActivate == null) throw new RuntimeException("Project to activate cannot be null!");
		this.solutionProjectToActivate = solutionProjectToActivate;

		// the following can be null
		this.nextJob = nextJob;
		this.exception = exception;
		this.finishedFlag = finishedFlag;
		this.solutionProjectToDeleteIfNextJobWillNotBeScheduled = solutionProjectToDeleteIfNextJobWillNotBeScheduled;
	}

	@Override
	protected IStatus run(IProgressMonitor m)
	{
		m.setTaskName("Activating resources project");
		m.worked(1);
		Job jobToScheduleNext = null;
		try
		{
			// activate dummy
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject dummyServoyProject = servoyModel.getServoyProject(solutionProjectToActivate.getName());
			servoyModel.setActiveProject(dummyServoyProject, false);

			runCodeAfterActivation();

			if (nextJob != null)
			{
				jobToScheduleNext = nextJob;
			}
		}
		catch (Exception e)
		{
			if (exception != null) exception[0] = e;
			else ServoyLog.logError(e);
			return new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage());
		}
		finally
		{
			try
			{
				if (jobToScheduleNext == null && solutionProjectToDeleteIfNextJobWillNotBeScheduled != null)
					solutionProjectToDeleteIfNextJobWillNotBeScheduled.delete(true, true, null);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
				return e.getStatus();
			}
			finally
			{
				if (finishedFlag != null) synchronized (finishedFlag)
				{
					finishedFlag[0] = (jobToScheduleNext == null);
					if (finishedFlag[0] == true) finishedFlag.notify();
				}
			}
		}
		if (jobToScheduleNext != null) jobToScheduleNext.schedule();
		return Status.OK_STATUS;
	}

	protected void runCodeAfterActivation() throws Exception
	{
		// in case subclasses want to do something here
	}

}
