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

/**
 * Wizard page which handles the configuration of the servoy properties file.
 * Properties set here will be reflected in the generated properties file.
 *   
 * @author acostache
 *
 */
public class ServoyPropertiesConfigurationPage extends WizardPage implements Listener
{
	private final ExportWarModel exportModel;
	private final IWizardPage nextPage;
	private Text usedRMIRegistryPortText;

	public ServoyPropertiesConfigurationPage(String title, ExportWarModel exportModel, IWizardPage nextPage)
	{
		super(title);
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Configuration settings for the generated servoy properties file"); //$NON-NLS-1$
		setDescription("Specify following settings"); //$NON-NLS-1$
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(4, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Port used by RMI Registry "); //$NON-NLS-1$

		usedRMIRegistryPortText = new Text(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		usedRMIRegistryPortText.setLayoutData(gd);
		usedRMIRegistryPortText.setText("1099"); //$NON-NLS-1$
		exportModel.setUsedRMIRegistryPort("1099"); //$NON-NLS-1$
		usedRMIRegistryPortText.addListener(SWT.KeyUp, this);

		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == usedRMIRegistryPortText)
		{
			String rmiPort = usedRMIRegistryPortText.getText();
			exportModel.setUsedRMIRegistryPort(rmiPort);
		}

		canFlipToNextPage();
		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return (usedRMIRegistryPortText.getText() != null && usedRMIRegistryPortText.getText().length() > 0);
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}
}