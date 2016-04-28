/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.util.Utils;

/**
 * Wizard page which handles the configuration of the default admin user.
 *
 * @author rgansevles
 *
 */
public class DefaultAdminConfigurationPage extends WizardPage implements Listener
{
	private final ExportWarModel exportModel;
	private final IWizardPage nextPage;
	private Text defaultAdminUserText;
	private Text defaultAdminPasswordText;
	private Text defaultAdminPasswordText2;

	public DefaultAdminConfigurationPage(String title, ExportWarModel exportModel, IWizardPage nextPage)
	{
		super(title);
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Configuration settings for the default admin user");
		setDescription("Specify default admin username and password");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(4, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);

		label.setText("Default Admin user ");

		defaultAdminUserText = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		defaultAdminUserText.setLayoutData(gd);
		if (exportModel.getDefaultAdminUser() != null) defaultAdminUserText.setText(exportModel.getDefaultAdminUser());
		defaultAdminUserText.addListener(SWT.KeyUp, this);

		label = new Label(composite, SWT.NONE);
		label.setText("Default Admin password ");

		defaultAdminPasswordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		defaultAdminPasswordText.setLayoutData(gd);
		if (exportModel.getDefaultAdminPassword() != null) defaultAdminPasswordText.setText(exportModel.getDefaultAdminPassword());
		defaultAdminPasswordText.addListener(SWT.KeyUp, this);

		label = new Label(composite, SWT.NONE);
		label.setText("Repeat Admin password ");

		defaultAdminPasswordText2 = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		defaultAdminPasswordText2.setLayoutData(gd);
		if (exportModel.getDefaultAdminPassword() != null) defaultAdminPasswordText2.setText(exportModel.getDefaultAdminPassword());
		defaultAdminPasswordText2.addListener(SWT.KeyUp, this);

		label = new Label(composite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		label.setText("\nThe default administrator user will give access to the servoy-admin page, as long as no admin user is created in the server.");

		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == defaultAdminUserText)
		{
			exportModel.setDefaultAdminUser(defaultAdminUserText.getText());
		}
		else if (event.widget == defaultAdminPasswordText)
		{
			exportModel.setDefaultAdminPassword(defaultAdminPasswordText.getText());
		}

		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		setErrorMessage(null);
		if (Utils.stringIsEmpty(defaultAdminUserText.getText()) || Utils.stringIsEmpty(defaultAdminPasswordText.getText()) ||
			Utils.stringIsEmpty(defaultAdminPasswordText2.getText()))
		{
			return false;
		}

		if (!defaultAdminPasswordText.getText().equals(defaultAdminPasswordText2.getText()))
		{
			setErrorMessage("Passwords are not the same");
			return false;
		}

		return true;
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}
}