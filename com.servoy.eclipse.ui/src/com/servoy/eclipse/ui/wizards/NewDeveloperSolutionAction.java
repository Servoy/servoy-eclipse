/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.nature.ServoyDeveloperProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public class NewDeveloperSolutionAction extends Action
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewDeveloperSolutionAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create developer solution");
		setToolTipText("Create developer solution");
	}

	@Override
	public void run()
	{
		String name = askDeveloperSolutionName(viewer.getViewSite().getShell());
		if (name != null)
		{
			IProject newDeveloperProject = ServoyModel.getWorkspace().getRoot().getProject(name);
			try
			{
				newDeveloperProject.create(null);
				newDeveloperProject.open(null);
				IProjectDescription developerProjectDescription = newDeveloperProject.getDescription();
				developerProjectDescription.setNatureIds(new String[] { ServoyDeveloperProject.NATURE_ID, JavaScriptNature.NATURE_ID });
				newDeveloperProject.setDescription(developerProjectDescription, null);
				if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() != null)
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().addDeveloperProject(newDeveloperProject);
				}
				EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
				try
				{
					Solution solution = (Solution)repository.createNewRootObject(name, IRepository.SOLUTIONS);

					solution.setVersion("1.0");
					solution.setSolutionType(SolutionMetaData.SOLUTION);

					// serialize Solution object to given project
					repository.updateRootObject(solution);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}

		}
	}

	public static String askDeveloperSolutionName(Shell shell)
	{
		InputDialog nameDialog = new InputDialog(shell, "Create developer solution", "Supply new developer solution name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() == 0) return "";

				if (!IdentDocumentValidator.isJavaIdentifier(newText))
				{
					return "Invalid Java identifier";
				}
				if (((AbstractServoyModel)ServoyModelManager.getServoyModelManager().getServoyModel()).getDeveloperProject(newText) != null)
				{
					return "Developer solution with this name already exists";
				}
				return null;
			}
		});
		int res = nameDialog.open();
		if (res == Window.OK)
		{
			String name = nameDialog.getValue();
			return name;
		}
		return null;
	}

}