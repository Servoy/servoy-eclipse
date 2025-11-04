/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author emera
 */
public class DatabaseImportPropertiesPage extends WizardPage implements IRestoreDefaultPage
{
	private final ExportWarModel exportModel;

	private Button allowDataModelChangeButton;
	private Button skipDatabaseViewsUpdate;
	private CheckboxTableViewer allowDataModelServers;
	private Button allowKeywordsButton;
	private Button updateSequencesButton;
	private Button overrideSequenceTypesButton;
	private Button overrideDefaultValuesButton;
	private Button overwriteGroupsButton;
	private final String ALLOW_DMC_TEXT = "Allow data model changes";

	public DatabaseImportPropertiesPage(ExportWarModel exportModel)
	{
		super("Database Import Properties");
		this.exportModel = exportModel;
		setTitle("Database Import Properties");
		setDescription("Select the database related import properties");
	}

	@Override
	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(1, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		allowDataModelChangeButton = new Button(composite, SWT.CHECK);
		if ("false".equals(exportModel.getAllowDataModelChanges()))
		{
			allowDataModelChangeButton.setSelection(false);
			allowDataModelChangeButton.setText(ALLOW_DMC_TEXT);
		}
		else
		{
			allowDataModelChangeButton.setSelection(true);
			allowDataModelChangeButton.setText(ALLOW_DMC_TEXT + " (for all servers)"); // can be altered later if there is a list of allowed servers
		}
		allowDataModelChangeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setGlobalAllowDataModelChangesFlag(allowDataModelChangeButton.getSelection());
			}

		});
		allowDataModelChangeButton.setToolTipText("Enable/Disable changes for all servers");

		Table table = new Table(composite, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION);
		allowDataModelServers = new CheckboxTableViewer(table);
		allowDataModelServers.setContentProvider(ArrayContentProvider.getInstance());
		allowDataModelServers.setInput(ApplicationServerRegistry.get().getServerManager().getServerNames(true, false, true, true));
		if ("true".equals(exportModel.getAllowDataModelChanges()))
		{
			allowDataModelServers.setAllChecked(true);
		}
		else if ("false".equals(exportModel.getAllowDataModelChanges()))
		{
			allowDataModelServers.setAllChecked(false);
		}
		else
		{
			allowDataModelServers.setCheckedElements(exportModel.getAllowDataModelChanges().split(","));
			allowDataModelChangeButton.setText(ALLOW_DMC_TEXT + " (only for servers checked below)");
		}

		allowDataModelServers.addCheckStateListener(e -> {
			boolean globalAllowValue = allowDataModelChangeButton.getSelection();
			if (globalAllowValue)
			{
				Object[] checkedElements = allowDataModelServers.getCheckedElements();
				if (checkedElements.length == 0)
				{
					setGlobalAllowDataModelChangesFlag(false);
				}
				else if (checkedElements.length == ((String[])allowDataModelServers.getInput()).length)
				{
					setGlobalAllowDataModelChangesFlag(true);
				}
				else
				{
					String selected = Arrays.stream(allowDataModelServers.getCheckedElements()).map(Object::toString).collect(Collectors.joining(","));
					exportModel.setAllowDataModelChanges(selected);
					allowDataModelChangeButton.setText(ALLOW_DMC_TEXT + " (only for servers checked below)");
				}
			}
		});
		allowDataModelServers.getTable().setBackground(composite.getBackground());
		allowDataModelServers.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 3));

		skipDatabaseViewsUpdate = new Button(composite, SWT.CHECK);
		skipDatabaseViewsUpdate.setText("Skip database views update");
		skipDatabaseViewsUpdate.setSelection(exportModel.isSkipDatabaseViewsUpdate());
		skipDatabaseViewsUpdate.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setSkipDatabaseViewsUpdate(skipDatabaseViewsUpdate.getSelection());
			}
		});

		allowKeywordsButton = new Button(composite, SWT.CHECK);
		allowKeywordsButton.setText("Allow sql keywords");
		allowKeywordsButton.setSelection(exportModel.isAllowSQLKeywords());
		allowKeywordsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setAllowSQLKeywords(allowKeywordsButton.getSelection());
			}
		});

		updateSequencesButton = new Button(composite, SWT.CHECK);
		updateSequencesButton.setText("Update sequences");
		updateSequencesButton.setSelection(exportModel.isUpdateSequences());
		updateSequencesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setUpdateSequences(updateSequencesButton.getSelection());
			}
		});

		overrideSequenceTypesButton = new Button(composite, SWT.CHECK);
		overrideSequenceTypesButton.setText("Override sequence types");
		overrideSequenceTypesButton.setSelection(exportModel.isOverrideSequenceTypes());
		overrideSequenceTypesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverrideSequenceTypes(overrideSequenceTypesButton.getSelection());
			}
		});

		overrideDefaultValuesButton = new Button(composite, SWT.CHECK);
		overrideDefaultValuesButton.setText("Override default values");
		overrideDefaultValuesButton.setSelection(exportModel.isOverrideDefaultValues());
		overrideDefaultValuesButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverrideDefaultValues(overrideDefaultValuesButton.getSelection());
			}
		});


		overwriteGroupsButton = new Button(composite, SWT.CHECK);
		overwriteGroupsButton.setText("Overwrite repository permission security settings with import version");
		overwriteGroupsButton.setSelection(exportModel.isOverwriteGroups());
		overwriteGroupsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				exportModel.setOverwriteGroups(overwriteGroupsButton.getSelection());
			}
		});

		setControl(composite);
	}

	private void setGlobalAllowDataModelChangesFlag(boolean newAllowValue)
	{
		allowDataModelChangeButton.setText(ALLOW_DMC_TEXT + (newAllowValue ? " (for all servers)" : ""));

		allowDataModelChangeButton.setSelection(newAllowValue);
		exportModel.setAllowDataModelChanges(Boolean.toString(newAllowValue));
		allowDataModelServers.getTable().setEnabled(newAllowValue);
		allowDataModelServers.setAllChecked(newAllowValue);
	}

	@Override
	public void restoreDefaults()
	{
		setGlobalAllowDataModelChangesFlag(true);
		exportModel.setAllowDataModelChanges("true");
		skipDatabaseViewsUpdate.setSelection(false);
		exportModel.setSkipDatabaseViewsUpdate(false);
		allowKeywordsButton.setSelection(false);
		exportModel.setAllowSQLKeywords(false);
		updateSequencesButton.setSelection(false);
		exportModel.setUpdateSequences(false);
		overrideSequenceTypesButton.setSelection(false);
		exportModel.setOverrideSequenceTypes(false);
		overrideDefaultValuesButton.setSelection(false);
		overwriteGroupsButton.setSelection(false);
		exportModel.setOverwriteGroups(false);
	}

	@Override
	public void setVisible(boolean visible)
	{
		if (visible)
		{
			allowDataModelChangeButton.setEnabled(exportModel.isExportActiveSolution());
			allowDataModelServers.getTable().setEnabled(exportModel.isExportActiveSolution());
			skipDatabaseViewsUpdate.setEnabled(exportModel.isExportActiveSolution());
			allowKeywordsButton.setEnabled(exportModel.isExportActiveSolution());
			updateSequencesButton.setEnabled(exportModel.isExportActiveSolution());
			overrideSequenceTypesButton.setEnabled(exportModel.isExportActiveSolution());
			overrideDefaultValuesButton.setEnabled(exportModel.isExportActiveSolution());
			overwriteGroupsButton.setEnabled(exportModel.isExportActiveSolution());
		}
		super.setVisible(visible);
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_databaseimport");
	}
}
