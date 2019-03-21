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

import org.apache.commons.lang3.StringUtils;
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
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.war.exporter.AbstractWarExportModel.License;
import com.servoy.eclipse.warexporter.export.ExportWarModel;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.Pair;

/**
 * Allows exporting client licenses into the war.
 * @author emera
 */
public class LicensePage extends WizardPage implements IRestoreDefaultPage
{

	final ExportWarModel exportModel;
	private Composite mainContainer;
	private ScrolledComposite sc;
	private LicenseFieldsComposite licenseFieldsComposite;

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
		private final Button licenseButton;
		private final Button deleteButton;

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

			licenseButton = new Button(container, SWT.NONE);
			licenseButton.setText("Save license");

			deleteButton = new Button(container, SWT.NONE);
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
					checkLicense(container, companyText.getText(), licenseText, noOfLicensesText.getText());
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
			final LicenseFieldsComposite currentContainer = this;
			ModifyListener modifyListener = new ModifyListener()
			{
				@Override
				public void modifyText(ModifyEvent e)
				{
					setEnabledButtons(licenseButton, deleteButton);
					if (currentContainer == licenseFieldsComposite)
					{
						if (exportModel.containsLicense(licenseText.getText()))
						{
							setMessage("License " + licenseText.getText() + " already exists.", IMessageProvider.ERROR);
							licenseText.setSelection(0, licenseText.getText().length());
							licenseButton.setEnabled(false);
						}
						else
						{
							setMessage("");
						}
					}
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
			setMessage("");
		}

		private void setEnabledButtons(final Button licenseButton, final Button deleteButton)
		{
			boolean enableButtons = !StringUtils.isEmpty(companyText.getText().trim()) && !StringUtils.isEmpty(licenseText.getText().trim()) &&
				!StringUtils.isEmpty(noOfLicensesText.getText().trim()) && StringUtils.isEmpty(getErrorMessage());
			licenseButton.setEnabled(enableButtons);
			deleteButton.setEnabled(enableButtons);
		}

		public void clear()
		{
			companyText.setText("");
			licenseText.setText("");
			noOfLicensesText.setText("");
			setEnabledButtons(licenseButton, deleteButton);
		}

	}

	private void checkLicense(Composite container, String companyText, Text licenseTxt, String noOfLicensesText)
	{
		setMessage("");

		String licenseText = licenseTxt.getText();
		License l = new License(companyText, licenseText, noOfLicensesText.trim());
		IApplicationServerSingleton server = ApplicationServerRegistry.get();
		if (server.checkClientLicense(companyText, licenseText, noOfLicensesText.trim()))
		{
			boolean isNew = !exportModel.containsLicense(l.getCode());
			exportModel.addLicense(l);

			if (isNew)
			{
				setMessage("License " + licenseText + " was saved.", IMessageProvider.INFORMATION);
				addNewLicenseFields();
			}
		}
		else
		{
			Pair<Boolean, String> code = server.upgradeLicense(companyText, licenseText, noOfLicensesText.trim());
			if (code != null && code.getLeft().booleanValue())
			{
				if (!licenseText.equals(code.getRight()))
				{
					setMessage("License " + l.getCompanyKey() + " " + l.getCode() + " was auto upgraded to " + code.getRight(), IMessageProvider.INFORMATION);
					if (!exportModel.containsLicense(code.getRight()) && !exportModel.containsLicense(l.getCode()))
					{
						addNewLicenseFields();
					}
					exportModel.replaceLicenseCode(l, code.getRight());
					licenseTxt.setText(code.getRight());
				}
			}
			else
			{
				setMessage("License " + licenseText + " invalid." + (code != null ? code.getRight() : ""), IMessageProvider.ERROR);
			}
		}
		container.layout();
	}


	protected void addNewLicenseFields()
	{
		Composite composite = new Composite(mainContainer, SWT.BORDER);
		licenseFieldsComposite = new LicenseFieldsComposite(composite, SWT.NONE, "", "", "");
		composite.moveAbove(mainContainer.getChildren()[0]);
		mainContainer.layout(new Control[] { licenseFieldsComposite });
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		sc.update();
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
		licenseFieldsComposite = new LicenseFieldsComposite(new Composite(mainContainer, SWT.BORDER | SWT.FILL), SWT.NONE, "", "", "");
		ctrls.add(licenseFieldsComposite);
		for (License license : exportModel.getLicenses())
		{
			LicenseFieldsComposite licenseComposite = new LicenseFieldsComposite(new Composite(mainContainer, SWT.BORDER), SWT.NONE, license.getCompanyKey(),
				license.getCode(), license.getNumberOfLicenses());
			ctrls.add(licenseComposite);
		}
		sc.setExpandHorizontal(true);
		sc.setExpandVertical(true);
		sc.setMinSize(mainContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT, true));
		mainContainer.layout();
		sc.layout();
	}

	@Override
	public void restoreDefaults()
	{
		Control[] children = mainContainer.getChildren();
		for (int i = 1; i < children.length - 1; i++)
		{
			children[i].dispose();
		}
		((LicenseFieldsComposite)((Composite)children[0]).getChildren()[0]).clear();
		mainContainer.update();
		exportModel.clearLicenses();
	}

	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.exporter.war.export_war_license");
	}
}
