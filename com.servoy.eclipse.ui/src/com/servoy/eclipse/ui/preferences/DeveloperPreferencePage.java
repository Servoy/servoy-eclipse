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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
	public DeveloperPreferencePage()
	{
	}

	private Label enhancedSecurityLabel;
	private Button changeButton;

	public void init(IWorkbench workbench)
	{
	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		// GridLayout
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		// GridData
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		enhancedSecurityLabel = new Label(composite, SWT.NONE);
		enhancedSecurityLabel.setText("loading...");

		changeButton = new Button(composite, SWT.NONE);
		changeButton.setText("Change");
		changeButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				new DeveloperPreferences(ServoyModel.getSettings()).setEnhancedSecurity(true);
				initializeFields();
			}
		});

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		if (new DeveloperPreferences(ServoyModel.getSettings()).getEnhancedSecurity())
		{
			enhancedSecurityLabel.setText("Servoy Application Server is running with Enhanced Security");
			enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
			changeButton.setVisible(false);
		}
		else
		{
			enhancedSecurityLabel.setText("Servoy Application Server NOT is running with Enhanced Security, this is strongly discouraged");
			enhancedSecurityLabel.setForeground(Display.getDefault().getSystemColor(SWT.COLOR_RED));
			changeButton.setVisible(true);
		}
	}

	@Override
	public boolean performOk()
	{
		return true;
	}

	@Override
	protected void performDefaults()
	{
		super.performDefaults();
	}
}
