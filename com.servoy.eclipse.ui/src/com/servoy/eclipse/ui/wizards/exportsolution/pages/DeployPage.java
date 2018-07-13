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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class DeployPage extends WizardPage
{
	private final ExportSolutionWizard exportSolutionWizard;

	public DeployPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page6");
		setTitle("Deploy");
		setDescription("Deploy to Servoy application server");
		this.exportSolutionWizard = exportSolutionWizard;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout(2, false);
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		Label lbl = new Label(composite, SWT.NONE);
		lbl.setText("Deploy URL");
		Text deployURLTxt = new Text(composite, SWT.BORDER);
		deployURLTxt.setText(exportSolutionWizard.getDeployURL());
		deployURLTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		deployURLTxt.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				exportSolutionWizard.setDeployURL(deployURLTxt.getText());
			}
		});

		lbl = new Label(composite, SWT.NONE);
		lbl.setText("Username");
		Text usernameTxt = new Text(composite, SWT.BORDER);
		usernameTxt.setText(exportSolutionWizard.getDeployUsername());
		usernameTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		usernameTxt.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				exportSolutionWizard.setDeployUsername(usernameTxt.getText());
			}
		});

		lbl = new Label(composite, SWT.NONE);
		lbl.setText("Password");
		// On MacOS, SWT 3.5 does not send events to listeners on password fields.
		// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
		int style = SWT.BORDER;
		if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
		Text passwordTxt = new Text(composite, style);
		passwordTxt.setText(exportSolutionWizard.getDeployPassword());
		passwordTxt.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		passwordTxt.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				exportSolutionWizard.setDeployPassword(passwordTxt.getText());
			}
		});
		if (Utils.isAppleMacOS()) passwordTxt.setEchoChar('\u2022');

		setControl(composite);
	}


	@Override
	public void performHelp()
	{
		PlatformUI.getWorkbench().getHelpSystem().displayHelp("com.servoy.eclipse.ui.export_solution_deploy");
	}
}