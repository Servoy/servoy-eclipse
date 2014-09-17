/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

/**
 * Deletes the selected components or services.
 * @author gganea
 */
public class DeleteComponentResourceAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	private final Shell shell;
	private final UserNodeType nodeType;


	public DeleteComponentResourceAction(Shell shell, String text, UserNodeType nodeType)
	{
		this.shell = shell;
		this.nodeType = nodeType;
		setText(text);
		setToolTipText(text);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run()
	{
		if (selection != null && MessageDialog.openConfirm(shell, getText(), "Are you sure you want to delete?"))
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			String[] componentName;
			while (it.hasNext())
			{
				SimpleUserNode next = it.next();
				Object realObject = next.getRealObject();
				if (realObject instanceof IResource)
				{
					IResource resource = (IResource)realObject;
					try
					{
						resource.delete(true, new NullProgressMonitor());
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		// allow multiple selection
		selection = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = true;
		Iterator<SimpleUserNode> it = sel.iterator();
		while (it.hasNext() && state)
		{
			SimpleUserNode node = it.next();
			state = (node.getType() == nodeType);
		}
		if (state)
		{
			selection = sel;
		}
		setEnabled(state);
	}


}
