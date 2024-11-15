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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
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
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.ITable;

public class TagsAndI18NTextCellEditor extends TextDialogCellEditor
{
	private Shell shell = null;
	private I18nCompositeText tableKyes = null;
	private String selectedValue = null;

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
				closeShell();
			}
		}
		super.deactivate();
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
				public void keyPressed(KeyEvent e)
				{
					if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN)
					{
						e.doit = false;
					}
				}

				@Override
				public void keyReleased(KeyEvent e)
				{
					if (text.getText().startsWith("i18n:") && (e.keyCode != SWT.CR && e.keyCode != SWT.KEYPAD_CR)) //$NON-NLS-1$
					{
						showShell();
						tableKyes.handleFilterChanged(text.getText().substring(5));
						Collection<I18NMessagesModelEntry> input = (Collection)tableKyes.getTableViewer().getInput();
						if ((e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) && input.size() > 0)
						{
							IStructuredSelection selection = tableKyes.getTableViewer().getStructuredSelection();
							// this will also handle no selection because then the first element is null and the index 0 will be set
							ArrayList<I18NMessagesModelEntry> lst = new ArrayList<>(input);
							int index = lst.indexOf(selection.getFirstElement());
							index = e.keyCode == SWT.ARROW_UP ? index - 1 : index + 1;
							index = index < 0 ? 0 : index;
							index = index >= input.size() ? input.size() - 1 : index;
							tableKyes.selectKey("i18n:" + lst.get(index).key);
						}
					}
					else
					{
						closeShell();
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
		if (getValue() instanceof String && ((String)getValue()).startsWith("i18n:")) //$NON-NLS-1$
		{
			Display.getCurrent().asyncExec(() -> {
				showShell();
			});
		}
		return contents;
	}

	private void setNewValue(String newValue)
	{
		markDirty();
		doSetValue(newValue);
		fireApplyEditorValue();
		closeShell();
	}

	@Override
	protected void handleDefaultSelection(SelectionEvent event)
	{
		if (shell != null)
		{
			setNewValue(tableKyes.getSelectedKey());
		}
		super.handleDefaultSelection(event);
	}

	private void showShell()
	{
		if (shell == null || shell.isDisposed())
		{
			shell = new Shell(SWT.NO_TRIM);
			shell.setLayout(new FillLayout(SWT.HORIZONTAL));
			tableKyes = new I18nCompositeText(shell, SWT.PUSH, application);

			String v = (String)getValue();
			tableKyes.handleFilterChanged(v.substring(5));
			tableKyes.selectKey(v);

			tableKyes.getTableViewer().addDoubleClickListener(new IDoubleClickListener()
			{

				@Override
				public void doubleClick(DoubleClickEvent event)
				{
					setNewValue(selectedValue);
				}

			});
			tableKyes.getTableViewer().addSelectionChangedListener(new ISelectionChangedListener()
			{

				@Override
				public void selectionChanged(SelectionChangedEvent event)
				{
					selectedValue = tableKyes.getSelectedKey();
				}
			});
			shell.setLocation(text.toDisplay(text.getLocation()).x - 275, text.toDisplay(text.getLocation()).y + 20);
			shell.pack();
			shell.setSize(550, 600);
			shell.setVisible(true);
		}

	}

	private void closeShell()
	{
		if (shell != null)
		{
			shell.dispose();
		}
		shell = null;
		tableKyes = null;
	}
}
