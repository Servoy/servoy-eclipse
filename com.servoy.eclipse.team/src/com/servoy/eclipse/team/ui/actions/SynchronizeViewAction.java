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
package com.servoy.eclipse.team.ui.actions;


import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

import com.servoy.eclipse.ui.views.solutionexplorer.SimpleResourceMapping;


public class SynchronizeViewAction extends Action
{
	protected final ISynchronizePageConfiguration configuration;

	public SynchronizeViewAction(ISynchronizePageConfiguration configuration)
	{
		this.configuration = configuration;
	}

	public ResourceMapping[] getSelectedMappings()
	{
		ISelectionProvider selectionProvider = configuration.getSite().getSelectionProvider();
		TreeSelection selection = (TreeSelection)selectionProvider.getSelection();
		TreePath[] selectionPaths = selection.getPaths();

		ResourceMapping[] selectedResources = new ResourceMapping[selectionPaths.length];
		int c = 0;
		for (TreePath treePath : selectionPaths)
		{
			selectedResources[c++] = new SimpleResourceMapping((IResource)treePath.getLastSegment());
		}

		return selectedResources;
	}
}
