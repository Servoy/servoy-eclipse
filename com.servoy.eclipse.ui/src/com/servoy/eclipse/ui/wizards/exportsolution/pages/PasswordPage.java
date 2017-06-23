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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.wizards.ExportSolutionWizard;
import com.servoy.j2db.util.Utils;

/**
 * @author gboros
 *
 */
public class PasswordPage extends WizardPage implements Listener
{
	private final ExportSolutionWizard exportSolutionWizard;
	private Text passwordText;

	public PasswordPage(ExportSolutionWizard exportSolutionWizard)
	{
		super("page4");
		setTitle("Choose a password");
		setDescription("Provide the password that will be used to protect the exported solution");
		this.exportSolutionWizard = exportSolutionWizard;

	}

	public void createControl(Composite parent)
	{
		GridLayout gridLayout = new GridLayout();
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(gridLayout);

		// On MacOS, SWT 3.5 does not send events to listeners on password fields.
		// See: http://www.eclipse.org/forums/index.php?t=msg&goto=508058&
		int style = SWT.BORDER;
		if (!Utils.isAppleMacOS()) style |= SWT.PASSWORD;
		passwordText = new Text(composite, style);
		if (Utils.isAppleMacOS()) passwordText.setEchoChar('\u2022');
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		passwordText.setLayoutData(gd);
		passwordText.addListener(SWT.KeyUp, this);

		setControl(composite);
	}

	public void handleEvent(Event event)
	{
		if (event.widget == passwordText) exportSolutionWizard.getModel().setPassword(passwordText.getText());
		getWizard().getContainer().updateButtons();
	}

	@Override
	public IWizardPage getNextPage()
	{
		if (exportSolutionWizard.getModel().useImportSettings()) return exportSolutionWizard.getImportPage();
		return null;
	}

	public void requestPasswordFieldFocus()
	{
		passwordText.setFocus();
	}
}