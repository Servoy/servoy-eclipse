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
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.IEditorPart;

import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Some changes of the active editor need to update node appearance in the tree. For example if the active editor is a form designer, the corresponding form in
 * the tree changes background.
 * 
 * @author acostescu
 */
public class HighlightNodeUpdater implements ActiveEditorListener
{

	private final TreeViewer treeViewer;
	private final SolutionExplorerTreeContentProvider treeContentProvider;
	private IPersist currentActiveEditorPersist;

	public HighlightNodeUpdater(TreeViewer treeViewer, SolutionExplorerTreeContentProvider treeContentProvider)
	{
		this.treeViewer = treeViewer;
		this.treeContentProvider = treeContentProvider;
	}

	public void activeEditorChanged(IEditorPart newActiveEditor)
	{
		IPersist oldActiveEditorPersist = currentActiveEditorPersist;
		if (newActiveEditor != null)
		{
			currentActiveEditorPersist = (IPersist)newActiveEditor.getAdapter(IPersist.class);
		}
		else
		{
			currentActiveEditorPersist = null;
		}

		if (currentActiveEditorPersist != oldActiveEditorPersist)
		{
			if (oldActiveEditorPersist instanceof Form)
			{
				Object oldNode = treeContentProvider.getNodesForPersist(oldActiveEditorPersist)[0];
				if (oldNode != null)
				{
					treeViewer.update(oldNode, null);
				}
			}
			if (currentActiveEditorPersist instanceof Form)
			{
				Object newNode = treeContentProvider.getNodesForPersist(currentActiveEditorPersist)[0];
				if (newNode != null)
				{
					treeViewer.update(newNode, null);
				}
			}
		}
	}

	public IPersist getActiveEditorPersist()
	{
		return currentActiveEditorPersist;
	}
}
