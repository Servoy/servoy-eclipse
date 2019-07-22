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

package com.servoy.eclipse.ui.editors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.dialogs.PageFormatDialog.NumberVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.CSSPositionUtils;

/**
 * @author lvostinar
 *
 */
public class CSSPositionDialog extends Dialog
{
	private String value;
	private Button notSetChoice;
	private Button pixelsChoice;
	private Text pixelsText;
	private Button percentageChoice;
	private Text percentageText;
	private Button calcChoice;
	private Text calc1Text;
	private Combo calc1Combo;
	private Text calc2Text;
	private Combo calc2Combo;
	private Combo signCombo;

	public CSSPositionDialog(Shell parent, Object value)
	{
		super(parent);
		this.value = value != null ? value.toString() : "-1";
	}

	public String getValue()
	{
		return value;
	}

	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);
		shell.setText("Edit css position property");
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings()
	{
		return EditorUtil.getDialogSettings("cssPositionDialog");
	}

	@Override
	protected Control createDialogArea(Composite parent)
	{
		Composite composite = (Composite)super.createDialogArea(parent);
		composite.setLayout(new GridLayout(5, false));
		GridData gridData = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		composite.setLayoutData(gridData);

		SelectionAdapter listener = new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				adjustEditableState();
			}
		};

		notSetChoice = new Button(composite, SWT.RADIO);
		notSetChoice.addSelectionListener(listener);
		notSetChoice.setText("not set");
		gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
		gridData.horizontalSpan = 5;
		notSetChoice.setLayoutData(gridData);


		pixelsChoice = new Button(composite, SWT.RADIO);
		pixelsChoice.addSelectionListener(listener);
		pixelsChoice.setText("px");

		pixelsText = new Text(composite, SWT.BORDER);
		pixelsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		pixelsText.addVerifyListener(new NumberVerifyListener());

		percentageChoice = new Button(composite, SWT.RADIO);
		percentageChoice.setText("%");
		percentageChoice.addSelectionListener(listener);

		percentageText = new Text(composite, SWT.BORDER);
		percentageText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 4, 1));
		percentageText.addVerifyListener(new NumberVerifyListener());

		calcChoice = new Button(composite, SWT.RADIO);
		calcChoice.setText("calc(x + y)");
		calcChoice.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 5, 1));
		calcChoice.addSelectionListener(listener);

		calc1Text = new Text(composite, SWT.BORDER);
		calc1Text.addVerifyListener(new NumberVerifyListener());
		calc1Combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		signCombo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);
		calc2Text = new Text(composite, SWT.BORDER);
		calc2Text.addVerifyListener(new NumberVerifyListener());
		calc2Combo = new Combo(composite, SWT.DROP_DOWN | SWT.READ_ONLY);

		calc1Combo.setItems("px", "%");
		calc1Combo.setText("%");
		calc2Combo.setItems("px", "%");
		calc2Combo.setText("px");
		signCombo.setItems("+", "-");
		signCombo.setText("-");

		pixelsText.setEditable(false);
		percentageText.setEditable(false);
		calc1Text.setEditable(false);
		calc2Text.setEditable(false);

		if (!CSSPositionUtils.isSet(value))
		{
			notSetChoice.setSelection(true);
		}
		else if (value.startsWith("calc"))
		{
			calcChoice.setSelection(true);
			calc1Text.setEditable(true);
			calc2Text.setEditable(true);
			String content = value.substring(value.indexOf("(") + 1, value.lastIndexOf(")"));
			content = content.trim();
			String[] values = content.split(" ");
			if (values.length == 3)
			{
				if (values[0].contains("%"))
				{
					calc1Combo.setText("%");
					calc1Text.setText(values[0].replace("%", ""));
				}
				else
				{
					calc1Combo.setText("px");
					calc1Text.setText(values[0].replace("px", ""));
				}
				if (values[2].contains("%"))
				{
					calc2Combo.setText("%");
					calc2Text.setText(values[2].replace("%", ""));
				}
				else
				{
					calc2Combo.setText("px");
					calc2Text.setText(values[2].replace("px", ""));
				}
				if (values[1].equals("+"))
				{
					signCombo.setText("+");
				}
				else
				{
					signCombo.setText("-");
				}
			}
		}
		else if (value.contains("%"))
		{
			percentageChoice.setSelection(true);
			percentageText.setEditable(true);
			percentageText.setText(value.replace("%", ""));
		}
		else
		{
			pixelsChoice.setSelection(true);
			pixelsText.setEditable(true);
			if (value.endsWith("px"))
			{
				pixelsText.setText(value.substring(0, value.length() - 2));
			}
			else
			{
				pixelsText.setText(value);
			}
		}
		return composite;
	}

	private void adjustEditableState()
	{
		pixelsText.setEditable(false);
		percentageText.setEditable(false);
		calc1Text.setEditable(false);
		calc2Text.setEditable(false);
		if (pixelsChoice.getSelection())
		{
			pixelsText.setEditable(true);
		}
		else if (percentageChoice.getSelection())
		{
			percentageText.setEditable(true);
		}
		else if (calcChoice.getSelection())
		{
			calc1Text.setEditable(true);
			calc2Text.setEditable(true);
		}
	}

	@Override
	protected void okPressed()
	{
		if (notSetChoice.getSelection())
		{
			value = "-1";
		}
		else if (pixelsChoice.getSelection())
		{
			value = pixelsText.getText() + "px";
		}
		else if (percentageChoice.getSelection())
		{
			value = percentageText.getText() + "%";
		}
		else if (calcChoice.getSelection())
		{
			value = "calc( " + calc1Text.getText() + calc1Combo.getText() + " " + signCombo.getText() + " " + calc2Text.getText() + calc2Combo.getText() + ")";
		}
		super.okPressed();
	}
}
