/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.rmi.RemoteException;
import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Table;

/**
 * Action used for toggling the hiddenInDeveloper flag of tables. Changes text to match situation based on selection.
 * @author acostescu
 */
public class HideUnhideTablesAction extends Action implements ISelectionChangedListener
{

	protected IStructuredSelection selection = null;

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;

		boolean areHidden = false;
		boolean areNormal = false;

		ISelection sel = event.getSelection();
		if (sel instanceof IStructuredSelection)
		{
			IStructuredSelection s = (IStructuredSelection)sel;

			boolean ok = s.size() > 0;
			Iterator<SimpleUserNode> it = s.iterator();
			while (it.hasNext() && ok)
			{
				SimpleUserNode tableNode = it.next();
				if (tableNode.getType() != UserNodeType.TABLE && tableNode.getType() != UserNodeType.VIEW)
				{
					ok = false;
				}
				else
				{
					boolean hidden = (tableNode.getAppearenceFlags() & SimpleUserNode.TEXT_GRAYED_OUT) != 0;
					if (hidden) areHidden = true;
					else areNormal = true;
				}
			}
			if (ok)
			{
				selection = s;
			}
		}

		if (selection != null)
		{
			setEnabled(true);
			String txt;
			if (areHidden && areNormal)
			{
				txt = Messages.HideUnhideTablesAction_toggle;
			}
			else
			{
				txt = areHidden ? Messages.HideUnhideTablesAction_unhide : Messages.HideUnhideTablesAction_hide;
			}

			setText(txt);
			setToolTipText(txt);
		}
		else
		{
			setEnabled(false);
			setText("");
			setToolTipText("");
		}
	}

	@Override
	public void run()
	{
		if (selection == null) return;
		IServerManagerInternal sm = ServoyModel.getServerManager();
		DataModelManager dmm = ServoyModelFinder.getServoyModel().getDataModelManager();

		Iterator<SimpleUserNode> it = selection.iterator();
		while (it.hasNext())
		{
			SimpleUserNode tableNode = it.next();
			if ((tableNode.getType() == UserNodeType.TABLE || tableNode.getType() == UserNodeType.VIEW) && (tableNode.getRealObject() instanceof TableWrapper))
			{
				TableWrapper tw = (TableWrapper)tableNode.getRealObject();
				IServer s = sm.getServer(tw.getServerName());
				try
				{
					Table t = (Table)s.getTable(tw.getTableName());
					((IServerInternal)s).setTableMarkedAsHiddenInDeveloper(tw.getTableName(), !t.isMarkedAsHiddenInDeveloper());
					dmm.updateAllColumnInfo(t, true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Error hiding/unhiding tables/views", e);
				}
				catch (RemoteException e)
				{
					ServoyLog.logError("Error hiding/unhiding tables/views", e);
				}
			}
		}
	}

}
