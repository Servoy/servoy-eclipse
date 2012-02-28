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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ContentViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.ImportMediaAction;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

/**
 * Listener for dropping files on a viewer with SimpleUserNode nodes.
 * Files will be added as media.
 * 
 * @author rgansevles
 * 
 */
public class UserNodeFileDropTargetListener extends ViewerDropAdapter
{
	private ServoyProject project;
	private final IWorkbenchPart workbenchPart;

	public UserNodeFileDropTargetListener(Viewer viewer, IWorkbenchPart workbenchPart)
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
		return false;
	}
}
