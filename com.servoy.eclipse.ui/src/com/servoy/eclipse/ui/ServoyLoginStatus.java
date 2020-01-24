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

package com.servoy.eclipse.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;

/**
 * @author gboros
 *
 */
public class ServoyLoginStatus extends WorkbenchWindowControlContribution implements IServoyLoginListener
{
	private Label statusBtn;

	public ServoyLoginStatus()
	{
		ServoyLoginDialog.addLoginListener(this);
	}

	/*
	 * @see org.eclipse.jface.action.ControlContribution#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createControl(Composite parent)
	{
		parent.getParent().setRedraw(true);
		statusBtn = new Label(parent, SWT.NONE);
		statusBtn.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				if (ServoyLoginDialog.getLoginToken() == null)
				{
					new ServoyLoginDialog(PlatformUI.getWorkbench().getDisplay().getActiveShell()).doLogin();
				}
				else
				{
					//showPopUp(e, PlatformUI.getWorkbench().getDisplay().getActiveShell());
				}
			}
		});
		statusBtn.setImage(Activator.getDefault().loadImageFromBundle("servoy_design.png"));
		statusBtn.setToolTipText("Click to connect to Servoy");
		return statusBtn;
	}

	@Override
	public boolean isDynamic()
	{
		return true;
	}

//	private void showPopUp(MouseEvent e, Shell shell)
//	{
//		Shell popup = new Shell(shell.getDisplay(), SWT.NO_TRIM | SWT.ON_TOP | SWT.MODELESS);
//		popup.setLayout(new FillLayout());
//		new Label(popup, SWT.NONE).setText("Go to Servoy forum");
//		popup.pack();
//		popup.open();
//		shell.forceFocus();
//
//		popup.setLocation(shell.getLocation().x + e.x, shell.getLocation().y + e.y);
//	}

	/*
	 * @see com.servoy.eclipse.ui.IServoyLoginListener#onLogin(java.lang.String)
	 */
	@Override
	public void onLogin(String username, String loginToken)
	{
		if (username != null)
		{
			statusBtn.setImage(Activator.getDefault().loadImageFromBundle("windowicon.png"));
			statusBtn.setToolTipText("Logged in as " + username);
		}
		else
		{
			statusBtn.setImage(Activator.getDefault().loadImageFromBundle("servoy_design.png"));
			statusBtn.setToolTipText("Click to connect to Servoy");
		}
	}
}