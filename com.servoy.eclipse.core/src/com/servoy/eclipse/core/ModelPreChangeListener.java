/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * 
 * @author acostescu
 */
public class ModelPreChangeListener implements IResourceChangeListener
{

	private final ServoyModel model;

	public ModelPreChangeListener(ServoyModel servoyModel)
	{
		this.model = servoyModel;
	}

	public void resourceChanged(IResourceChangeEvent event)
	{
		IResource changed = event.getResource();
		if (changed instanceof IProject)
		{
			final IProject projectThatWillChange = (IProject)changed;
			if (event.getType() == IResourceChangeEvent.PRE_DELETE || event.getType() == IResourceChangeEvent.PRE_CLOSE)
			{
				// if an used resources project gets deleted or closed, remember it was a resources project so that the reference to it can be removed
				// if the user changes the resources project for a solution that used to it
				try
				{
					if (projectThatWillChange.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						for (ServoyProject sp : model.getServoyProjects())
						{
							final IProject solutionP = sp.getProject();
							if (solutionP.exists() && solutionP.isOpen())
							{
								for (IProject referencedP : solutionP.getReferencedProjects())
								{
									if (projectThatWillChange == referencedP)
									{
										// remember that this was a resources project for the solution
										WorkspaceJob job = new WorkspaceJob("Marking deleted/closed resources project") //$NON-NLS-1$
										{
											@Override
											public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
											{
												if (solutionP.exists() && solutionP.isOpen())
												{
													QualifiedName qn = new QualifiedName(Activator.PLUGIN_ID,
														ResourcesProjectSetupJob.CLOSED_DELETED_RESOURCES_PROJECT_KEY);
													solutionP.setPersistentProperty(
														qn,
														addToken(solutionP.getPersistentProperty(qn),
															ResourcesProjectSetupJob.CLOSED_DELETED_RESOURCES_PROJECT_DELIM, projectThatWillChange.getName()));
												}
												return Status.OK_STATUS;
											}
										};
										job.setRule(solutionP);
										job.setSystem(true);
										job.schedule();
										break;
									}
								}
							}
						}
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError("Exception while checking for urpdoc.", e); //$NON-NLS-1$
				}
			}
		}
	}

	protected String addToken(String persistentProperty, String delim, String valueToAdd)
	{
		String retVal = valueToAdd;
		if (persistentProperty != null)
		{
			for (String t : persistentProperty.split(delim))
			{
				if (!valueToAdd.equals(t))
				{
					retVal += t;
				}
			}
		}
		return retVal;
	}

}
