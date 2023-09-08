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
package com.servoy.eclipse.core.quickfix;

import java.util.ArrayList;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.ResourcesProjectSetupJob;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * This class is able to quick-fix multiple/no resources problems markers by letting the user choose the one resources project to remain referenced to the
 * servoy solution project.
 *
 * @author acostescu
 */
// TODO I think this whole class is obsolete; it could be removed as ChangeResourcesProjectQuickFix should provide (or could be made to provide) the same functionality
public abstract class ChooseResourcesProjectQuickFix implements IMarkerResolution
{

	private final String quickFixString;
	private final String dialogText;

	/**
	 * Creates a new quick fix for referenced resources projects.
	 *
	 * @param quickFixString the string that will appear in the quick fix list.
	 * @param dialogText a string describing what the problem is and what the quick fix will do.
	 */
	public ChooseResourcesProjectQuickFix(String quickFixString, String dialogText)
	{
		this.quickFixString = quickFixString;
		this.dialogText = dialogText;
	}

	public String getLabel()
	{
		return quickFixString;
	}

	public void run(IMarker marker)
	{
		// quick fix
		final IProject servoyProject = (IProject)marker.getResource();
		try
		{
			final IProject[] projectList = getProjectListToFilter(servoyProject);
			final ArrayList<ServoyResourcesProject> resourcesProjects = new ArrayList<ServoyResourcesProject>();
			for (IProject p : projectList)
			{
				if (p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID))
				{
					resourcesProjects.add((ServoyResourcesProject)p.getNature(ServoyResourcesProject.NATURE_ID));
				}
			}
			Runnable toRun = null;
			if (resourcesProjects.size() > 0)
			{
				final String[] tmpList = new String[resourcesProjects.size()];
				int i = 0;
				for (ServoyResourcesProject tmp : resourcesProjects)
				{
					tmpList[i++] = tmp.getProject().getName();
				}

				toRun = new Runnable()
				{
					public void run()
					{
						int selectedProject = UIUtils.showOptionDialog(UIUtils.getActiveShell(), "Select Servoy Resources Project", "Solution \"" +
							servoyProject.getName() + "\" " + dialogText, tmpList, 0);

						if (selectedProject >= 0)
						{
							// the user selected a resources project he wants to use
							setReferencedResourcesProject(servoyProject, resourcesProjects.get(selectedProject).getProject());
						}
					}
				};
			}
			else
			{
				toRun = new Runnable()
				{
					public void run()
					{
						InputDialog dialog = new InputDialog(UIUtils.getActiveShell(), "Create & use new Servoy Resources Project",
							"No Servoy Resources Projects were found in the workspace.\nA new one will be created and used. Please specify it's name:",
							"resources", new IInputValidator()
							{

								public String isValid(String resourcesProjectName)
								{
									String error = null;
									// check the validity of the new resource project name (to see that it does not exist
									if (resourcesProjectName.trim().length() == 0)
									{
										error = "Please give a name for the new resource project";
									}
									else if (ServoyModel.getWorkspace().getRoot().getProject(resourcesProjectName).exists())
								{
									error = "A project with the given name already exists in the workspace";
								}
									else
								{
									IStatus validationResult = ServoyModel.getWorkspace().validateName(resourcesProjectName, IResource.PROJECT);
									if (!validationResult.isOK())
									{
										error = "The name of the resource project to be created is not valid: " + validationResult.getMessage();
									}
								}
									return error;
								}

							});

						if (dialog.open() == Window.OK)
						{
							// create the resources project
							IProject resourceProject = servoyProject.getWorkspace().getRoot().getProject(dialog.getValue());
							try
							{
								resourceProject.create(null);
								resourceProject.open(null);
								IProjectDescription resourceProjectDescription = resourceProject.getDescription();
								resourceProjectDescription.setNatureIds(new String[] { ServoyResourcesProject.NATURE_ID });
								resourceProject.setDescription(resourceProjectDescription, null);
								// the user created a resources project he wants to use
								setReferencedResourcesProject(servoyProject, resourceProject);
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
				};
			}

			if (Display.getCurrent() == null)
			{
				// not in a SWT thread
				Display.getDefault().syncExec(toRun);
			}
			else
			{
				toRun.run();
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private void setReferencedResourcesProject(IProject servoyProject, IProject servoyResourcesProject)
	{
		// ok now associate the selected(create if necessary) resources project with the solution resources project
		// create new resource project if necessary and reference it from selected solution
		WorkspaceJob job = new ResourcesProjectSetupJob("Setting up resources project for solution '" + servoyProject.getName() + "'", servoyResourcesProject,
			null, servoyProject, true);
		job.setRule(servoyProject.getWorkspace().getRoot());
		job.setUser(true);
		job.schedule();
	}

	/**
	 * Should return a list of projects from which the Servoy Resource projects will be presented to the user.
	 * @return the list of Servoy Resource projects that will be presented to the user. Only projects in this list that have nature ServoyResourcesProject.NATURE_ID are taken into account.
	 */
	protected abstract IProject[] getProjectListToFilter(IProject servoyProject) throws CoreException;

}