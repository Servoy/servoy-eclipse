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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;

/**
 * @author gboros
 *
 */
public class ImportSettingsPage extends WizardPage implements Listener
{
	private static final String ENTER_MAINTENANCE_MODE = "emm";
	private static final String ACTIVATE_NEW_RELEASE = "ac";
	private static final String COMPACT_BEFORE_INPUT = "cmpt";
	private static final String OVERWRITE_STYLE = "os";
	private static final String OVERWRITE_GROUP_SECURITY = "og";
	private static final String CLEAN_IMPORT = "clean";

	private static final String NEW_SOLUTION_NAME = "newname";

	private static final String OVERRIDE_SEQUENCES = "fs";
	private static final String UPDATE_SEQUENCES = "useq";
	private static final String OVERRIDE_DEFAUL_VALUES = "fd";

	private static final String ALLOW_RESERVED_SQL_KEYWORDS = "ak";
	private static final String ALLOWED_DATA_MODEL_CHANGES = "dm";
	private static final String SKIP_VIEWS = "sv";
	private static final String DISPLAY_DATA_MODEL_CHANGES = "dmc";
	private static final String IMPORT_META_DATA = "md";
	private static final String IMPORT_SAMPLE_DATA = "sd";
	private static final String IMPORT_I18N_DATA = "id";
	private static final String INSERT_NEW_I18N_DATA = "io";

	private static final String USER_IMPORT = "up";

	private static final String ALLOW_ADMIN_USER = "aa";

	private final ExportSolutionWizard exportSolutionWizard;
	private JSONObject importSettings;

	public ImportSettingsPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page5");
		setTitle("Choose import settings");
		setDescription("Specify the settings for your import");
		this.exportSolutionWizard = exportSolutionWizard;

