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

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.sablo.specification.Package.IPackageReader;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Creates a new folder in component/layout/service.
 * @author emera
 */
public class NewWebPackageFolderAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private SimpleUserNode selectedNode;

	public NewWebPackageFolderAction(SolutionExplorerView viewer, String text)
	{
		this.viewer = viewer;
		setText(text);
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		selectedNode = null;
		if (sel.size() == 1)
		{
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.COMPONENT || type == UserNodeType.SERVICE || type == UserNodeType.LAYOUT || type == UserNodeType.WEB_OBJECT_FOLDER ||
				type == UserNodeType.COMPONENTS_PROJECT_PACKAGE || type == UserNodeType.SERVICES_PROJECT_PACKAGE || type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				selectedNode = (SimpleUserNode)(sel.getFirstElement());
			}
			setEnabled(selectedNode != null && ((type == UserNodeType.WEB_OBJECT_FOLDER || type == UserNodeType.COMPONENTS_PROJECT_PACKAGE ||
				type == UserNodeType.SERVICES_PROJECT_PACKAGE || type == UserNodeType.LAYOUT_PROJECT_PACKAGE) ||
				"file".equals(((WebObjectSpecification)selectedNode.getRealObject()).getSpecURL().getProtocol())));
		}
		else
		{
			setEnabled(false);
		}
	}

	@Override
	public void run()
	{
		if (selectedNode == null) return;

		InputDialog newFolderNameDlg = new InputDialog(viewer.getSite().getShell(), "New folder", "Specify a folder name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() < 1)
				{
					return "Name cannot be empty";
				}
				else if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0)
				{
					return "Invalid new folder name";
				}

				return null;
			}
		});

		newFolderNameDlg.setBlockOnOpen(true);
		newFolderNameDlg.open();
		if (newFolderNameDlg.getReturnCode() == Window.OK)
		{
			WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			IContainer f = null;
			UserNodeType type = selectedNode.getType();
			if (type == UserNodeType.WEB_OBJECT_FOLDER)
			{
				f = (IContainer)selectedNode.getRealObject();
			}
			else if (type == UserNodeType.COMPONENTS_PROJECT_PACKAGE || type == UserNodeType.SERVICES_PROJECT_PACKAGE ||
				type == UserNodeType.LAYOUT_PROJECT_PACKAGE)
			{
				f = ServoyModel.getWorkspace().getRoot().getProject(((IPackageReader)selectedNode.getRealObject()).getPackageName());
			}
			else
			{
				WebObjectSpecification spec = (WebObjectSpecification)selectedNode.getRealObject();
				f = SolutionExplorerTreeContentProvider.getFolderFromSpec(
					(IProject)SolutionExplorerTreeContentProvider.getResource((IPackageReader)selectedNode.parent.getRealObject()), spec);
				if (f == null)
				{
					ServoyLog.logInfo("cannot find web object name from " + spec.getName());
				}
			}

			if (f != null)
			{
				try
				{
					wsa.createFolder(f.getFullPath() + "/" + newFolderNameDlg.getValue());
					viewer.refreshTreeCompletely();
				}
				catch (IOException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
	}

}
