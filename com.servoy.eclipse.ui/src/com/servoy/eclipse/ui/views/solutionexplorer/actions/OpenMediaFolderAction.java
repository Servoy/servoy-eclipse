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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * This action opens the media folder in the solex list
 * 
 * @author gboros
 */
public class OpenMediaFolderAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView sev;
	private String currentMediaFolder;

	public OpenMediaFolderAction(SolutionExplorerView sev)
	{
		this.sev = sev;
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			SimpleUserNode sun = (SimpleUserNode)sel.getFirstElement();
			UserNodeType type = sun.getType();
			state = (type == UserNodeType.MEDIA_FOLDER) || (type == UserNodeType.MEDIA_PARENT_FOLDER);
			if (state)
			{
				currentMediaFolder = ((Pair<Solution, String>)sun.getRealObject()).getRight();
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		sev.setCurrentMediaFolder(currentMediaFolder);
	}
}