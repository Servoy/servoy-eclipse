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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

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
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;


/**
 * Action to delete media files and folders
 * 
 * @author gboros
 */

public class DeleteMediaAction extends Action implements ISelectionChangedListener
{
	private List<MediaNode> selectedMediaFolders;
	private List<IPersist> selectedMedias;
	private final SolutionExplorerView viewer;

	public DeleteMediaAction(String text, SolutionExplorerView viewer)
	{
		this.viewer = viewer;
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));
		setText(text);
		setToolTipText(text);
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		selectedMediaFolders = null;
		selectedMedias = null;
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0);
		if (state)
		{
			Iterator<SimpleUserNode> selit = sel.iterator();
			List<MediaNode> selectedFolders = new ArrayList<MediaNode>();
			List<IPersist> selectedPersists = new ArrayList<IPersist>();
			while (state && selit.hasNext())
			{
				SimpleUserNode node = selit.next();
				UserNodeType nodeType = node.getType();
				state = (nodeType == UserNodeType.MEDIA_FOLDER) || (nodeType == UserNodeType.MEDIA_IMAGE);
				if (state)
				{
					if (nodeType == UserNodeType.MEDIA_FOLDER) selectedFolders.add((MediaNode)node.getRealObject());
					else selectedPersists.add((IPersist)node.getRealObject());
				}
			}
			if (state)
			{
				selectedMediaFolders = selectedFolders;
				selectedMedias = selectedPersists;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		if (((selectedMediaFolders != null && selectedMediaFolders.size() > 0) || (selectedMedias != null && selectedMedias.size() > 0)) &&
			MessageDialog.openConfirm(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), getText(), "Are you sure you want to delete?")) //$NON-NLS-1$
		{
			Job job = new WorkspaceJob("Delete Media") //$NON-NLS-1$
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Deleting Media", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					try
					{

						if (selectedMediaFolders != null && selectedMediaFolders.size() > 0)
						{
							WorkspaceFileAccess wsa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
							for (MediaNode mediaFolder : selectedMediaFolders)
							{
								Solution rootObject = null;
								IMediaProvider mediaProvider = mediaFolder.getMediaProvider();
								if (mediaProvider instanceof Solution)
								{
									rootObject = (Solution)mediaProvider;
								}
								else if (mediaProvider instanceof FlattenedSolution)
								{
									rootObject = ((FlattenedSolution)mediaProvider).getSolution();
								}

								if (rootObject != null)
								{
									ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
										rootObject.getName());
									EclipseRepository repository = (EclipseRepository)rootObject.getRepository();

									Iterator<Media> mediaIte = rootObject.getMedias(false);
									Media media;
									ArrayList<Media> deletedMedias = new ArrayList<Media>();
									while (mediaIte.hasNext())
									{
										media = mediaIte.next();
										if (media.getName().startsWith(mediaFolder.getPath()))
										{
											deletedMedias.add(media);
										}
									}

									for (final Media m : deletedMedias)
									{
										try
										{
											IPersist editingNode = servoyProject.getEditingPersist(m.getUUID());
											repository.deleteObject(editingNode);
											Display.getDefault().asyncExec(new Runnable()
											{
												public void run()
												{
													EditorUtil.closeEditor(m);
												}
											});
										}
										catch (RepositoryException e)
										{
											ServoyLog.logError("Could not delete media", e); //$NON-NLS-1$
										}
									}

									try
									{
										wsa.delete(rootObject.getName() + "/" + SolutionSerializer.MEDIAS_DIR + "/" + mediaFolder.getPath());
										viewer.refreshTreeCompletely();
									}
									catch (IOException ex)
									{
										ServoyLog.logError(ex);
									}

									try
									{
										servoyProject.saveEditingSolutionNodes(deletedMedias.toArray(new IPersist[0]), true);
									}
									catch (RepositoryException e)
									{
										ServoyLog.logError("Could not save editing solution when deleting media folder", e); //$NON-NLS-1$
									}
								}
							}
						}
						if (selectedMedias != null && selectedMedias.size() > 0)
						{
							for (final IPersist media : selectedMedias)
							{
								IRootObject rootObject = media.getRootObject();
								if (rootObject instanceof Solution)
								{
									ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
										rootObject.getName());
									EclipseRepository repository = (EclipseRepository)rootObject.getRepository();

									IPersist editingNode = servoyProject.getEditingPersist(media.getUUID());
									repository.deleteObject(editingNode);
									servoyProject.saveEditingSolutionNodes(new IPersist[] { editingNode }, true);
									Display.getDefault().asyncExec(new Runnable()
									{
										public void run()
										{
											EditorUtil.closeEditor(media);
										}
									});

								}
							}
						}
					}
					catch (RepositoryException e)
					{
						MessageDialog.openError(viewer.getSite().getShell(), "Error", "Could not delete media: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
						ServoyLog.logError("Could not delete media files", e); //$NON-NLS-1$
					}
					catch (Exception e)
					{
						ServoyLog.logError("Could not delete media files", e); //$NON-NLS-1$
					}
					finally
					{
						monitor.done();
					}
					return Status.OK_STATUS;
				}

			};
			job.setUser(true);
			// we must have the ws root rule, because EclipseRepository.updateNodesInWorkspace may run,
			// and we may modifying the medias of the solution
			job.setRule(ResourcesPlugin.getWorkspace().getRoot());
			job.schedule();
		}
	}
}