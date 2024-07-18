/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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
import java.io.InputStream;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
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
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.Solution;

/**
 * @author rlazar
 *
 */
public class CreateMediaFileAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private SimpleUserNode selectedFile;
	protected Solution solution;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 *
	 * @param viewer the solution view to use.
	 */
	public CreateMediaFileAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Create media file");
		setToolTipText(getText());
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	 */
	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		selectedFile = null;
		if (sel.size() == 1 && (SimpleUserNode)sel.getFirstElement() != null && ((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA) ||
			(((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA_FOLDER)))
		{
			selectedFile = ((SimpleUserNode)sel.getFirstElement());
		}
		setEnabled(selectedFile != null);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void run()
	{
		if (selectedFile == null) return;
		solution = getSolution();
		if (solution != null)
		{
			String mediaFolder = null;
			Object selectedFileRealObject = selectedFile.getRealObject();
			if (selectedFileRealObject instanceof Solution)
			{
				mediaFolder = "";
			}
			else if (selectedFileRealObject instanceof MediaNode)
			{
				IMediaProvider mp = ((MediaNode)selectedFileRealObject).getMediaProvider();
				if (mp instanceof Solution)
				{
					mediaFolder = ((MediaNode)selectedFileRealObject).getPath();
				}
			}
			final String pathString = solution.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + mediaFolder;
			String fileName = getFileName(pathString);
			if (fileName != null)
			{
				if (!fileName.contains(".")) fileName = fileName + ".txt";
				WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
				try
				{
					wsa.createFolder(solution.getName() + "/" + SolutionSerializer.MEDIAS_DIR);
					IPath path = new Path(pathString + fileName);
					IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
					if (file != null && !file.exists())
					{
						InputStream inputStream = getContentInputStream();
						file.create(inputStream, false, null);
						viewer.refreshTreeCompletely();
						inputStream.close();
						EditorUtil.openFileEditor(file);
					}
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
				catch (IOException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	protected Solution getSolution()
	{
		Solution thisSolution = null;
		if (selectedFile != null)
		{
			Object selectedFileRealObject = selectedFile.getRealObject();
			if (selectedFileRealObject instanceof Solution)
			{
				thisSolution = (Solution)selectedFileRealObject;
			}
			else if (selectedFileRealObject instanceof MediaNode)
			{
				IMediaProvider mp = ((MediaNode)selectedFileRealObject).getMediaProvider();
				if (mp instanceof Solution)
				{
					thisSolution = (Solution)mp;
				}
			}
		}
		return thisSolution;
	}

	protected String getFileName(final String pathString)
	{
		final String regex = "[_a-zA-Z0-9\\-\\.]+";
		InputDialog newFileNameDlg = new InputDialog(viewer.getSite().getShell(), "New media file", "Specify a file name", "", new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() < 1)
				{
					return "Name cannot be empty";
				}
				Pattern pattern = Pattern.compile(regex);
				if (!pattern.matcher(newText).matches())
				{
					return "Invalid new media name";
				}
				if (!newText.contains(".")) newText = newText + ".txt";
				IPath path = new Path(pathString + newText);
				if (ResourcesPlugin.getWorkspace().getRoot().getFile(path).exists())
				{
					return "A file with this name already exists. Please modify the file name.";
				}

				return null;
			}
		});

		newFileNameDlg.setBlockOnOpen(true);
		newFileNameDlg.open();
		if (newFileNameDlg.getReturnCode() == Window.OK)
		{
			return newFileNameDlg.getValue();
		}
		return null;
	}

	protected InputStream getContentInputStream()
	{
		return new java.io.StringBufferInputStream("");
	}
}