		importSettings = exportSolutionWizard.getModel().getImportSettings();
		if (importSettings == null)
		{
			importSettings = new JSONObject();
		}
	}


	private Button createCheckbox(String label, String property, Composite parent)
	{
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		Button checkbox = new Button(parent, SWT.CHECK);
		checkbox.setLayoutData(gd);
		checkbox.setText(label);
		checkbox.setSelection(importSettings.optBoolean(property));
		checkbox.setData("importProperty", property);
		checkbox.addListener(SWT.Selection, this);
		return checkbox;
	}

	private Button createRadio(String label, String property, Composite parent, int value)
	{
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		Button radio = new Button(parent, SWT.RADIO);
		radio.setLayoutData(gd);
		radio.setText(label);
		radio.setSelection(importSettings.optInt(property) == value);
		radio.setData("importProperty", property);
		radio.setData("importPropertyValue", Integer.toString(value));
		radio.addListener(SWT.Selection, this);
		return radio;
	}

	private Label createNewLine(Composite parent)
	{
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		Label newLine = new Label(parent, SWT.NONE);
		newLine.setLayoutData(gd);
		return newLine;
	}

	private Label createHeader(String text, Composite parent)
	{
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		Label header = new Label(parent, SWT.NONE);
		header.setLayoutData(gd);
		header.setText(text);
		header.setFont(exportSolutionWizard.getBoldFont(header));
		return header;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent)
	{
		ScrolledComposite myScrolledComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		myScrolledComposite.setExpandHorizontal(true);
		myScrolledComposite.setExpandVertical(true);

		GridLayout gridLayout = new GridLayout(3, false);

		Composite composite = new Composite(myScrolledComposite, SWT.NONE);
		composite.setLayout(gridLayout);
		myScrolledComposite.setContent(composite);

		createHeader("General options...", composite);

		createCheckbox("Enter maintenance mode", ENTER_MAINTENANCE_MODE, composite);
		createCheckbox("Activate new release of imported solution and modules", ACTIVATE_NEW_RELEASE, composite);
		createCheckbox("Compact all the solutions/modules first before import", COMPACT_BEFORE_INPUT, composite);
		createCheckbox("Overwrite repository styles with import version", OVERWRITE_STYLE, composite);
		createCheckbox("Overwrite repository permission security settings with import version", OVERWRITE_GROUP_SECURITY, composite);


		GridData gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		createCheckbox("Clean import  -  New solution name", CLEAN_IMPORT, composite).setLayoutData(gd);

		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		gd.widthHint = 250;

		final Text newSolutionNameField = new Text(composite, SWT.BORDER);
		newSolutionNameField.setLayoutData(gd);
		newSolutionNameField.setText(importSettings.optString(NEW_SOLUTION_NAME));
		newSolutionNameField.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				importSettings.put(NEW_SOLUTION_NAME, newSolutionNameField.getText());
				exportSolutionWizard.getModel().setImportSettings(importSettings);
			}
		});

		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;

		Label newSolutionWarningLabel = new Label(composite, SWT.NONE);
		newSolutionWarningLabel.setText("WARNING: Styles will be overwritten!");
		newSolutionWarningLabel.setLayoutData(gd);


		createNewLine(composite);

		createCheckbox("Override existing sequence type definitions (in repository) with the sequence types contained in the import file", OVERRIDE_SEQUENCES,
			composite);
		createCheckbox("Update sequences for all tables on all servers used by the imported solution and modules", UPDATE_SEQUENCES, composite);
		createCheckbox("Override existing default values (in repository) with the default values contained in the import file", OVERRIDE_DEFAUL_VALUES,
			composite);

		gd = new GridData();
		gd.horizontalSpan = 3;
		Label overrideDefaultWarningLabel = new Label(composite, SWT.NONE);
		overrideDefaultWarningLabel.setLayoutData(gd);
		overrideDefaultWarningLabel.setText(
			"WARNING: This may break other solutions using the same tables, or cause tables to use nonexistent dbidentity or dbsequence sequences or other database auto enter types!");

		createNewLine(composite);

		createCheckbox("Allow reserved SQL keywords as table or column names (will fail unless supported by the backend database)", ALLOW_RESERVED_SQL_KEYWORDS,
			composite);

		String servers = importSettings.optString(ALLOWED_DATA_MODEL_CHANGES);
		gd = new GridData();
		gd.horizontalSpan = 1;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;
		final Button checkbox = new Button(composite, SWT.CHECK);
		checkbox.setSelection(true);
		if ("false".equals(servers))
		{
			checkbox.setSelection(false);
		}
		checkbox.setText("Allow data model (database) changes");
		checkbox.addSelectionListener(new SelectionListener()
		{

			@Override
			public void widgetSelected(SelectionEvent e)
			{
				importSettings.put(ALLOWED_DATA_MODEL_CHANGES, Boolean.toString(checkbox.getSelection()));
				exportSolutionWizard.getModel().setImportSettings(importSettings);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{

			}
		});
		checkbox.setToolTipText("Enable/Disable changes for all servers");

		gd = new GridData();
		gd.horizontalSpan = 2;
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.FILL;

		final Text allowedServers = new Text(composite, SWT.BORDER);
		allowedServers.setLayoutData(gd);
		allowedServers.setText(!"true".equals(servers) && !"false".equals(servers) ? servers : "");
		allowedServers.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				checkbox.setSelection(true);
				importSettings.put(ALLOWED_DATA_MODEL_CHANGES, "".equals(allowedServers.getText()) ? Boolean.toString(true) : allowedServers.getText());
				exportSolutionWizard.getModel().setImportSettings(importSettings);
			}
		});
		allowedServers.setToolTipText("Comma separated server names where changes are allowed");

		createCheckbox("Skip database views import", SKIP_VIEWS, composite);
		createCheckbox("Display data model (database) changes", DISPLAY_DATA_MODEL_CHANGES, composite);
		createCheckbox("Import solution meta data", IMPORT_META_DATA, composite);
		createCheckbox("Import solution sample data", IMPORT_SAMPLE_DATA, composite);
		createCheckbox("Import internationalization (i18n) data (inserts and updates)", IMPORT_I18N_DATA, composite);
		createCheckbox("Insert new internationalization (i18n) keys only(inserts only, no updates)", INSERT_NEW_I18N_DATA, composite);

		createNewLine(composite);

		createHeader("User import options...", composite);

		createRadio("Do not import users contained in import", USER_IMPORT, composite, 0);
		createRadio("Create nonexisting users and give existing users the permissions specified in import", USER_IMPORT, composite, 1);
		createRadio("Overwrite existing users completely (USE WITH CARE)", USER_IMPORT, composite, 2);

		createCheckbox("Allow users to be give the Administrators permission", ALLOW_ADMIN_USER, composite);

		createNewLine(composite);

		createHeader("Other options...", composite);

		gd = new GridData();
		gd.horizontalSpan = 3;
		final Button deployButton = new Button(composite, SWT.CHECK);
		deployButton.setLayoutData(gd);
		deployButton.setText("Deploy to Servoy application server");
		deployButton.setSelection(exportSolutionWizard.isDeployToApplicationServer());
		deployButton.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				exportSolutionWizard.setDeployToApplicationServer(deployButton.getSelection());
				getWizard().getContainer().updateButtons();
			}
		});

		gd = new GridData();
		gd.horizontalSpan = 3;
		final Button saveToDiskButton = new Button(composite, SWT.CHECK);
		saveToDiskButton.setLayoutData(gd);
		saveToDiskButton.setText("Save import settings to disk (beside the exported solution file)");
		saveToDiskButton.setSelection(exportSolutionWizard.getModel().isSaveImportSettingsToDisk());
		saveToDiskButton.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				exportSolutionWizard.getModel().setSaveImportSettingsToDisk(saveToDiskButton.getSelection());
				getWizard().getContainer().updateButtons();
			}
		});

		myScrolledComposite.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

		setControl(myScrolledComposite);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.swt.widgets.Listener#handleEvent(org.eclipse.swt.widgets.Event)
	 */
	@Override
	public void handleEvent(Event event)
	{
		if (event.widget instanceof Button)
		{
			String importProperty = (String)event.widget.getData("importProperty");
			if (importProperty != null)
			{
				String importPropertyValue = (String)event.widget.getData("importPropertyValue");
				importSettings.put(importProperty, importPropertyValue != null ? importPropertyValue : Boolean.valueOf(((Button)event.widget).getSelection()));
				exportSolutionWizard.getModel().setImportSettings(importSettings);
			}
		}
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportSolutionWizard.isDeployToApplicationServer()) return exportSolutionWizard.getDeployPage();
		return null;
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.export_solution_import_settings");
	}
}