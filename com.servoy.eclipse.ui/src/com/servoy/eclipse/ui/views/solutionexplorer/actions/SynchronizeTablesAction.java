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

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard;
import com.servoy.j2db.persistence.IServerInternal;

/**
 * Action that allows the user to check for .dbi files that do not have a corresponding table in the database. If such files are found, the user can choose
 * either to create a new table according to that file, or delete the file.<BR>
 * This action will also create default .dbi files for tables that exist, but do not have a corresponding file.
 * 
 * @author Andrei Costescu
 */
public class SynchronizeTablesAction extends OpenWizardAction implements ISelectionChangedListener
{

	private IStructuredSelection selection;

	public SynchronizeTablesAction()
	{
		super(SynchronizeDBIWithDBWizard.class, Activator.loadImageDescriptorFromBundle("sync_tables.png"), Messages.SolutionExplorerView_synchronizeTables);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = sel.size() > 0;
		if (state)
		{
			// enabled if either the servers node is selected, the resources node is selected, or one or more enabled and valid server nodes are selected
			if (sel.size() != 1 ||
				(((SimpleUserNode)sel.getFirstElement()).getType() != UserNodeType.SERVERS && ((SimpleUserNode)sel.getFirstElement()).getType() != UserNodeType.RESOURCES))
			{
				Iterator<SimpleUserNode> it = sel.iterator();
				while (it.hasNext() && state)
				{
					SimpleUserNode un = it.next();
					if (un.getType() != UserNodeType.SERVER)
					{
						state = false;
					}
					else
					{
						IServerInternal s = (IServerInternal)un.getRealObject();
						if (!s.getConfig().isEnabled() || !s.isValid())
						{
							state = false;
						}
					}
				}
				if (state)
				{
					selection = sel;
				}
			} // else state remains true
		}
		setEnabled(state);
	}

}