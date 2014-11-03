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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Deletes the selected components or services.
 * @author gganea
 */
public class DeleteComponentResourceAction extends Action implements ISelectionChangedListener
{

	private IStructuredSelection selection;
	private final Shell shell;
	private final UserNodeType nodeType;
	private final SolutionExplorerView viewer;


	public DeleteComponentResourceAction(SolutionExplorerView viewer, Shell shell, String text, UserNodeType nodeType)
	{
		this.viewer = viewer;
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
		PlatformSimpleUserNode parent = (PlatformSimpleUserNode)viewer.getSelectedTreeNode().parent;
		IProject resources = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getResourcesProject().getProject();
		boolean deleted = false;
		if (selection != null && MessageDialog.openConfirm(shell, getText(), "Are you sure you want to delete?"))
		{
			Iterator<SimpleUserNode> it = selection.iterator();
			while (it.hasNext())
			{
				SimpleUserNode next = it.next();
				IResource resource = null;
				if (next.getType() == UserNodeType.COMPONENTS_PACKAGE || next.getType() == UserNodeType.SERVICES_PACKAGE)
				{
					resource = resources.getFolder((next.getType() == UserNodeType.COMPONENTS_PACKAGE ? SolutionSerializer.COMPONENTS_DIR_NAME
						: SolutionSerializer.SERVICES_DIR_NAME) + "/" + (String)next.getRealObject());
				}
				else
				{
					Object realObject = next.getRealObject();
					if (realObject instanceof IResource)
					{
						resource = (IResource)realObject;
					}
				}
				if (resource != null)
				{
					try
					{
						resource.delete(true, new NullProgressMonitor());
						resources.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
						deleted = true;
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}

			}
			if (deleted)
			{
				parent.children = null;
				viewer.refreshTreeNodeFromModel(parent);
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
