/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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


import java.text.MessageFormat;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;

import com.servoy.eclipse.ui.Activator;

/**
 * A cell editor that manages a font field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public abstract class DialogCellEditor extends org.eclipse.jface.viewers.DialogCellEditor implements IDialogDirectCellEditor
{
	public static final ILabelProvider DEFAULT_LABEL_PROVIDER = new LabelProvider();
	public static final Image OPEN_IMAGE = Activator.getDefault().loadImageFromBundle("open.png");

	private Button editorButton;
	private Control contents;

	private final ILabelProvider labelProvider;
	private final IValueEditor valueEditor;
	private ValueEditorCellLayout valueEditorCellLayout;
	private final boolean readOnly;

	/**
	 * Creates a new font cell editor parented under the given control.
	 *
	 * @param parent the parent control
	 * @param valueEditor
	 */
	public DialogCellEditor(Composite parent, ILabelProvider labelProvider, IValueEditor valueEditor, boolean readOnly, int style)
	{
		super(parent, style);
		if (valueEditorCellLayout != null) valueEditorCellLayout.setValueEditor(valueEditor);
		this.valueEditor = valueEditor;
		this.readOnly = readOnly;
		this.labelProvider = labelProvider == null ? DEFAULT_LABEL_PROVIDER : labelProvider;
	}

	/**
	 * @return the readOnly
	 */
	public boolean isReadOnly()
	{
		return readOnly;
	}

	public ILabelProvider getLabelProvider()
	{
		return labelProvider;
	}

	/**
	 * Get a selection based on value
	 *
	 * @return
	 */
	public StructuredSelection getSelection()
	{
		Object value = getValue();
		return getSelection(value);
	}

	protected StructuredSelection getSelection(Object value)
	{
		if (value == null) return StructuredSelection.EMPTY;
		if (value instanceof Object[])
		{
			return new StructuredSelection((Object[])value);
		}
		return new StructuredSelection(value);
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor. The focus is set to the cell editor's control.
	 */
	@Override
	protected void doSetFocus()
	{
		contents.setFocus();
	}

	@Override
	protected Control createControl(Composite parent)
	{
		Font font = parent.getFont();
		Color bg = parent.getBackground();

		Composite control = new Composite(parent, getStyle());
		control.setFont(font);
		control.setBackground(bg);
		valueEditorCellLayout = new ValueEditorCellLayout();
		control.setLayout(valueEditorCellLayout);

		contents = createContents(control);
		contents.setFont(font);
		contents.setBackground(bg);

		editorButton = createButton(control);
		editorButton.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				if (valueEditor != null)
				{
					valueEditor.openEditor(getValue());
				}
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});

		control.addKeyListener(new KeyAdapter()
		{

			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.character == '\u001b')
				{ // Escape
					fireCancelEditor();
				}
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
				if (e.keyCode == SWT.CR)
				{
					editValue(getControl());
				}
			}
		});

		contents.addMouseListener(new MouseAdapter()
		{
			/**
			 * left-click -> select value <br>
			 * cntrl+left-click -> open value editor
			 */
			@Override
			public void mouseDown(MouseEvent e)
			{
				contentsMouseDown(e);
			}
		});

		setValueValid(true);

		return control;
	}

	@Override
	protected Button createButton(Composite parent)
	{
		Button result = new Button(parent, SWT.FLAT);
		result.setImage(OPEN_IMAGE);
		return result;
	}

	/**
	 * Open the dialog to edit the value
	 */
	public void editValue(Control control)
	{
		if (readOnly)
		{
			return;
		}

		Object newValue = openDialogBox(control);
		if (newValue == null ||
			(doGetValue() != null &&
				(doGetValue().equals(newValue) || doGetValue().equals(newValue.toString()))))
		{
			fireCancelEditor();
		}
		else
		{
			if (isCorrect(newValue))
			{
				markDirty();
				doSetValue(newValue);
			}
			else
			{
				// try to insert the current value into the error message.
				setErrorMessage(MessageFormat.format(getErrorMessage(), new Object[] { newValue.toString() }));
			}
			fireApplyEditorValue();
			updateContents(newValue);
		}
	}

	@Override
	protected void updateContents(Object value)
	{
		if (!editorButton.isDisposed()) editorButton.setVisible(valueEditor != null && value != null && valueEditor.canEdit(value));
		if (labelProvider != null)
		{
			Label label = getDefaultLabel();
			if (label != null && !label.isDisposed())
			{
				String text = labelProvider.getText(value);
				if (text != null)
				{
					label.setText(text);
				}
			}
			return;
		}

		super.updateContents(value);
	}

	public void contentsMouseDown(MouseEvent e)
	{
		if (e.button == 1)
		{
			if (valueEditor != null && (e.stateMask & SWT.MOD1) > 0)
			{
				Object value = getValue();
				if (value != null && valueEditor.canEdit(value))
				{
					valueEditor.openEditor(value);
					return;
				}
			}
			editValue(getControl());
		}
	}

	public static class ValueEditorCellLayout extends Layout
	{
		private IValueEditor valueEditor;

		@Override
		public void layout(Composite editor, boolean force)
		{
			Rectangle bounds = editor.getClientArea();
			// we expect two children here
			Control contents = editor.getChildren()[0];
			Control editorButton = editor.getChildren()[1];
			if (valueEditor == null)
			{
				contents.setBounds(0, 0, bounds.width, bounds.height);
			}
			else
			{
				Point size = editorButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
				editorButton.setBounds(bounds.width - size.x, 0, size.x, bounds.height);
				contents.setBounds(0, 0, bounds.width - size.x, bounds.height);
			}
		}

		@Override
		public Point computeSize(Composite editor, int wHint, int hHint, boolean force)
		{
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
			{
				return new Point(wHint, hHint);
			}
			// we expect two children here
			Control contents = editor.getChildren()[0];
			Control editorButton = editor.getChildren()[1];
			if (valueEditor == null)
			{
				return contents.computeSize(wHint, hHint, force);
			}
			Point contentsSize = contents.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			Point buttonSize = editorButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			// Just return the button width to ensure the button is not clipped
			// if the label is long.
			// The label will just use whatever extra width there is
			Point result = new Point(buttonSize.x, Math.max(contentsSize.y, buttonSize.y));
			return result;
		}

		public void setValueEditor(IValueEditor valueEditor)
		{
			this.valueEditor = valueEditor;
		}
	}
}
