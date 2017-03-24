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
import org.eclipse.swt.widgets.Text;

/**
 * @author gboros
 *
 */
public class ExportConfirmationPage extends WizardPage
{
	private Text message;

	public ExportConfirmationPage()
	{
		super("export confirmation page");
		setTitle("Export solution confirmation");
		setErrorMessage("One or more databases used in the solution are unreacheable");
	}

	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 1;

		message = new Text(container, SWT.WRAP | SWT.MULTI | SWT.BORDER | SWT.V_SCROLL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalSpan = 1;
		message.setLayoutData(gridData);
		message.setEditable(false);
		message.setText("Are you sure you want to proceed with the export?  \nYou can continue to export based on dbi(database information) files only.");

		setControl(container);
		setPageComplete(true);
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return null;
	}
}