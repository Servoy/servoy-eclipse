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

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Action to rename media folder.
 *
 * @author gboros
 */
public class RenameMediaFolderAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private Solution solution;
	private SimpleUserNode selection;

	public RenameMediaFolderAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setText("Rename");
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
		solution = null;
		selection = null;
		if (sel.size() == 1 && (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA_FOLDER))
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			SimpleUserNode solutionNode = node.getAncestorOfType(Solution.class);
			if (solutionNode != null)
			{
				// make sure you have the in-memory version of the solution
				solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
					((Solution)solutionNode.getRealObject()).getName()).getEditingSolution();
				selection = node;
			}

		}
		setEnabled(solution != null);
	}

	@Override
	public void run()
	{
		if (solution == null || selection == null) return;

		InputDialog renameFolderNameDlg = new InputDialog(viewer.getSite().getShell(), "Rename media folder", "Specify a new folder name", selection.getName(),
			new IInputValidator()
			{
				public String isValid(String newText)
				{
					if (newText.length() < 1)
					{
						return "Name cannot be empty";
					}
					else if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0 || newText.endsWith(".") || newText.startsWith("."))
					{
						return "Invalid new media name";
					}
					else if (newText.equalsIgnoreCase(selection.getName()))
					{
						return "Please enter a different name";
					}
					return null;
				}
			});

		renameFolderNameDlg.setBlockOnOpen(true);
		renameFolderNameDlg.open();
		if (renameFolderNameDlg.getReturnCode() == Window.OK)
		{
			WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName());
			EclipseRepository repository = (EclipseRepository)solution.getRepository();
			String replaceName = ((MediaNode)selection.getRealObject()).getPath();
			int replaceStartIdx = replaceName.substring(0, replaceName.length() - 1).lastIndexOf('/');

			String newName = (replaceStartIdx == -1) ? renameFolderNameDlg.getValue() + "/"
				: replaceName.substring(0, replaceStartIdx) + "/" + renameFolderNameDlg.getValue() + "/";

			ArrayList<Media> createdMedias = new ArrayList<Media>();
			ArrayList<IPersist> newMedias = new ArrayList<IPersist>();
			ArrayList<IPersist> removedMedias = new ArrayList<IPersist>();
			ArrayList<IPersist> conflictingMedias = new ArrayList<IPersist>();
			Iterator<Media> mediaIte = solution.getMedias(false);
			Media media, movedMedia;

			try
			{
				while (mediaIte.hasNext())
				{
					media = mediaIte.next();
					String newMediaName = media.getName().substring(media.getName().lastIndexOf('/') + 1, media.getName().length());
					if (media.getName().equals(newName + newMediaName))
					{
						conflictingMedias.add(media);
					}
					if (media.getName().startsWith(replaceName))
					{
						createdMedias.add(media);
						removedMedias.add(servoyProject.getEditingPersist(media.getUUID()));
					}
				}

				//abort operation if duplicate medias are found
				if (conflictingMedias.size() > 0)
				{
					StringBuilder sb = new StringBuilder();
					for (IPersist conflictedMedia : conflictingMedias)
					{
						sb.append(conflictedMedia.getAncestor(IRepository.SOLUTIONS)).append("  ->  ").append(
							((Media)conflictedMedia).getName()).append("\n");
					}
					UIUtils.showScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.ERROR, "Error",
						"Cannot rename folder becasue the folowing media items already present in the solution", sb.toString());
					return;
				}
				for (Media m : createdMedias)
				{
					movedMedia = solution.createNewMedia(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(),
						m.getName().replaceFirst(replaceName, newName));
					movedMedia.setMimeType(m.getMimeType());
					movedMedia.setPermMediaData(m.getMediaData());
					movedMedia.flagChanged();
					newMedias.add(movedMedia);
				}

				for (IPersist m : newMedias)
				{
					repository.copyPersistIntoSolution(m, solution, true);
				}

				for (IPersist m : removedMedias)
				{
					repository.deleteObject(m);
					EditorUtil.closeEditor(m);
				}

				ArrayList<IPersist> changedMedias = new ArrayList<IPersist>(newMedias);
				changedMedias.addAll(removedMedias);
				ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solution.getName()).saveEditingSolutionNodes(
					changedMedias.toArray(new IPersist[changedMedias.size()]), true, false);
				wsa.delete(solution.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + replaceName);
				if (createdMedias.isEmpty())
				{
					// rename empty folder
					wsa.createFolder(solution.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + newName);
					viewer.refreshTreeCompletely();
				}
			}
			catch (RepositoryException ex)
			{
				UIUtils.reportError("Error", ex.getMessage());
				ServoyLog.logError(ex);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}
}