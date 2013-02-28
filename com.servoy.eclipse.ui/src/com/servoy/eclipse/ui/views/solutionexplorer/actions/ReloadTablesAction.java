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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;

public class ReloadTablesAction extends Action implements ISelectionChangedListener
{

	protected IStructuredSelection selectedServers = null;

	public ReloadTablesAction()
	{
		setText(Messages.ReloadTablesAction_reloadTables);
		setToolTipText(Messages.ReloadTablesAction_reloadTablesDescription);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedServers = null;
		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;
			if (s.size() == 1)
			{
				// is "Servers node" selected?
				SimpleUserNode node = (SimpleUserNode)s.getFirstElement();
				UserNodeType type = node.getType();
				if ((type == UserNodeType.SERVER && ((IServerInternal)node.getRealObject()).getConfig().isEnabled()) ||
					(type == UserNodeType.SERVERS && node.children != null && node.children.length > 0))
				{
					selectedServers = s;
				}
			}
			else if (s.size() > 1)
			{
				boolean ok = true;
				Iterator<SimpleUserNode> it = s.iterator();
				while (it.hasNext() && ok)
				{
					SimpleUserNode serverNode = it.next();
					if (serverNode.getType() != UserNodeType.SERVER || !((IServerInternal)serverNode.getRealObject()).getConfig().isEnabled())
					{
						ok = false;
					}
				}
				if (ok)
				{
					selectedServers = s;
				}
			}
		}

		setEnabled(selectedServers != null);
	}

	@Override
	public void run()
	{
		if (selectedServers == null) return;

		try
		{
			if (selectedServers.size() == 1)
			{
				// is "Servers node" selected?
				SimpleUserNode node = (SimpleUserNode)selectedServers.getFirstElement();
				UserNodeType type = node.getType();
				if (type == UserNodeType.SERVERS)
				{
					for (SimpleUserNode serverNode : node.children)
					{
						reload(serverNode);
					}
				}
				else if (type == UserNodeType.SERVER)
				{
					reload(node);
				}
			}
			else if (selectedServers.size() > 1)
			{
				Iterator<SimpleUserNode> it = selectedServers.iterator();
				while (it.hasNext())
				{
					SimpleUserNode serverNode = it.next();
					if (serverNode.getType() == UserNodeType.SERVER)
					{
						reload(serverNode);
					}
				}
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Error reloading servers", e);
		}
	}

	private void reload(SimpleUserNode serverNode) throws RepositoryException
	{
		IServerInternal s = (IServerInternal)serverNode.getRealObject();
		if (s.getConfig().isEnabled() && s.isValid())
		{
			s.reloadTables();
				try
				{
					for (String tableName : s.getTableNames(false))
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().flushDataProvidersForTable(s.getTable(tableName));
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
		}
		else if (s.getConfig().isEnabled())
		{
			try
			{
				s.testConnection(0);
				s.flagValid();
			}
			catch (Exception e)
			{
				// still invalid
			}
		}
	}
}