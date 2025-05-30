/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.nature.ServoyDeveloperProject;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.ArrayLabelProvider;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;


public class AddDevSolutionAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;

	public AddDevSolutionAction(Shell shell)
	{
		this.shell = shell;
		setText("Add developer solution");
		setToolTipText("Add a developer solution");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("add_as_module.png"));
	}

	@Override
	public void run()
	{
		AbstractServoyModel servoyModel = (AbstractServoyModel)ServoyModelManager.getServoyModelManager().getServoyModel();


		List<String> alldevProjectsNames = servoyModel.getDeveloperProjectNames();
		String[] projectNames = servoyModel.getActiveProject().getDeveloperProjects().stream().map(project -> project.getProject().getName())
			.toArray(String[]::new);

		StructuredSelection theSelection = null;
		if (projectNames == null) theSelection = StructuredSelection.EMPTY;
		else
		{
			theSelection = new StructuredSelection(projectNames);
		}
		Collections.sort(alldevProjectsNames, String.CASE_INSENSITIVE_ORDER);

		ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;

		int treeStyle = SWT.MULTI | SWT.CHECK;

		TreeSelectDialog dialog = new TreeSelectDialog(shell, false, false, TreePatternFilter.FILTER_LEAFS, contentProvider, new ArrayLabelProvider(null),
			null,
			new LeafnodesSelectionFilter(contentProvider), treeStyle, "Select developer solutions", alldevProjectsNames.toArray(), theSelection, true,
			"Select developer solutions",
			null, false);

		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL) return;

		Object[] selectedProjsArray = ((IStructuredSelection)dialog.getSelection()).toArray();

		ServoyProject activeSolution = servoyModel.getActiveProject();
		if (activeSolution == null) return;
		List<IProject> referencedProjects = new ArrayList<IProject>();
		if (selectedProjsArray != null)
		{
			for (Object selectedProj : selectedProjsArray)
			{
				referencedProjects.add(ResourcesPlugin.getWorkspace().getRoot().getProject(selectedProj.toString()));
			}
		}
		try
		{
			IProjectDescription developerProjectDescription = activeSolution.getProject().getDescription();
			IProject[] oldProjects = developerProjectDescription.getReferencedProjects();
			for (IProject oldProject : oldProjects)
			{
				if (!referencedProjects.contains(oldProject) && !oldProject.hasNature(ServoyDeveloperProject.NATURE_ID))
				{
					referencedProjects.add(oldProject); // keep the old projects
				}
			}
			developerProjectDescription.setReferencedProjects(referencedProjects.toArray(new IProject[referencedProjects.size()]));
			activeSolution.getProject().setDescription(developerProjectDescription, null);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			return;
		}

	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.DEVELOPER_SOLUTIONS);
		}
		setEnabled(state);
	}
}
