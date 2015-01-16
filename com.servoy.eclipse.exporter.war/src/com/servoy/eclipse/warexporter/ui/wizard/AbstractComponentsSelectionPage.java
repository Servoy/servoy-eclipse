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

import java.util.Arrays;
import java.util.Set;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.servoy.eclipse.warexporter.export.ExportWarModel;

/**
 * Page which shows the list of used components and services and allows the user to select additional components/services.
 * @author emera
 */
public abstract class AbstractComponentsSelectionPage extends WizardPage
{

	protected Set<String> componentsUsed;
	protected final IWizardPage nextPage;
	protected final ExportWarModel exportModel;
	protected List selectedComponentsList;
	private Button btnSelect;
	private Button btnRemove;
	private Button btnSelectAll;
	protected List availableComponentsList;

	protected Set<String> selectedComponents;
	private Set<String> availableComponents;
	private final String type;

	/**
	 * @param exportModel
	 * @param pageName
	 * @param title
	 * @param type
	 * @param titleImage
	 */
	protected AbstractComponentsSelectionPage(ExportWarModel exportModel, String pageName, String title, String description, IWizardPage nextPage, String type)
	{
		super(pageName);
		setTitle(title);
		setDescription(description);
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		this.type = type;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label exportedLabel = new Label(container, SWT.NULL);
		exportedLabel.setText("Exported " + type + "s");

		Label availableLabel = new Label(container, SWT.NULL);
		availableLabel.setText("Available " + type + "s");

		selectedComponentsList = new List(container, SWT.BORDER);
		selectedComponentsList.setItems(selectedComponents.toArray(new String[selectedComponents.size()]));

		btnSelect = new Button(container, SWT.NONE);
		btnSelect.setToolTipText("Export " + type);
		btnSelect.setText("<");

		btnRemove = new Button(container, SWT.NONE);
		btnRemove.setToolTipText("Remove " + type);
		btnRemove.setText(">");

		btnSelectAll = new Button(container, SWT.NONE);
		btnSelectAll.setToolTipText("Export all " + type + "s");
		btnSelectAll.setText("<<");

		availableComponentsList = new List(container, SWT.BORDER);
		availableComponents = getAvailableItems();
		availableComponentsList.setItems(availableComponents.toArray(new String[availableComponents.size()]));

		SelectionListener selectedComponentsListener = new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				btnRemove.setEnabled(!componentsUsed.contains(selectedComponentsList.getSelection()[0]));
				btnSelect.setEnabled(false);
				availableComponentsList.setSelection(-1);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				btnRemove.setEnabled(!componentsUsed.contains(selectedComponentsList.getSelection()[0]));
				btnSelect.setEnabled(false);
				availableComponentsList.setSelection(-1);
			}
		};
		selectedComponentsList.addSelectionListener(selectedComponentsListener);
		SelectionListener availableComponentsListener = new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				btnRemove.setEnabled(false);
				btnSelect.setEnabled(true);
				selectedComponentsList.setSelection(-1);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				btnRemove.setEnabled(false);
				btnSelect.setEnabled(true);
				selectedComponentsList.setSelection(-1);
			}
		};
		availableComponentsList.addSelectionListener(availableComponentsListener);

		btnSelect.setEnabled(false);
		btnRemove.setEnabled(false);
		btnSelectAll.setEnabled(!availableComponents.isEmpty());

		btnSelect.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				String selection = availableComponentsList.getSelection()[0];
				availableComponentsList.remove(selection);
				selectedComponentsList.add(selection);
				String[] selected = selectedComponentsList.getItems();
				Arrays.sort(selected);
				selectedComponentsList.setItems(selected);
				btnSelect.setEnabled(false);
			}
		});
		btnRemove.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				String selection = selectedComponentsList.getSelection()[0];
				if (componentsUsed.contains(selection)) return;
				availableComponentsList.add(selection);
				String[] available = availableComponentsList.getItems();
				Arrays.sort(available);
				availableComponentsList.setItems(available);
				selectedComponentsList.remove(selectedComponentsList.getSelectionIndex());
				btnRemove.setEnabled(false);
			}
		});
		btnSelectAll.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				for (String componentName : availableComponentsList.getItems())
				{
					selectedComponentsList.add(componentName);
				}

				String[] selected = selectedComponentsList.getItems();
				Arrays.sort(selected);
				selectedComponentsList.setItems(selected);
				availableComponentsList.removeAll();

				btnSelectAll.setEnabled(false);
				btnSelect.setEnabled(false);
			}
		});

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup().add(
			groupLayout.createParallelGroup().add(exportedLabel).add(selectedComponentsList, GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE)).addPreferredGap(
			LayoutStyle.UNRELATED).add(
			groupLayout.createParallelGroup().add(btnSelect, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE).add(btnRemove, GroupLayout.PREFERRED_SIZE, 50,
				Short.MAX_VALUE).add(btnSelectAll, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.UNRELATED).add(
			groupLayout.createParallelGroup().add(availableLabel).add(availableComponentsList, GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup().add(
			groupLayout.createSequentialGroup().add(exportedLabel).addPreferredGap(LayoutStyle.RELATED).add(selectedComponentsList, GroupLayout.PREFERRED_SIZE,
				250, Short.MAX_VALUE)).add(
			groupLayout.createSequentialGroup().add(50).add(btnSelect).addPreferredGap(LayoutStyle.RELATED).add(btnRemove).addPreferredGap(LayoutStyle.RELATED).add(
				btnSelectAll)).add(
			groupLayout.createSequentialGroup().add(availableLabel).addPreferredGap(LayoutStyle.RELATED).add(availableComponentsList,
				GroupLayout.PREFERRED_SIZE, 250, Short.MAX_VALUE)));
		container.setLayout(groupLayout);
	}

	protected abstract Set<String> getAvailableItems();
}
