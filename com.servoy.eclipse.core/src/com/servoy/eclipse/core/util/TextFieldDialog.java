/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog with a text field.
 * @author emera
 */
public class TextFieldDialog extends MessageDialog
{

	private Text text;
	private final String defaultText;
	private String selectedText;
	private final boolean isTextArea;

	public TextFieldDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
		String[] dialogButtonLabels)
	{
		this(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, "");
	}

	public TextFieldDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
		String[] dialogButtonLabels, String defaultText)
	{
		this(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, defaultText, false);
	}

	public TextFieldDialog(Shell parentShell, String dialogTitle, Image dialogTitleImage, String dialogMessage, int dialogImageType,
		String[] dialogButtonLabels, String defaultText, boolean isTextArea)
	{
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage, dialogImageType, dialogButtonLabels, 0);
		setBlockOnOpen(true);
		this.defaultText = defaultText;
		this.isTextArea = isTextArea;
	}

	@Override
	protected Control createCustomArea(Composite parent)
	{
		text = new Text(parent, isTextArea ? SWT.BORDER | SWT.MULTI | SWT.WRAP : SWT.BORDER);
		text.setText(defaultText);
		if (defaultText != null && defaultText.length() > 0) text.selectAll();
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		if (isTextArea)
		{
			gridData.heightHint = 300;
		}
		text.setLayoutData(gridData);
		return text;
	}

	@Override
	public boolean close()
	{
		selectedText = (text != null ? text.getText() : null);
		return super.close();
	}

	public String getSelectedText()
	{
		return selectedText;
	}

}
