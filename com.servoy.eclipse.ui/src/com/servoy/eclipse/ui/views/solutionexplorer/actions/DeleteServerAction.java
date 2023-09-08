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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class DeleteServerAction extends Action implements ISelectionChangedListener
{

	private final SolutionExplorerView viewer;
	private IStructuredSelection selection;

	/**
	 * Creates a new delete table action.
	 */
	public DeleteServerAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Delete database connection");
		setToolTipText("Delete database connection");
	}

	@Override
	public void run()
	{
		if (selection != null &&
			MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?"))
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			try
			{
				IServerManagerInternal serverManager = ApplicationServerRegistry.get().getServerManager();
				while (it.hasNext())
				{
					IServerInternal server = (IServerInternal)it.next().getRealObject();
					serverManager.saveServerConfig(server.getName(), null);
					EditorUtil.closeEditor(server);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
				MessageDialog.openInformation(UIUtils.getActiveShell(), "Server delete", "Cannot delete server: " + e.getMessage());
			}
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> it = sel.iterator();
			while (it.hasNext() && state)
			{
				SimpleUserNode node = it.next();
				state = (node.getType() == UserNodeType.SERVER);
			}
		}
		if (state)
		{
			selection = sel;
		}
		else
		{
			selection = null;
		}
		setEnabled(state);
	}

}