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

import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.team.ServoyTeamProvider;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;

public class UpdateOperation extends SolutionOperation
{
	private boolean overwriteOutgoing;

	public UpdateOperation(IWorkbenchPart part, ISynchronizationScopeManager manager)
	{
		super(part, manager);
	}

	@Override
	protected void execute(final ServoyTeamProvider provider, ResourceTraversal[] traversals, IProgressMonitor monitor) throws CoreException
	{
		try
		{
			provider.getOperations().checkRemoteRepository();
			provider.getProjectToTempDir(provider.getProject().hasNature(ServoyResourcesProject.NATURE_ID));
			provider.getOperations().update(traversals, isOverwriteOutgoing(), monitor);
			if (!isOverwriteOutgoing() && hasIncomingChanges(traversals))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openWarning(getShell(), "Warning", "Could not get all changes for '" + provider.getProject().getName() +
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
		return "Update";
	}

	/**
	 * Indicate whether the operation should overwrite outgoing changes. By default, the get operation does not override local modifications.
	 * 
	 * @return whether the operation should overwrite outgoing changes.
	 */
	protected boolean isOverwriteOutgoing()
	{
		return overwriteOutgoing;
	}

	/**
	 * Set whether the operation should overwrite outgoing changes.
	 * 
	 * @param overwriteOutgoing whether the operation should overwrite outgoing changes
	 */
	public void setOverwriteOutgoing(boolean overwriteOutgoing)
	{
		this.overwriteOutgoing = overwriteOutgoing;
	}

	private boolean hasIncomingChanges(ResourceTraversal[] traversals) throws CoreException
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
						if (twd.getDirection() == IThreeWayDiff.INCOMING || twd.getDirection() == IThreeWayDiff.CONFLICTING)
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
