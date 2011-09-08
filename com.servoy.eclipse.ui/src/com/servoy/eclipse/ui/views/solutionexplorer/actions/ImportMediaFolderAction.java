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

import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;

public class ImportMediaFolderAction extends ImportMediaAction implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private Solution solution;

	/**
	 * Creates a new "create new method" action for the given solution view.
	 * 
	 * @param viewer the solution view to use.
	 */
	public ImportMediaFolderAction(SolutionExplorerView viewer)
	{
		super(viewer);
		this.viewer = viewer;

		setImageDescriptor(Activator.loadImageDescriptorFromBundle("import_folder.gif"));
		setText("Import media folder");
		setToolTipText(getText());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		solution = null;
		if (sel.size() == 1 && (((SimpleUserNode)sel.getFirstElement()).getType() == UserNodeType.MEDIA))
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

		DirectoryDialog dd = new DirectoryDialog(viewer.getSite().getShell(), SWT.OPEN | SWT.MULTI);
		final String folderName = dd.open();

		if (folderName != null && folderName.equals("") == false)
		{
			Job job = new WorkspaceJob("Import Media Folder") //$NON-NLS-1$
			{

				@Override
				public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException
				{
					monitor.beginTask("Importing Media Folder", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
					try
					{
						addMediaFiles(solution, null, new String[] { folderName }, viewer.getCurrentMediaFolder());
					}
					catch (RepositoryException e)
					{
						MessageDialog.openError(viewer.getSite().getShell(), "Error", "Could not import media files: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
						ServoyLog.logError("Could not import media files", e); //$NON-NLS-1$
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
			job.setRule(ServoyModel.getWorkspace().getRoot());
			job.setUser(true);
			job.schedule();

		}
	}
}
