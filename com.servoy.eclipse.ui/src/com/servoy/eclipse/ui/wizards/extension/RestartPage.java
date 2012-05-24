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

package com.servoy.eclipse.ui.wizards.extension;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.extension.Message;

/**
 * Page that prompts for restart and optionally shows messages.
 * It allows restart later.
 * @author acostescu
 */
public class RestartPage extends ShowMessagesPage
{

	protected InstallExtensionState state;

	/**
	 * Creates a new restart page with the given messages.<br>
	 * The page might change the state.mustRestart flag.
	 * @param state the install state.
	 * @param messages null or a list of messages to be presented to the user.
	 */
	public RestartPage(InstallExtensionState state, Message[] messages)
	{
		super("InstRst", "Servoy Developer restart required", "A restart is needed in order to complete the install process.", null, messages, false, null); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		this.state = state;
	}

	@Override
	protected void createCustomControl(Composite parent)
	{
		final Button restartCheck = new Button(parent, SWT.CHECK);
		restartCheck.setSelection(true);
		restartCheck.setText("restart developer when I press finish (recommended)"); //$NON-NLS-1$

		restartCheck.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				state.mustRestart = restartCheck.getSelection();
			}
		});
		restartCheck.setLayoutData(new GridData(SWT.BEGINNING, SWT.TOP, false, false));
	}

}
