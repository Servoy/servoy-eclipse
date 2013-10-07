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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
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
public class NewWorkingSetAction extends Action implements ISelectionChangedListener
{
	private SimpleUserNode selection = null;

	public NewWorkingSetAction()
	{
		setText("Create working set");
		setToolTipText("Create working set");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("servoy_workingset.gif")); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.FORMS;
			selection = (SimpleUserNode)sel.getFirstElement();
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		String workingSetName = askWorkingSetName();
		if (workingSetName != null)
		{
			IFile[] initialFiles = new IFile[0];
			if (selection != null && selection.getRealObject() instanceof Solution)
			{
				String solName = ((Solution)selection.getRealObject()).getName();
				ServoyModelManager.getServoyModelManager().getServoyModel();
				IProject project = ServoyModel.getWorkspace().getRoot().getProject(solName);
				// add .project file so we know to which project it belongs
				initialFiles = new IFile[] { project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME) };
			}
			IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().createWorkingSet(workingSetName, initialFiles);
			// setid shouldn't trigger a change event
			ws.setId(ServoyModel.SERVOY_WORKING_SET_ID);
			PlatformUI.getWorkbench().getWorkingSetManager().addWorkingSet(ws);
		}
	}

	private String askWorkingSetName()
	{
		InputDialog nameDialog = new InputDialog(UIUtils.getActiveShell(), "Create working set", "Supply working set name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				String message = null;
				if (newText.length() == 0)
				{
					message = "";
				}
				else
				{
					IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(newText);
					if (ws != null)
					{
						message = "Working set already exists.";
					}
				}
				return message;
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
