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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.Location;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.MovePersistAction;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class CreateLoginSolutionQuickFix implements IMarkerResolution
{
	private static final String DIALOG_TITLE = "Create login solution";
	private final String solutionName;

	public CreateLoginSolutionQuickFix(String solutionName)
	{
		this.solutionName = solutionName;
	}

	public String getLabel()
	{
		return "Create login solution with the login form";
	}

	public void run(IMarker marker)
	{
		if (solutionName != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				Solution solution = servoyProject.getEditingSolution();
				if (solution != null)
				{
					Form loginForm = solution.getForm(solution.getLoginFormID());
					if (loginForm != null)
					{
						// request login solution name
						InputDialog loginSolutionNameDlg = new InputDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DIALOG_TITLE,
							"Please enter a name for the login solution", "login", null);

						if (loginSolutionNameDlg.open() == Window.OK)
						{
							String loginSolutionName = loginSolutionNameDlg.getValue();
							try
							{
								// create login solution and its project
								IProject newProject = ServoyModel.getWorkspace().getRoot().getProject(loginSolutionName);
								if (newProject.exists())
								{
									MessageDialog.openError(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), DIALOG_TITLE,
										"Solution with the same name already exists");
									return;
								}

								EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
								Solution loginSolution = (Solution)repository.createNewRootObject(loginSolutionName, IRepository.SOLUTIONS);
								if (loginSolution != null)
								{
									IProject resourceProject = servoyProject.getResourcesProject().getProject();
									newProject.create(null);
									newProject.open(null);

									loginSolution.setSolutionType(SolutionMetaData.LOGIN_SOLUTION);
									repository.updateRootObject(loginSolution);

									IProjectDescription description = newProject.getDescription();
									description.setNatureIds(new String[] { ServoyProject.NATURE_ID, JavaScriptNature.NATURE_ID });

									description.setReferencedProjects(new IProject[] { resourceProject });
									newProject.setDescription(description, null);

									// update the affected properties in the current solution
									solution.setLoginSolutionName(loginSolutionName);
									solution.setLoginFormID(null);
									servoyProject.saveEditingSolutionNodes(new IPersist[] { solution }, false);

									// move the login form to the login solution
									new MoveForm().move(loginForm,
										ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(loginSolutionName));
								}
							}
							catch (Exception ex)
							{
								ServoyLog.logError(ex);
							}
						}
					}
				}
			}
		}
	}

	class MoveForm extends MovePersistAction
	{
		MoveForm()
		{
			super(null);
		}

		public void move(Form form, ServoyProject destProject)
		{
			location = new Location(form.getName(), destProject);
			doWork(new IPersist[] { form }, ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator());
		}
	}
}
