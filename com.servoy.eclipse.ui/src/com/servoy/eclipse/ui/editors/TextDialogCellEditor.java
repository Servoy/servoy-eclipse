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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A cell editor that manages a text field and includes a '...' button for editing the text field in a dialog.
 * <p>
 */
public abstract class TextDialogCellEditor extends TextCellEditor
{

	/**
	 * The editor control.
	 */
	private Composite editor;

	/**
	 * The current contents, Text for sinlgeLine, Label for multiline values.
	 */
	private Control contents;
	private boolean singleLine;

	/**
	 * The button.
	 */
	private Button button;
	private FocusListener buttonFocusListener;

	/**
	 * The value of this cell editor; initially <code>null</code>.
	 */
	private Object value = null;

	public final static Object CANCELVALUE = new Object();

	/**
	 * label provider used when multi-line values are shown
	 */

	ILabelProvider labelProvider;

	public TextDialogCellEditor(Composite parent, int style, ILabelProvider labelProvider)
	{
		super(parent, style);
		this.labelProvider = labelProvider;
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	@Override
	protected Control createControl(Composite parent)
	{
		Font font = parent.getFont();
		Color bg = parent.getBackground();

		editor = new Composite(parent, getStyle());
		editor.setFont(font);
		editor.setBackground(bg);
		editor.setLayout(new TextDialogCellLayout());

		// This may create a Text or a label depending on the value
		contents = null; // contents will be created later, it depends on the value// createContents(editor);

		button = createButton(editor);
		button.setFont(font);

		button.addFocusListener(getButtonFocusListener());

		button.addKeyListener(new KeyAdapter()
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			@Override
			public void keyReleased(KeyEvent e)
			{
				if (e.character == '\u001b')
				{ // Escape
					fireCancelEditor();
				}
			}
		});

		button.addSelectionListener(new SelectionAdapter()
		{
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent event)
			{
				editValue(editor);
			}
		});

		setValueValid(true);

		return editor;
	}

	protected Control createMultineLabel(Composite cell)
	{
		Label label = new Label(cell, SWT.LEFT);
		label.setFont(cell.getFont());
		label.setBackground(cell.getBackground());
		label.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent e)
			{
				editValue(getControl());
			}
		});

		return label;
	}

	/**
	 * Return a listener for button focus.
	 * 
	 * @return FocusListener
	 */
	private FocusListener getButtonFocusListener()
	{
		if (buttonFocusListener == null)
		{
			buttonFocusListener = new FocusListener()
			{

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.FocusListener#focusGained(org.eclipse.swt.events.FocusEvent)
				 */
				public void focusGained(FocusEvent e)
				{
					// Do nothing
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
				 */
				public void focusLost(FocusEvent e)
				{
					TextDialogCellEditor.this.focusLost();
				}
			};
		}

		return buttonFocusListener;
	}

	public abstract Object openDialogBox(Control cellEditorWindow);

	protected boolean editing = false;

	private boolean ignoreFocusLost = true;

	public void setIgnoreFocusLost(boolean b)
	{
		ignoreFocusLost = b;
	}

	protected void editValue(Control control)
	{
		if (editing) return;
		editing = true;
		try
		{
			setIgnoreFocusLost(true);
			Object newValue = openDialogBox(control);
			if (CANCELVALUE.equals(newValue))
			{
				fireCancelEditor();
			}
			else
			{
				boolean newValidState = isCorrect(newValue);
				if (newValidState)
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
			}
		}
		finally
		{
			editing = false;
		}
	}

	/**
	 * Creates the button for this cell editor under the given parent control.
	 * <p>
	 * The default implementation of this framework method creates the button display on the right hand side of the dialog cell editor. Subclasses may extend or
	 * reimplement.
	 * </p>
	 * 
	 * @param parent the parent control
	 * @return the new button control
	 */
	protected Button createButton(Composite parent)
	{
		Button result = new Button(parent, SWT.DOWN);
		result.setText("..."); //$NON-NLS-1$
		return result;
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor. The focus is set to the cell editor's button.
	 */
	@Override
	protected void doSetFocus()
	{
		if (contents != null)
		{
			if (contents instanceof Text)
			{
				contents.setFocus();
				((Text)contents).selectAll();
			}
			else
			{
				button.setFocus();
			}
		}
	}

	@Override
	protected void focusLost()
	{
		if (!ignoreFocusLost && isActivated())
		{
			// Handle lost focus in a job so that we can still process the button
			// click and keep the editor active.
			// Note: disabled focus lost handling, on some OSes (winXp) we saw that the button could still not be processed
			new WorkbenchJob("looseFocusJob")
			{
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor)
				{
					if (!editing)
					{
						TextDialogCellEditor.super.focusLost();
					}
					return Status.OK_STATUS;
				}
			}.schedule(100);
		}
	}

	@Override
	protected void doSetValue(Object value)
	{
		this.value = value;
		updateContents(value);
	}

	@Override
	protected Object doGetValue()
	{
		Object val;
		if (contents instanceof Text)
		{
			val = super.doGetValue();
		}
		else
		{
			val = this.value;
		}
		return "".equals(val) ? null : val;
	}

	/**
	 * Updates the controls showing the value of this cell editor.
	 * <p>
	 * The default implementation of this framework method just converts the passed object to a string using <code>toString</code> and sets this as the text
	 * of the label widget.
	 * </p>
	 * <p>
	 * Subclasses may reimplement. If you reimplement this method, you should also reimplement <code>createContents</code>.
	 * </p>
	 * 
	 * @param val the new value of this cell editor
	 */
	protected void updateContents(Object val)
	{
		boolean newSingleLine = (val == null) || (val instanceof String && ((String)val).indexOf('\n') < 0);
		if (contents == null /* TODO handle state change || singleLine != newSingleLine */)
		{
			singleLine = newSingleLine;
			if (contents != null)
			{
				// state changed, delete old contents
				contents.dispose();
			}
			if (singleLine)
			{
				// Single line text value
				contents = super.createControl(editor);
			}
			else
			{
				// multiLine, cannot edit in Text widget nicely
				contents = createMultineLabel(editor);
			}
		}

		String stringValue = "";//$NON-NLS-1$
		if (val != null)
		{
			stringValue = val.toString();
			stringValue = stringValue.replaceAll("\\\\n", "\n");
		}
		if (contents instanceof Text)
		{
			// the editor shouldn't be marked as dirty
			super.doSetValue(stringValue);
			((Text)contents).setFocus();
		}
		if (contents instanceof Label)
		{
			((Label)contents).setText(labelProvider == null ? stringValue : labelProvider.getText(val).replaceAll("\\\\n", "\n"));
		}
	}

	/**
	 * Internal class for laying out the dialog.
	 */
	private class TextDialogCellLayout extends Layout
	{
		@Override
		public void layout(Composite composite, boolean force)
		{
			Rectangle bounds = composite.getClientArea();
			Point size = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			if (contents != null)
			{
				contents.setBounds(0, 0, bounds.width - size.x, bounds.height);
			}
			button.setBounds(bounds.width - size.x, 0, size.x, bounds.height);
		}

		@Override
		public Point computeSize(Composite composite, int wHint, int hHint, boolean force)
		{
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
			{
				return new Point(wHint, hHint);
			}
			Point contentsSize = contents.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			Point buttonSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
			// Just return the button width to ensure the button is not clipped
			// if the label is long.
			// The label will just use whatever extra width there is
			Point result = new Point(contentsSize.x + buttonSize.x, Math.max(contentsSize.y, buttonSize.y));
			return result;
		}
	}

}
