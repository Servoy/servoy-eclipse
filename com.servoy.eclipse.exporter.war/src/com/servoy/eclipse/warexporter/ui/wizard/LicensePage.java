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

package com.servoy.eclipse.warexporter.ui.wizard;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Allows exporting client licenses into the war.
 * @author emera
 */
public class LicensePage extends WizardPage
{
	final ExportWarModel exportModel;
	private final WizardPage nextPage;
	private Composite mainContainer;

	public LicensePage(String pageName, String title, ExportWarModel exportModel, WizardPage next)
	{
		super(pageName);
		setTitle(title);
		this.exportModel = exportModel;
		this.nextPage = next;
	}

	private class LicenseFieldsComposite extends Composite
	{
		private final Text companyText;
		private final Text licenseText;
		private final Text noOfLicensesText;
		private final WizardPage page;

		public LicenseFieldsComposite(WizardPage page, final Composite container, int style, String company, String license, String licensesNo)
		{
			super(container, style);
			this.page = page;

			Label companyLabel = new Label(container, SWT.NONE);
			companyLabel.setText("Company name");

			Label numberOfLicensesLabel = new Label(container, SWT.NONE);
			numberOfLicensesLabel.setText("Number of licenses");

			Label licenseLabel = new Label(container, SWT.NONE);
			licenseLabel.setText("License code");


			companyText = new Text(container, SWT.BORDER);
			noOfLicensesText = new Text(container, SWT.BORDER);
			licenseText = new Text(container, SWT.BORDER);

			final Button licenseButton = new Button(container, SWT.NONE);
			licenseButton.setText("Save license");

			final Button deleteButton = new Button(container, SWT.NONE);
			deleteButton.setText("Delete license");

			final GroupLayout groupLayout = new GroupLayout(container);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(companyLabel).add(numberOfLicensesLabel).add(licenseLabel)).addPreferredGap(
					LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.TRAILING).add(companyText, GroupLayout.PREFERRED_SIZE, 130, Short.MAX_VALUE).add(
							noOfLicensesText, GroupLayout.PREFERRED_SIZE, 130, Short.MAX_VALUE).add(licenseText, GroupLayout.PREFERRED_SIZE, 130,
								Short.MAX_VALUE).add(
									groupLayout.createSequentialGroup().add(deleteButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE).add(
										licenseButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE))).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(companyText,
					GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).add(companyLabel)).addPreferredGap(
						LayoutStyle.RELATED).add(
							groupLayout.createParallelGroup(GroupLayout.LEADING).add(noOfLicensesText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE).add(numberOfLicensesLabel)).addPreferredGap(LayoutStyle.RELATED).add(
									groupLayout.createParallelGroup(GroupLayout.LEADING).add(licenseText, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE).add(licenseLabel)).addPreferredGap(LayoutStyle.RELATED).add(
											groupLayout.createParallelGroup(GroupLayout.BASELINE).add(deleteButton).add(licenseButton)).addPreferredGap(
												LayoutStyle.RELATED).addContainerGap()));
			container.setLayout(groupLayout);

			licenseButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					checkLicense(container);
				}
			});

			if (!StringUtils.isEmpty(company))
			{
				companyText.setText(company);
			}
			if (!StringUtils.isEmpty(license))
			{
				licenseText.setText(license);
			}
			if (!StringUtils.isEmpty(licensesNo))
			{
				noOfLicensesText.setText(licensesNo);
			}
			setEnabledButtons(licenseButton, deleteButton);
			ModifyListener modifyListener = new ModifyListener()
			{
				@Override
				public void modifyText(ModifyEvent e)
				{
					setEnabledButtons(licenseButton, deleteButton);
				}
			};
			companyText.addModifyListener(modifyListener);
			licenseText.addModifyListener(modifyListener);
			noOfLicensesText.addModifyListener(modifyListener);

			deleteButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					removeLicense();
				}
			});
		}

		private void removeLicense()
		{
			exportModel.removeLicense(licenseText.getText());
			setVisible(false);
			for (Control c : mainContainer.getChildren())
			{
				if (c.equals(this))
				{
					c.dispose();
				}
			}
			mainContainer.layout(true);
		}

		private void checkLicense(Composite container)
		{
			page.setMessage("");
			try
			{
				int numLicenses = Integer.parseInt(noOfLicensesText.getText().trim());
				if (numLicenses < 1)
				{
					page.setMessage("The number of licenses must be greater or equal to 1.", IMessageProvider.ERROR);
				}
				else if (ApplicationServerRegistry.get().checkClientLicense(companyText.getText(), licenseText.getText(), numLicenses))
				{
					page.setMessage("License " + licenseText.getText() + " is correct.", IMessageProvider.INFORMATION);
					exportModel.addLicense(new License(companyText.getText(), licenseText.getText(), numLicenses));

					//add new
					LicenseFieldsComposite license = new LicenseFieldsComposite(page, new Composite(mainContainer, SWT.BORDER), SWT.NONE, "", "", "");
					mainContainer.layout(new Control[] { license });
				}
				else
				{
					page.setMessage("License " + licenseText.getText() + " invalid.", IMessageProvider.ERROR);
				}
			}
			catch (Exception e)
			{
				page.setMessage("Please enter a number in the 'Number of licenses' field. " + noOfLicensesText.getText() + " is not a number.",
					IMessageProvider.ERROR);
			}
			container.layout();
		}

		private void setEnabledButtons(final Button licenseButton, final Button deleteButton)
		{
			boolean enableButtons = !StringUtils.isEmpty(companyText.getText().trim()) && !StringUtils.isEmpty(licenseText.getText().trim()) &&
				!StringUtils.isEmpty(noOfLicensesText.getText().trim());
			licenseButton.setEnabled(enableButtons);
			deleteButton.setEnabled(enableButtons);
		}

	}

	public void createControl(Composite parent)
	{
		mainContainer = new Composite(parent, SWT.NONE);
		setControl(mainContainer);
		mainContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
		FillLayout layout = new FillLayout();
		layout.type = SWT.VERTICAL;
		layout.marginHeight = 5;
		mainContainer.setLayout(layout);


		List<Control> ctrls = new ArrayList<>();
		LicenseFieldsComposite licenseFieldsComposite = new LicenseFieldsComposite(this, new Composite(mainContainer, SWT.BORDER | SWT.FILL), SWT.NONE, "", "",
			"");
		ctrls.add(licenseFieldsComposite);
		for (License license : exportModel.getLicenses())
		{
			LicenseFieldsComposite licenseComposite = new LicenseFieldsComposite(this, new Composite(mainContainer, SWT.BORDER), SWT.NONE,
				license.getCompanyKey(), license.getCode(), Integer.toString(license.getNumberOfLicenses()));
			ctrls.add(licenseComposite);
		}
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}
}
