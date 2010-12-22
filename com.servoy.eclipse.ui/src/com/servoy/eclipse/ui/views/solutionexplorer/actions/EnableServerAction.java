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
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ServerConfig;


public class EnableServerAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection currentSelection;
	private final Shell shell;

	public EnableServerAction(Shell shell)
	{
		this.shell = shell;
		setText("Enable/Disable Server");
		setToolTipText("Enable/Disable Server");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("serverDisabled.gif"));
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		currentSelection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> it = sel.iterator();
			while (it.hasNext() && state)
			{
				state = (it.next().getType() == UserNodeType.SERVER);
			}
		}
		if (state)
		{
			currentSelection = sel;
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (currentSelection == null) return;

		Iterator<SimpleUserNode> it = currentSelection.iterator();
		while (it.hasNext())
		{
			SimpleUserNode node = it.next();
			if (node.getRealObject() instanceof IServerInternal)
			{
				IServerInternal server = (IServerInternal)node.getRealObject();
				setServerEnabled(shell, server, !server.getConfig().isEnabled());
			}
		}
		ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
	}

	public static void setServerEnabled(Shell shell, IServerInternal server, boolean enabled)
	{
		try
		{
			ServerConfig serverConfig = server.getConfig().getEnabledCopy(enabled);
			IServerManagerInternal serverManager = server.getServerManager();
			if (enabled)
			{
				serverManager.testServerConfigConnection(serverConfig, 0);
			}
			serverManager.saveServerConfig(server.getName(), serverConfig);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
			MessageDialog.openError(shell, "Cannot enable server", e.getMessage());
		}
	}
}
