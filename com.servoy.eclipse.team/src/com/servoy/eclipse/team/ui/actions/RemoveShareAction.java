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
package com.servoy.eclipse.team.ui.actions;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.team.TeamProviderProperties;
import com.servoy.eclipse.team.ui.ResourceDecorator;

public class RemoveShareAction extends SolutionAction
{

	@Override
	protected void executeAction(IAction action) throws InvocationTargetException, InterruptedException
	{
		IProject[] selectedProjects = getSelectedProjects();
		if (selectedProjects.length == 0) // it is the resource project
		{
			IProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject();
			selectedProjects = new IProject[] { project };
		}

		for (IProject project : selectedProjects)
		{
			try
			{
				// delete team provider property file
				TeamProviderProperties teamProviderProperties = new TeamProviderProperties(new File(project.getWorkspace().getRoot().getLocation().toFile(),
					project.getName()));
				teamProviderProperties.delete();
				// unmap project from team provider
				RepositoryProvider.unmap(project);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		ResourceDecorator rd = (ResourceDecorator)PlatformUI.getWorkbench().getDecoratorManager().getBaseLabelProvider(ResourceDecorator.ID);
		rd.fireChanged(null);
	}

}
