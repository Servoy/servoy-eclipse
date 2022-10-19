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

package com.servoy.eclipse.ui.dialogs.autowizard.nattable;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * @author emera
 */
public class NatTextDialogControl extends Composite
{
	private Control control;
	protected Image iconImage;
	private final IStyle cellStyle;
	private final String title;
	private final IDialogOpener dialogOpener;
	private final ILabelProvider labelProvider;

	public NatTextDialogControl(Composite parent, int style, IStyle cellStyle, Image icon, String title, IDialogOpener dialogOpener,
		ILabelProvider labelProvider)
	{
		super(parent, style);
		this.dialogOpener = dialogOpener;
		this.cellStyle = cellStyle;
		this.iconImage = icon;
		this.title = title;
		this.labelProvider = labelProvider;

		GridLayout gridLayout = new GridLayout(2, false);
		gridLayout.marginWidth = 0;
		gridLayout.marginHeight = 0;
		gridLayout.horizontalSpacing = 0;
		setLayout(gridLayout);
		createTextControl(style);
	}

	private void createTextControl(int style)
	{
		int textStyle = style | SWT.LEFT;
		this.control = labelProvider == null ? new Text(this, textStyle) : new Label(this, textStyle);
		this.control.setBackground(this.cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR));
		this.control.setForeground(this.cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR));
		this.control.setFont(this.cellStyle.getAttributeValue(CellStyleAttributes.FONT));

		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
		this.control.setLayoutData(gridData);
		gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);

		Button button = new Button(this, SWT.PUSH);
		button.setImage(iconImage);
		button.setLayoutData(gridData);
		button.setToolTipText(title);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent e)
			{
				dialogOpener.openDialog(getShell(), getValue());
			}
		});
	}

	public String getValue()
	{
		return control instanceof Text ? ((Text)control).getText() : ((Label)control).getText();
	}

	public void setValue(String value)
	{
		if (control instanceof Text)
		{
			((Text)control).setText(value);
		}
		else
		{
			((Label)control).setText(labelProvider.getText(value));
		}
	}
}
