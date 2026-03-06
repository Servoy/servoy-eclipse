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

import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.edit.editor.AbstractCellEditor;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer.MoveDirectionEnum;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ITable;

/**
 * A cell editor for i18n keys that has a text field and a button to open the i18n dialog.
 * It also supports keyboard navigation and opening the dialog with space/enter keys.
 * @author emera
 */
public class I18NTextDialogCellEditor extends AbstractCellEditor
{

	private final boolean hideTags;
	private final PersistContext persistContext;
	private final IApplication application;
	private final ITable table;
	private final FlattenedSolution flattenedSolution;
	private final String title;

	private Text text;
	private Button button;
	private Composite editorComposite;
	private final Image icon;

	public I18NTextDialogCellEditor(boolean hideTags, PersistContext persistContext, IApplication application,
		ITable table, FlattenedSolution flattenedSolution, String title, Image icon)
	{
		super();
		this.hideTags = hideTags;
		this.persistContext = persistContext;
		this.application = application;
		this.table = table;
		this.flattenedSolution = flattenedSolution;
		this.title = title;
		this.icon = icon;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.nebula.widgets.nattable.edit.editor.ICellEditor#createEditorControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Control createEditorControl(Composite parent)
	{
		editorComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false); // 2 columns, not equal width
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 2;
		editorComposite.setLayout(layout);

		text = new Text(editorComposite, SWT.NONE);
		text.setBackground(this.cellStyle.getAttributeValue(CellStyleAttributes.BACKGROUND_COLOR));
		text.setForeground(this.cellStyle.getAttributeValue(CellStyleAttributes.FOREGROUND_COLOR));
		text.setFont(this.cellStyle.getAttributeValue(CellStyleAttributes.FONT));
		GridData textData = new GridData(SWT.FILL, SWT.CENTER, true, true); // vertically centered
		text.setLayoutData(textData);

		button = new Button(editorComposite, SWT.PUSH);
		button.setImage(com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle("i18n.png"));
		GridData buttonData = new GridData(SWT.LEFT, SWT.CENTER, false, true); // vertically centered
		button.setLayoutData(buttonData);
		button.addListener(SWT.Selection, e -> {
			openDialog(parent.getShell());
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
						openDialog(parent.getShell());
						break;
					case SWT.ARROW_LEFT :
						text.setSelection(text.getText().length());
						text.setFocus();
						break;
					case SWT.ARROW_UP :
						commit(MoveDirectionEnum.UP, true);
						break;
					case SWT.ARROW_DOWN :
						commit(MoveDirectionEnum.DOWN, true);
						break;
					case SWT.ARROW_RIGHT :
						commit(MoveDirectionEnum.RIGHT, true);
						break;
					case SWT.ESC :
						close();
				}
			}

			@Override
			public void keyPressed(KeyEvent e)
			{
			}
		});

		button.addTraverseListener(new TraverseListener()
		{
			@Override
			public void keyTraversed(TraverseEvent e)
			{
				if (e.detail == SWT.TRAVERSE_TAB_NEXT)
				{
					commit(MoveDirectionEnum.RIGHT, true);
				}
				else if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
				{
					text.setSelection(text.getText().length());
					text.setFocus();
				}
			}
		});


		text.addKeyListener(new KeyListener()
		{
			@Override
			public void keyReleased(KeyEvent e)
			{

			}

			@Override
			public void keyPressed(KeyEvent e)
			{
				_keyPressed(e);
			}
		});

		return editorComposite;
	}

	public void _keyPressed(KeyEvent e)
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
	protected Control activateCell(Composite parent, Object originalCanonicalValue)
	{
		createEditorControl(parent);
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

	public void openDialog(Shell shell)
	{
		TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(
			shell,
			persistContext,
			flattenedSolution,
			table,
			text.getText(),
			title,
			application,
			hideTags);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			text.setText(dialog.getValue().toString());
		}
	}
}