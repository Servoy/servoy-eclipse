/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

package com.servoy.eclipse.ngclient.ui;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;

/**
 * @author jcompagner
 * @since 2021.03
 */
public class CopySourceFolderAction extends Action
{
	public CopySourceFolderAction()
	{
		setText("Copy the Titanium NGClient sources");
		setToolTipText("Copies the ngclient sources to the workspace/.metadata/.plugins/com.servoy.eclipse.ngclient.ui/target/ folder");
	}

	@Override
	public void run()
	{
		final int choice = MessageDialog.open(MessageDialog.QUESTION_WITH_CANCEL, Display.getCurrent().getActiveShell(), "Copy the Titanium NGClient sources",
			"This action will perform an npm install/ng build as well.\n" +
				"Should we do a normal install or a clean install (npm ci)?\n\n" +

				"Choosing 'Copy && Build' will copy sources and run a normal ng build.\n" +
				"Choosing 'Copy && Clean build' will clean out the 'node_modules' dir and do a full npm -ci. (do this if there are problems when building (see 'Titanium NG Build Console' in the 'Console' view).",
			SWT.NONE, new String[] { "Copy && Build", "Copy && Clean build", "Cancel" });

		if (choice < 0 || choice == 2) return; // cancel

		Job deleteJob = null;
		if (choice == 1)
		{
			deleteJob = Job.createSystem("delete .angular and packages cache", (monitor) -> {
				WebPackagesListener.setIgnore(true);
				FileUtils.deleteQuietly(Activator.getInstance().getMainTargetFolder());
			});
		}
		NodeFolderCreatorJob copySources = new NodeFolderCreatorJob(Activator.getInstance().getSolutionProjectFolder(), false, true);
		copySources.addJobChangeListener(new JobChangeAdapter()
		{
			@Override
			public void done(IJobChangeEvent event)
			{
				if (choice == 1) WebPackagesListener.setIgnoreAndCheck(false, false);
				WebPackagesListener.checkPackages(choice == 1);
			}
		});
		if (choice == 1)
		{
			deleteJob.addJobChangeListener(new JobChangeAdapter()
			{
				@Override
				public void done(IJobChangeEvent event)
				{
					copySources.schedule();
				}
			});
			deleteJob.schedule();
		}
		else
		{
			copySources.schedule();
		}
	}
}
