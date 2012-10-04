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

package com.servoy.eclipse.ui.wizards;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.quickfix.ChangeResourcesProjectQuickFix.IValidator;

/**
 * @author lvostinar
 *
 */
public class FinishPage extends WizardPage implements IValidator
{
	protected Text message;

	public FinishPage(String pageName)
	{
		super(pageName);
		setTitle(pageName);
	}

	public void setTextMessage(String msg)
	{
		message.setText(msg);
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

		setControl(container);
		setPageComplete(true);
	}

	public String validate()
	{
		return null;
	}

	public boolean canFinish()
	{
		return isCurrentPage();
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return null;
	}
}