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

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

/**
 * @author jcompagner
 * @since 2021.03
 */
public class CopySourceFolderAction extends Action
{
	public CopySourceFolderAction()
	{
		setText("Copy the NGClient2 sources");
		setToolTipText("Copies the ngclient sources to the workspace/.metadata/.plugins/com.servoy.eclipse.ngclient.ui/target/ folder");
	}

	@Override
	public void run()
	{
		final boolean cleanInstall = MessageDialog.openQuestion(Display.getCurrent().getActiveShell(), "Should we do a clean install (npm ci)?",
			"This cleans out the node_modules. A full npm ci is done.\nDo this if there are problems when building (See NGConsole in the console view)");
		NodeFolderCreatorJob copySources = new NodeFolderCreatorJob(Activator.getInstance().getProjectFolder(), false, true);
		copySources.addJobChangeListener(new JobChangeAdapter()
		{
			@Override
			public void done(IJobChangeEvent event)
			{
				WebPackagesListener.checkPackages(cleanInstall);
			}
		});
		copySources.schedule();

	}
}
