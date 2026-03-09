/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.edit.editor.AbstractCellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * @author emera
 */
public class VerticallyCenteredTextCellEditor extends AbstractCellEditor
{
	private Text text;
	private Composite editorComposite;

	public VerticallyCenteredTextCellEditor()
	{
		super();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.nebula.widgets.nattable.edit.editor.ICellEditor#createEditorControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createEditorControl(Composite parent_)
	{
		editorComposite = new Composite(parent_, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		editorComposite.setLayout(layout);
		editorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		text = new Text(editorComposite, SWT.NONE);
		text.setBackground(this.cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR));
		text.setForeground(this.cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR));
		text.setFont(this.cellStyle.getAttributeValue(CellStyleAttributes.FONT));
		GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, true); // vertically centered
		text.setLayoutData(textData);

		text.addKeyListener(new KeyListener()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{

			}

			@Override
			public void keyPressed(KeyEvent e)
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
						if (text.getText().length() == 0 || text.getCaretPosition() == 0)
							commit(MoveDirectionEnum.LEFT, true);
						break;
					case SWT.ARROW_RIGHT :
						if (text.getText().length() == 0 || text.getCaretPosition() == text.getText().length())
							commit(MoveDirectionEnum.RIGHT, true);
						break;
					case SWT.ESC :
						if (e.stateMask == 0) close();
						break;
				}
			}
		});

		return editorComposite;
	}

	@Override
	public boolean activateOnTraversal(IConfigRegistry configRegistry, List<String> configLabels)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.nebula.widgets.nattable.edit.editor.ICellEditor#getEditorControl()
	 */
	@Override
	public Control getEditorControl()
	{
		return editorComposite;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.nebula.widgets.nattable.edit.editor.AbstractCellEditor#activateCell(org.eclipse.swt.widgets.Composite, java.lang.Object)
	 */
	@Override
	protected Control activateCell(Composite parent_, Object originalCanonicalValue)
	{
		createEditorControl(parent_);
		setEditorValue(originalCanonicalValue);
		text.setFocus();
		return editorComposite;
	}

	@Override
	public Object getEditorValue()
	{
		return text != null && !text.isDisposed() ? text.getText() : null;
	}

	@Override
	public void setEditorValue(Object value)
	{
		if (text != null && !text.isDisposed())
		{
			text.setText(value != null ? value.toString() : "");
		}
	}
}
