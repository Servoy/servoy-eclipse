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

import java.io.IOException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.Solution;

/**
 * 
 * Action to create media folder.
 * 
 * @author gboros
 */
public class CreateMediaFolderAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private SimpleUserNode selectedFolder;

	public CreateMediaFolderAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Create media folder");
		setToolTipText(getText());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		selectedFolder = null;
		if (sel.size() == 1 &&
			((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA) || (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA_FOLDER)))
		{
			selectedFolder = ((SimpleUserNode)sel.getFirstElement());
		}
		setEnabled(selectedFolder != null);
	}

	@Override
	public void run()
	{
		if (selectedFolder == null) return;

		InputDialog newFolderNameDlg = new InputDialog(viewer.getSite().getShell(), "New media folder", "Specify a folder name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() < 1)
				{
					return "Name cannot be empty";
				}
				else if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0 || newText.endsWith(".") ||
					newText.startsWith("."))
				{
					return "Invalid new media name";
				}

				return null;
			}
		});

		newFolderNameDlg.setBlockOnOpen(true);
		newFolderNameDlg.open();
		if (newFolderNameDlg.getReturnCode() == Window.OK)
		{
			WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			Solution selectedSolution = null;
			String mediaFolder = null;
			Object selectedFolderRealObject = selectedFolder.getRealObject();
			if (selectedFolderRealObject instanceof Solution)
			{
				selectedSolution = (Solution)selectedFolderRealObject;
				mediaFolder = "";
			}
			else if (selectedFolderRealObject instanceof MediaNode)
			{
				IMediaProvider mp = ((MediaNode)selectedFolderRealObject).getMediaProvider();
				if (mp instanceof Solution)
				{
					selectedSolution = (Solution)mp;
					mediaFolder = ((MediaNode)selectedFolderRealObject).getPath();
				}
			}

			if (selectedSolution != null)
			{
				try
				{
					wsa.createFolder(selectedSolution.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + mediaFolder + newFolderNameDlg.getValue());
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
