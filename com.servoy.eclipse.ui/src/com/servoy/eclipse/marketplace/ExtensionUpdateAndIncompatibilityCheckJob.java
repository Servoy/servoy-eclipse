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
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

/**
 * Job that does a background check for updates. If updates are found, it will show the Manage Extensions dialog.
 * @author acostescu
 */
public class ExtensionUpdateAndIncompatibilityCheckJob extends Job
{

	private volatile Thread checkingForUpdates;

	public ExtensionUpdateAndIncompatibilityCheckJob(String name)
	{
		super(name);
	}

	@Override
	protected void canceling()
	{
		super.canceling();
		if (checkingForUpdates != null) checkingForUpdates.interrupt(); // it's probably doing network I/O
	}

	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		IApplicationServerSingleton applicationServer = ApplicationServerRegistry.get();
		final InstalledExtensionsDialog dialog = InstalledExtensionsDialog.getOrCreateInstance(null);

		if (applicationServer.hadIncompatibleExtensionsWhenStarted())
		{
			showInstalledExtensionsDialog(
				dialog,
				"Incompatible extension found", "One or more installed extensions are incompatible with the current Servoy release (probably due to a Servoy update).\nDo you want to check for extension updates?", "Check for updates", "Ignore", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		else
		{
			// inside else, because there is no use doing this if incompatible extensions were found, because the user will be prompted to check for updates anyway;
			// it would be a bit strange if the user said "ignore" but still a check for updates would be done in background

			checkingForUpdates = Thread.currentThread(); // make sure user cancel can interrupt a possibly long duration network connection attempt
			boolean updatesFound = false;
			try
			{
				updatesFound = dialog.checkForUpdates(monitor);
			}
			finally
			{
				checkingForUpdates = null;
			}
			if (updatesFound)
			{
				showInstalledExtensionsDialog(dialog,
					"Extension update checker", "New versions are available for installed extensions.", "Show updates", "Ignore", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}
		}
		return Status.OK_STATUS;
	}

	private void showInstalledExtensionsDialog(final InstalledExtensionsDialog dialog, final String title, final String message, final String buttonShow,
		final String buttonIgnore, final boolean startUpdateCheck)
	{
		UIUtils.runInUI(new Runnable()
		{
			public void run()
			{
				MessageDialog q = new MessageDialog(Display.getCurrent().getActiveShell(), title, null, message, MessageDialog.QUESTION,
					new String[] { buttonShow, buttonIgnore }, 0);
				q.setBlockOnOpen(true);

				if (q.open() == 0)
				{
					dialog.setBlockOnOpen(false);
					dialog.open();
					dialog.setBlockOnOpen(true);
					if (startUpdateCheck)
					{
						dialog.simulateUpdateCheckButtonClick();
					}
				}
			}
		}, false);
	}
}
