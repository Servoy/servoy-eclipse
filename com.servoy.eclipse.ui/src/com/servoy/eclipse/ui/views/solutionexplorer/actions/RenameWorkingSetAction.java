/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * @author lvostinar
 *
 */
public class RenameWorkingSetAction extends Action implements ISelectionChangedListener
{
	private SimpleUserNode selection = null;

	public RenameWorkingSetAction()
	{
		setText("Rename working set");
		setToolTipText("Rename working set");
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.WORKING_SET;
			selection = (SimpleUserNode)sel.getFirstElement();
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		String workingSetName = selection.getName();
		IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
		if (ws != null)
		{
			InputDialog renameWSNameDlg = new InputDialog(UIUtils.getActiveShell(), "Rename working set", "Specify a new working set name", workingSetName,
				new IInputValidator()
				{
					public String isValid(String newText)
					{
						if (newText.equalsIgnoreCase(selection.getName()))
						{
							return "Please enter a different name";
						}
						else if (PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(newText) != null)
						{
							return "Working set already exists";
						}
						else return null;
					}
				});

			renameWSNameDlg.setBlockOnOpen(true);
			renameWSNameDlg.open();
			if (renameWSNameDlg.getReturnCode() == Window.OK)
			{
				ws.setName(renameWSNameDlg.getValue());
			}
		}
	}
}
