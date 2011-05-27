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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.ui.dialogs.TagsAndI18NTextDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Table;

public class TagsAndI18NTextCellEditor extends TextDialogCellEditor
{
	private final Table table;
	private final String title;
	private final IApplication application;
	private final FlattenedSolution flattenedSolution;
	private final PersistContext persistContext;

	public TagsAndI18NTextCellEditor(Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ILabelProvider labelProvider,
		Table table, String title, IApplication application)
	{
		super(parent, SWT.NONE, labelProvider);
		this.persistContext = persistContext;
		this.flattenedSolution = flattenedSolution;
		this.table = table;
		this.title = title;
		this.application = application;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		TagsAndI18NTextDialog dialog = new TagsAndI18NTextDialog(cellEditorWindow.getShell(), persistContext, flattenedSolution, table, getValue(), title,
			application);
		dialog.open();

		if (dialog.getReturnCode() != Window.CANCEL)
		{
			return dialog.getValue();
		}
		return TextDialogCellEditor.CANCELVALUE;
	}
}
