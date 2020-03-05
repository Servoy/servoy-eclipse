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

package com.servoy.eclipse.core.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

/**
 * @author Diana
 * Class that will create a dialog containing radio buttons.
 */
public class RadioButtonsDialog extends Dialog
{

	private final List<Button> radioButtons = new ArrayList<Button>();
	private List<String> radioButtonsTexts = new ArrayList<String>();
	private final String dialogTitle;
	private Button selectedButton;

	/**
	 * @param parentShell
	 * @param radioButtonsTexts
	 * @param dialogTitle
	 */
	public RadioButtonsDialog(Shell parentShell, List<String> radioButtonsTexts, String dialogTitle)
	{
		super(parentShell);
		this.dialogTitle = dialogTitle;
		this.radioButtonsTexts = radioButtonsTexts;
	}

	@Override
	protected void configureShell(Shell newShell)
	{
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}

	@Override
	protected Button createButton(Composite parent, int id, String label, boolean defaultButton)
	{
		if (id == IDialogConstants.CANCEL_ID) return null;
		return super.createButton(parent, id, label, defaultButton);
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite area = (Composite)super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);
		createRadioButtonListToDisplay(container);
		return area;
	}

	@Override
	protected boolean isResizable()
	{
		return true;
	}

	private void createRadioButtonListToDisplay(Composite container)
	{
		SelectionListener selectionListener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				selectedButton = ((Button)event.widget);
			}
		};

		for (final String radioButtonText : radioButtonsTexts)
		{
			Button radioButton = new Button(container, SWT.RADIO);
			radioButton.setText(radioButtonText);
			radioButton.addSelectionListener(selectionListener);
			radioButtons.add(radioButton);

		}
	}

	/**
	 * The method returns the order of the radioButton
	 */
	@Override
	public int open()
	{
		int open = super.open();
		open = radioButtons.indexOf(selectedButton);
		return open;
	}
}
