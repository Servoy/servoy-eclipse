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

package com.servoy.eclipse.mobileexporter.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.mobileexporter.export.PhoneGapConnector;
import com.servoy.eclipse.ui.wizards.FinishPage;

/**
 * @author lvostinar
 *
 */
public class PhoneGapApplicationPage extends WizardPage
{
	private final FinishPage finishPage;
	private final PhoneGapConnector connector;
	private Combo applicationNameCombo;

	public PhoneGapApplicationPage(String name, FinishPage finishPage)
	{
		super(name);
		this.finishPage = finishPage;
		this.connector = new PhoneGapConnector();
		setTitle("PhoneGap Application");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);

		Label lblApplicationName = new Label(container, SWT.NONE);
		lblApplicationName.setText("Application Title");

		applicationNameCombo = new Combo(container, SWT.BORDER);

		Label lblVersion = new Label(container, SWT.NONE);
		lblVersion.setText("Version");

		Text txtVersion = new Text(container, SWT.BORDER);

		Label lblDescription = new Label(container, SWT.NONE);
		lblDescription.setText("Description");

		Text txtDescription = new Text(container, SWT.MULTI | SWT.BORDER);

		Button btnPublic = new Button(container, SWT.CHECK);
		btnPublic.setText("Public Application");

		final GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING, false).add(lblApplicationName).add(lblVersion).add(lblDescription)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(applicationNameCombo, GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE).add(txtVersion,
					GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE).add(txtDescription, GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE).add(btnPublic,
					GroupLayout.PREFERRED_SIZE, 276, Short.MAX_VALUE)).addContainerGap()));

		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(applicationNameCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblApplicationName)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(txtVersion, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblVersion)).add(7).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(txtDescription, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblDescription)).add(10).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(btnPublic))));

		container.setLayout(groupLayout);
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return getErrorMessage() == null;
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (canFlipToNextPage())
		{
			finishPage.setTextMessage("Solution exported to PhoneGap application.");
		}
		return finishPage;
	}

	@Override
	public String getErrorMessage()
	{
		if (applicationNameCombo.getText() == null || "".equals(applicationNameCombo.getText()))
		{
			return "No PhoneGap application name specified.";
		}
		return null;
	}

	public PhoneGapConnector getConnector()
	{
		return connector;
	}
}
