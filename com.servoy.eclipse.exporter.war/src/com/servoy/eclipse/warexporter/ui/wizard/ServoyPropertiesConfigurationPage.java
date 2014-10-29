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
	private Button useRMI;
	private Text startRMIPortText;

	public ServoyPropertiesConfigurationPage(String title, ExportWarModel exportModel, IWizardPage nextPage)
	{
		super(title);
		this.exportModel = exportModel;
		this.nextPage = nextPage;
		setTitle("Configuration settings for the generated servoy properties file");
		setDescription("Specify following settings");
	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(4, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		Label label = new Label(composite, SWT.NONE);
		label.setText("Allow running smart clients");

		useRMI = new Button(composite, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		useRMI.setLayoutData(gd);
		useRMI.setSelection(exportModel.getStartRMI());
		useRMI.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				boolean bUseRMI = useRMI.getSelection();
				startRMIPortText.setEnabled(bUseRMI);
				exportModel.setStartRMI(bUseRMI);
				getWizard().getContainer().updateButtons();
				getWizard().getContainer().updateMessage();

			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});

		label = new Label(composite, SWT.NONE);
		label.setText("Port used by RMI Registry ");

		startRMIPortText = new Text(composite, SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		startRMIPortText.setLayoutData(gd);
		startRMIPortText.setText(exportModel.getStartRMIPort());
		startRMIPortText.addListener(SWT.KeyUp, this);
		startRMIPortText.setEnabled(exportModel.getStartRMI());

		label = new Label(composite, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		label.setLayoutData(gd);
		label.setText("\nNOTE: If running of smart clients is enabled, please take in consideration\nthat on each restart of the application context in the web container,\nRMI related classes cannot be GC and that may lead to out-of-memory errors.");


		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == startRMIPortText)
		{
			String rmiPort = startRMIPortText.getText();
			exportModel.setStartRMIPort(rmiPort);
		}

		getWizard().getContainer().updateButtons();
		getWizard().getContainer().updateMessage();
	}

	@Override
	public boolean canFlipToNextPage()
	{
		return !useRMI.getSelection() || (startRMIPortText.getText() != null && startRMIPortText.getText().length() > 0);
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}
}