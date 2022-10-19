/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2022 Servoy BV

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

package com.servoy.eclipse.model.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.j2db.util.Pair;

public class ScrollableDialog extends TitleAreaDialog
{
	private final String title;
	private final String text;
	private final String scrollableText;
	//ex IMessageProvider.ERROR
	private final int messageIcon;
	private List<Pair<Integer, String>> buttonsAndLabels = new ArrayList<Pair<Integer, String>>();

	public ScrollableDialog(Shell parentShell, int messageIcon, String title, String text, String scrollableText)
	{
		super(parentShell);
		this.messageIcon = messageIcon;
		this.title = title;
		this.text = text;
		this.scrollableText = scrollableText;
		// add default ok button
		buttonsAndLabels.add(new Pair<Integer, String>(OK, "OK"));
	}

	/**
	 * Use this function if you want extra buttons besides standard OK button
	 * @param buttonsAndLabels alist of pairs of Button ID and label , for ex: IDialogConstants.YES_TO_ALL_ID  , "Overwrite all" , this code will be returned code for myDlgVar.open()
	 */
	public void setCustomBottomBarButtons(List<Pair<Integer, String>> buttonsAndLabels)
	{
		this.buttonsAndLabels = buttonsAndLabels;
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite composite = (Composite)super.createDialogArea(parent); // Let the dialog create the parent composite

		GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true; // Layout vertically, too!
		gridData.verticalAlignment = GridData.FILL;

		Text scrollable = new Text(composite, SWT.BORDER | SWT.V_SCROLL | SWT.READ_ONLY);
		scrollable.setLayoutData(gridData);
		scrollable.setText(scrollableText);

		return composite;
	}

	@Override
	public void create()
	{
		super.create();

		// This is not necessary; the dialog will become bigger as the text grows but at the same time,
		// the user will be able to see all (or at least more) of the error message at once
		//getShell ().setSize (300, 300);
		setTitle(title);
		setMessage(text, messageIcon);

	}

	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{
		for (Pair<Integer, String> buttonAndLabel : buttonsAndLabels)
		{
			Button button = createButton(parent, buttonAndLabel.getLeft(), buttonAndLabel.getRight(), true);
			final int dialogReturnCode = buttonAndLabel.getLeft();
			button.addSelectionListener(new SelectionAdapter()
			{

				@Override
				public void widgetSelected(SelectionEvent e)
				{
					setReturnCode(dialogReturnCode);
					close();
				}
			});
		}
	}

	@Override
	protected boolean isResizable()
	{
		return true; // Allow the user to change the dialog size!
	}
}