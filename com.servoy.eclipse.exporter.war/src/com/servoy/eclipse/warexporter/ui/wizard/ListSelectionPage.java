/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.wizards.ICheckBoxView;
import com.servoy.eclipse.ui.wizards.SelectAllButtonsBar;

/**
 *
 * @author jcompagner
 * @since 6.1
 */
public class ListSelectionPage extends WizardPage implements ICheckStateListener, ICheckBoxView, IRestoreDefaultPage
{
	public static final String REQUIRED_LABEL = " (required)";

	private final List<String> input;
	private final List<String> selection;
	private CheckboxTableViewer checkboxTableViewer;
	private SelectAllButtonsBar selectAllButtons;
	private final boolean selectAll;

	/**
	 * @param string
	 * @param string2
	 * @param string3
	 * @param pluginDir
	 * @param beanSelectionPage
	 */
	public ListSelectionPage(String pagename, String title, String description, List<String> input, List<String> files, boolean selectAll)
	{
		super(pagename);
		this.input = input;
		this.selection = files;
		this.selectAll = selectAll;
		setTitle(title);
		setDescription(description);
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout();
		Composite container = new Composite(parent, SWT.NONE);
		setControl(container);
		container.setLayout(gridLayout);

		checkboxTableViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER | SWT.FULL_SELECTION);
		checkboxTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		checkboxTableViewer.setInput(input);
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		checkboxTableViewer.getTable().setLayoutData(gridData);
		checkboxTableViewer.addCheckStateListener(this);
		selectAllButtons = new SelectAllButtonsBar(this, container, false);
		if (selectAll)
		{
			checkboxTableViewer.setAllChecked(true);
		}
		else
		{
			checkboxTableViewer.setCheckedElements(selection.toArray());
		}
		if (checkboxTableViewer.getTable().getItemCount() == 0)
		{
			selectAllButtons.disableButtons();
		}
		else
		{
			selectAllButtons.enableAll();
		}
	}


	public void storeInput()
	{
		selection.clear();
		Object[] checkedElements = checkboxTableViewer.getCheckedElements();
		for (Object el : checkedElements)
			selection.add(el.toString());
	}

	@Override
	public void dispose()
	{
		super.dispose();
		if (checkboxTableViewer != null) checkboxTableViewer.removeCheckStateListener(this);
	}

	@Override
	public void checkStateChanged(CheckStateChangedEvent event)
	{
		if (checkboxTableViewer.getCheckedElements().length < checkboxTableViewer.getTable().getItemCount())
		{
			selectAllButtons.enableAll();
		}
		else
		{
			selectAllButtons.disableSelectAll();
		}
	}

	@Override
	public void selectAll()
	{
		checkboxTableViewer.setAllChecked(true);
	}

	@Override
	public void deselectAll()
	{
		checkboxTableViewer.setAllChecked(false);
	}

	@Override
	public void restoreDefaults()
	{
		checkboxTableViewer.setAllChecked(false);
		selectAllButtons.enableAll();
		selectAllButtons.disableDeselectAll();
	}
}