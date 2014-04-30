/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.exporter.mobile.ui.wizard;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.exporter.mobile.ui.wizard.ExportMobileWizard.IMobileExportPropertiesPage;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * @author lvostinar
 *
 */
public class LicensePage extends WizardPage implements IMobileExportPropertiesPage
{
	public static String COMPANY_KEY = "company";
	public static String LICENSE_KEY = "license";

	private Text companyText;
	private Text licenseText;
	private final WizardPage nextPage;
	private final MobileExporter mobileExporter;

	public LicensePage(String pageName, WizardPage nextPage, MobileExporter mobileExporter)
	{
		super(pageName);
		this.nextPage = nextPage;
		setTitle("Servoy Mobile License");
		this.mobileExporter = mobileExporter;
	}

	public void createControl(Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label companyLabel = new Label(container, SWT.NONE);
		companyLabel.setText("Company name");

		Label licenseLabel = new Label(container, SWT.NONE);
		licenseLabel.setText("License code");

		final Label validationLabel = new Label(container, SWT.NONE);
		validationLabel.setText("No license key entered, mobile solution will run in trial mode.");

		companyText = new Text(container, SWT.BORDER);
		licenseText = new Text(container, SWT.BORDER);

		Button licenseButton = new Button(container, SWT.NONE);
		licenseButton.setText("Check License");

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(companyLabel).add(licenseLabel)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(companyText, GroupLayout.PREFERRED_SIZE, 130, Short.MAX_VALUE).add(licenseText,
					GroupLayout.PREFERRED_SIZE, 130, Short.MAX_VALUE).add(licenseButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).add(
					validationLabel)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(companyText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(companyLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(licenseText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(licenseLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(licenseButton)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(validationLabel)).addContainerGap()));
		container.setLayout(groupLayout);

		licenseButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				checkLicense(container, validationLabel);
			}
		});

		String company = getDialogSettings().get(COMPANY_KEY);
		if (company != null)
		{
			companyText.setText(company);
		}
		String license = getDialogSettings().get(LICENSE_KEY);
		if (license != null)
		{
			licenseText.setText(license);
		}
		if (company != null && !"".equals(company))
		{
			checkLicense(container, validationLabel);
		}
	}

	private void checkLicense(Composite container, Label validationLabel)
	{
		if (ApplicationServerRegistry.get().checkMobileLicense(companyText.getText(), licenseText.getText()))
		{
			validationLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_BLACK));
			validationLabel.setText("License OK");
			mobileExporter.setSkipConnect(true);
		}
		else
		{
			validationLabel.setForeground(container.getDisplay().getSystemColor(SWT.COLOR_RED));
			validationLabel.setText("License invalid");
			mobileExporter.setSkipConnect(false);
		}
		container.layout();
	}

	public boolean saveProperties()
	{
		getDialogSettings().put(COMPANY_KEY, companyText.getText());
		getDialogSettings().put(LICENSE_KEY, licenseText.getText());
		return true;
	}

}
