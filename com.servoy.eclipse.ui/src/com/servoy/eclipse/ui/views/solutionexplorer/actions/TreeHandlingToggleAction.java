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

import org.eclipse.jface.action.Action;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * This action is responsible to add or remove the tree handling actions (Refresh View, Expand, Collapse tree)
 * from the context menu of solex. This action is added to the solex view menu.
 * @author alorincz
 *
 */
public class TreeHandlingToggleAction extends Action
{
	private final SolutionExplorerView solexView;

	public TreeHandlingToggleAction(SolutionExplorerView solexView, boolean checked)
	{
		super("", AS_CHECK_BOX);
		this.solexView = solexView;
		setText("Show Tree Handling Group In Context Menu");
		setChecked(checked);
	}

	@Override
	public void run()
	{
		solexView.showContextMenuTreeHandling(isChecked());
	}
}
