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

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.DirPackageReader;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager.ContainerPackageReader;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * @author gganea@servoy.com
 *
 */
public class AddAsWebPackageAction extends AddAsSolutionReference
{

	public AddAsWebPackageAction(Shell shell)
	{
		super(shell, UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE);
		setText("Add reference to this Package Project");
		setToolTipText("Adds a Package Project reference to the active solution or it's modules.");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("add_as_module.png"));
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
				ServoyLog.logError(e);
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		super.selectionChanged(event);
		if (selectedProjects.size() > 1)
		{
			setText("Add references to these Package Projects");
			setToolTipText("Adds Package Project references to the active solution or it's modules.");
		}
		else if (selectedProjects.size() == 1)
		{
			setText("Add reference to this Package Project");
			setToolTipText("Adds a Package Project reference to the active solution or it's modules.");
		}
		if (isEnabled() && selectedProjects.size() > 0)
		{
			List<IPackageReader> readers = ServoyModelFinder.getServoyModel().getNGPackageManager().getAllPackageReaders();
			for (IPackageReader pr : readers)
			{
				String packageName = pr.getPackageName();
				for (String projectName : selectedProjects)
				{
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project == null || project.getLocationURI() == null) return; // project was deleted
					File dir = new File(project.getLocationURI());
					if (dir.exists())
					{
						DirPackageReader reader = new DirPackageReader(dir);
						if (packageName.equals(reader.getPackageName()))
						{
							setEnabled(false);
							return;
						}
					}
				}
			}
		}
	}

	@Override
	protected String solutionChooseDialogMessage()
	{
		return "You may add Servoy Package Projects to the active solution or to any of it's modules.\nPlease choose one of these solutions.";
	}

	public static void addReferencedProjectToDescription(IProject newProject, IProjectDescription solutionProjectDescription)
	{
		IProject[] oldReferencedProjectsArray = solutionProjectDescription.getReferencedProjects();
		for (IProject iProject : oldReferencedProjectsArray)
		{
			if (iProject.equals(newProject)) return;
		}
		IProject[] newReferencesArray = new IProject[oldReferencedProjectsArray.length + 1];
		System.arraycopy(oldReferencedProjectsArray, 0, newReferencesArray, 0, oldReferencedProjectsArray.length);
		newReferencesArray[oldReferencedProjectsArray.length] = newProject;
		solutionProjectDescription.setReferencedProjects(newReferencesArray);
	}

	@Override
	protected String getSelectedNodeName(SimpleUserNode node)
	{
		Object realObject = node.getRealObject();
		if (realObject instanceof IResource) return ((IResource)realObject).getName();
		if (realObject instanceof ContainerPackageReader) return ((ContainerPackageReader)realObject).getContainerName();
		return ((IPackageReader)realObject).getName();
	}

}
