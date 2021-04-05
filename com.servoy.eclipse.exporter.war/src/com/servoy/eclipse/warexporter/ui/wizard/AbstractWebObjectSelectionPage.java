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
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
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
 * Page which shows the list of services/components detected automatically to be used by the solution (these can't be unselected) and allows the user to select additional components/services.
 *
 * @author emera
 */
public abstract class AbstractWebObjectSelectionPage extends WizardPage implements IRestoreDefaultPage
{

	protected Set<String> webObjectsUsedExplicitlyBySolution;
	protected final ExportWarModel exportModel;
	protected List selectedWebObjectsList;
	private Button btnSelect;
	private Button btnRemove;
	private Button btnSelectAll;
	protected List availableWebObjectsList;

	protected Set<String> selectedWebObjectsForListCreation;
	private Set<String> availableWebObjectsForListCreation;
	private final String type;

	protected AbstractWebObjectSelectionPage(ExportWarModel exportModel, String pageName, String title, String description, String type)
	{
		super(pageName);
		setTitle(title);
		setDescription(description);
		this.exportModel = exportModel;
		this.type = type;

		webObjectsUsedExplicitlyBySolution = getWebObjectsExplicitlyUsedBySolution();
		selectedWebObjectsForListCreation = new TreeSet<String>(webObjectsUsedExplicitlyBySolution);
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label exportedLabel = new Label(container, SWT.NULL);
		exportedLabel.setText("Available " + type + "s");

		Label availableLabel = new Label(container, SWT.NULL);
		availableLabel.setText("Exported " + type + "s");

		selectedWebObjectsList = new List(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		selectedWebObjectsList.setItems(selectedWebObjectsForListCreation.toArray(new String[selectedWebObjectsForListCreation.size()]));

		btnSelect = new Button(container, SWT.NONE);
		btnSelect.setToolTipText("Export " + type);
		btnSelect.setText(">");

		btnRemove = new Button(container, SWT.NONE);
		btnRemove.setToolTipText("Remove " + type);
		btnRemove.setText("<");

		btnSelectAll = new Button(container, SWT.NONE);
		btnSelectAll.setToolTipText("Export all " + type + "s");
		btnSelectAll.setText(">>");

		availableWebObjectsList = new List(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		availableWebObjectsForListCreation = getAvailableItems(false);
		availableWebObjectsList.setItems(availableWebObjectsForListCreation.toArray(new String[availableWebObjectsForListCreation.size()]));

		selectedWebObjectsList.addSelectionListener(new SelectionListener()
		{
			private void selected()
			{
				btnRemove.setEnabled(selectedWebObjectsList.getSelection().length > 0 &&
					Collections.disjoint(webObjectsUsedExplicitlyBySolution, Arrays.asList(selectedWebObjectsList.getSelection())));
				btnSelect.setEnabled(false);
				availableWebObjectsList.setSelection(-1);
			}

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selected();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				selected();
			}
		});
		selectedWebObjectsList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDoubleClick(MouseEvent e)
			{
				removeSelectedWebObjects();
			}
		});

		availableWebObjectsList.addSelectionListener(new SelectionListener()
		{
			private void selected()
			{
				btnRemove.setEnabled(false);
				btnSelect.setEnabled(availableWebObjectsList.getSelection().length > 0);
				selectedWebObjectsList.setSelection(-1);
			}

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				selected();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				selected();
			}
		});
		availableWebObjectsList.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDoubleClick(MouseEvent e)
			{
				addSelectedWebObjects();
			}
		});

		btnSelect.setEnabled(false);
		btnRemove.setEnabled(false);
		btnSelectAll.setEnabled(!availableWebObjectsForListCreation.isEmpty());

		btnSelect.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				addSelectedWebObjects();
			}
		});

		btnRemove.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				removeSelectedWebObjects();
			}
		});

		btnSelectAll.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				for (String webObjectName : availableWebObjectsList.getItems())
				{
					selectedWebObjectsList.add(webObjectName);
				}

				String[] selected = selectedWebObjectsList.getItems();
				Arrays.sort(selected);
				selectedWebObjectsList.setItems(selected);
				availableWebObjectsList.removeAll();

				btnSelectAll.setEnabled(false);
				btnSelect.setEnabled(false);
			}
		});


		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup().add(exportedLabel).add(availableWebObjectsList,
			GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE)).addPreferredGap(LayoutStyle.UNRELATED).add(
				groupLayout.createParallelGroup().add(btnSelect, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE).add(btnRemove, GroupLayout.PREFERRED_SIZE, 50,
					Short.MAX_VALUE).add(btnSelectAll, GroupLayout.PREFERRED_SIZE, 50, Short.MAX_VALUE))
			.addPreferredGap(LayoutStyle.UNRELATED).add(
				groupLayout.createParallelGroup().add(availableLabel).add(selectedWebObjectsList, GroupLayout.PREFERRED_SIZE, 150, Short.MAX_VALUE)));
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup().add(groupLayout.createSequentialGroup().add(exportedLabel).addPreferredGap(LayoutStyle.RELATED).add(
				availableWebObjectsList, GroupLayout.PREFERRED_SIZE, 250, Short.MAX_VALUE)).add(
					groupLayout.createSequentialGroup().add(50).add(btnSelect).addPreferredGap(LayoutStyle.RELATED).add(btnRemove).addPreferredGap(
						LayoutStyle.RELATED).add(btnSelectAll))
				.add(
					groupLayout.createSequentialGroup().add(availableLabel).addPreferredGap(LayoutStyle.RELATED).add(selectedWebObjectsList,
						GroupLayout.PREFERRED_SIZE, 250, Short.MAX_VALUE)));
		container.setLayout(groupLayout);
	}

	public void restoreDefaults()
	{
		webObjectsUsedExplicitlyBySolution = getWebObjectsExplicitlyUsedBySolution();
		selectedWebObjectsForListCreation = new TreeSet<String>(webObjectsUsedExplicitlyBySolution);
		selectedWebObjectsList.setItems(selectedWebObjectsForListCreation.toArray(new String[selectedWebObjectsForListCreation.size()]));
		availableWebObjectsForListCreation = getAvailableItems(false);
		availableWebObjectsList.setItems(availableWebObjectsForListCreation.toArray(new String[availableWebObjectsForListCreation.size()]));
	}

	protected abstract Set<String> getWebObjectsExplicitlyUsedBySolution();

	private void addSelectedWebObjects()
	{
		for (String selection : availableWebObjectsList.getSelection())
		{
			availableWebObjectsList.remove(selection);
			selectedWebObjectsList.add(selection);
		}

		String[] selected = selectedWebObjectsList.getItems();
		Arrays.sort(selected);
		selectedWebObjectsList.setItems(selected);
		btnSelect.setEnabled(false);
	}

	private void removeSelectedWebObjects()
	{
		for (String selection : selectedWebObjectsList.getSelection())
		{
			if (!webObjectsUsedExplicitlyBySolution.contains(selection))
			{
				availableWebObjectsList.add(selection);
				selectedWebObjectsList.remove(selection);
			}
		}

		String[] available = availableWebObjectsList.getItems();
		Arrays.sort(available);
		availableWebObjectsList.setItems(available);
		btnRemove.setEnabled(false);
	}

	protected boolean checkThatAllPickableArePresentIn(Set<String> exportedList)
	{
		return getAvailableItems(true).stream().noneMatch((item) -> !exportedList.contains(item));
	}

	protected abstract Set<String> getAvailableItems(boolean alreadyPickedAtListCreationShouldBeInThere);

	protected abstract void joinWithLastUsed();

	public abstract void storeInput();

	@Override
	public IWizardPage getNextPage()
	{
		storeInput();
		return super.getNextPage();
	}

	public void setWebObjectsExplicitlyUsedBySolution(Set<String> webObjectsExplicitlyUsedBySolution)
	{
		this.webObjectsUsedExplicitlyBySolution = webObjectsExplicitlyUsedBySolution;
		selectedWebObjectsForListCreation = new TreeSet<String>(webObjectsExplicitlyUsedBySolution);
		joinWithLastUsed();
		selectedWebObjectsList.setItems(selectedWebObjectsForListCreation.toArray(new String[selectedWebObjectsForListCreation.size()]));
		availableWebObjectsForListCreation = getAvailableItems(false);
		availableWebObjectsList.setItems(availableWebObjectsForListCreation.toArray(new String[availableWebObjectsForListCreation.size()]));
	}

}
