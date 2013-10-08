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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportMediaAction;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
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
		SimpleUserNode targetNode = null;

		Object input = (target == null && getViewer() instanceof ContentViewer) ? ((ContentViewer)getViewer()).getInput() : target;
		if (input instanceof SimpleUserNode &&
			(((SimpleUserNode)input).getRealType() == UserNodeType.MEDIA || ((SimpleUserNode)input).getRealType() == UserNodeType.MEDIA_FOLDER))
		{
			targetNode = (SimpleUserNode)input;
		}
		if (input instanceof SimpleUserNode &&
			(((SimpleUserNode)input).getRealType() == UserNodeType.WORKING_SET || ((SimpleUserNode)input).getRealType() == UserNodeType.FORMS))
		{
			if (UserNodeListDragSourceListener.dragObjects != null && UserNodeListDragSourceListener.dragObjects.length == 1 &&
				UserNodeListDragSourceListener.dragObjects[0] instanceof PersistDragData)
			{
				PersistDragData dragData = (PersistDragData)UserNodeListDragSourceListener.dragObjects[0];
				if (dragData.type == IRepository.FORMS)
				{
					// only possible to drag a form in a working set within the tree
					return true;
				}
			}
		}
		project = null;
		if (targetNode != null && FileTransfer.getInstance().isSupportedType(transferType))
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
			Job job = new WorkspaceJob("Import Media") //$NON-NLS-1$
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Importing Media", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
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
								MessageDialog.openError(Display.getDefault().getActiveShell(), "Error", "Could not import media files: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
								ServoyLog.logError("Could not import media files", e); //$NON-NLS-1$
							}
						});
					}
					catch (Exception e)
					{
						ServoyLog.logError("Could not import media files", e); //$NON-NLS-1$
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
		if (data instanceof Object[] && ((Object[])data).length == 1 && ((Object[])data)[0] instanceof PersistDragData)
		{
			PersistDragData formDragData = (PersistDragData)((Object[])data)[0];
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(formDragData.solutionName);
			IPersist persist = project.getSolution().getChild(formDragData.uuid);
			if (persist instanceof Form)
			{
				Form form = (Form)persist;
				if (getCurrentTarget() instanceof SimpleUserNode &&
					(((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.WORKING_SET || ((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.FORMS))
				{
					Pair<String, String> formFilePath = SolutionSerializer.getFilePath(form, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(formFilePath.getLeft() + formFilePath.getRight()));

					IFile scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getScriptPath(form, false)));
					ServoyResourcesProject resourcesProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject();
					if (resourcesProject != null)
					{
						String workingSetName = resourcesProject.getContainingWorkingSet(form.getName(),
							ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getFlattenedSolution().getSolutionNames());
						if (workingSetName != null)
						{
							IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
							if (ws != null)
							{
								List<IAdaptable> files = new ArrayList<IAdaptable>(Arrays.asList(ws.getElements()));
								boolean modified = files.remove(scriptFile);
								if (files.remove(file) || modified)
								{
									ws.setElements(files.toArray(new IAdaptable[0]));
								}
							}
						}
					}

					if (((SimpleUserNode)getCurrentTarget()).getRealType() == UserNodeType.WORKING_SET)
					{
						String workingSetName = ((SimpleUserNode)getCurrentTarget()).getName();
						IWorkingSet ws = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSet(workingSetName);
						if (ws != null)
						{
							PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(file, new IWorkingSet[] { ws });
						}
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
