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

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;

public class OpenServerAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	/**
	 * Creates a new open action that uses the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public OpenServerAction(SolutionExplorerView sev)
	{
		viewer = sev;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("open_server.png"));
		setText("Open server");
		setToolTipText("Open server");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			state = type == UserNodeType.SERVER;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node.getType() == UserNodeType.SERVER)
		{
			IServerInternal server = ((IServerInternal)node.getRealObject());
			if (server != null)
			{
				// re-lookup the server config here, when the server is a duplicate the server is actually the other server object.
				IServerManagerInternal serverManager = server.getServerManager();
				EditorUtil.openServerEditor(serverManager.getServerConfig(node.getName()), serverManager.getServerSettings(node.getName()));
			}
		}
	}
}