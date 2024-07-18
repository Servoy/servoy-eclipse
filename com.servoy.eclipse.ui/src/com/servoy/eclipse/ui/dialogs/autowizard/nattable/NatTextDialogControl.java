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
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.IStyle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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

		this.control.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		Button button = new Button(this, SWT.PUSH);
		button.setImage(iconImage);
		button.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, true));
		button.setToolTipText(title);
		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent e)
			{
				dialogOpener.openDialog(getShell(), getValue());
			}
		});
		button.addKeyListener(new KeyListener()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{
				switch (e.keyCode)
				{
					case SWT.SPACE :
					case SWT.CR :
						dialogOpener.openDialog(getShell(), getValue());
						break;
					case SWT.ARROW_LEFT :
						if (control instanceof Text)
						{
							((Text)control).setSelection(getValue().length());
							control.setFocus();
						}
						break;
					case SWT.ARROW_UP :
						dialogOpener.commit(MoveDirectionEnum.UP, true);
						break;
					case SWT.ARROW_DOWN :
						dialogOpener.commit(MoveDirectionEnum.DOWN, true);
						break;
					case SWT.TAB :
					case SWT.ARROW_RIGHT :
						dialogOpener.commit(MoveDirectionEnum.RIGHT, true);
						break;
					case SWT.ESC :
						dialogOpener.close();
				}
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
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

	public Object getControl()
	{
		return control;
	}
}
