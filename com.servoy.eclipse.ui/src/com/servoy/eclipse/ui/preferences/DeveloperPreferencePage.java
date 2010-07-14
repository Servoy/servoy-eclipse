/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.debug.DeveloperPreferences;

/**
 * Preferences page for developer settings.
 * 
 * @author rgansevles
 * 
 */
public class DeveloperPreferencePage extends PreferencePage implements IWorkbenchPreferencePage
{

	private Button enhancedSecurityButton;
//	private Button useDummyAuthButton;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		enhancedSecurityButton = new Button(composite, SWT.CHECK);
		enhancedSecurityButton.setText("Run Servoy Application Server with Enhanced Security");

//		useDummyAuthButton = new Button(composite, SWT.CHECK);
//		useDummyAuthButton.setText("Use dummy authentication for debug smart client (when server is in Enhanced Security Mode)");

		enhancedSecurityButton.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
//				useDummyAuthButton.setEnabled(enhancedSecurityButton.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		DeveloperPreferences prefs = new DeveloperPreferences(ServoyModel.getSettings());

		enhancedSecurityButton.setSelection(prefs.getEnhancedSecurity());
//		useDummyAuthButton.setSelection(prefs.getUseDummyAuth());
//		useDummyAuthButton.setEnabled(enhancedSecurityButton.getSelection());
	}

	@Override
	public boolean performOk()
	{
		DeveloperPreferences prefs = new DeveloperPreferences(ServoyModel.getSettings());
		prefs.setEnhancedSecurity(enhancedSecurityButton.getSelection());
//		prefs.setUseDummyAuth(useDummyAuthButton.getSelection());

		return true;
	}

	@Override
	protected void performDefaults()
	{
		enhancedSecurityButton.setSelection(DeveloperPreferences.ENHANCED_SECURITY_DEFAULT);
//		useDummyAuthButton.setSelection(DeveloperPreferences.DUMMY_AUTHENTICATION_DEFAULT);
//		useDummyAuthButton.setEnabled(enhancedSecurityButton.getSelection());

		super.performDefaults();
	}
}
