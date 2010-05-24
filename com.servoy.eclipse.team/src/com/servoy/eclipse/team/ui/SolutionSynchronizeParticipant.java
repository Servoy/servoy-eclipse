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
package com.servoy.eclipse.team.ui;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipantActionGroup;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.team.subscriber.SolutionSubscriber;
import com.servoy.eclipse.team.ui.actions.SynchronizeViewCommit;
import com.servoy.eclipse.team.ui.actions.SynchronizeViewRevert;
import com.servoy.eclipse.team.ui.actions.SynchronizeViewUpdate;
import com.servoy.j2db.util.Debug;

public class SolutionSynchronizeParticipant extends ModelSynchronizeParticipant
{
	/**
	 * The participant id.
	 */
	public static final String ID = "com.servoy.eclipse.team.ui.SolutionSynchronizeParticipant"; //$NON-NLS-1$	

	/**
	 * Create a solution participant. This method is invoked by the Synchronize view when a persisted participant is being restored. Participants that are
	 * persisted must override the {@link #restoreContext(ISynchronizationScopeManager)} method to recreate the context and may also need to override the
	 * {@link #createScopeManager(ResourceMapping[])} method if they require a custom scope manager.
	 */
	public SolutionSynchronizeParticipant()
	{
		super();
	}

	/**
	 * Create the participant for the given context. This method is used by the plugin to create a participant and then add it to the sync view (or show it is
	 * some other container).
	 * 
	 * @param context the synchronization context
	 */
	public SolutionSynchronizeParticipant(SynchronizationContext context)
	{
		super(context);
		try
		{
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor(ID));
		}
		catch (CoreException ex)
		{
			Debug.error("Error on SolutionSynchronizeParticipant", ex);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration)
	{
		super.initializeConfiguration(configuration);
		configuration.addActionContribution(new SynchronizeAdditionsActionGroup());
	}

	@Override
	protected ModelSynchronizeParticipantActionGroup createMergeActionGroup()
	{
		return new SynchronizeMergeActionGroup();
	}


	/**
	 * Action group that contributes the get an put menus to the context menu in the synchronize view
	 */
	private class SynchronizeAdditionsActionGroup extends SynchronizePageActionGroup
	{
		private SynchronizeViewCommit commit;
		private SynchronizeViewUpdate update;
		private SynchronizeViewRevert revert;

		@Override
		public void initialize(ISynchronizePageConfiguration configuration)
		{
			super.initialize(configuration);
			commit = new SynchronizeViewCommit(configuration);
			update = new SynchronizeViewUpdate(configuration);
			revert = new SynchronizeViewRevert(configuration);
			appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, "additions", commit);
			appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, "additions", update);
			appendToGroup(ISynchronizePageConfiguration.P_CONTEXT_MENU, "additions", revert);
		}

		@Override
		public void fillContextMenu(IMenuManager menu)
		{
			TreeSelection treeSelection = (TreeSelection)getContext().getSelection();
			TreePath[] selectedPaths = treeSelection.getPaths();

			commit.setEnabled(false);
			update.setEnabled(false);
			revert.setEnabled(false);
			for (TreePath selectedPath : selectedPaths)
			{
				try
				{
					SyncInfo syncInfo = SolutionSubscriber.getInstance().getSyncInfo((IResource)selectedPath.getLastSegment());
					if (SyncInfo.getDirection(syncInfo.getKind()) == SyncInfo.OUTGOING)
					{
						commit.setEnabled(true);
						revert.setEnabled(true);
//						if (SyncInfo.getChange(syncInfo.getKind()) == SyncInfo.DELETION)
//						{
//							revert.setEnabled(true);
//						}
					}
					else if (SyncInfo.getDirection(syncInfo.getKind()) == SyncInfo.INCOMING)
					{
						update.setEnabled(true);
					}
					else if (SyncInfo.getDirection(syncInfo.getKind()) == SyncInfo.IN_SYNC)
					{
						commit.setEnabled(true);
						update.setEnabled(true);
						revert.setEnabled(true);
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			super.fillContextMenu(menu);
		}
	}

	private class SynchronizeMergeActionGroup extends ModelSynchronizeParticipantActionGroup
	{
		@Override
		public void fillContextMenu(IMenuManager menu)
		{
			TreeSelection treeSelection = (TreeSelection)getContext().getSelection();
			TreePath[] selectedPaths = treeSelection.getPaths();

			// only show merge for changes
			for (TreePath selectedPath : selectedPaths)
			{
				try
				{
					SyncInfo syncInfo = SolutionSubscriber.getInstance().getSyncInfo((IResource)selectedPath.getLastSegment());
					int change = SyncInfo.getChange(syncInfo.getKind());
					int direction = SyncInfo.getDirection(syncInfo.getKind());
					if (change == SyncInfo.CHANGE || direction == SyncInfo.CONFLICTING || direction == SyncInfo.AUTOMERGE_CONFLICT ||
						direction == SyncInfo.MANUAL_CONFLICT || direction == SyncInfo.PSEUDO_CONFLICT)
					{
						super.fillContextMenu(menu);
						return;
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
	}
}