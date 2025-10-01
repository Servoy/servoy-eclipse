/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Solution;

/**
 * Create a new Servoy working set.
 *
 * @author lvostinar
 *
 */
public class AddWorkingSetAction extends Action implements ISelectionChangedListener
{
	public static final AddWorkingSetAction INSTANCE = new AddWorkingSetAction();

	private SimpleUserNode selection = null;

	public AddWorkingSetAction()
	{
		setText("Add working set");
		setToolTipText("Add working set");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("new_servoy_workingset.png"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.FORMS || type == UserNodeType.COMPONENT_FORMS;
			selection = (SimpleUserNode)sel.getFirstElement();
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (selection != null && selection.getRealObject() instanceof Solution)
		{
			String solName = ((Solution)selection.getRealObject()).getName();
			String workingSetName = askWorkingSetName(solName);
			if (workingSetName != null)
			{
				IFile[] projectFiles = new IFile[0];
				if (selection != null && selection.getRealObject() instanceof Solution)
				{
					IProject project = ServoyModel.getWorkspace().getRoot().getProject(solName);
					// add .project file so we know to which project it belongs
					projectFiles = new IFile[] { project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME) };
				}
				IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
				if (ws == null)
				{
					ws = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(workingSetName, projectFiles);
					// setid shouldn't trigger a change event
					ws.setId(ServoyModel.SERVOY_WORKING_SET_ID);
					PlatformUI.getWorkbench().getWorkingSetManager().addWorkingSet(ws);
				}
				else if (ServoyModel.SERVOY_WORKING_SET_ID.equals(ws.getId()))
				{
					boolean changed = false;
					for (IFile file : projectFiles)
					{
						List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
						if (files.size() > 0)
						{
							if (!files.contains(file))
							{
								changed = true;
								PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(file, new IWorkingSet[] { ws });
							}
						}
					}
					if (!changed)
					{
						UIUtils.reportWarning("Existing working set",
							"Working set '" + ws.getName() + "' already exists and solution was already added to it.");
					}
				}
				else
				{
					// cannot add to non servoy working set
					UIUtils.reportError("Cannot modify working set",
						"Working set '" + ws.getName() + "' already exists but is not a Servoy working set. Cannot modify it.");
				}
			}
		}
	}

	private String askWorkingSetName(String solutionName)
	{
		List<String> servoyWorkingSets = new ArrayList<String>();
		IWorkingSet[] allWorkingSets = PlatformUI.getWorkbench().getWorkingSetManager().getAllWorkingSets();
		if (allWorkingSets != null)
		{
			List<String> existingWorkingSets = null;
			if (ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject() != null)
			{
				existingWorkingSets = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getServoyWorkingSets(
					new String[] { solutionName });
			}
			for (IWorkingSet ws : allWorkingSets)
			{
				if (ServoyModel.SERVOY_WORKING_SET_ID.equals(ws.getId()) && (existingWorkingSets == null || !existingWorkingSets.contains(ws.getName())))
				{
					servoyWorkingSets.add(ws.getName());
				}
			}
		}
		return UIUtils.showEditableOptionDialog(UIUtils.getActiveShell(), "Add working set", "Create a new working set or add project to existing working set",
			servoyWorkingSets.toArray(new String[0]), -1);
	}
}
