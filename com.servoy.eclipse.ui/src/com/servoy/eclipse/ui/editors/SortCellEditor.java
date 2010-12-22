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

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.SortDialog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ITableDisplay;

/**
 * A cell editor that manages a sort field.
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 */
public class SortCellEditor extends DialogCellEditor
{
	private final String title;
	private final ITableDisplay tableDisplay;
	private final FlattenedSolution flattenedEditingSolution;

	public SortCellEditor(Composite parent, FlattenedSolution flattenedEditingSolution, ITableDisplay tableDisplay, String title, ILabelProvider labelProvider)
	{
		super(parent, labelProvider, null, false, SWT.NONE);
		this.flattenedEditingSolution = flattenedEditingSolution;
		this.tableDisplay = tableDisplay;
		this.title = title;
	}

	@Override
	public Object openDialogBox(Control cellEditorWindow)
	{
		try
		{
			SortDialog dialog = new SortDialog(cellEditorWindow.getShell(), flattenedEditingSolution, tableDisplay.getTable(), getValue(), title);
			dialog.open();

			if (dialog.getReturnCode() != Window.CANCEL)
			{
				return dialog.getValue();
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}
}
