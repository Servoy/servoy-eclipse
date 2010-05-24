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
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;

/**
 * Action able to select all nodes of a TreeViewer or TableViewer.
 * 
 * @author Andrei Costescu
 */
public class SelectAllAction extends Action
{
	private final StructuredViewer viewer;

	public SelectAllAction(StructuredViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void run()
	{
		if (viewer instanceof TreeViewer)
		{
			((TreeViewer)viewer).getTree().selectAll();
		}
		else if (viewer instanceof TableViewer)
		{
			((TableViewer)viewer).getTable().selectAll();
		}
		else
		{
			return;
		}
		// fire selection events
		viewer.setSelection(viewer.getSelection());
	}

}
