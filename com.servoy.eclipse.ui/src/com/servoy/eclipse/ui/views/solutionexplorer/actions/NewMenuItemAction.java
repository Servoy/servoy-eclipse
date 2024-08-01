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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action to create a new valuelist depending on the selection of a solution view.
 *
 * @author jcompagner
 */
public class NewMenuItemAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewMenuItemAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setText("Create menu item");
		setToolTipText("Create menu item");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.MENU || type == UserNodeType.MENU_ITEM;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getRealObject() instanceof Menu || node.getRealObject() instanceof MenuItem)
		{
			IPersist parent = (IPersist)node.getRealObject();
			Solution realSolution = (Solution)parent.getRootObject();

			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(realSolution.getName());
			Solution editingSolution = servoyProject.getEditingSolution();
			if (editingSolution == null)
			{
				return;
			}
			parent = editingSolution.searchChild(parent.getUUID()).get();
			if (parent == null)
			{
				return;
			}
			String name = askMenuItemName(viewer.getViewSite().getShell());
			if (name != null)
			{
				IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
				try
				{
					MenuItem mn = parent instanceof Menu ? ((Menu)parent).createNewMenuItem(validator, name)
						: ((MenuItem)parent).createNewMenuItem(validator, name);
					servoyProject.saveEditingSolutionNodes(new IPersist[] { mn.getAncestor(IRepository.MENUS) }, true);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
					MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Save failed: " + e.getMessage());
				}
			}
		}
	}

	public static String askMenuItemName(Shell shell)
	{
		InputDialog nameDialog = new InputDialog(shell, "Create menu item", "Supply menu item name(id)", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				boolean valid = IdentDocumentValidator.isJavaIdentifier(newText);
				return valid ? null : (newText.length() == 0 ? "" : "Invalid menu item name(id)");
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
