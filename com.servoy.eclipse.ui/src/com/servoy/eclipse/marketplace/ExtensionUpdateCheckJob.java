/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.marketplace;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.util.UIUtils;

/**
 * Job that does a background check for updates. If updates are found, it will show the Manage Extensions dialog.
 * @author acostescu
 */
public class ExtensionUpdateCheckJob extends Job
{

	public ExtensionUpdateCheckJob(String name)
	{
		super(name);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		final InstalledExtensionsDialog dialog = InstalledExtensionsDialog.getOrCreateInstance(null);
		if (dialog.checkForUpdates(monitor))
		{
			UIUtils.runInUI(new Runnable()
			{
				public void run()
				{
					MessageDialog q = new MessageDialog(Display.getCurrent().getActiveShell(), "Extension update checker", null, //$NON-NLS-1$
						"New versions are available for installed extensions.", MessageDialog.QUESTION, new String[] { "Show updates", "Ignore" }, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					q.setBlockOnOpen(true);

					if (q.open() == 0)
					{
						dialog.open();
					}
				}
			}, false);
		}
		return Status.OK_STATUS;
	}

}
