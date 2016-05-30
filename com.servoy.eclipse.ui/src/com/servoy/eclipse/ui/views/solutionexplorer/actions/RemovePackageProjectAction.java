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

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyNGPackageProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.j2db.util.Debug;

/**
 * @author gganea@servoy.com
 *
 */
public class RemovePackageProjectAction extends Action implements ISelectionChangedListener
{


	private final Shell shell;
	private ServoyNGPackageProject selectedProject;

	/**
	 * Creates a new remove package project action.
	 *
	 * @param shell shell that might be used to display a dialog to the user (too choose a parent solution).
	 */
	public RemovePackageProjectAction(Shell shell)
	{
		this.shell = shell;
		setText("Remove reference to this Package Project");
		setToolTipText(
			"Removes a Package Project reference from one of the active modules that references it (the package project itself will not be deleted)");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("remove_module.gif"));
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
			removeProjecReference(project, projectToBeRemoved);
		}
	}

	public static void removeProjecReference(IProject fromProject, IProject projectToBeRemoved)
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
			Debug.log(e);
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
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		IProject[] referencingProjects = selectedProject.getProject().getReferencingProjects();


		ArrayList<ServoyProject> activeParentModules = new ArrayList<ServoyProject>();

		for (IProject servoyProject : referencingProjects)
		{
			try
			{
				if (servoyProject.hasNature(ServoyProject.NATURE_ID))
				{
					activeParentModules.add((ServoyProject)servoyProject.getNature(ServoyProject.NATURE_ID));
				}
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
		}

		if (activeParentModules.size() == 1)
		{
			return activeParentModules.get(0);
		}
		else if (activeParentModules.size() > 1)
		{
			ServoyProject activeProject = servoyModel.getActiveProject();
			int defaultIndex = 0;
			String options[] = new String[activeParentModules.size()];
			for (int i = activeParentModules.size() - 1; i >= 0; i--)
			{
				if (activeParentModules.get(i) == activeProject)
				{
					defaultIndex = i;
				}
				options[i] = activeParentModules.get(i).getProject().getName();
			}
			int selectedProjectIndex = UIUtils.showOptionDialog(shell, "Choose parent solution",
				"You may remove the selected solution (as module) from any of the following parent solutions (all of them are modules of the active solution that declare the selected solution as a module).\nPlease choose one of these solutions.",
				options, defaultIndex);

			if (selectedProjectIndex >= 0)
			{
				// the user selected a project
				return activeParentModules.get(selectedProjectIndex);
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
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		selectedProject = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.COMPONENTS_PROJECT_PACKAGE || node.getType() == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				node.getType() == UserNodeType.LAYOUT_PROJECT_PACKAGE);
			if (state) try
			{
				IResource packageRoot = SolutionExplorerTreeContentProvider.getResource((IPackageReader)node.getRealObject());
				if (packageRoot != null) selectedProject = (ServoyNGPackageProject)packageRoot.getProject().getNature(ServoyNGPackageProject.NATURE_ID);
			}
			catch (CoreException e)
			{
				Debug.log(e);
			}
		}
		setEnabled(state);
	}

}
