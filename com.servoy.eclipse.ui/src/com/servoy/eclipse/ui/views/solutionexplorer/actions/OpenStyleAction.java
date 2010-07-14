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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Style;

/**
 * This action opens in the editor the style element currently selected in the outline of the solution view.
 * 
 * @author acostescu
 */
public class OpenStyleAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;

	/**
	 * Creates a new open action that uses the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public OpenStyleAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("open.gif")); //$NON-NLS-1$
		setText("Open style");
		setToolTipText("Open style");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		Iterator it = sel.iterator();
		boolean state = it.hasNext();
		while (it.hasNext())
		{
			UserNodeType type = ((SimpleUserNode)it.next()).getType();
			if (type != UserNodeType.STYLE_ITEM)
			{
				state = false;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		IStructuredSelection sel = viewer.getListSelection();
		Iterator it = sel.iterator();
		while (it.hasNext())
		{
			open((SimpleUserNode)it.next());
		}
	}

	private void open(SimpleUserNode node)
	{
		if (node != null)
		{
			IPersist persist = (IPersist)node.getRealObject();
			if (node.getType() == UserNodeType.STYLE_ITEM)
			{
				EditorUtil.openStyleEditor((Style)persist);
			}
		}
	}
}