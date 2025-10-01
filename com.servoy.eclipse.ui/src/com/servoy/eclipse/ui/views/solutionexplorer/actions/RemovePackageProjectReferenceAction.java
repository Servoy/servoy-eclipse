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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;
import org.sablo.specification.Package.IPackageReader;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;

/**
 * @author gganea@servoy.com
 *
 */
public class RemovePackageProjectReferenceAction extends Action implements ISelectionChangedListener
{

	private final Shell shell;
	private ServoyNGPackageProject selectedProject;
	private ServoyProject parentSolutionProject;

	/**
	 * Creates a new remove package project action.
	 *
	 * @param shell shell that might be used to display a dialog to the user (too choose a parent solution).
	 */
	public RemovePackageProjectReferenceAction(Shell shell)
	{
		this.shell = shell;
		setText("Remove reference to this Package Project");
		setToolTipText(
			"Removes a Package Project reference from one of the active modules that references it (the package project itself will not be deleted)");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("remove_module.png"));
	}

	@Override
	public void run()
	{
		if (selectedProject != null)
		{
			ServoyProject parentProject = askUserForParentProject();
			if (parentProject == null) return;
			IProject project = parentProject.getProject();
			IProject projectToBeRemoved = selectedProject.getProject();
			removeProjectReference(project, projectToBeRemoved);
//			ServoyModelManager.getServoyModelManager().getServoyModel().getNGPackageManager()
//				.reloadAllNGPackages(ILoadedNGPackagesListener.CHANGE_REASON.RESOURCES_UPDATED_ON_ACTIVE_PROJECT, null);
		}
	}

	public static void removeProjectReference(IProject fromProject, IProject projectToBeRemoved)
	{
		try
		{
			IProject[] referencedProjects = fromProject.getReferencedProjects();
			referencedProjects = removeFromArray(referencedProjects, projectToBeRemoved);
			IProjectDescription description = fromProject.getDescription();
			description.setReferencedProjects(referencedProjects);
			fromProject.setDescription(description, new NullProgressMonitor());
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

	private static IProject[] removeFromArray(IProject[] referencedProjects, IProject project)
	{
		List<IProject> asList = new ArrayList<IProject>(Arrays.asList(referencedProjects));
		asList.remove(project);
		return asList.toArray(new IProject[asList.size()]);
	}

	private ServoyProject askUserForParentProject()
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		IProject[] referencingProjects = selectedProject.getProject().getReferencingProjects();


		ArrayList<ServoyProject> activeReferencingModules = new ArrayList<ServoyProject>();

		for (IProject referencingProject : referencingProjects)
		{
			if (servoyModel.isSolutionActive(referencingProject.getName()))
			{
				activeReferencingModules.add(servoyModel.getServoyProject(referencingProject.getName()));
			}
		}

		if (activeReferencingModules.size() == 1)
		{
			return activeReferencingModules.get(0);
		}
		else if (activeReferencingModules.size() > 1)
		{
			int defaultIndex = 0;
			String options[] = new String[activeReferencingModules.size()];
			for (int i = activeReferencingModules.size() - 1; i >= 0; i--)
			{
				if (activeReferencingModules.get(i) == parentSolutionProject)
				{
					defaultIndex = i;
				}
				options[i] = activeReferencingModules.get(i).getProject().getName();
			}
			int selectedProjectIndex = UIUtils.showOptionDialog(shell, "Choose parent solution",
				"You may remove the selected solution (as module) from any of the following parent solutions (all of them are modules of the active solution that declare the selected solution as a module).\nPlease choose one of these solutions.",
				options, defaultIndex);

			if (selectedProjectIndex >= 0)
			{
				// the user selected a project
				return activeReferencingModules.get(selectedProjectIndex);
			}
			else
			{
				// the user canceled the dialog
				return null;
			}
		}
		else
		{
			ServoyLog.logError("Trying to remove modules while there is no active modules that contain them!", null);
			return null;
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		selectedProject = null;
		parentSolutionProject = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				node.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE) || (node.getType() == UserNodeType.WEB_PACKAGE_PROJECT_IN_WORKSPACE && node.isEnabled());
			if (state) try
			{
				Object realObject = node.getRealObject();
				if (realObject instanceof IPackageReader)
				{
					IResource packageRoot = SolutionExplorerTreeContentProvider.getResource((IPackageReader)realObject);
					if (packageRoot != null)
					{
						selectedProject = (ServoyNGPackageProject)packageRoot.getProject().getNature(ServoyNGPackageProject.NATURE_ID);
						SimpleUserNode parentSolutionNode = node.getAncestorOfType(ServoyProject.class);
						parentSolutionProject = (parentSolutionNode != null ? (ServoyProject)parentSolutionNode.getRealObject() : null);
					}
				}
				else if (realObject instanceof IProject && ((IProject)realObject).isAccessible() &&
					((IProject)realObject).hasNature(ServoyNGPackageProject.NATURE_ID))
				{
					selectedProject = (ServoyNGPackageProject)((IProject)realObject).getNature(ServoyNGPackageProject.NATURE_ID);
				}
				else state = false;
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		setEnabled(state);
	}

}
