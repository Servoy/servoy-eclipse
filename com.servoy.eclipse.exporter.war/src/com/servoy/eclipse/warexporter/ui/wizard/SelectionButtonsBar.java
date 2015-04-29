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

package com.servoy.eclipse.warexporter.ui.wizard;

import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author rlazar
 * @since 8
 */
public class SelectionButtonsBar
{
	private final Button selectAll;
	private final Button deselectAll;

	public SelectionButtonsBar(final CheckboxTableViewer checkboxTableViewer, FormData fd_table, Composite container)
	{
		selectAll = new Button(container, SWT.NONE);
		fd_table.bottom = new FormAttachment(selectAll, -6);
		FormData fd_selectAll = new FormData();
		fd_selectAll.bottom = new FormAttachment(100);
		fd_selectAll.left = new FormAttachment(checkboxTableViewer.getTable(), 0, SWT.LEFT);
		selectAll.setLayoutData(fd_selectAll);
		selectAll.setText("Select All");
		selectAll.addSelectionListener(new SelectionAdapter()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				deselectAll.setEnabled(true);
				selectAll.setEnabled(false);
				checkboxTableViewer.setAllChecked(true);
			}

		});

		deselectAll = new Button(container, SWT.NONE);
		FormData fd_deselectAll = new FormData();
		fd_deselectAll.top = new FormAttachment(100, -25);
		fd_deselectAll.bottom = new FormAttachment(100);
		fd_deselectAll.left = new FormAttachment(selectAll, 6);
		deselectAll.setLayoutData(fd_deselectAll);
		deselectAll.setText("Deselect All");
		deselectAll.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selectAll.setEnabled(true);
				deselectAll.setEnabled(false);
				checkboxTableViewer.setAllChecked(false);
			}
		});
	}

	public void diableButtons()
	{

		selectAll.setEnabled(false);
		deselectAll.setEnabled(false);
	}

	public void disableSelectAll()
	{
		selectAll.setEnabled(false);
		deselectAll.setEnabled(true);
	}

}
