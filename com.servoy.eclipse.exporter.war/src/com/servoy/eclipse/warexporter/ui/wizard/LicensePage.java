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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
	private Composite mainContainer;
	private ScrolledComposite sc;

	public LicensePage(String pageName, String title, String description, ExportWarModel exportModel)
	{
		super(pageName);
		setTitle(title);
		setDescription(description);
		this.exportModel = exportModel;
	}

	private class LicenseFieldsComposite extends Composite
	{
		private final Text companyText;
		private final Text licenseText;
		private final Text noOfLicensesText;

		public LicenseFieldsComposite(final Composite container, int style, String company, String license, String licensesNo)
		{
			super(container, style);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

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
					checkLicense(container, companyText.getText(), licenseText.getText(), noOfLicensesText.getText());
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
			for (Control c : mainContainer.getChildren())
			{
				if (c.equals(this.getParent()))
				{
					c.dispose();
					break;
				}
			}
			mainContainer.update();
		}

		private void setEnabledButtons(final Button licenseButton, final Button deleteButton)
		{
			boolean enableButtons = !StringUtils.isEmpty(companyText.getText().trim()) && !StringUtils.isEmpty(licenseText.getText().trim()) &&
				!StringUtils.isEmpty(noOfLicensesText.getText().trim());
			licenseButton.setEnabled(enableButtons);
			deleteButton.setEnabled(enableButtons);
		}

	}

	private void checkLicense(Composite container, String companyText, String licenseText, String noOfLicensesText)
	{
		setMessage("");
		try
		{
			int numLicenses = Integer.parseInt(noOfLicensesText.trim());
			if (numLicenses < 1)
			{
				setMessage("The number of licenses must be greater or equal to 1.", IMessageProvider.ERROR);
			}
			else if (ApplicationServerRegistry.get().checkClientLicense(companyText, licenseText, numLicenses))
			{
				setMessage("License " + licenseText + " is correct.", IMessageProvider.INFORMATION);
				License l = new License(companyText, licenseText, numLicenses);
				boolean isNew = !exportModel.getLicenses().contains(l);
				exportModel.addLicense(l);

				if (isNew)
				{
					Composite composite = new Composite(mainContainer, SWT.BORDER);
					LicenseFieldsComposite license = new LicenseFieldsComposite(composite, SWT.NONE, "", "", "");
					composite.moveAbove(mainContainer.getChildren()[0]);
					mainContainer.layout(new Control[] { license });
					sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
					sc.update();
				}
			}
			else
			{
				setMessage("License " + licenseText + " invalid.", IMessageProvider.ERROR);
			}
		}
		catch (Exception e)
		{
			setMessage("Please enter a number in the 'Number of licenses' field. " + noOfLicensesText + " is not a number.", IMessageProvider.ERROR);
		}
		container.layout();
	}


	public void createControl(Composite parent)
	{
		Composite rootComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		rootComposite.setLayout(layout);
		rootComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		sc = new ScrolledComposite(rootComposite, SWT.V_SCROLL | SWT.BORDER);
		sc.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainContainer = new Composite(sc, SWT.NONE);
		sc.setContent(mainContainer);
		setControl(rootComposite);
		mainContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		mainContainer.setLayout(layout);


		List<Control> ctrls = new ArrayList<>();
		LicenseFieldsComposite licenseFieldsComposite = new LicenseFieldsComposite(new Composite(mainContainer, SWT.BORDER | SWT.FILL), SWT.NONE, "", "", "");
		ctrls.add(licenseFieldsComposite);
		for (License license : exportModel.getLicenses())
		{
			LicenseFieldsComposite licenseComposite = new LicenseFieldsComposite(new Composite(mainContainer, SWT.BORDER), SWT.NONE, license.getCompanyKey(),
				license.getCode(), Integer.toString(license.getNumberOfLicenses()));
			ctrls.add(licenseComposite);
		}
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		mainContainer.layout();
		sc.layout();
	}
}
