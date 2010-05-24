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
package com.servoy.eclipse.team.ui.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffVisitor;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyResourcesProject;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.eclipse.team.SynchronizeUpdater;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;

/**
 * Operation for copying the selected resources to the servoy repository
 */
public class CommitOperation extends SolutionOperation
{
	private boolean overwriteIncoming;

	public CommitOperation(IWorkbenchPart part, ISynchronizationScopeManager manager)
	{
		super(part, manager);
	}

	@Override
	protected void execute(final ServoyTeamProvider provider, final ResourceTraversal[] traversals, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			provider.getOperations().checkRemoteRepository();
			provider.getProjectToTempDir(provider.getProject().hasNature(ServoyResourcesProject.NATURE_ID));

			SynchronizeUpdater synchronizeUpdater = new SynchronizeUpdater(SolutionSubscriber.getInstance().getSynchronizer());
			List<IResource> changedResources = new ArrayList<IResource>();
			provider.getOperations().commit(traversals, isOverwriteIncoming(), monitor, changedResources, synchronizeUpdater);
			provider.getOperations().writeProjectToRepository(changedResources);
			synchronizeUpdater.update();

			if (!isOverwriteIncoming() && hasOutgoingChanges(traversals))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openWarning(getShell(), "Warning", "Could not put all changes for '" + provider.getProject().getName() +
							"' project due to conflicts, resolve conflicts first.");
					}
				});
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
			if (!monitor.isCanceled()) throw new TeamException(ex.getMessage());
		}
	}

	@Override
	protected String getTaskName()
	{
		return "commit";
	}

	/**
	 * Return whether incoming changes should be overwritten.
	 * 
	 * @return whether incoming changes should be overwritten
	 */
	public boolean isOverwriteIncoming()
	{
		return overwriteIncoming;
	}

	/**
	 * Set whether incoming changes should be overwritten.
	 * 
	 * @param overwriteIncoming whether incoming changes should be overwritten
	 */
	public void setOverwriteIncoming(boolean overwriteIncoming)
	{
		this.overwriteIncoming = overwriteIncoming;
	}

	private boolean hasOutgoingChanges(ResourceTraversal[] traversals) throws CoreException
	{
		final RuntimeException found = new RuntimeException();
		try
		{
			SolutionSubscriber.getInstance().accept(traversals, new IDiffVisitor()
			{
				public boolean visit(IDiff diff)
				{
					if (diff instanceof IThreeWayDiff)
					{
						IThreeWayDiff twd = (IThreeWayDiff)diff;
						if (twd.getDirection() == IThreeWayDiff.OUTGOING || twd.getDirection() == IThreeWayDiff.CONFLICTING)
						{
							throw found;
						}
					}
					return false;
				}
			});
		}
		catch (RuntimeException e)
		{
			if (e == found) return true;
			throw e;
		}

		return false;
	}
}
