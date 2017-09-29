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

package com.servoy.eclipse.ui.wizards;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

/**
 * @author rlazar
 * @since 8
 */
public class SelectAllButtonsBar
{
	protected final Button selectAll;
	protected final Button deselectAll;
	protected final Composite container;
	private Button restoreDefaults;

	public SelectAllButtonsBar(final ICheckBoxView checkBoxView, Composite parent)
	{
		this(checkBoxView, parent, false);
	}

	public SelectAllButtonsBar(final ICheckBoxView checkBoxView, Composite parent, boolean withRestoreButton)
	{
		container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(withRestoreButton ? 3 : 2, false));

		selectAll = new Button(container, SWT.NONE);
		selectAll.setText("Select All");
		selectAll.setLayoutData(new GridData(SWT.BEGINNING));
		selectAll.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				deselectAll.setEnabled(true);
				selectAll.setEnabled(false);
				checkBoxView.selectAll();
			}
		});

		deselectAll = new Button(container, SWT.NONE);
		deselectAll.setText("Deselect All");
		deselectAll.setLayoutData(new GridData());
		deselectAll.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selectAll.setEnabled(true);
				deselectAll.setEnabled(false);
				checkBoxView.deselectAll();
			}
		});

		if (withRestoreButton)
		{
			restoreDefaults = new Button(container, SWT.PUSH);
			restoreDefaults.setText("Restore Defaults");
			restoreDefaults.setLayoutData(new GridData(SWT.END));
		}
	}

	public void disableButtons()
	{
		selectAll.setEnabled(false);
		deselectAll.setEnabled(false);
	}

	public void disableSelectAll()
	{
		selectAll.setEnabled(false);
		deselectAll.setEnabled(true);
	}

	public void disableDeselectAll()
	{
		selectAll.setEnabled(true);
		deselectAll.setEnabled(false);
	}

	public void enableAll()
	{
		selectAll.setEnabled(true);
		deselectAll.setEnabled(true);
	}

	public void addRestoreSelectionListener(SelectionListener selectionListener)
	{
		if (restoreDefaults != null)
		{
			restoreDefaults.addSelectionListener(selectionListener);
		}
	}
}
