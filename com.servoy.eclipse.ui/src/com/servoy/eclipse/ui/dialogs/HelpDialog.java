/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2018 Servoy BV

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

package com.servoy.eclipse.ui.dialogs;

import org.eclipse.jface.dialogs.DialogTray;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author jcompagner
 * @since 8.4
 *
 */
public abstract class HelpDialog extends TrayDialog
{
	/**
	 * Creates a help  dialog instance. Note that the window will have no visual
	 * representation (no widgets) until it is told to open.
	 *
	 * @param shell the parent shell, or <code>null</code> to create a top-level shell
	 */
	protected HelpDialog(Shell shell)
	{
		super(shell);
	}

	/**
	 * Creates a help dialog with the given parent.
	 *
	 * @param parentShell the object that returns the current parent shell
	 */
	protected HelpDialog(IShellProvider parentShell)
	{
		super(parentShell);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.window.Window#configureShell(org.eclipse.swt.widgets.Shell)
	 */
	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		// Register help listener on the shell
		newShell.addHelpListener(event -> {
			// call perform help on the current page
			String helpId = getHelpID();
			if (helpId != null)
			{
				PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpId);
			}
		});
	}

	public abstract String getHelpID();

	@Override
	public boolean close()
	{
		boolean trayOpen = getTray() != null;
		if (trayOpen)
		{
			closeTray();
		}
		getDialogBoundsSettings().put("tray_open", trayOpen);
		return super.close();
	}

	@Override
	public void create()
	{
		super.create();

		String trayOpen = getDialogBoundsSettings().get("tray_open");
		final String helpId = getHelpID();
		if (helpId != null && (trayOpen == null || Boolean.valueOf(trayOpen).booleanValue()))
		{
			getShell().getDisplay().asyncExec(new Runnable()
			{
				@Override
				public void run()
				{
					PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpId);
				}
			});
		}
	}

	@Override
	public void openTray(DialogTray tray) throws IllegalStateException, UnsupportedOperationException
	{
		super.openTray(tray);
		Rectangle bounds = getShell().getBounds();
		bounds.width = bounds.width + 150;
		getShell().setBounds(bounds);
	}
}
