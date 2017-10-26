/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.io.IOException;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Delete a folder in component/layout/service.
 * @author emera
 */
public class DeleteWebPackageFolder extends Action implements ISelectionChangedListener
{
	private SimpleUserNode selectedFolder;
	private final SolutionExplorerView viewer;
	private final Shell shell;

	public DeleteWebPackageFolder(SolutionExplorerView solutionExplorerView, Shell shell, String text)
	{
		this.viewer = solutionExplorerView;
		this.shell = shell;
		setText(text);
		setToolTipText(getText());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		selectedFolder = null;
		if (sel.size() == 1)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.WEB_OBJECT_FOLDER)
			{
				selectedFolder = (SimpleUserNode)(sel.getFirstElement());
			}
		}
		setEnabled(selectedFolder != null);
	}

	@Override
	public void run()
	{
		if (selectedFolder == null) return;

		if (MessageDialog.openConfirm(shell, getText(), "Are you sure you want to delete?"))
		{
			WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			try
			{
				wsa.delete(((IFolder)selectedFolder.getRealObject()).getFullPath().toString());
				viewer.refreshTreeCompletely();
			}
			catch (IOException e)
			{
				ServoyLog.logError(e);
			}
		}
	}
}
