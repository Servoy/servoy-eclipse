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

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;

import com.servoy.eclipse.ui.node.SimpleUserNode;

/**
 * Collapses a TreeViewer.
 * 
 * @author acostescu
 */
public class ExpandNodeAction extends Action implements ISelectionChangedListener
{

	private final TreeViewer treeViewer;
	private IStructuredSelection selection;

	/**
	 * Creates a new collapse action for the given TreeViewer.
	 * 
	 * @param treeViewer the TreeViewer to use.
	 */
	public ExpandNodeAction(TreeViewer treeViewer)
	{
		this.treeViewer = treeViewer;
		setText("Expand");
	}

	@Override
	public void run()
	{
		if (selection != null)
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			while (it.hasNext())
			{
				treeViewer.expandToLevel(it.next(), AbstractTreeViewer.ALL_LEVELS);
			}
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean enabled = false;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			selection = (IStructuredSelection)sel;
			if (selection.size() > 0)
			{
				enabled = true;
				Iterator< ? > it = selection.iterator();
				while (enabled && it.hasNext())
				{
					if (!(it.next() instanceof SimpleUserNode))
					{
						enabled = false;
						selection = null;
					}
				}
			}
		}
		setEnabled(enabled);
	}
}