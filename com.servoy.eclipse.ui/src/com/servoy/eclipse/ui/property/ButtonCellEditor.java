/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Helpful for building 1-button cell editors.
 *
 * @author acostescu
 */
public abstract class ButtonCellEditor extends CellEditor
{
	protected Object oldValue;
	private Button button;

	// constructors similar to super
	public ButtonCellEditor()
	{
	}

	public ButtonCellEditor(Composite parent)
	{
		super(parent);
	}

	public ButtonCellEditor(Composite parent, int style)
	{
		super(parent, style);
	}

	@Override
	protected Control createControl(Composite parent)
	{
//		Composite composite = new Composite(parent, SWT.NONE);
//		composite.setLayout(new FormLayout());

//		button = new Button(composite, SWT.NONE);
		button = new Button(parent, SWT.NONE);
//		FormData fd_button = new FormData();
//		fd_button.top = new FormAttachment(0);
//		fd_button.right = new FormAttachment(100);
//		fd_button.bottom = new FormAttachment(100);
//		button.setLayoutData(fd_button);
		button.setEnabled(false);
		button.setText("");

		button.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseUp(MouseEvent e)
			{
				buttonClicked();
			}
		});
		updateButtonState(button, oldValue);

		setValueValid(true);

//		composite.pack();
//		return composite;
		return button;
	}

	@Override
	protected Object doGetValue()
	{
		return oldValue;
	}

	@Override
	protected void doSetFocus()
	{
		button.setFocus();
	}

	@Override
	protected void doSetValue(Object value)
	{
		this.oldValue = value;
		if (button != null) updateButtonState(button, value);
	}

	protected abstract void updateButtonState(Button button, Object value);

	/**
	 * Triggered when the button is clicked. You can do anything you'd like here.
	 */
	protected abstract void buttonClicked();

	public void applyValue(Object newValue)
	{
		if (newValue != doGetValue())
		{
			doSetValue(newValue);
			markDirty();
			fireApplyEditorValue();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.CellEditor#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
		button = null;
	}
}
