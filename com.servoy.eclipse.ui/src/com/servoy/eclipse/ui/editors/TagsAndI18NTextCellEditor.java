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

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.dialogs.I18nCompositeText;
import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.ITable;

public class TagsAndI18NTextCellEditor extends TextDialogCellEditor
{
	private Shell shell = new Shell(SWT.NO_TRIM);

	private final ITable table;
	private final String title;
	private final IApplication application;
	private final FlattenedSolution flattenedSolution;
	private final PersistContext persistContext;
	private final boolean hideTags;


	public TagsAndI18NTextCellEditor(Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ILabelProvider labelProvider,
		ITable table, String title, IApplication application, boolean hideTags)
	{
		super(parent, SWT.NONE, labelProvider);
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.title = title;
		this.application = application;
		this.hideTags = hideTags;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(cellEditorWindow.getShell(), persistContext, flattenedSolution, table, getValue(), title,
			application, hideTags);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return dialog.getValue();
		}
		return TextDialogCellEditor.CANCELVALUE;
	}

	private boolean isMouseOver(Shell s)
	{
		if (Display.getCurrent().isDisposed() || s.isDisposed())
		{
			return false;
		}
		return s.getBounds().contains(Display.getCurrent().getCursorLocation());
	}

	@Override
	public void deactivate()
	{
		if (shell != null)
		{
			boolean isMouseOnShell = isMouseOver(shell);
			if (this.getControl() != null && !this.getControl().isDisposed() && !isMouseOnShell)
			{
				this.getControl().setVisible(false);
				shell.dispose();
			}
		}
	}

	@Override
	protected Control createContents(Composite parent, boolean createSingleLine)
	{
		Control contents = super.createContents(parent, createSingleLine);
		if (contents instanceof Text)
		{
			contents.addKeyListener(new KeyAdapter()
			{
				@Override
				public void keyReleased(KeyEvent e)
				{
					super.keyReleased(e);
					if (text.getText().startsWith("i18n:")) //$NON-NLS-1$
					{
						shell.dispose();
						showShell();
					}
				}
			});
			contents.addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseDown(MouseEvent e)
				{
					super.mouseDown(e);
					if (text.getText().startsWith("i18n:")) //$NON-NLS-1$
					{
						showShell();
					}
				}
			});
		}
		return contents;
	}

	private void setNewValue(String newValue)
	{
		markDirty();
		doSetValue(newValue);
		fireApplyEditorValue();
		shell.dispose();
	}

	private void showShell()
	{
		shell = new Shell(SWT.NO_TRIM);
		shell.setLayout(new FillLayout());
		shell.setMinimumSize(text.getSize().x, text.getSize().y);

		I18nCompositeText tableKyes = new I18nCompositeText(shell, SWT.PUSH, application, false);
		tableKyes.handleFilterChanged(text.getText().substring(5));

		tableKyes.getTableViewer().addDoubleClickListener(new IDoubleClickListener()
		{

			@Override
			public void doubleClick(DoubleClickEvent event)
			{
				setNewValue(tableKyes.getSelectedKey());
			}

		});

		shell.setLocation(text.toDisplay(text.getLocation()).x, text.toDisplay(text.getLocation()).y + 20);
		shell.pack();
		shell.setVisible(true);
	}
}
