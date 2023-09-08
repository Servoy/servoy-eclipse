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
package com.servoy.eclipse.ui.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.core.util.UIUtils.YesYesToAllNoNoToAllAsker;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.IDragData;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportMediaAction;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

/**
 * Listener for dropping files and nodes on a viewer with SimpleUserNode nodes.
 * Files will be added as media.
 *
 * @author rgansevles
 *
 */
public class UserNodeDropTargetListener extends ViewerDropAdapter
{
	private ServoyProject project;
	private final IWorkbenchPart workbenchPart;

	public UserNodeDropTargetListener(Viewer viewer, IWorkbenchPart workbenchPart)
	{
		super(viewer);
		this.workbenchPart = workbenchPart;
	}


	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType)
	{
		Object input = (target == null && getViewer() instanceof ContentViewer) ? ((ContentViewer)getViewer()).getInput() : target;
		if (input instanceof SimpleUserNode &&
			(((SimpleUserNode)input).getRealType() == UserNodeType.WORKING_SET || ((SimpleUserNode)input).getRealType() == UserNodeType.FORMS))
		{
			if (UserNodeListDragSourceListener.dragObjects != null && UserNodeListDragSourceListener.dragObjects.length > 0 &&
				UserNodeListDragSourceListener.dragObjects[0] instanceof PersistDragData)
			{
				boolean onlyForms = false;
				for (IDragData dragData : UserNodeListDragSourceListener.dragObjects)
				{
					onlyForms = false;
					if (dragData instanceof PersistDragData)
					{
						if (((PersistDragData)dragData).type == IRepository.FORMS)
						{
							onlyForms = true;
						}
					}
					if (!onlyForms)
					{
						break;
					}
				}
				if (onlyForms) return true;
			}
		}

		SimpleUserNode targetNode = null;
		if (input instanceof SimpleUserNode &&
			(((SimpleUserNode)input).getRealType() == UserNodeType.MEDIA || ((SimpleUserNode)input).getRealType() == UserNodeType.MEDIA_FOLDER))
		{
			targetNode = (SimpleUserNode)input;
		}
		project = null;
		if (targetNode != null && (FileTransfer.getInstance().isSupportedType(transferType) || UserNodeListDragSourceListener.dragObjects != null))
		{
			SimpleUserNode projectNode = targetNode.getAncestorOfType(ServoyProject.class);
			if (projectNode != null)
			{
				project = (ServoyProject)projectNode.getRealObject();
			}
		}
		return project != null;
	}

	@Override
	public boolean performDrop(final Object data)
	{
		if (project != null && data instanceof String[])
		{
			Object currentTarget = getCurrentTarget();
			MediaNode targetMediaNode = null;
			if (currentTarget == null)
			{
				// active the part that was dropped on
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().activate(workbenchPart);
				if (workbenchPart instanceof SolutionExplorerView && ((SolutionExplorerView)workbenchPart).getCurrentMediaFolder() != null)
				{
					targetMediaNode = ((SolutionExplorerView)workbenchPart).getCurrentMediaFolder();
				}
			}
			else
			{
				if (currentTarget instanceof PlatformSimpleUserNode)
				{
					Object nodeObject = ((PlatformSimpleUserNode)currentTarget).getRealObject();
					if (nodeObject instanceof MediaNode && ((MediaNode)nodeObject).getType() == MediaNode.TYPE.FOLDER)
					{
						targetMediaNode = (MediaNode)nodeObject;
					}
				}
			}

			final String targetParentPath = targetMediaNode != null ? targetMediaNode.getPath() : null;
			final Solution editingSolution = project.getEditingSolution();
			Job job = new WorkspaceJob("Import Media")
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Importing Media", IProgressMonitor.UNKNOWN);
					try
					{
						ImportMediaAction.addMediaFiles(editingSolution, null, (String[])data, targetParentPath);
					}
					catch (final RepositoryException e)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Could not import media files: " + e.getMessage());
								ServoyLog.logError("Could not import media files", e);
							}
						});
					}
					catch (Exception e)
					{
						ServoyLog.logError("Could not import media files", e);
					}
					finally
					{
						monitor.done();
					}
					return Status.OK_STATUS;
				}

			};
			job.setUser(true);
			job.schedule();
			project = null;
			return true;
		}
		if (data instanceof Object[])
		{
			final ArrayList<Media> draggedMedias = new ArrayList<Media>();
			List<IAdaptable> addToWorkingSet = new ArrayList<IAdaptable>();
			String[] solutionName = new String[1];
			Map<String, List<Form>> toRemove = new HashMap<>();
			for (Object o : (Object[])data)
			{
				if (o instanceof PersistDragData)
				{
					PersistDragData persistDragData = (PersistDragData)o;
					solutionName[0] = persistDragData.solutionName;
					ServoyProject prj = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName[0]);
					final IPersist persist = prj.getSolution().getChild(persistDragData.uuid);

					if (persist instanceof Form)
					{
						Form form = (Form)persist;
						if (getCurrentTarget() instanceof SimpleUserNode && (((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.WORKING_SET ||
							((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.FORMS))
						{
							ServoyResourcesProject resourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
							if (resourcesProject != null)
							{
								String workingSetName = resourcesProject.getContainingWorkingSet(form.getName(),
									ServoyModelFinder.getServoyModel().getFlattenedSolution().getSolutionNames());
								if (workingSetName != null)
								{
									IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
									if (ws != null)
									{
										List<Form> list = toRemove.get(workingSetName);
										if (list == null)
										{
											list = new ArrayList<Form>();
											toRemove.put(workingSetName, list);
										}
										list.add(form);
									}
								}
							}

							if (((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.WORKING_SET)
							{
								String workingSetName = ((SimpleUserNode)getCurrentTarget()).getName();
								IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
								if (ws != null)
								{
									Pair<String, String> formFilePath = SolutionSerializer.getFilePath(form, false);
									IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()));
									addToWorkingSet.add(ws.adaptElements(new IAdaptable[] { file })[0]);
								}
							}
						}
					}
					else if (persist instanceof Media)
					{
						draggedMedias.add((Media)persist);
					}
				}
			}
			if (toRemove.size() > 0)
			{
				toRemove.entrySet().forEach(entry -> {
					IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(entry.getKey());
					entry.getValue().forEach(form -> {
						Pair<String, String> formFilePath = SolutionSerializer.getFilePath(form, false);
						IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()));
						IFile scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getScriptPath(form, false)));

						List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
						boolean modified = files.remove(scriptFile);
						if (files.remove(file) || modified)
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName[0]).getResourcesProject()
								.saveWorkingSet(files, solutionName[0], ws.getName());
						}
					});

				});
			}
			if (addToWorkingSet.size() > 0)
			{
				String workingSetName = ((SimpleUserNode)getCurrentTarget()).getName();
				IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);

				List<IAdaptable> filesAlreadyInWS = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
				if (filesAlreadyInWS.size() > 0)
				{
					addToWorkingSet.addAll(filesAlreadyInWS);
				}
				ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName[0]).getResourcesProject()
					.saveWorkingSet(addToWorkingSet, solutionName[0], ws.getName());
			}

			if (draggedMedias.size() > 0)
			{
				Object currentTarget = getCurrentTarget();
				if (currentTarget instanceof PlatformSimpleUserNode)
				{
					Object nodeObject = ((PlatformSimpleUserNode)currentTarget).getRealObject();
					String targetSolutionName = null;
					String targetFolder = null;
					if (nodeObject instanceof MediaNode && ((MediaNode)nodeObject).getType() == MediaNode.TYPE.FOLDER)
					{
						targetFolder = ((MediaNode)nodeObject).getPath();
						targetSolutionName = ((MediaNode)nodeObject).getMediaProvider().getName();
					}
					else if (nodeObject instanceof Solution)
					{
						targetFolder = "";
						targetSolutionName = ((Solution)nodeObject).getName();
					}
					if (targetFolder != null && targetSolutionName != null)
					{
						final YesYesToAllNoNoToAllAsker overwriteDlg = new YesYesToAllNoNoToAllAsker(UIUtils.getActiveShell(), "Warning");
						final int currentOp = getCurrentOperation();
						final String fTargetFolder = targetFolder;
						final String fTargetSolutionName = targetSolutionName;
						Job job = new WorkspaceJob("Copy/Move Media")
						{
							@Override
							public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
							{
								for (Media m : draggedMedias)
								{
									String mediaName = m.getName();
									String mediaParentName;
									int idxPathSeparator = mediaName.lastIndexOf('/');
									if (idxPathSeparator > 0) mediaParentName = mediaName.substring(0, idxPathSeparator);
									else mediaParentName = "";

									Solution persistSolution = (Solution)m.getAncestor(IRepository.SOLUTIONS);
									if (!fTargetFolder.equals(mediaParentName) ||
										(currentOp != DND.DROP_COPY && !fTargetSolutionName.equals(persistSolution.getName())))
									{
										ServoyProject targetProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
											fTargetSolutionName);
										IFolder dest = targetProject.getProject().getFolder(new Path(SolutionSerializer.MEDIAS_DIR + '/' + fTargetFolder));
										Pair<String, String> mediaFilePath = SolutionSerializer.getFilePath(m, false);
										IFile source = ServoyModel.getWorkspace().getRoot().getFile(
											new Path(mediaFilePath.getLeft() + mediaFilePath.getRight()));

										try
										{
											if (currentOp == DND.DROP_COPY || currentOp == DND.DROP_MOVE || currentOp == DND.DROP_DEFAULT)
											{
												WorkspaceFileAccess.mkdirs(dest);
												IPath destFile = dest.getFullPath().append('/' + source.getName());
												boolean doCopyMove = true;
												if (targetProject.getProject().getWorkspace().getRoot().exists(destFile))
												{
													doCopyMove = false;
													overwriteDlg.setMessage("Media '" + mediaName + "' already exist, overwrite ?");
													if (overwriteDlg.userSaidYes())
													{
														IFile f = targetProject.getProject().getWorkspace().getRoot().getFile(destFile);
														f.delete(true, null);
														doCopyMove = true;
													}
												}
												if (doCopyMove)
												{
													if (currentOp == DND.DROP_COPY) source.copy(destFile, true, null);
													else source.move(destFile, true, null);
												}
											}
										}
										catch (final CoreException ex)
										{
											Display.getDefault().asyncExec(new Runnable()
											{
												public void run()
												{
													MessageDialog.openError(UIUtils.getActiveShell(), "Error",
														"Could not copy/move media : " + ex.getMessage());
													ServoyLog.logError("Could not copy/move media", ex);
												}
											});
										}
									}
								}
								return Status.OK_STATUS;
							}
						};
						job.setUser(true);
						job.setRule(ResourcesPlugin.getWorkspace().getRoot());
						job.schedule();
					}
				}
			}
		}
		return false;
	}

	@Override
	protected int determineLocation(DropTargetEvent event)
	{
		// we don't do any reorderings in the tree
		return LOCATION_ON;
	}
}
