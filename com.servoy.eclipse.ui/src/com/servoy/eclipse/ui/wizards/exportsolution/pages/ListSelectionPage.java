/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.wizards.exportsolution.pages;

import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.wizards.ICheckBoxView;
import com.servoy.eclipse.ui.wizards.SelectAllButtonsBar;


/**
 * @author gboros
 *
 */
public abstract class ListSelectionPage extends WizardPage implements ICheckStateListener, ICheckBoxView
{
	protected CheckboxTreeViewer treeViewer;
	protected SelectAllButtonsBar selectAllButtons;

	ListSelectionPage(String pageName, String title, String description)
	{
		super(pageName);
		setTitle(title);
		setDescription(description);
	}

	abstract String[] getEntries();

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		final String[] entries = getEntries();

		treeViewer = new CheckboxTreeViewer(composite);
		gridLayout.numColumns = 2;
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessVerticalSpace = true;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		treeViewer.getTree().setLayoutData(gridData);
		treeViewer.setContentProvider(new ITreeContentProvider()
		{
			public Object[] getChildren(Object parentElement)
			{
				return null;
			}

			public Object getParent(Object element)
			{
				return null;
			}

			public boolean hasChildren(Object element)
			{
				return false;
			}

			public Object[] getElements(Object inputElement)
			{
				return entries;
			}

			public void dispose()
			{
			}

			public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
			{
			}
		});
		treeViewer.setInput(entries);
		treeViewer.setCheckedElements(entries);
		selectAllButtons = new SelectAllButtonsBar(this, composite);
		if (treeViewer.getTree().getItemCount() == 0)
		{
			selectAllButtons.disableButtons();
		}
		else
		{
			selectAllButtons.enableAll();
		}
		treeViewer.addCheckStateListener(this);
		setControl(composite);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#selectAll()
	 */
	@Override
	public void selectAll()
	{
		treeViewer.setAllChecked(true);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.wizards.ICheckBoxView#deselectAll()
	 */
	@Override
	public void deselectAll()
	{
		treeViewer.setAllChecked(false);
	}
}