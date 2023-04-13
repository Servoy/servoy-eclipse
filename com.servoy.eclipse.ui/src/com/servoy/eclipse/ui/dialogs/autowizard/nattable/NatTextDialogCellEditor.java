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

import java.util.List;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.convert.DisplayConverter;
import org.eclipse.nebula.widgets.nattable.edit.editor.AbstractCellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author emera
 */
public abstract class NatTextDialogCellEditor extends AbstractCellEditor implements IDialogOpener
{
	private NatTextDialogControl editor;

	protected final String title;
	private final Image icon;
	private ILabelProvider labelProvider;

	protected Object canonicalValue;

	public NatTextDialogCellEditor(String title, Image icon)
	{
		super();
		this.title = title;
		this.icon = icon;
	}

	public NatTextDialogCellEditor(String title, Image icon, ILabelProvider labelProvider)
	{
		super();
		this.title = title;
		this.icon = icon;
		this.labelProvider = labelProvider;
	}


	@Override
	public Object getEditorValue()
	{
		return editor.getValue();
	}

	@Override
	public void setEditorValue(Object value)
	{
		String val = labelProvider != null ? labelProvider.getText(value) : String.valueOf(value != null ? value.toString() : "");
		this.editor.setValue(val);
	}

	@Override
	public Control getEditorControl()
	{
		return editor;
	}

	@Override
	public NatTextDialogControl createEditorControl(Composite parent)
	{
		NatTextDialogControl control = new NatTextDialogControl(parent, SWT.NONE, cellStyle, icon, title, this, labelProvider);
		if (control.getControl() instanceof Text)
		{
			Text textControl = (Text)control.getControl();
			textControl.addKeyListener(new KeyListener()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					_keyReleased(e, textControl);
				}

				@Override
				public void keyPressed(KeyEvent e)
				{
				}
			});
		}
		return control;
	}

	@Override
	protected Control activateCell(Composite parent, Object originalCanonicalValue)
	{
		editor = createEditorControl(parent);
		this.canonicalValue = originalCanonicalValue;
		DisplayConverter converter = getDisplayConverter();
		setEditorValue(converter != null ? converter.canonicalToDisplayValue(originalCanonicalValue) : originalCanonicalValue);
		editor.setFocus();
		return editor;
	}

	public void _keyReleased(KeyEvent e, Text textControl)
	{
		switch (e.keyCode)
		{
			case SWT.ARROW_DOWN :
			case SWT.CR :
			case SWT.KEYPAD_CR :
				commit(MoveDirectionEnum.DOWN, true);
				break;
			case SWT.ARROW_UP :
				commit(MoveDirectionEnum.UP, true);
				break;
			case SWT.ARROW_LEFT :
				if (textControl.getText().length() == 0 || textControl.getCaretPosition() == 0)
					commit(MoveDirectionEnum.LEFT, true);
				break;
			case SWT.ARROW_RIGHT :
				if (textControl.getText().length() == 0 || textControl.getCaretPosition() == textControl.getText().length())
					commit(MoveDirectionEnum.RIGHT, true);
				break;
			case SWT.ESC :
				if (e.stateMask == 0) close();
				break;
		}
	}

	@Override
	public boolean activateOnTraversal(IConfigRegistry configRegistry, List<String> configLabels)
	{
		return true;
	}
}
