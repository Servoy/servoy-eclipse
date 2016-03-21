/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea@servoy.com
 *
 */
public class AddAsWebPackageAction extends AddAsSolutionReference
{

	/**
	 * @param shell
	 */
	public AddAsWebPackageAction(Shell shell)
	{
		super(shell, UserNodeType.WEB_PACKAGE);
	}

	@Override
	public void run()
	{
		if (selectedProjects.size() > 0)
		{
			// let the user choose the active module to add the selected projects to (as modules)
			ServoyProject activeModule = askUserForActiveModuleToUse();
			if (activeModule == null) return;
			try
			{
				IProjectDescription description = activeModule.getProject().getDescription();
				for (String string : selectedProjects)
				{
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(string);
					if (project != null)
					{
						addReferencedProjectToDescription(project, description);
					}
				}
				activeModule.getProject().setDescription(description, new NullProgressMonitor());
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		if (selectedProjects.size() > 1)
		{
			setText("Add as Web Package Projects");
			setToolTipText("Add as Web Package Projects to an already active module");
		}
		else if (selectedProjects.size() == 1)
		{
			setText("Add as Web Package Project");
			setToolTipText("Add as a Web Package Project to an already active module");
		}
	}

	@Override
	protected String solutionChooseDialogMessage()
	{
		return "You may add Web Package Projects to the active solution or to any of it's modules.\nPlease choose one of these solutions.";
	}

	/**
	 * @param newProject
	 * @param solutionProjectDescription
	 */
	public static void addReferencedProjectToDescription(IProject newProject, IProjectDescription solutionProjectDescription)
	{
		IProject[] oldReferencedProjectsArray = solutionProjectDescription.getReferencedProjects();
		IProject[] newReferencesArray = new IProject[oldReferencedProjectsArray.length + 1];
		System.arraycopy(oldReferencedProjectsArray, 0, newReferencesArray, 0, oldReferencedProjectsArray.length);
		newReferencesArray[oldReferencedProjectsArray.length] = newProject;
		solutionProjectDescription.setReferencedProjects(newReferencesArray);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.views.solutionexplorer.actions.AddAsSolutionReference#getSelectedNodeName(com.servoy.eclipse.ui.node.SimpleUserNode)
	 */
	@Override
	protected String getSelectedNodeName(SimpleUserNode node)
	{
		return ((IProject)node.getRealObject()).getName();
	}

}
