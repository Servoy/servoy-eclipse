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
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerTreeContentProvider;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.IPersist;
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
				solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(((Solution)solutionNode.getRealObject()).getName()).getEditingSolution();
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
					else if (newText.indexOf('\\') >= 0 || newText.indexOf('/') >= 0 || newText.indexOf(' ') >= 0)
					{
						return "Invalid new media name";
					}
					else if (newText.equalsIgnoreCase(selection.getName()))
					{
						return "Please enter a different name";
					}
					else
					{
						return checkForMediaFolderDuplicates(newText, selection, viewer.getTreeContentProvider());
					}
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

			String newName = (replaceStartIdx == -1) ? renameFolderNameDlg.getValue() + "/" : replaceName.substring(0, replaceStartIdx) + "/" +
				renameFolderNameDlg.getValue() + "/";

			ArrayList<Media> createdMedias = new ArrayList<Media>();
			ArrayList<IPersist> newMedias = new ArrayList<IPersist>();
			ArrayList<IPersist> removedMedias = new ArrayList<IPersist>();
			Iterator<Media> mediaIte = solution.getMedias(false);
			Media media, movedMedia;

			try
			{
				while (mediaIte.hasNext())
				{
					media = mediaIte.next();
					if (media.getName().startsWith(replaceName))
					{
						createdMedias.add(media);
						removedMedias.add(servoyProject.getEditingPersist(media.getUUID()));
					}
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
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	/**
	 * @param newText
	 * @param currentselection
	 * @param contentProvider TODO
	 * @return
	 */
	public static String checkForMediaFolderDuplicates(String newText, SimpleUserNode currentselection, SolutionExplorerTreeContentProvider treeContentProvider)
	{
		SimpleUserNode rootSolution = currentselection;
		while (rootSolution.parent != null && rootSolution.getType() != UserNodeType.SOLUTION)
		{
			rootSolution = rootSolution.parent;
		}
		//SimpleUserNode solutionNode = currentselection.getAncestorOfType(ServoyProject.class);
		String folder = getMediaFolderPath(currentselection);
		String folderPathWithoutSelectedFolder = folder.substring(0, folder.lastIndexOf('/') == -1 ? 0 : folder.lastIndexOf('/'));

		String newFolderPath = folderPathWithoutSelectedFolder.length() == 0 ? newText : folderPathWithoutSelectedFolder + '/' + newText;
		SimpleUserNode existingNode = checkForDuplicates(rootSolution, newFolderPath, treeContentProvider);
		if (existingNode != null)
		{
			return "Media folder " + newFolderPath + " already exists in " + existingNode.getAncestorOfType(ServoyProject.class).getName();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Does a breadth first search in the solex solution tree for media and media folders  (also goes through modules)
	 * @param root
	 * @param newFolderPath
	 * @param treeContentProvider TODO
	 * @return
	 */
	private static SimpleUserNode checkForDuplicates(SimpleUserNode root, String newFolderPath, SolutionExplorerTreeContentProvider treeContentProvider)
	{
		Queue<SimpleUserNode> queue = new LinkedList<SimpleUserNode>();
		queue.add(root);
		while (queue.size() > 0)
		{
			// Take the next node from the front of the queue
			SimpleUserNode node = queue.poll();
			// Process the node 'node'
			if (node.getType() == UserNodeType.MEDIA_FOLDER)
			{
				String currentSearchFolder = getMediaFolderPath(node);
				if (currentSearchFolder.equalsIgnoreCase(newFolderPath))
				{
					return node;
				}
			}
			if (node.children == null)
			{ //lazy load modules
				treeContentProvider.getChildren(node);
			}
			if (node.children != null)
			{
				// Add the node’s children to the back of the queue
				for (SimpleUserNode childNode : node.children)
				{
					if (childNode.getType() == UserNodeType.MODULES || childNode.getType() == UserNodeType.MEDIA ||
						childNode.getType() == UserNodeType.MEDIA_FOLDER || childNode.getType() == UserNodeType.SOLUTION_ITEM) queue.add(childNode);
				}
			}

		}
		// None of the nodes matched the specified predicate.
		return null;
	}

	private static String getMediaFolderPath(SimpleUserNode node)
	{
		StringBuilder _folderPath = new StringBuilder(node.getName());
		SimpleUserNode parent = node.parent;
		while (parent != null && parent.getType() != UserNodeType.MEDIA && parent.getType() != UserNodeType.SOLUTION_ITEM &&
			parent.getType() != UserNodeType.SOLUTION)
		{
			//add the folder path at the beginning
			_folderPath.insert(0, '/').insert(0, parent.getName());
			parent = parent.parent;
		}
		return _folderPath.toString();
	}
}