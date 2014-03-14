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


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.ScrollableDialog;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.MimeTypes;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

/**
 * Action to import media from file.
 * 
 * @author rgansevles
 */
public class ImportMediaAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private Solution solution;
	private static volatile int overwriteReturnCode = IDialogConstants.YES_TO_ALL_ID;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 * 
	 * @param viewer the solution view to use.
	 */
	public ImportMediaAction(SolutionExplorerView viewer)
	{
		this.viewer = viewer;

		setImageDescriptor(Activator.loadImageDescriptorFromOldLocations("import.gif"));
		setText("Import media");
		setToolTipText(getText());
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		solution = null;
		if (sel.size() == 1 &&
			((((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA) || (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA_FOLDER)))
		{
			SimpleUserNode node = ((SimpleUserNode)sel.getFirstElement());
			SimpleUserNode solutionNode = node.getAncestorOfType(Solution.class);
			if (solutionNode != null)
			{
				// make sure you have the in-memory version of the solution
				solution = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(((Solution)solutionNode.getRealObject()).getName()).getEditingSolution();
			}
		}
		setEnabled(solution != null);
	}

	@Override
	public void run()
	{
		if (solution == null) return;

		FileDialog fd = new FileDialog(viewer.getSite().getShell(), SWT.OPEN | SWT.MULTI);
		fd.open();
		String[] fileNames = fd.getFileNames();
		String filterPath = fd.getFilterPath();
		if (fileNames == null || fileNames.length == 0)
		{
			return;
		}
		try
		{
			addMediaFiles(solution, filterPath, fileNames, viewer.getCurrentMediaFolder() != null ? viewer.getCurrentMediaFolder().getPath() : null);
		}
		catch (RepositoryException e)
		{
			MessageDialog.openError(viewer.getSite().getShell(), "Error", "Could not import media files: " + e.getMessage());
			ServoyLog.logError("Could not import media files", e);
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not import media files", e);
		}
	}

	/**
	 * Add media files to an editing solution
	 * 
	 * @param editingSolution
	 * @param directory null when filenames are absolute
	 * @param fileNames
	 * @throws IOException
	 * @throws RepositoryException
	 */
	public static void addMediaFiles(Solution editingSolution, String directory, String[] fileNames, String targetParentPath) throws IOException,
		RepositoryException
	{
		List<Pair<File, String>> filesToSave = new ArrayList<Pair<File, String>>(fileNames.length + 1);
		List<Media> existingMediasInCurrentSolution = new ArrayList<Media>();
		EclipseRepository repository = (EclipseRepository)editingSolution.getRepository();
		for (String fileName : fileNames)
		{
			File file = directory == null ? new File(fileName) : new File(directory, fileName);
			getFiles(filesToSave, existingMediasInCurrentSolution, repository, editingSolution, file, (targetParentPath == null ? "" : targetParentPath));
		}

		//check if conflicted media
		//if no conflicted media the import proceeds as in the case with Overwrite all
		// if conflicting media from another module , throws RepositoryException
		//IMPORTANT!!: an example use case for this "Override All , Cancel" Dialog is when a user works with an external directory ,
		//imports the directory , content gets added/images modified in the external directory , the user reimports the directory to get the latest version of the directory
		overwriteReturnCode = IDialogConstants.YES_TO_ALL_ID;
		if (existingMediasInCurrentSolution.size() > 0)
		{
			StringBuilder sb = new StringBuilder();
			for (IPersist conflictedMedia : existingMediasInCurrentSolution)
			{
				sb.append(((Media)conflictedMedia).getName()).append("\n");
			}
			final String text = sb.toString();
			final String editingSolutionName = editingSolution.getName();
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					ScrollableDialog dialog = new ScrollableDialog(UIUtils.getActiveShell(), IMessageProvider.ERROR, "Error",
						"The folowing media files already exist in the current solution: " + editingSolutionName, text);
					List<Pair<Integer, String>> buttonsAndLabels = new ArrayList<Pair<Integer, String>>();
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.YES_TO_ALL_ID, "Overwrite all"));
					buttonsAndLabels.add(new Pair<Integer, String>(IDialogConstants.CANCEL_ID, "Cancel"));
					dialog.setCustomBottomBarButtons(buttonsAndLabels);
					//shell.setSize(400, 500);
					overwriteReturnCode = dialog.open();
				}
			}, true);
		}
		if (overwriteReturnCode == IDialogConstants.YES_TO_ALL_ID)
		{
			List<IPersist> nodesToSave = saveFiles(repository, editingSolution, filesToSave);
			nodesToSave.add(editingSolution);
			ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(editingSolution.getName()).saveEditingSolutionNodes(
				nodesToSave.toArray(new IPersist[nodesToSave.size()]), false);
		}
	}

	/**
	 * fails fast on first duplicate in another module (throw RepositoryException)
	 */
	private static void getFiles(List<Pair<File, String>> filesToSave, List<Media> conflictingMedia, EclipseRepository repository, Solution editingSolution,
		File file, String targetParentPath) throws IOException, RepositoryException
	{
		if (file == null || !file.exists())
		{
			return;
		}

		if (file.isDirectory())
		{
			final String[] fileNames = file.list();
			if (fileNames != null)
			{
				String newParentPath = targetParentPath == null ? file.getName() + '/' : targetParentPath + file.getName() + '/';
				for (String fileName : fileNames)
				{
					getFiles(filesToSave, conflictingMedia, repository, editingSolution, new File(file, fileName), newParentPath);
				}
			}
			return;
		}

		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		Media media = flattenedSolution.getMedia(targetParentPath + Utils.stringReplace(file.getName(), " ", "_"));
		if (media != null)
		{
			//if duplicate media is not from current editing solution fail fast
			if (!((Solution)media.getRootObject()).getName().equals(editingSolution.getName()))
			{
				throw new RepositoryException("The name '" + media.getName() + "' already exists as media in " + media.getRootObject().getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else
			{
				conflictingMedia.add(media);
			}
		}
		filesToSave.add(new Pair<File, String>(file, targetParentPath));

	}

	/**
	 * @param repository
	 * @param editingSolution
	 * @param file
	 * @param targetParentPath
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws RepositoryException
	 */
	private static List<IPersist> saveFiles(EclipseRepository repository, Solution editingSolution, List<Pair<File, String>> filesToSave)
		throws FileNotFoundException, IOException, RepositoryException
	{
		List<IPersist> nodesToSave = new ArrayList<IPersist>();
		for (Pair<File, String> pair : filesToSave)
		{
			File file = pair.getLeft();
			String targetParentPath = pair.getRight();
			// a plain file
			ByteArrayOutputStream baos = new ByteArrayOutputStream((int)file.length());
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			Utils.streamCopy(bis, baos);
			byte[] media_data = baos.toByteArray();

			Utils.closeInputStream(bis);
			Utils.closeInputStream(fis);
			Utils.closeOutputStream(baos);

			String mime = MimeTypes.getContentType(media_data, file.getName());
			if (mime == null)
			{
				mime = repository.getContentType(file.getName());
			}
			String name = Utils.stringReplace(targetParentPath != null ? targetParentPath + file.getName() : file.getName(), " ", "_");
			Media media = editingSolution.getMedia(name);
			if (media == null)
			{
				media = editingSolution.createNewMedia(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), name);
			}
			// Save the media in the repository.
			media.setMimeType(mime);
			media.setPermMediaData(media_data);
			media.flagChanged();
			repository.copyPersistIntoSolution(media, editingSolution, true);
			nodesToSave.add(media);
		}
		return nodesToSave;
	}
}
