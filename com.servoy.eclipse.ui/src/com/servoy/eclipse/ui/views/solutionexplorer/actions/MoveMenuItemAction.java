/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

import static java.util.Arrays.asList;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.editors.MenuEditor;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.Solution;

/**
 * @author lvostinar
 *
 */
public class MoveMenuItemAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private final boolean moveUp;

	/**
	 * @param solutionExplorerView
	 * @param b
	 */
	public MoveMenuItemAction(SolutionExplorerView solutionExplorerView, boolean moveUp)
	{
		viewer = solutionExplorerView;
		this.moveUp = moveUp;
		setText("Move " + (moveUp ? "Up" : "Down"));
		setToolTipText("Move " + (moveUp ? "Up" : "Down"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			UserNodeType type = node.getType();
			state = (type == UserNodeType.MENU_ITEM);
			if (state)
			{
				MenuItem menu = (MenuItem)node.getRealObject();
				List<IPersist> children = ((AbstractBase)menu.getParent()).getAllObjectsAsList();
				int index = children.indexOf(menu);
				if (children.size() <= 1)
				{
					state = false;
				}
				else if (moveUp && index == 0)
				{
					state = false;
				}
				else if (!moveUp && index == children.size() - 1)
				{
					state = false;
				}
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		String solutionName = null;
		if (node.getRealObject() instanceof MenuItem menuItem)
		{
			solutionName = ((Solution)((IPersist)node.getRealObject()).getRootObject()).getName();

			if (solutionName != null)
			{
				ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
				if (servoyProject == null)
				{
					return;
				}
				Solution editingSolution = servoyProject.getEditingSolution();
				MenuItem editingMenuItem = (MenuItem)editingSolution.searchChild(menuItem.getUUID()).get();
				if (editingMenuItem != null)
				{
					((AbstractBase)editingMenuItem.getParent()).moveChild(editingMenuItem, moveUp);
					try
					{
						servoyProject.saveEditingSolutionNodes(new IPersist[] { editingMenuItem.getAncestor(IRepository.MENUS) }, true);
						MenuEditor.refreshEditor();
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
						MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Save failed: " + e.getMessage());
					}
					((AbstractBase)menuItem.getParent()).moveChild(menuItem, moveUp);
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(true,
						asList(new IPersist[] { menuItem.getAncestor(IRepository.MENUS) }));
				}
			}
		}
	}
}
